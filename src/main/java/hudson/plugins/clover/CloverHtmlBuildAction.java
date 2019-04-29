package hudson.plugins.clover;


import hudson.model.DirectoryBrowserSupport;
import hudson.model.Action;
import hudson.FilePath;
import java.io.File;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import jenkins.util.VirtualFile;


/**
 */
public class CloverHtmlBuildAction implements Action {

    @Deprecated
    transient FilePath buildReportPath; // location of the clover html for each build

    private String buildReportLocation;

    public CloverHtmlBuildAction(FilePath buildReportPath) {
        this.buildReportLocation = buildReportPath.getRemote();
    }

    private Object readResolve() {
        if (buildReportPath != null) {
            buildReportLocation = buildReportPath.getRemote();
        }
        return this;
    }

    public String getDisplayName() {
        return Messages.CloverHtmlBuildAction_DisplayName();
    }

    public DirectoryBrowserSupport doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException,
            InterruptedException {
        return new DirectoryBrowserSupport(this, VirtualFile.forFile(new File(buildReportLocation)), "Clover Html Report", CloverProjectAction.ICON, false);
    }

    public String getIconFileName() {
        return CloverProjectAction.ICON;
    }

    public String getUrlName() {
        return "clover-report";
    }
}
