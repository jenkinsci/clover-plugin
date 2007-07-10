package hudson.plugins.clover;

import org.kohsuke.stapler.StaplerProxy;
import hudson.model.HealthReportingAction;
import hudson.model.HealthReport;
import hudson.model.Build;
import hudson.model.Result;
import hudson.plugins.clover.results.*;

import java.lang.ref.WeakReference;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 03-Jul-2007 08:43:08
 */
public class CloverBuildAction extends AbstractPackageAggregatedMetrics implements HealthReportingAction, StaplerProxy {
    public final Build owner;
    private String buildBaseDir;

    private transient WeakReference<ProjectCoverage> report;

    public HealthReport getBuildHealth() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getIconFileName() {
        return "graph.gif";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDisplayName() {
        return "Coverage Report";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getUrlName() {
        return "clover";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getTarget() {
        return getResult();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CloverBuildAction getPreviousResult() {
        return getPreviousResult(owner);
    }

    /** Gets the previous {@link CloverBuildAction} of the given build. */
    /*package*/
    static CloverBuildAction getPreviousResult(Build start) {
        Build<?, ?> b = start;
        while (true) {
            b = b.getPreviousBuild();
            if (b == null)
                return null;
            if (b.getResult() == Result.FAILURE)
                continue;
            CloverBuildAction r = b.getAction(CloverBuildAction.class);
            if (r != null)
                return r;
        }
    }

    CloverBuildAction(Build owner, String workspacePath, ProjectCoverage r) {
        this.owner = owner;
        this.report = new WeakReference<ProjectCoverage>(r);
        this.buildBaseDir = workspacePath;
        if (this.buildBaseDir == null) {
            this.buildBaseDir = File.separator;
        } else if (!this.buildBaseDir.endsWith(File.separator)) {
            this.buildBaseDir += File.separator;
        }
        r.setOwner(owner);
    }


    /** Obtains the detailed {@link CoverageReport} instance. */
    public synchronized ProjectCoverage getResult() {
        if (report != null) {
            ProjectCoverage r = report.get();
            if (r != null) return r;
        }

        File reportFile = CloverPublisher.getCloverReport(owner);
        try {

            ProjectCoverage r = CloverCoverageParser.parse(reportFile, buildBaseDir);;
            r.setOwner(owner);

            report = new WeakReference<ProjectCoverage>(r);
            return r;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load " + reportFile, e);
            return null;
        }
    }



    // the following is ugly but I might need it

    /** {@inheritDoc} */
    public PackageCoverage findPackageCoverage(String name) {
        return getResult().findPackageCoverage(name);
    }

    /** {@inheritDoc} */
    public FileCoverage findFileCoverage(String name) {
        return getResult().findFileCoverage(name);
    }

    /** {@inheritDoc} */
    public ClassCoverage findClassCoverage(String name) {
        return getResult().findClassCoverage(name);
    }

    /** {@inheritDoc} */
    public int getPackages() {
        return getResult().getPackages();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getFiles() {
        return getResult().getFiles();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getClasses() {
        return getResult().getClasses();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getLoc() {
        return getResult().getLoc();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getNcloc() {
        return getResult().getNcloc();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public Ratio getMethodCoverage() {
        return getResult().getMethodCoverage();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public Ratio getStatementCoverage() {
        return getResult().getStatementCoverage();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public Ratio getConditionalCoverage() {
        return getResult().getConditionalCoverage();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public Ratio getElementCoverage() {
        return getResult().getElementCoverage();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getConditionals() {
        return getResult().getConditionals();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getMethods() {
        return getResult().getMethods();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getCoveredstatements() {
        return getResult().getCoveredstatements();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getCoveredmethods() {
        return getResult().getCoveredmethods();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getCoveredconditionals() {
        return getResult().getCoveredconditionals();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getStatements() {
        return getResult().getStatements();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getCoveredelements() {
        return getResult().getCoveredelements();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /** {@inheritDoc} */
    public int getElements() {
        return getResult().getElements();    //To change body of overridden methods use File | Settings | File Templates.
    }

    private static final Logger logger = Logger.getLogger(CloverBuildAction.class.getName());

    public static CloverBuildAction load(Build<?, ?> build, String workspacePath, ProjectCoverage result) {
        return new CloverBuildAction(build, workspacePath, result);
    }
}
