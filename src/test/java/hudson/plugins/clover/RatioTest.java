package hudson.plugins.clover;

import junit.framework.TestCase;

/**
 * JUnit test for {@link Ratio}
 */
public class RatioTest extends TestCase {

    final void assertRatio(Ratio r, float numerator, float denominator) {
        assertEquals(numerator, r.numerator);
        assertEquals(denominator, r.denominator);
    }

    /**
     * Tests that {@link Ratio#parseValue(String)} parses correctly float
     * numbers with either dot or comma as decimal point.
     *
     * @throws Exception
     */
    public void testParseValue() throws Exception {
        assertRatio(Ratio.create(1,2), 1.0f, 2.0f);
    }
    
    /**
     * Tests that {@link Ratio#getPercentageFloat()} handles appropriate
     * values for numerator and denominator.  Specifically:
     * 
     * 1 - Ratios where the denominator is 0 (no tests), return 100%.
     * 
     * 2 - All other ratios return the numerator / denominiator.
     *
     * @throws Exception
     */
    public void testGetPercentageFloat() throws Exception {
        assertEquals(Ratio.create(  0,0).getPercentageFloat(), 100.0f, 0.005f);
        assertEquals(Ratio.create(  1,0).getPercentageFloat(), 100.0f, 0.005f);
        assertEquals(Ratio.create(100,0).getPercentageFloat(), 100.0f, 0.005f);

        assertEquals(Ratio.create(0,5).getPercentageFloat(),   0.0f, 0.005f);
        assertEquals(Ratio.create(4,5).getPercentageFloat(),  80.0f, 0.005f);
        assertEquals(Ratio.create(5,5).getPercentageFloat(), 100.0f, 0.005f);
    }

}
