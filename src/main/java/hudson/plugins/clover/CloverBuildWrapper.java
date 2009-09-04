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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

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
        addCloverProjectAction(build, listener);
        return new Environment() {};
    }

    private void addCloverPublisher(AbstractBuild build, BuildListener listener) throws IOException {
        DescribableList publishers = build.getProject().getPublishersList();
        if (!publishers.contains(CloverPublisher.DESCRIPTOR)) {
            final String reportDir = "clover";
            listener.getLogger().println("Adding Clover Publisher with reportDir: " + reportDir);
            build.getProject().getPublishersList().add(new CloverPublisher(reportDir));
        }
    }

    private void addCloverProjectAction(AbstractBuild build, BuildListener listener) {
        try {
            if (build.getProject().getAction(CloverProjectAction.class) == null) {
                if (build.getProject() instanceof Project) {
                    // only add the project action, if this is a Project for now
                    build.getProject().addAction(new CloverProjectAction((Project) build.getProject()));
                }
            }
        } catch (UnsupportedOperationException e) {
            // TODO: determine why the action list sometimes becomes unmodifiable.. (hot deployment maybe?)
            listener.getLogger().println("Clover Project Action not added since action list is Unmodifiable: " + e.getMessage());
        }
    }

    @Override
    public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {


        final DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);

        final String license = Util.nullify(licenseCert) == null ? descriptor.licenseCert : licenseCert;
        final CIOptions.Builder options = new CIOptions.Builder().
                json(this.json).
                historical(this.historical).
                licenseCert(license).
                fullClean(true);

        final Launcher outer = launcher;
        return new Launcher(outer) {
            @Override
            public Proc launch(ProcStarter starter) throws IOException {

                if (!starter.cmds().isEmpty() && !starter.cmds().get(0).endsWith("ant")) {

                } else {

                    Integrator integrator = Integrator.Factory.newAntIntegrator(options.build());
                    // decorateArguments takes a list of just the targets. does not include '/usr/bin/ant'
                    integrator.decorateArguments(starter.cmds().subList(1, starter.cmds().size() - 1));

                    // masks.length must equal cmds.length
                    boolean[] masks = new boolean[starter.cmds().size()];
                    for (int i = 0; i < starter.masks().length; i++) {
                        masks[i] = starter.masks()[i];
                    }
                    starter.masks(masks);
                }
                return outer.launch(starter);
            }

            @Override
            public Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String, String> envVars) throws IOException, InterruptedException {
                return outer.launchChannel(cmd, out, workDir, envVars);
            }

            @Override
            public void kill(Map<String, String> modelEnvVars) throws IOException, InterruptedException {
                outer.kill(modelEnvVars);
            }

        };
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
}
