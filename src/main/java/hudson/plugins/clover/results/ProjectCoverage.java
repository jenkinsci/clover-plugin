package hudson.plugins.clover.results;

import hudson.model.Run;
import hudson.plugins.clover.CloverBuildAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Clover Coverage results for the entire project.
 */
public class ProjectCoverage extends AbstractPackageAggregatedMetrics {

    private final List<PackageCoverage> packageCoverages = new ArrayList<>();

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

    public PackageCoverage getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return findPackageCoverage(token);
    }

    public AbstractCloverMetrics getPreviousResult() {
        CloverBuildAction action = getPreviousCloverBuildAction();
        if (action == null) {
            return null;
        }
        return action.getResult();
    }

    @Override
    public void setOwner(Run<?, ?> owner) {
        super.setOwner(owner);    //To change body of overridden methods use File | Settings | File Templates.
        for (PackageCoverage p: packageCoverages) {
            p.setOwner(owner);
        }
    }
}
