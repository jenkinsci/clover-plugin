package hudson.plugins.clover.results;

import hudson.plugins.clover.CloverBuildAction;

/**
 * Clover Coverage results for a specific class.
 * @author Stephen Connolly
 */
public class ClassCoverage extends AbstractCloverMetrics {
    public AbstractCloverMetrics getPreviousResult() {
        CloverBuildAction action = getPreviousCloverBuildAction();
        if (action == null) {
            return null;
        }
        return action.findClassCoverage(getName());
    }
}

