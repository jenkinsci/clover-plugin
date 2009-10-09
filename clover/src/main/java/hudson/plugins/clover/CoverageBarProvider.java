package hudson.plugins.clover;

/**
 * An interface that exposes enough data for a coverage bar to be rendered.
 *
 * @see /tags/coverage-bar.jelly
 */
public interface CoverageBarProvider {


    public String getPcWidth();
    
    public String getPcUncovered();

    public String getPcCovered();

    public String getHasData();

}
