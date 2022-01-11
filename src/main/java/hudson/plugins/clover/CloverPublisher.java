package hudson.plugins.clover;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.clover.results.ProjectCoverage;
import hudson.plugins.clover.slave.GetPathFileCallable;
import hudson.plugins.clover.targets.CoverageMetric;
import hudson.plugins.clover.targets.CoverageTarget;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Clover {@link Publisher}.
 */
public class CloverPublisher extends Recorder implements SimpleBuildStep {

    private final String cloverReportDir;
    private final String cloverReportFileName;

    private CoverageTarget healthyTarget;
    private CoverageTarget unhealthyTarget;
    private CoverageTarget failingTarget;

    public CloverPublisher(String cloverReportDir, String cloverReportFileName) {
        this.cloverReportDir = cloverReportDir;
        this.cloverReportFileName = cloverReportFileName;
        this.healthyTarget = new CoverageTarget();
        this.unhealthyTarget = new CoverageTarget();
        this.failingTarget = new CoverageTarget();
    }

    /**
     * @param cloverReportDir report directory
     * @param cloverReportFileName file name
     * @param healthyTarget target values for healthy build
     * @param unhealthyTarget target values for unhealthy build
     * @param failingTarget target values for failing build
     */
    @DataBoundConstructor
    public CloverPublisher(String cloverReportDir, String cloverReportFileName, CoverageTarget healthyTarget, CoverageTarget unhealthyTarget, CoverageTarget failingTarget) {
        this.cloverReportDir = cloverReportDir;
        this.cloverReportFileName = cloverReportFileName;
        this.healthyTarget = healthyTarget;
        this.unhealthyTarget = unhealthyTarget;
        this.failingTarget = failingTarget;
    }

    public String getCloverReportDir() {
        return cloverReportDir;
    }

    public String getCloverReportFileName() {
        return cloverReportFileName == null
                || cloverReportFileName.trim().length() == 0 ? "clover.xml" : cloverReportFileName;
    }

    /**
     * Getter for property 'healthyTarget'.
     *
     * @return Value for property 'healthyTarget'.
     */
    public CoverageTarget getHealthyTarget() {
        return healthyTarget;
    }

    /**
     * Setter for property 'healthyTarget'.
     *
     * @param healthyTarget Value to set for property 'healthyTarget'.
     */
    public void setHealthyTarget(CoverageTarget healthyTarget) {
        this.healthyTarget = healthyTarget;
    }

    /**
     * Getter for property 'unhealthyTarget'.
     *
     * @return Value for property 'unhealthyTarget'.
     */
    public CoverageTarget getUnhealthyTarget() {
        return unhealthyTarget;
    }

    /**
     * Setter for property 'unhealthyTarget'.
     *
     * @param unhealthyTarget Value to set for property 'unhealthyTarget'.
     */
    public void setUnhealthyTarget(CoverageTarget unhealthyTarget) {
        this.unhealthyTarget = unhealthyTarget;
    }

    /**
     * Getter for property 'failingTarget'.
     *
     * @return Value for property 'failingTarget'.
     */
    public CoverageTarget getFailingTarget() {
        return failingTarget;
    }

    /**
     * Setter for property 'failingTarget'.
     *
     * @param failingTarget Value to set for property 'failingTarget'.
     */
    public void setFailingTarget(CoverageTarget failingTarget) {
        this.failingTarget = failingTarget;
    }

    /**
     * Gets the directory where the Clover Report is stored for the given build.
     */
    static File getCloverXmlReport(Run<?, ?> build) {
        return new File(build.getRootDir(), "clover.xml");
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher,
                        @NonNull TaskListener listener) throws InterruptedException, IOException {
        performImpl(run, workspace, listener);
    }

