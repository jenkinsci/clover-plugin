package hudson.plugins.clover.results;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.clover.CloverBuildAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Clover Coverage results for the entire project.
 * @author Stephen Connolly
 */
public class ProjectCoverage extends AbstractPackageAggregatedMetrics {

    private List<PackageCoverage> packageCoverages = new ArrayList<PackageCoverage>();

    public boolean addPackageCoverage(PackageCoverage result) {
        return packageCoverages.add(result);
    }

    public List<PackageCoverage> getPackageCoverages() {
        return packageCoverages;
    }

    public List<PackageCoverage> getChildren() {
        return getPackageCoverages();
    }

    public PackageCoverage findPackageCoverage(String name) {
        for (PackageCoverage i : packageCoverages) {
            if (name.equals(i.getName())) return i;
        }
        return null;
    }

    public FileCoverage findFileCoverage(String name) {
        for (PackageCoverage i : packageCoverages) {
            FileCoverage j = i.findFileCoverage(name);
            if (j != null) return j;
        }
        return null;
    }

    public ClassCoverage findClassCoverage(String name) {
        for (PackageCoverage i : packageCoverages) {
            final String prefix = i.getName() + '.';
            if (name.startsWith(prefix)) {
                ClassCoverage j = i.findClassCoverage(name);
                if (j != null) return j;
            }
        }
        return null;
    }

    public PackageCoverage getDynamic(String token, StaplerRequest req, StaplerResponse rsp) throws IOException {
        return findPackageCoverage(token);
    }

    public AbstractCloverMetrics getPreviousResult() {
        if (owner == null) return null;
        Run prevBuild = owner.getPreviousBuild();
        if (prevBuild == null) return null;
        CloverBuildAction action = prevBuild.getAction(CloverBuildAction.class);
        if (action == null) return null;
        return action.getResult();
    }

    @Override
    public void setOwner(AbstractBuild owner) {
        super.setOwner(owner);    //To change body of overridden methods use File | Settings | File Templates.
        for (PackageCoverage p: packageCoverages) {
            p.setOwner(owner);
        }
    }
}
