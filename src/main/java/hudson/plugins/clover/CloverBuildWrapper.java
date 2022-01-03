package hudson.plugins.clover;

import com.atlassian.clover.api.ci.CIOptions;
import jenkins.model.Jenkins;
import org.openclover.ci.AntIntegrationListener;
import org.openclover.util.ClassPathUtil;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    public final String clover;
    public final boolean putValuesInQuotes;

    @DataBoundConstructor
    public CloverBuildWrapper(boolean historical, boolean json, String clover, boolean putValuesInQuotes) {
        this.historical = historical;
        this.json = json;
        this.clover = clover;
        this.putValuesInQuotes = putValuesInQuotes;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) {
        addCloverPublisher(build, listener);
        return new Environment() {
        };
    }

    /**
     * Add CloverPublisher to the project. Used in case of automatic Clover integration. Do not add if there is
     * another CloverPublisher defined already (e.g. was added manually by user).
     *
     * @param build    build
     * @param listener listener
     */
    private void addCloverPublisher(AbstractBuild build, BuildListener listener) {
        final String DEFAULT_REPORT_DIR = "clover";
        final DescribableList<Publisher, Descriptor<Publisher>> publishers = build.getProject().getPublishersList();
        boolean isAlreadyDefined = false;

        // search for existing CloverPublisher
        for (Publisher publisher : publishers) {
            if (publisher instanceof CloverPublisher) {
                isAlreadyDefined = true;
                break;
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
            return Collections.singletonList(new CloverProjectAction(job));
        }
        return super.getProjectActions(job);
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
                       justification = "")
    private CloverInstallation getInstallationForBuild(@NonNull AbstractBuild build, @NonNull Launcher launcher)
        throws IOException, InterruptedException {
        CloverInstallation installation = CloverInstallation.forName(clover);
        return installation == null ? null : installation.forNode(build.getBuiltOn(), launcher.getListener());
    }

    @Override
    public Launcher decorateLauncher(@NonNull AbstractBuild build, @NonNull Launcher launcher, @NonNull BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {

        final CIOptions.Builder options = new CIOptions.Builder()
                .json(this.json)
                .historical(this.historical)
                .fullClean(true)
                .putValuesInQuotes(this.putValuesInQuotes);

        return new CloverDecoratingLauncher(this, getInstallationForBuild(build, launcher), launcher, options);
    }

    /**
     * Descriptor for {@link CloverPublisher}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * See <tt>views/hudson/plugins/clover/CloverPublisher/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

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
        public boolean configure(StaplerRequest req, JSONObject json) {
            req.bindParameters(this, "clover.");
            save();
            return true;
        }

        public boolean isApplicable(AbstractProject item) {
            // OpenClover automatic integration should be enabled only for Ant project,
            // which is usually configured as a Freestyle Project
            return item instanceof FreeStyleProject;
        }
    }

    public static class CloverDecoratingLauncher extends Launcher {
        private final Launcher outer;
        private final CloverBuildWrapper wrapper;
        private final CloverInstallation clover;

        public CloverDecoratingLauncher(CloverBuildWrapper cloverBuildWrapper, CloverInstallation clover, Launcher outer, CIOptions.Builder options) {
            super(outer);
            this.wrapper = cloverBuildWrapper;
            this.clover = clover;
            this.outer = outer;
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

        private enum ParseState {
            PRE, USER, POST
        }

        public void decorateArgs(ProcStarter starter) throws IOException {

            final List<String> userArgs = new LinkedList<>();
            List<String> preSystemArgs = new LinkedList<>();
            List<String> postSystemArgs = new LinkedList<>();

            List<String> cmds = new ArrayList<>(starter.cmds());

            // on windows - the cmds are wrapped of the form:
            // "cmd.exe", "/C", "\"ant.bat clean test.run    &&  exit %%ERRORLEVEL%%\""
            // or:
            // "cmd.exe", "/C", "\"ant.bat" "clean" "test.run" "&&" "exit" "%%ERRORLEVEL%%\""

            // remove "cmd.exe /C" prefix
            if (isCmdExe(cmds)) {
                final int numPreSystemCmds = 2; // "cmd.exe" "/C"
                preSystemArgs.addAll(cmds.subList(0, numPreSystemCmds));
                cmds = cmds.subList(numPreSystemCmds, cmds.size());
            }

            if (cmds.size() == 1 && cmds.get(0).startsWith("\"")) {
                // probably we have "ant.bat all targets as one string"
                // so split one command argument into separate ones
                cmds = Arrays.asList(cmds.get(0).split(" "));
            }

            // wrap only ant commands, skip all other batch tasks called
            if (isAntBat(cmds)) {
                // windows 'ant.bat'
                // remove leading and trailing " as they may interfere with Clover's ones
                cmds = Lists.transform(cmds, trimDoubleQuotes);
                // and split the list into three to find arguments for ant
                splitArgumentsIntoPreUserPost(cmds, preSystemArgs, userArgs, postSystemArgs, true);
            } else if (isAnt(cmds)) {
                // linux 'ant', we don't look for '&&'
                splitArgumentsIntoPreUserPost(cmds, preSystemArgs, userArgs, postSystemArgs, false);
            } else {
                listener.getLogger().printf("Clover did not found Ant command in '%s' - not integrating.%n",
                        StringUtils.join(cmds, " "));
            }

            // we add OpenClover only if any targets are specified (if there are no targets defined, then Ant calls the
            // default one; adding OpenClover targets in such case would cause that the default target will not be called)
            if (!userArgs.isEmpty()) {
                // We can't use clover AntDecorator as this one isn't serializable on jenkins remoting
                // and expect to be loaded from clover.jar, not remoting classloader

                addReportSkipping(userArgs);
                addAntIntegrationListener(userArgs);
                if (clover != null) {
                    addLibCloverFromHome(userArgs);
                } else {
                    // Fall back to the embedded clover.jar
                    addLibCloverFromBundledJar(userArgs, starter, listener);
                }

                // re-assemble all commands
                List<String> allCommands = new ArrayList<>();
                allCommands.addAll(preSystemArgs);
                allCommands.addAll(userArgs);
                allCommands.addAll(postSystemArgs);
                starter.cmds(allCommands);

                // masks.length must equal cmds.length
                boolean[] masks = new boolean[starter.cmds().size()];
                boolean[] starterMasks = starter.masks();
                if (starterMasks != null) {
                    System.arraycopy(starterMasks, 0, masks, 0, starterMasks.length);
                }
                starter.masks(masks);
            }
        }

        static final Function<String, String> trimDoubleQuotes = new Function<String, String>() {
            @Override
            public String apply(@Nullable String s) {
                return StringUtils.removeStart(StringUtils.removeEnd(s, "\""), "\"");
            }
        };

        static boolean isCmdExe(List<String> args) {
            return args.size() >= 2 && args.get(0).endsWith("cmd.exe") && args.get(1).equals("/C");
        }

        static boolean isAntBat(List<String> cmds) {
            return cmds.size() > 1 && (cmds.get(0).endsWith("ant.bat"));
        }

        static boolean isAnt(List<String> cmds) {
            return cmds.size() > 1 && (cmds.get(0).endsWith("ant"));
        }

        static void splitArgumentsIntoPreUserPost(List<String> cmds,
                                                   List<String> preSystemArgs,
                                                   List<String> userArgs,
                                                   List<String> postSystemArgs,
                                                   boolean lookForPostSystemArgs) {
            ParseState state = ParseState.PRE;
            for (String arg : cmds) {
                switch (state) {
                    case PRE:
                        // copy only first argument which we assume is "ant.bat" or "ant"
                        preSystemArgs.add(arg);
                        state = ParseState.USER;
                        break;
                    case USER:
                        // on Windows we may have "&& exit %%ERRORLEVEL%%" which are not Ant options
                        if (lookForPostSystemArgs && "&&".equals(arg)) {
                            state = ParseState.POST;
                            postSystemArgs.add(arg);
                        } else {
                            userArgs.add(arg);
                        }
                        break;
                    case POST:
                        postSystemArgs.add(arg);
                        break;
                }
            }
        }

        private void addReportSkipping(List<String> userArgs) {
            if (!wrapper.json) {
                userArgs.add("-Dclover.skip.json=true");
            }
            if (!wrapper.historical) {
                userArgs.add("-Dclover.skip.report=true");
            } else {
                userArgs.add("-Dclover.skip.current=true");
            }
        }

        private void addAntIntegrationListener(List<String> userArgs) {
            userArgs.add("-listener");
            userArgs.add(AntIntegrationListener.class.getName());
        }

        private void addLibCloverFromHome(List<String> userArgs) {
            userArgs.add("-lib");
            userArgs.add("\"" + clover.getHome() + "\"");
        }

        private void addLibCloverFromBundledJar(List<String> userArgs, @NonNull ProcStarter starter, TaskListener listener) throws IOException {
            FilePath starterPwd = starter.pwd();
            if (starterPwd == null) {
                listener.getLogger().print("Could not get clover jar path from " + starter);
                return;
            }
            FilePath path = new FilePath(new FilePath(starterPwd, ".clover"), "clover.jar");
            try {
                String cloverJarLocation = ClassPathUtil.getCloverJarPath();
                if (cloverJarLocation == null) {
                    listener.getLogger().print("Could not get clover jar path at: " + path + ".  Please supply '-lib /path/to/clover.jar'.");
                    return;
                }
                path.copyFrom(new FilePath(new File(cloverJarLocation)));
                userArgs.add("-lib");
                userArgs.add("\"" + path.getRemote() + "\"");
            } catch (InterruptedException e) {
                listener.getLogger().print("Could not create clover library file at: " + path + ".  Please supply '-lib /path/to/clover.jar'.");
                listener.getLogger().print(e.getMessage());
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
         * Copied from {@link org.openclover.ci.AntIntegrator#addQuotesIfNecessary(String)}
         * Don't add quotes on Windows, because it causes problems when passing such -Dname=value args to JVM via exec.
         * Don't add quotes for new versions of Ant either (by default the isPutValuesInQuotes is false)
         * as since Ant 1.9.7 problem of passing args to JVM has been fixed.
         * See CLOV-1956, BAM-10740 and BDEV-11740 for more details.
         */
        private String addQuotesIfNecessary(String input) {
            return !wrapper.putValuesInQuotes || isWindows() ? input : '"' + input + '"';
        }

        /**
         * Copied from {@link org.openclover.ci.AntIntegrator#isWindows()}
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
