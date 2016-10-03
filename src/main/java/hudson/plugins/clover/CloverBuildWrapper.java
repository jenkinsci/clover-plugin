package hudson.plugins.clover;

import com.atlassian.clover.api.ci.CIOptions;
import com.atlassian.clover.ci.AntIntegrationListener;
import com.atlassian.clover.util.ClassPathUtil;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.Run;
import hudson.remoting.Channel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A BuildWrapper that decorates the command line just before a build starts with targets and properties that will automatically
 * integrate Clover into the Ant build.
 */
public class CloverBuildWrapper extends BuildWrapper {


    public boolean historical = true;
    public boolean json = true;
    public String licenseCert;
    public String clover;
    public boolean putValuesInQuotes;

    @DataBoundConstructor
    public CloverBuildWrapper(boolean historical, boolean json, String licenseCert, String clover, boolean putValuesInQuotes) {
        this.historical = historical;
        this.json = json;
        this.licenseCert = licenseCert;
        this.clover = clover;
        this.putValuesInQuotes = putValuesInQuotes;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        addCloverPublisher(build, listener);
        return new Environment() {};
    }

    /**
     * Add CloverPublisher to the project. Used in case of automatic Clover integration. Do not add if there is
     * another CloverPublisher defined already (i.e. was added manually by user) having the default value of the
     * report directory.
     * @param build
     * @param listener
     * @throws IOException
     */
    private void addCloverPublisher(AbstractBuild build, BuildListener listener) throws IOException {
        final String DEFAULT_REPORT_DIR = "clover";
        final DescribableList<Publisher,Descriptor<Publisher>> publishers = build.getProject().getPublishersList();
        boolean isAlreadyDefined = false;

        // search for existing CloverPublisher with the same report directory
        for (Publisher publisher : publishers) {
            if (publisher instanceof CloverPublisher) {
                if ( DEFAULT_REPORT_DIR.equals(((CloverPublisher)publisher).getCloverReportDir()) ) {
                    isAlreadyDefined = true;
                    break;
                }
            }
        }

        if (!isAlreadyDefined) {
            listener.getLogger().println("Adding Clover Publisher with reportDir: " + DEFAULT_REPORT_DIR);
            build.getProject().getPublishersList().add(new CloverPublisher(DEFAULT_REPORT_DIR, null));
        }
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject job) {
        // ensure only one project action exists on the project
        if (job.getAction(CloverProjectAction.class) == null) {
            return Collections.singletonList(new CloverProjectAction((Project) job));
        }
        return super.getProjectActions(job);
    }

    @Override
    public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {

        final DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);

        final String license = Util.nullify(licenseCert) == null ? descriptor.licenseCert : licenseCert;
        final CIOptions.Builder options = new CIOptions.Builder()
                .json(this.json)
                .historical(this.historical)
                .fullClean(true)
                .putValuesInQuotes(this.putValuesInQuotes);

        final Launcher outer = launcher;

        CloverInstallation installation = CloverInstallation.forName(clover);
        if (installation != null) {
            installation = installation.forNode(build.getBuiltOn(), outer.getListener());
        }

