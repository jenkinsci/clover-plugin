package hudson.plugins.clover;

import hudson.FilePath;
import hudson.model.*;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

/**
 * Project level action.
 *
 * @author Stephen Connolly
 */
public class CloverProjectAction extends Actionable implements ProminentProjectAction {

    private final Project<?, ?> project;

    public CloverProjectAction(Project project) {
        this.project = project;
    }

    public String getIconFileName() {
        if (new File(CloverPublisher.getCloverReportDir(project), "index.html").exists())
            return "graph.gif";
        else if (new File(CloverPublisher.getCloverReportDir(project), "clover.pdf").exists())
            return "graph.gif";
        else if (new File(CloverPublisher.getCloverReportDir(project), "clover.xml").exists())
            return "graph.gif";
        else
            return null;
    }

    public String getDisplayName() {
        if (new File(CloverPublisher.getCloverReportDir(project), "index.html").exists())
            return "Clover Coverage Report";
        else if (new File(CloverPublisher.getCloverReportDir(project), "clover.pdf").exists())
            return "Clover Coverage PDF";
        else if (new File(CloverPublisher.getCloverReportDir(project), "clover.xml").exists())
            return "Coverage Report";
        else
            return null;
    }

    public String getUrlName() {
        if (new File(CloverPublisher.getCloverReportDir(project), "index.html").exists())
            return "clover";
        else if (new File(CloverPublisher.getCloverReportDir(project), "clover.pdf").exists())
            return "clover";
        else if (new File(CloverPublisher.getCloverReportDir(project), "clover.xml").exists())
            return "clover";
        return "clover";
    }

    public CloverBuildAction getLastResult() {
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
        if (getLastResult() != null)
            getLastResult().getResult().doGraph(req, rsp);
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException,
            InterruptedException {
        new DirectoryBrowserSupport(this).serveFile(req, rsp,
                new FilePath(CloverPublisher.getCloverReportDir(project)), "graph.gif", false);
    }

    public String getSearchUrl() {
        return getUrlName();
    }
}
