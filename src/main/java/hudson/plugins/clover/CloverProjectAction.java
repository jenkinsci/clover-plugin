package hudson.plugins.clover;

import hudson.model.*;
import hudson.FilePath;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;

/**
 * Project level action.
 *
 * @author Stephen Connolly
 */
public class CloverProjectAction extends Actionable implements ProminentProjectAction {

    private final AbstractItem project;

    public CloverProjectAction(AbstractItem project) {
        this.project = project;
    }

    public String getIconFileName() {
        if (new File(CloverPublisher.getCloverReportDir(project), "index.html").exists())
            return "graph.gif";
        else if (new File(CloverPublisher.getCloverReportDir(project), "clover.pdf").exists())
            return "graph.gif";
        else
            return null;
    }

    public String getDisplayName() {
        if (new File(CloverPublisher.getCloverReportDir(project), "index.html").exists())
            return "Current Coverage Report";
        else if (new File(CloverPublisher.getCloverReportDir(project), "clover.pdf").exists())
            return "Current Coverage PDF";
        else
            return null;
    }

    public String getUrlName() {
        return "clover";
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException,
            InterruptedException {
        new DirectoryBrowserSupport(this).serveFile(req, rsp,
                new FilePath(CloverPublisher.getCloverReportDir(project)), "graph.gif", false);
    }
}
