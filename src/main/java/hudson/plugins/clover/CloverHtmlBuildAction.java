package hudson.plugins.clover;


import hudson.FilePath;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Run;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


public class CloverHtmlBuildAction implements RunAction2 {

    private transient Run<?, ?> build;

    public CloverHtmlBuildAction() {
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        build = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        build = r;
    }

    public String getDisplayName() {
        return Messages.CloverHtmlBuildAction_DisplayName();
    }

    public DirectoryBrowserSupport doDynamic(StaplerRequest req, StaplerResponse rsp) {
        return new DirectoryBrowserSupport(this, new FilePath(build.getRootDir()), "Clover Html Report", CloverProjectAction.ICON, false);
    }

    public String getIconFileName() {
        return CloverProjectAction.ICON;
    }

    public String getUrlName() {
        return "clover-report";
    }
}
