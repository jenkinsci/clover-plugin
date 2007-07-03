package hudson.plugins.clover.results;

import java.util.List;
import java.util.ArrayList;

/**
 * Clover Coverage results for a specific file.
 * @author Stephen Connolly
 */
public class FileCoverage extends AbstractClassAggregatedMetrics {

    private List<ClassCoverage> classCoverages = new ArrayList<ClassCoverage>();

    public boolean addClassCoverage(ClassCoverage result) {
        return classCoverages.add(result);
    }

    public List<ClassCoverage> getClassCoverages() {
        return classCoverages;
    }

    public ClassCoverage findClassCoverage(String name) {
        for (ClassCoverage i : classCoverages) {
            if (name.equals(i.getName())) return i;
        }
        return null;
    }


}