    private void performImpl(Run<?, ?> run, FilePath workspace, TaskListener listener)
            throws IOException, InterruptedException {
        final EnvVars env = run.getEnvironment(listener);
        final File buildRootDir = run.getRootDir(); // should this top level?
        final FilePath buildTarget = new FilePath(buildRootDir);
        String reportDir = env.expand(cloverReportDir);
        FilePath coverageReportDir = workspace.child(reportDir);
        try {
            listener.getLogger().println("Publishing Clover coverage report...");

            // search one deep for the report dir, if it doesn't exist.
            if (!coverageReportDir.exists()) {
                coverageReportDir = findOneDirDeep(workspace, reportDir);
            }

            // if the run has failed, then there's not much point in reporting an error
            final Result result = run.getResult();
            final boolean buildFailure = result != null && result.isWorseOrEqualTo(Result.FAILURE);
            final boolean missingReport = !coverageReportDir.exists();

            if (buildFailure || missingReport) {
                listener.getLogger().println("No Clover report will be published due to a "
                        + (buildFailure ? "build Failure" : "missing report"));
                return;
            }

            final boolean htmlExists = copyHtmlReport(coverageReportDir, buildTarget, listener);
            copyXmlReport(coverageReportDir, buildTarget, listener, env.expand(getCloverReportFileName()));

            if (htmlExists) {
                // only add the HTML run action, if the HTML report is available
                run.addAction(new CloverHtmlBuildAction());
            }
            processCloverXml(run, workspace, listener, coverageReportDir, buildTarget);

        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("Unable to copy coverage from " + coverageReportDir + " to " + buildTarget));
            run.setResult(Result.FAILURE);
        }
    }

    /**
     * Process the clover.xml from the build directory. The clover.xml must have been already copied to the build dir.
     */
    private void processCloverXml(Run<?, ?> build, FilePath workspace, TaskListener listener,
                                  FilePath coverageReport, FilePath buildTarget) throws InterruptedException {
        listener.getLogger().println("Processing Clover XML report ...");

        final String workspacePath = withTrailingSeparator(getWorkspacePath(listener, workspace));

        final File cloverXmlReport = getCloverXmlReport(build);
        if (cloverXmlReport.exists()) {
            ProjectCoverage result = null;
            try {
                result = CloverCoverageParser.parse(cloverXmlReport, workspacePath);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("Unable to copy coverage from " + coverageReport + " to " + buildTarget));
                build.setResult(Result.FAILURE);
            }

            listener.getLogger().println("Publishing Clover coverage results...");
            build.addAction(CloverBuildAction.load(workspacePath, result, healthyTarget, unhealthyTarget));

            final Set<CoverageMetric> failingMetrics = getFailingMetrics(result);
            if (!failingMetrics.isEmpty()) {
                logFailingMetrics(listener, failingMetrics);
                build.setResult(Result.UNSTABLE);
            }

        } else {
            flagMissingCloverXml(listener);
        }
    }

    @NonNull
    private String getWorkspacePath(TaskListener listener, FilePath workspace) throws InterruptedException {
        try {
            return workspace.act(new GetPathFileCallable());
        } catch (IOException e) {
            listener.getLogger().println("IOException when checking workspace path:" + e.getMessage());
            return "";
        }
    }

    @NonNull
    private String withTrailingSeparator(@NonNull String path) {
        return path.endsWith(File.separator) ? path : (path + File.separator);
    }

    @NonNull
    private Set<CoverageMetric> getFailingMetrics(ProjectCoverage result) {
        return failingTarget != null
                ? failingTarget.getFailingMetrics(result)
                : Collections.<CoverageMetric>emptySet();
    }

    private void logFailingMetrics(TaskListener listener, Set<CoverageMetric> failingMetrics) {
        listener.getLogger().println("Code coverage enforcement failed for the following metrics:");
        for (CoverageMetric metric : failingMetrics) {
            listener.getLogger().println("    " + metric);
        }
        listener.getLogger().println("Setting Build to unstable.");
    }

    private boolean copyXmlReport(FilePath coverageReport, FilePath buildTarget, TaskListener listener, String fileName)
            throws IOException, InterruptedException {
        // check one directory deep for a clover.xml, if there is not one in the coverageReport dir already
        // the clover auto-integration saves clover reports in: clover/${ant.project.name}/clover.xml
        final FilePath cloverXmlPath = findOneDirDeep(coverageReport, fileName);
        if (!cloverXmlPath.exists()) {
            listener.getLogger().printf(
                    "Clover XML file '%s' does not exist in '%s' and was not copied!%n", fileName, coverageReport);
            return false;
        }
        listener.getLogger().println("Publishing Clover XML report...");
        final FilePath toFile = buildTarget.child("clover.xml");
        cloverXmlPath.copyTo(toFile);
        return true;
    }

    private boolean copyHtmlReport(FilePath coverageReport, FilePath buildTarget, TaskListener listener)
            throws IOException, InterruptedException {
        // Copy the HTML coverage report
        final FilePath htmlIndexHtmlPath = findOneDirDeep(coverageReport, "index.html");
        if (!htmlIndexHtmlPath.exists()) {
            listener.getLogger().printf(
                    "Clover HTML report '%s' does not exist and was not copied!%n", coverageReport);
            return false;
        }
        final FilePath htmlDirPath = htmlIndexHtmlPath.getParent();
        if (htmlDirPath == null) {
            listener.getLogger().println("Parent directory of " + htmlIndexHtmlPath.getRemote() + " is null, not publishing Clover HTML report.");
            return false;
        }
        listener.getLogger().println("Publishing Clover HTML report...");
        htmlDirPath.copyRecursiveTo("**/*", buildTarget);
        return true;
    }

    /**
     * Searches the current directory and its immediate children directories for filename.
     * The first occurrence is returned.
     * @param startDir the dir to start searching in
     * @param filename the filename to search for
     * @return the path of filename
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    private FilePath findOneDirDeep(final FilePath startDir, final String filename)
            throws IOException, InterruptedException {

        FilePath dirContainingFile = startDir;
        if (!dirContainingFile.child(filename).exists()) {
            // use the first directory with filename in it
            final List<FilePath> dirs = dirContainingFile.listDirectories();
            if (dirs != null) {
                for (FilePath dir : dirs) {
                    if (dir.child(filename).exists()) {
                        dirContainingFile = dir;
                        break;
                    }
                }
            }
        }
        return dirContainingFile.child(filename);
    }

    private void flagMissingCloverXml(TaskListener listener) {
        listener.getLogger().println("Could not find '" + cloverReportDir + "/" + getCloverReportFileName()
                + "'.  Did you generate the XML report for Clover?");
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return DESCRIPTOR;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Descriptor for {@link CloverPublisher}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * See <tt>views/hudson/plugins/clover/CloverPublisher/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    @Symbol("clover")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        DescriptorImpl() {
            super(CloverPublisher.class);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.CloverPublisher_DisplayName();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindParameters(this, "clover.");
            save();
            return super.configure(req, formData);
        }

        /**
         * Creates a new instance of {@link CloverPublisher} from a submitted form.
         */
        @Override
        public CloverPublisher newInstance(@NonNull StaplerRequest req, @NonNull JSONObject formData) {
            final CloverPublisher instance = new CloverPublisher(
                    req.getParameter("clover.cloverReportDir"),
                    req.getParameter("clover.cloverReportFileName"),
                    fromRequest(req, "cloverHealthyTarget."),
                    fromRequest(req, "cloverUnhealthyTarget."),
                    fromRequest(req, "cloverFailingTarget.")
            );
            // start ugly hack
            if (instance.healthyTarget.isEmpty()) {
                instance.healthyTarget = new CoverageTarget(70, 80, 80);
            }
            // end ugly hack
            return instance;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        private CoverageTarget fromRequest(StaplerRequest req, String namePrefix) {
            return new CoverageTarget(
                    getIntParameter(req, namePrefix + "methodCoverage"),
                    getIntParameter(req, namePrefix + "conditionalCoverage"),
                    getIntParameter(req, namePrefix + "statementCoverage")
            );
        }

        private Integer getIntParameter(StaplerRequest req, String name) {
            try {
                return Integer.valueOf(req.getParameter(name));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}
