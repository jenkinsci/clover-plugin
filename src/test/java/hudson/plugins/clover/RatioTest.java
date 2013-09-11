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
     * There is a case here that is in the code, but is caused by invalid data:
     * 
     * 3 - The numerator is larger than the denominator.
     * 
     * There are other invalid data points as well:
     * 
     * 4 - Numerator is larger than the denominator
     * 
     * 5 - Either the numerator or the denominator is < 0
     *
     * @throws Exception
     */
    public void testGetPercentageFloat() throws Exception {
        assertEquals("0/0   => 100", 100.0f, Ratio.create(  0,0).getPercentageFloat(), 0.005f);
        // assertEquals("1/0   => 100", 100.0f, Ratio.create(  1,0).getPercentageFloat(), 0.005f);
        // assertEquals("100/0 => 100", 100.0f, Ratio.create(100,0).getPercentageFloat(), 0.005f);

        assertEquals("0/5 =>   0",   0.0f, Ratio.create(0,5).getPercentageFloat(), 0.005f);
        assertEquals("4/5 =>  80",  80.0f, Ratio.create(4,5).getPercentageFloat(), 0.005f);
        assertEquals("5/5 => 100", 100.0f, Ratio.create(5,5).getPercentageFloat(), 0.005f);
    }

}
