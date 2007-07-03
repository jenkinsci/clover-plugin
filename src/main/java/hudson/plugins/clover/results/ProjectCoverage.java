package hudson.plugins.clover.results;

import java.util.List;
import java.util.ArrayList;

/**
 * Clover Coverage results for the entire project.
 * @author Stephen Connolly
 */
public class ProjectCoverage extends AbstractFileAggregatedMetrics {

    private int packages;

    private List<PackageCoverage> packageCoverages = new ArrayList<PackageCoverage>();

    public boolean addPackageCoverage(PackageCoverage result) {
        return packageCoverages.add(result);
    }

    public List<PackageCoverage> getPackageCoverages() {
        return packageCoverages;
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
                ClassCoverage j = i.findClassCoverage(name.substring(prefix.length()));
                if (j != null) return j;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public int getPackages() {
        return packages;
    }

    /** {@inheritDoc} */
    public void setPackages(int packages) {
        this.packages = packages;
    }

}
