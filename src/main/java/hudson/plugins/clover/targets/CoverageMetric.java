package hudson.plugins.clover.targets;

public enum CoverageMetric {
    METHOD("Methods"),
    CONDITIONAL("Conditionals"),
    STATEMENT("Statements"),
    ELEMENT("Elements");

    private final String name;

    CoverageMetric(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
