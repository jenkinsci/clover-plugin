package hudson.plugins.clover.results;

import hudson.model.Build;

import java.util.List;
import java.util.ArrayList;

/**
 * Clover Coverage results for a specific package.
 * @author Stephen Connolly
 */
public class PackageCoverage extends AbstractFileAggregatedMetrics {

    private List<FileCoverage> fileCoverages = new ArrayList<FileCoverage>();

    public boolean addFileCoverage(FileCoverage result) {
        return fileCoverages.add(result);
    }

    public List<FileCoverage> getFileCoverages() {
        return fileCoverages;
    }

    public FileCoverage findFileCoverage(String name) {
        for (FileCoverage i : fileCoverages) {
            if (name.equals(i.getName())) return i;
        }
        return null;
    }

    public ClassCoverage findClassCoverage(String name) {
        for (FileCoverage i : fileCoverages) {
            ClassCoverage j = i.findClassCoverage(name);
            if (j != null) return j;
        }
        return null;
    }


    public AbstractCloverMetrics getPreviousResult() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setOwner(Build owner) {
        super.setOwner(owner);    //To change body of overridden methods use File | Settings | File Templates.
        for (FileCoverage fileCoverage : fileCoverages) {
            fileCoverage.setOwner(owner);
        }
    }
}
