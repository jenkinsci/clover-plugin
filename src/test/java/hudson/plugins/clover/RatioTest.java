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
        assertRatio(Ratio.parseValue("X% (1/2)"), 1.0f, 2.0f);
        assertRatio(Ratio.parseValue("X% (1,3/2)"), 1.3f, 2.0f);
        assertRatio(Ratio.parseValue("X% (1.3/2)"), 1.3f, 2.0f);
        assertRatio(Ratio.parseValue("X% (,3/2)"), 0.3f, 2.0f);
        assertRatio(Ratio.parseValue("X% (.3/2)"), 0.3f, 2.0f);
        assertRatio(Ratio.parseValue("X% (1./2)"), 1.0f, 2.0f);
        assertRatio(Ratio.parseValue("X% (1,/2)"), 1.0f, 2.0f);
        try {
            Ratio.parseValue("X% (1.a/2)");
            fail("Ratio.parseValue() should have raised NumberFormatException.");
        } catch (NumberFormatException e) {
            // OK, we are expecting this.
        }
    }
}