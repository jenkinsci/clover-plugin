package hudson.plugins.clover;

import hudson.Launcher;
import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import hudson.plugins.clover.results.ProjectCoverage;
import hudson.plugins.clover.targets.CoverageTarget;
import hudson.plugins.clover.targets.CoverageMetric;
import hudson.model.*;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.util.Set;

/**
 * Clover {@link Publisher}.
 *
 * @author Stephen Connolly
 */
public class CloverPublisher extends Publisher {

    private final String cloverReportDir;

    private CoverageTarget healthyTarget;
    private CoverageTarget unhealthyTarget;
    private CoverageTarget failingTarget;

    /**
     *
     * @param name
     * @stapler-constructor
     */
    public CloverPublisher(String cloverReportDir) {
        this.cloverReportDir = cloverReportDir;
        this.healthyTarget = new CoverageTarget();
        this.unhealthyTarget = new CoverageTarget();
        this.failingTarget = new CoverageTarget();
    }

    public String getCloverReportDir() {
        return cloverReportDir;
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

    /** Gets the directory where the Clover Report is stored for the given project. */
    /*package*/ static File getCloverReportDir(AbstractItem project) {
        return new File(project.getRootDir(), "clover");
    }

    /** Gets the directory where the Clover Report is stored for the given project. */
    /*package*/
    static File getCloverReport(AbstractBuild<?, ?> build) {
        return new File(build.getRootDir(), "clover.xml");
    }


    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        listener.getLogger().println("Publishing Clover coverage report...");
        FilePath coverageReport = build.getParent().getWorkspace().child(cloverReportDir);

        FilePath target = new FilePath(getCloverReportDir(build.getParent()));
        final File buildCloverDir = build.getRootDir();
        FilePath buildTarget = new FilePath(buildCloverDir);

        try {
            // if the build has failed, then there's not
            // much point in reporting an error
            if (build.getResult().isWorseOrEqualTo(Result.FAILURE) && !coverageReport.exists())
                return true;

            // Copy the code
            coverageReport.copyRecursiveTo("**/*", target);
            // Copy the xml report

            coverageReport.copyRecursiveTo("clover.xml", buildTarget);
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("Unable to copy coverage from " + coverageReport + " to " + target));
            build.setResult(Result.FAILURE);
        }

        String workspacePath = "";
        try {
            workspacePath = build.getParent().getWorkspace().act(new FilePath.FileCallable<String>() {
                public String invoke(File file, VirtualChannel virtualChannel) throws IOException {
                    try {
                        return file.getCanonicalPath();
                    } catch (IOException e) {
                        return file.getAbsolutePath();
                    }
                }
            });
        } catch (IOException e) {
        }
        if (!workspacePath.endsWith(File.separator)) {
            workspacePath += File.separator;
        }

        File cloverXmlReport = getCloverReport(build);
        if (cloverXmlReport.exists()) {
            listener.getLogger().println("Publishing Clover coverage results...");
            ProjectCoverage result = null;
            try {
                result = CloverCoverageParser.parse(cloverXmlReport, workspacePath);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("Unable to copy coverage from " + coverageReport + " to " + target));
                build.setResult(Result.FAILURE);
            }
            final CloverBuildAction action = CloverBuildAction.load(build, workspacePath, result, healthyTarget, unhealthyTarget);

            build.getActions().add(action);
            Set<CoverageMetric> failingMetrics = failingTarget.getFailingMetrics(result);
            if (!failingMetrics.isEmpty()) {
                listener.getLogger().println("Code coverage enforcement failed for the following metrics:");
                for (CoverageMetric metric : failingMetrics) {
                    listener.getLogger().println("    " + metric);
                }
                listener.getLogger().println("Setting Build to unstable.");
                build.setResult(Result.UNSTABLE);
            }

        } else {
            flagMissingCloverXml(listener, build);
        }

        return true;
    }

    private void flagMissingCloverXml(BuildListener listener, AbstractBuild<?, ?> build) {
        listener.getLogger().println("Could not find '" + cloverReportDir + "/clover.xml'.  Did you generate " +
                "the XML report for Clover?");
        build.setResult(Result.FAILURE);
    }


    public Action getProjectAction(Project project) {
        return new CloverProjectAction(project);
    }

    public Descriptor<Publisher> getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return DESCRIPTOR;
    }

    /** Descriptor should be singleton. */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Descriptor for {@link CloverPublisher}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * <p/>
     * <p/>
     * See <tt>views/hudson/plugins/clover/CloverPublisher/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    public static final class DescriptorImpl extends Descriptor<Publisher> {
        DescriptorImpl() {
            super(CloverPublisher.class);
        }

        /** This human readable name is used in the configuration screen. */
        public String getDisplayName() {
            return "Publish Clover Coverage Report";
        }


        public boolean configure(StaplerRequest req) throws FormException {
            req.bindParameters(this, "clover.");
            save();
            return super.configure(req);    //To change body of overridden methods use File | Settings | File Templates.
        }

        /** Creates a new instance of {@link CloverPublisher} from a submitted form. */
        public CloverPublisher newInstance(StaplerRequest req) throws FormException {
            CloverPublisher instance = req.bindParameters(CloverPublisher.class, "clover.");
            req.bindParameters(instance.failingTarget, "cloverFailingTarget.");
            req.bindParameters(instance.healthyTarget, "cloverHealthyTarget.");
            req.bindParameters(instance.unhealthyTarget, "cloverUnhealthyTarget.");
            // start ugly hack
            if (instance.healthyTarget.isEmpty()) {
                instance.healthyTarget = new CoverageTarget(70, 80, 80);
            }
            // end ugly hack
            return instance;
        }
    }
}
