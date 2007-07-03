package hudson.plugins.clover;

import hudson.Launcher;
import hudson.FilePath;
import hudson.Util;
import hudson.plugins.clover.results.ProjectCoverage;
import hudson.model.*;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.*;

/**
 * Clover {@link Publisher}.
 *
 * @author Stephen Connolly
 */
public class CloverPublisher extends Publisher {

    private final String cloverReportDir;

    /**
     *
     * @param name
     * @stapler-constructor
     */
    public CloverPublisher(String cloverReportDir) {
        this.cloverReportDir = cloverReportDir;
    }

    public String getCloverReportDir() {
        return cloverReportDir;
    }

    /** Gets the directory where the Clover Report is stored for the given project. */
    /*package*/ static File getCloverReportDir(AbstractItem project) {
        return new File(project.getRootDir(), "clover");
    }

    /** Gets the directory where the Clover Report is stored for the given project. */
    /*package*/
    static File getCloverReport(Build build) {
        return new File(build.getRootDir(), "clover.xml");
    }


    public boolean perform(Build<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
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

        File cloverXmlReport = getCloverReport(build);
        if (cloverXmlReport.exists()) {
            listener.getLogger().println("Publishing Clover coverage results...");
            ProjectCoverage result = null;
            try {
                result = CloverCoverageParser.parse(cloverXmlReport);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("Unable to copy coverage from " + coverageReport + " to " + target));
                build.setResult(Result.FAILURE);
            }
            final CloverBuildAction action = CloverBuildAction.load(build, result);

            build.getActions().add(action);

        } else {
            flagMissingXloverXml(listener, build);
        }

        return true;
    }

    private void flagMissingXloverXml(BuildListener listener, Build<?, ?> build) {
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
            return req.bindParameters(CloverPublisher.class, "clover.");
        }
    }
}
