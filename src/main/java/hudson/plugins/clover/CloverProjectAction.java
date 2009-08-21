package hudson.plugins.clover;

import hudson.FilePath;
import hudson.model.Project;
import hudson.model.ProminentProjectAction;
import hudson.model.Build;
import hudson.model.Result;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Actionable;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

/**
 * Project level action.
 *
 * TODO: refactor this action in a similar manner to JavadocArchiver and BaseJavadocAction etc to avoid duplication.
 *
 * @author Stephen Connolly
 */
public class CloverProjectAction extends Actionable implements ProminentProjectAction {

    static final String ICON = "/plugin/clover/clover_48x48.png";
    
    private final Project<?, ?> project;

    public CloverProjectAction(Project project) {
        this.project = project;
    }

    public String getIconFileName() {

        final File reportDir = getLastBuildReportDir();
        if (reportDir != null &&
             (new File(reportDir, "index.html").exists()
           || new File(reportDir, "clover.pdf").exists()
           || new File(reportDir, "clover.xml").exists())) {
            return ICON;
        } else {
            return null;
        }
    }

    private File getLastBuildReportDir() {
        if (project.getLastBuild() == null) {
            // no clover report links, until there is at least one build
            return null;
        }
        final File reportDir = project.getLastBuild().getRootDir();
        return reportDir;
    }

    public String getDisplayName() {
        final File reportDir = getLastBuildReportDir();

        if (reportDir == null) return null;
        if (new File(reportDir, "index.html").exists()) return "Clover HTML Coverage Report";
        if (new File(reportDir, "clover.pdf").exists()) return "Clover PDF Coverage";
        if (new File(reportDir, "clover.xml").exists()) return "Coverage Report";

        return null;

    }

    public String getUrlName() {
        return "clover";
    }

    /**
     * Returns the last Result that was successful.
     *
     * WARNING: this method is invoked dynamically from CloverProjectAction/floatingBox.jelly
     * @return the last successful build result
     */
    public CloverBuildAction getLastSuccessfulResult() {
        for (Build<?, ?> b = project.getLastBuild(); b != null; b = b.getPreviousBuild()) {
            if (b.getResult() == Result.FAILURE)
                continue;
            CloverBuildAction r = b.getAction(CloverBuildAction.class);
            if (r != null)
                return r;
        }
        return null;
    }

    public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (getLastSuccessfulResult() != null) {
            getLastSuccessfulResult().getResult().doGraph(req, rsp);
        }
    }

    public DirectoryBrowserSupport doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException,
            InterruptedException {

        // there is a report if there was a build already, and there is a report
        if (project.getLastBuild() != null && getDisplayName() != null) {
            return new DirectoryBrowserSupport(this,
                    new FilePath(project.getLastBuild().getRootDir()),"Clover Html Report",  "/clover/clover.gif", false);

        } else {
            return null;
        }        
    }

    public String getSearchUrl() {
        return getUrlName();
    }
}
