package hudson.plugins.clover.results;

import hudson.model.Run;
import hudson.plugins.clover.CloverBuildAction;
import hudson.plugins.clover.Ratio;
import hudson.plugins.clover.graphs.GraphImpl;
import hudson.util.ChartUtil;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;

import java.util.Calendar;

/**
 * Abstract Clover Coverage results.
 */
abstract public class AbstractCloverMetrics {


    private String name;

    private int methods;
    private int coveredmethods;

    private int conditionals;
    private int coveredconditionals;

    private int statements;
    private int coveredstatements;

    private int elements;
    private int coveredelements;
    private Run<?, ?> owner = null;

    public Ratio getMethodCoverage() {
        return Ratio.create(coveredmethods, methods);
    }

    public Ratio getConditionalCoverage() {
        return Ratio.create(coveredconditionals, conditionals);
    }

    public Ratio getStatementCoverage() {
        return Ratio.create(coveredstatements, statements);
    }

    public Ratio getElementCoverage() {
        return Ratio.create(coveredelements, elements);
    }

    /**
     * Getter for property 'conditionals'.
     *
     * @return Value for property 'conditionals'.
     */
    public int getConditionals() {
        return conditionals;
    }

    /**
     * Setter for property 'conditionals'.
     *
     * @param conditionals Value to set for property 'conditionals'.
     */
    public void setConditionals(int conditionals) {
        this.conditionals = conditionals;
    }

    /**
     * Getter for property 'methods'.
     *
     * @return Value for property 'methods'.
     */
    public int getMethods() {
        return methods;
    }

    /**
     * Setter for property 'methods'.
     *
     * @param methods Value to set for property 'methods'.
     */
    public void setMethods(int methods) {
        this.methods = methods;
    }

    /**
     * Getter for property 'coveredstatements'.
     *
     * @return Value for property 'coveredstatements'.
     */
    public int getCoveredstatements() {
        return coveredstatements;
    }

    /**
     * Setter for property 'coveredstatements'.
     *
     * @param coveredstatements Value to set for property 'coveredstatements'.
     */
    public void setCoveredstatements(int coveredstatements) {
        this.coveredstatements = coveredstatements;
    }

    /**
     * Getter for property 'coveredmethods'.
     *
     * @return Value for property 'coveredmethods'.
     */
    public int getCoveredmethods() {
        return coveredmethods;
    }

    /**
     * Setter for property 'coveredmethods'.
     *
     * @param coveredmethods Value to set for property 'coveredmethods'.
     */
    public void setCoveredmethods(int coveredmethods) {
        this.coveredmethods = coveredmethods;
    }

    /**
     * Getter for property 'coveredconditionals'.
     *
     * @return Value for property 'coveredconditionals'.
     */
    public int getCoveredconditionals() {
        return coveredconditionals;
    }

    /**
     * Setter for property 'coveredconditionals'.
     *
     * @param coveredconditionals Value to set for property 'coveredconditionals'.
     */
    public void setCoveredconditionals(int coveredconditionals) {
        this.coveredconditionals = coveredconditionals;
    }

    /**
     * Getter for property 'statements'.
     *
     * @return Value for property 'statements'.
     */
    public int getStatements() {
        return statements;
    }

    /**
     * Setter for property 'statements'.
     *
     * @param statements Value to set for property 'statements'.
     */
    public void setStatements(int statements) {
        this.statements = statements;
    }

    /**
     * Getter for property 'coveredelements'.
     *
     * @return Value for property 'coveredelements'.
     */
    public int getCoveredelements() {
        return coveredelements;
    }

    /**
     * Setter for property 'coveredelements'.
     *
     * @param coveredelements Value to set for property 'coveredelements'.
     */
    public void setCoveredelements(int coveredelements) {
        this.coveredelements = coveredelements;
    }

    /**
     * Getter for property 'elements'.
     *
     * @return Value for property 'elements'.
     */
    public int getElements() {
        return elements;
    }

    /**
     * Setter for property 'elements'.
     *
     * @param elements Value to set for property 'elements'.
     */
    public void setElements(int elements) {
        this.elements = elements;
    }

    /**
     * Getter for property 'name'.
     *
     * @return Value for property 'name'.
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for property 'name'.
     *
     * @param name Value to set for property 'name'.
     */
    public void setName(String name) {
        this.name = name;
    }

    public Run<?, ?> getOwner() {
        return owner;
    }

    public void setOwner(Run<?, ?> owner) {
        this.owner = owner;
    }

    abstract public AbstractCloverMetrics getPreviousResult();

    protected CloverBuildAction getPreviousCloverBuildAction() {
        if (owner == null) {
            return null;
        }
        
        Run<?, ?> prevBuild = owner.getPreviousBuild();
        if (prevBuild == null) {
            return null;
        }
        
        CloverBuildAction action = prevBuild.getAction(CloverBuildAction.class);
        while (action == null && prevBuild != null) {
            prevBuild = prevBuild.getPreviousBuild();
            if (prevBuild != null) {
                action = prevBuild.getAction(CloverBuildAction.class);
            }
        }

        return action;
    }

    public Graph getTrendGraph() {
        Run<?, ?> build = getOwner();
        Calendar t = build.getTimestamp();
        return new GraphImpl(this, t) {
            @Override
            protected DataSetBuilder<String, NumberOnlyBuildLabel> createDataSet(AbstractCloverMetrics metrics) {
                DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb
                        = new DataSetBuilder<>();
                for (AbstractCloverMetrics m = metrics; m != null; m = m.getPreviousResult()) {
                    ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(m.getOwner());
                    dsb.add(m.getMethodCoverage().getPercentageFloat(), Messages.AbstractCloverMetrics_Label_method(), label);
                    dsb.add(m.getConditionalCoverage().getPercentageFloat(), Messages.AbstractCloverMetrics_Label_conditional(), label);
                    dsb.add(m.getStatementCoverage().getPercentageFloat(), Messages.AbstractCloverMetrics_Label_statement(), label);
                }
                return dsb;
            }
        };
    }

}
