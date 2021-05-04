package hudson.plugins.clover.results;

public abstract class AbstractPackageAggregatedMetrics extends AbstractFileAggregatedMetrics {
    private int packages;

    public abstract PackageCoverage findPackageCoverage(String name);

    /** {@inheritDoc} */
    public int getPackages() {
        return packages;
    }

    /** {@inheritDoc} */
    public void setPackages(int packages) {
        this.packages = packages;
    }
}