        return new CloverDecoratingLauncher(this, installation, outer, options, license);
    }

    /**
     * Descriptor for {@link CloverPublisher}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * <p/>
     * <p/>
     * See <tt>views/hudson/plugins/clover/CloverPublisher/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public String licenseCert;

        public DescriptorImpl() {
            super(CloverBuildWrapper.class);
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.CloverBuildWrapper_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/clover/help-cloverConfig.html";
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindParameters(this, "clover.");
            save();
            return true;
        }

        public boolean isApplicable(AbstractProject item) {
            // TODO: is there a better way to detect Ant builds?
            // should only be enabled for Ant projects.
            return (item instanceof FreeStyleProject);

        }
    }

    public static class CloverDecoratingLauncher extends Launcher {
        private final Launcher outer;
        private final CIOptions.Builder options;
        private final String license;
        private final CloverBuildWrapper wrapper;
        private final CloverInstallation clover;

        public CloverDecoratingLauncher(CloverBuildWrapper cloverBuildWrapper, CloverInstallation clover, Launcher outer, CIOptions.Builder options, String license) {
            super(outer);
            this.wrapper = cloverBuildWrapper;
            this.clover = clover;
            this.outer = outer;
            this.options = options;
            this.license = license;
        }

        @Override
        public boolean isUnix() {
            return outer.isUnix();
        }



        @Override
        public Proc launch(ProcStarter starter) throws IOException {

            decorateArgs(starter);
            return outer.launch(starter);
        }

        public void decorateArgs(ProcStarter starter) throws IOException {

            final List<String> userArgs = new LinkedList<String>();
            List<String> preSystemArgs = new LinkedList<String>();
            List<String> postSystemArgs = new LinkedList<String>();

            final List<String>  cmds = new ArrayList<String>();
            cmds.addAll(starter.cmds());

            // on windows - the cmds are wrapped of the form:
            // "cmd.exe", "/C", "\"ant.bat clean test.run    &&  exit %%ERRORLEVEL%%\""
            // this hacky code is used to parse out just the user specified args. ie clean test.run

            final int numPreSystemCmds = 2; // hack hack hack - there are 2 commands prepended on windows...
            final String sysArgSplitter = "&&";

            if (!cmds.isEmpty() && cmds.size() >= numPreSystemCmds && !cmds.get(0).endsWith("ant"))
            {
                preSystemArgs.addAll(cmds.subList(0, numPreSystemCmds));

                // get the index of the "ant.bat 
                String argString = cmds.get(numPreSystemCmds);
                // trim leading and trailing " if they exist...
                argString = argString.replaceAll("\"", "");

                String[] tokens = argString.split(" ");
                preSystemArgs.add(tokens[0]);

                for (int i = 1; i < tokens.length; i++)
                {   // chop the ant.bat
                    String arg = tokens[i];
                    if (sysArgSplitter.equals(arg))
                    {
                        // anything after the &&, break.
                        postSystemArgs.addAll(Arrays.asList(tokens).subList(i, tokens.length));
                        break;
                    }
                    userArgs.add(arg);
                }
            }
            else
            {
                if (cmds.size() > 0)
                {
                    preSystemArgs.add(cmds.get(0));
                }
                if (cmds.size() > 1)
                {
                    userArgs.addAll(cmds.subList(1, cmds.size()));
                }
            }

            if (!userArgs.isEmpty())
            {
                // We can't use clover AntDecorator as this one isn't serializable on jenkins remoting
                // and expect to be loaded from clover.jar, not remoting classloader

                // TODO: full clean needs to be an option. see http://jira.atlassian.com/browse/CLOV-736
                userArgs.add(0, "clover.fullclean");

                // As decompiled from com.atlassian.clover.ci.AntIntegrator;
                if(!wrapper.json) {
                    userArgs.add("-Dclover.skip.json=true");
                }

                if(!wrapper.historical) {
                    userArgs.add("-Dclover.skip.report=true");
                } else {
                    userArgs.add("-Dclover.skip.current=true");
                }

                userArgs.add("-listener");
                userArgs.add(AntIntegrationListener.class.getName());


                if (clover != null) {
                    userArgs.add("-lib");
                    userArgs.add("\"" + clover.getHome() + "\"");
                } else {
                    // Fall back to the embedded clover.jar
                    FilePath path = new FilePath( new FilePath(starter.pwd(), ".clover"), "clover.jar");
                    try {
                        String cloverJarLocation = ClassPathUtil.getCloverJarPath();
                        path.copyFrom(new FilePath(new File(cloverJarLocation)));
                        userArgs.add("-lib");
                        userArgs.add("\"" + path.getRemote() + "\"");
                    } catch (InterruptedException e) {
                        listener.getLogger().print("Could not create clover library file at: " + path + ".  Please supply '-lib /path/to/clover.jar'.");
                        listener.getLogger().print(e.getMessage());
                    }
                }


                FilePath licenseFile = new FilePath( new FilePath(starter.pwd(), ".clover"), "clover.license");
                try {
                    if (license == null) {
                        listener.getLogger().println("No Clover license configured. Please download a free 30 day license from http://my.atlassian.com.");
                        return;
                    }
                    licenseFile.write(license, "UTF-8");
                    userArgs.add("-Dclover.license.path=" + addQuotesIfNecessary(licenseFile.getRemote()));
                } catch (InterruptedException e) {
                    listener.getLogger().print("Could not create license file at: " + licenseFile + ". Setting as a system property.");
                    listener.getLogger().print(e.getMessage());
                    userArgs.add("-Dclover.license.cert=" + addQuotesIfNecessary(license));
                }

                // re-assemble all commands
                List<String> allCommands = new ArrayList<String>();
                allCommands.addAll(preSystemArgs);
                allCommands.addAll(userArgs);
                allCommands.addAll(postSystemArgs);
                starter.cmds(allCommands);

                // masks.length must equal cmds.length
                boolean[] masks = new boolean[starter.cmds().size()];
                for (int i = 0; i < starter.masks().length; i++) {
                    masks[i] = starter.masks()[i];
                }
                starter.masks(masks);
            }
        }

        @Override
            public Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String, String> envVars) throws IOException, InterruptedException {
            return outer.launchChannel(cmd, out, workDir, envVars);
        }

        @Override
            public void kill(Map<String, String> modelEnvVars) throws IOException, InterruptedException {
            outer.kill(modelEnvVars);
        }

        /**
         * Copied from {@link com.atlassian.clover.ci.AntIntegrator#addQuotesIfNecessary(String)}
         *
         * Don't add quotes on Windows, because it causes problems when passing such -Dname=value args to JVM via exec.
         * Don't add quotes for new versions of Ant either (by default the isPutValuesInQuotes is false)
         * as since Ant 1.9.7 problem of passing args to JVM has been fixed.
         *
         * See CLOV-1956, BAM-10740 and BDEV-11740 for more details.
         */
        private String addQuotesIfNecessary(String input) {
            return !wrapper.putValuesInQuotes || isWindows() ? input : '"' + input + '"';
        }

        /**
         * Copied from {@link com.atlassian.clover.ci.AntIntegrator#isWindows()}
         */
        private static boolean isWindows() {
            final String osName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    try {
                        return System.getProperty("os.name");
                    } catch (SecurityException ex) {
                        return null;
                    }
                }
            });
            return osName != null && osName.toLowerCase().indexOf("windows") == 0;
        }
    }

}
