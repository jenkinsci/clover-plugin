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
 * Clover Coverage results for a specific file.
 * @author Stephen Connolly
 */
public class FileCoverage extends AbstractClassAggregatedMetrics {

    private List<ClassCoverage> classCoverages = new ArrayList<ClassCoverage>();

    public List<ClassCoverage> getChildren() {
        return getClassCoverages();
    }

    public ClassCoverage getDynamic(String token, StaplerRequest req, StaplerResponse rsp) throws IOException {
        return findClassCoverage(token);
    }

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

    public AbstractCloverMetrics getPreviousResult() {
        if (owner == null) return null;
        Run prevBuild = owner.getPreviousBuild();
        if (prevBuild == null) return null;
        CloverBuildAction action = prevBuild.getAction(CloverBuildAction.class);
        if (action == null) return null;
        return action.findFileCoverage(getName());
    }

    public void setOwner(AbstractBuild owner) {
        super.setOwner(owner);    //To change body of overridden methods use File | Settings | File Templates.
        for (ClassCoverage classCoverage : classCoverages) {
            classCoverage.setOwner(owner);
        }
    }
}
