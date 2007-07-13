package hudson.plugins.clover.results;

import hudson.model.Run;
import hudson.plugins.clover.CloverBuildAction;

/**
 * Clover Coverage results for a specific class.
 * @author Stephen Connolly
 */
public class ClassCoverage extends AbstractCloverMetrics {
    public AbstractCloverMetrics getPreviousResult() {
        if (owner == null) return null;
        Run prevBuild = owner.getPreviousBuild();
        if (prevBuild == null) return null;
        CloverBuildAction action = prevBuild.getAction(CloverBuildAction.class);
        if (action == null) return null;
        return action.findClassCoverage(getName());
    }
}
