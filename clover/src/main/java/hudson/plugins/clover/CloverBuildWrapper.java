package hudson.plugins.clover;

import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.Launcher;
import hudson.Proc;
import hudson.FilePath;
import hudson.Extension;
import hudson.Util;
import hudson.util.DescribableList;
import hudson.remoting.Channel;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.Descriptor;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.FreeStyleProject;
import hudson.model.Action;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.ArrayList;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import net.sf.json.JSONObject;
import com.atlassian.clover.api.ci.CIOptions;
import com.atlassian.clover.api.ci.Integrator;

/**
 * A BuildWrapper that decorates the command line just before a build starts with targets and properties that will automatically
 * integrate Clover into the Ant build.
 */
public class CloverBuildWrapper extends BuildWrapper {


    public boolean historical = true;
    public boolean json = true;
    public String licenseCert;

    @DataBoundConstructor
    public CloverBuildWrapper(boolean historical, boolean json, String licenseCert) {
        this.historical = historical;
        this.json = json;
        this.licenseCert = licenseCert;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        addCloverPublisher(build, listener);
        return new Environment() {};
    }

    private void addCloverPublisher(AbstractBuild build, BuildListener listener) throws IOException {
        DescribableList publishers = build.getProject().getPublishersList();
        if (!publishers.contains(CloverPublisher.DESCRIPTOR)) {
            final String reportDir = "clover";
            listener.getLogger().println("Adding Clover Publisher with reportDir: " + reportDir);
            build.getProject().getPublishersList().add(new CloverPublisher(reportDir, null));
        }
    }

    @Override
    public Action getProjectAction(AbstractProject job) {
        // ensure only one project action exists on the project
        if (job.getAction(CloverProjectAction.class) == null) {
            return new CloverProjectAction((Project) job);
        }
        return super.getProjectAction(job);
    }

    @Override
    public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {


        final DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);

        final String license = Util.nullify(licenseCert) == null ? descriptor.licenseCert : licenseCert;
        final CIOptions.Builder options = new CIOptions.Builder().
                json(this.json).
                historical(this.historical).
                fullClean(true);

        final Launcher outer = launcher;
        return new CloverDecoratingLauncher(outer, options, license);
    }

    public static final Descriptor<BuildWrapper> DESCRIPTOR = new DescriptorImpl();


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
            return "<img src='"+CloverProjectAction.ICON+"' height='24'/> Automatically record and report Code Coverage using <a href='http://atlassian.com/clover'>Clover.</a>. Currently for Ant builds only.";
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

        public CloverDecoratingLauncher(Launcher outer, CIOptions.Builder options, String license) {
            super(outer);
            this.outer = outer;
            this.options = options;
            this.license = license;
        }

        @Override
        public Proc launch(ProcStarter starter) throws IOException {

            decorateArgs(starter);
            return outer.launch(starter);
        }

        public void decorateArgs(ProcStarter starter) throws IOException {

            List<String> userArgs = new LinkedList<String>();
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

                // TODO: full clean needs to be an option. see http://jira.atlassian.com/browse/CLOV-736
                options.fullClean(true);

                setupLicense(starter);

                Integrator integrator = Integrator.Factory.newAntIntegrator(options.build());
                
                integrator.decorateArguments(userArgs);
                starter.cmds(new ArrayList<String>());

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

        private void setupLicense(ProcStarter starter) throws IOException {

            if (license == null) {
                listener.getLogger().println("No Clover license configured. Please download a free 30 day license from http://my.atlassian.com.");
                return;
            }

            // create a tmp license file.
            FilePath licenseFile = new FilePath(starter.pwd(), ".clover/clover.license");
            try {
                licenseFile.write(license, "UTF-8");
                options.license(new File(licenseFile.toURI()));
            } catch (InterruptedException e) {
                listener.getLogger().print("Could not create license file at: " + licenseFile + ". Setting as a system property.");
                listener.getLogger().print(e.getMessage());
                options.licenseCert(license);
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

    }
}
