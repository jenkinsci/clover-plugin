package hudson.plugins.clover;


import hudson.model.DirectoryBrowserSupport;
import hudson.model.Action;
import hudson.FilePath;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;


/**
 */
public class CloverHtmlBuildAction implements Action {

    final FilePath buildReportPath; // location of the clover html for each build

    public CloverHtmlBuildAction(FilePath buildReportPath) {
        this.buildReportPath = buildReportPath;
    }

    public String getDisplayName() {
        return "Clover HTML Report";
    }

    public DirectoryBrowserSupport doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException,
            InterruptedException {
        return new DirectoryBrowserSupport(this, buildReportPath, "Clover Html Report", CloverProjectAction.ICON, false);
    }

    public String getIconFileName() {
        return CloverProjectAction.ICON;
    }

    public String getUrlName() {
        return "clover-report";
    }
}
