package hudson.plugins.clover.targets;

import hudson.plugins.clover.Ratio;
import hudson.plugins.clover.results.AbstractCloverMetrics;

import java.io.Serializable;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Holds the target coverage for a specific condition;
 *
 * @author Stephen Connolly
 * @since 1.1
 */
public class CoverageTarget implements Serializable {

    private Integer methodCoverage;

    private Integer conditionalCoverage;

    private Integer statementCoverage;

    private Integer elementCoverage;

    public CoverageTarget() {
    }

    public CoverageTarget(Integer methodCoverage, Integer conditionalCoverage, Integer statementCoverage) {
        this.methodCoverage = methodCoverage;
        this.conditionalCoverage = conditionalCoverage;
        this.statementCoverage = statementCoverage;
        this.elementCoverage = null;
    }

    public boolean isAlwaysMet() {
        return (methodCoverage == null || methodCoverage < 0) &&
                (conditionalCoverage == null || conditionalCoverage < 0) &&
                (statementCoverage == null || statementCoverage < 0) &&
                (elementCoverage == null || elementCoverage < 0);
    }

    public boolean isEmpty() {
        return methodCoverage == null &&
                conditionalCoverage == null &&
                statementCoverage == null &&
                elementCoverage == null;
    }

    public Set<CoverageMetric> getFailingMetrics(AbstractCloverMetrics coverage) {
        Set<CoverageMetric> result = new HashSet<CoverageMetric>();

        if (methodCoverage != null && coverage.getMethodCoverage().getPercentage() < methodCoverage) {
            result.add(CoverageMetric.METHOD);
        }

        if (conditionalCoverage != null && coverage.getConditionalCoverage().getPercentage() < conditionalCoverage) {
            result.add(CoverageMetric.CONDITIONAL);
        }

        if (statementCoverage != null && coverage.getStatementCoverage().getPercentage() < statementCoverage) {
            result.add(CoverageMetric.STATEMENT);
        }

        if (elementCoverage != null && coverage.getElementCoverage().getPercentage() < elementCoverage) {
            result.add(CoverageMetric.ELEMENT);
        }

        return result;
    }

    public Map<CoverageMetric, Integer> getRangeScores(CoverageTarget min, AbstractCloverMetrics coverage) {
        Integer j;
        Map<CoverageMetric, Integer> result = new HashMap<CoverageMetric, Integer>();

        j = calcRangeScore(methodCoverage, min.methodCoverage, coverage.getMethodCoverage().getPercentage());
        if (j != null) {
            result.put(CoverageMetric.METHOD, Integer.valueOf(j));
        }
        j = calcRangeScore(conditionalCoverage, min.conditionalCoverage, coverage.getConditionalCoverage().getPercentage());
        if (j != null) {
            result.put(CoverageMetric.CONDITIONAL, Integer.valueOf(j));
        }
        j = calcRangeScore(statementCoverage, min.statementCoverage, coverage.getStatementCoverage().getPercentage());
        if (j != null) {
            result.put(CoverageMetric.STATEMENT, Integer.valueOf(j));
        }
        j = calcRangeScore(elementCoverage, min.elementCoverage, coverage.getElementCoverage().getPercentage());
        if (j != null) {
            result.put(CoverageMetric.ELEMENT, Integer.valueOf(j));
        }
        return result;
    }

    private static int calcRangeScore(Integer max, Integer min, int value) {
        if (min == null || min < 0) min = 0;
        if (max == null || max > 100) max = 100;
        if (min > max) min = max - 1;
        int result = (int)(100f * (value - min.floatValue()) / (max.floatValue() - min.floatValue()));
        if (result < 0) return 0;
        if (result > 100) return 100;
        return result;
    }

    /**
     * Getter for property 'methodCoverage'.
     *
     * @return Value for property 'methodCoverage'.
     */
    public Integer getMethodCoverage() {
        return methodCoverage;
    }

    /**
     * Setter for property 'methodCoverage'.
     *
     * @param methodCoverage Value to set for property 'methodCoverage'.
     */
    public void setMethodCoverage(Integer methodCoverage) {
        this.methodCoverage = methodCoverage;
    }

    /**
     * Getter for property 'conditionalCoverage'.
     *
     * @return Value for property 'conditionalCoverage'.
     */
    public Integer getConditionalCoverage() {
        return conditionalCoverage;
    }

    /**
     * Setter for property 'conditionalCoverage'.
     *
     * @param conditionalCoverage Value to set for property 'conditionalCoverage'.
     */
    public void setConditionalCoverage(Integer conditionalCoverage) {
        this.conditionalCoverage = conditionalCoverage;
    }

    /**
     * Getter for property 'statementCoverage'.
     *
     * @return Value for property 'statementCoverage'.
     */
    public Integer getStatementCoverage() {
        return statementCoverage;
    }

    /**
     * Setter for property 'statementCoverage'.
     *
     * @param statementCoverage Value to set for property 'statementCoverage'.
     */
    public void setStatementCoverage(Integer statementCoverage) {
        this.statementCoverage = statementCoverage;
    }

    /**
     * Getter for property 'elementCoverage'.
     *
     * @return Value for property 'elementCoverage'.
     */
    public Integer getElementCoverage() {
        return elementCoverage;
    }

    /**
     * Setter for property 'elementCoverage'.
     *
     * @param elementCoverage Value to set for property 'elementCoverage'.
     */
    public void setElementCoverage(Integer elementCoverage) {
        this.elementCoverage = elementCoverage;
    }
}
