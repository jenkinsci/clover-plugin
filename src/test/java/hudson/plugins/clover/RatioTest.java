package hudson.plugins.clover;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * JUnit test for {@link Ratio}
 */
class RatioTest {

    /**
     * Tests {@link Ratio#create(float, float)}
     */
    @Test
    void testParseValue() {
        assertEquals(1.0f, Ratio.create(1,2).numerator);
        assertEquals(2.0f, Ratio.create(1,2).denominator);
    }

    /**
     * Tests that {@link Ratio#getPercentageFloat()} handles appropriate
     * values for numerator and denominator.  Specifically:
     *
     * 1 - Ratios where the denominator is 0 (no tests), return 100%.
     * 2 - All other ratios return the numerator / denominator.
     *
     * There is a case here that is in the code, but is caused by invalid data:
     * 3 - The numerator is larger than the denominator.
     *
     * There are other invalid data points as well:
     *
     * 4 - Numerator is larger than the denominator
     * 5 - Either the numerator or the denominator is < 0
     */
    @Test
    void testGetPercentageFloat() {
        // denominator is 0 - no data - always return 100%
        assertEquals(100.0f, Ratio.create(  0,0).getPercentageFloat(), 0.005f, "0/0   => 100");
        assertEquals(100.0f, Ratio.create(  1,0).getPercentageFloat(), 0.005f, "1/0   => 100");
        assertEquals(100.0f, Ratio.create(100,0).getPercentageFloat(), 0.005f, "100/0 => 100");

        // non-zero denominator
        assertEquals(0.0f,   Ratio.create(0,5).getPercentageFloat(), 0.005f, "0/5 =>   0");
        assertEquals(80.0f,  Ratio.create(4,5).getPercentageFloat(), 0.005f, "4/5 =>  80");
        assertEquals(100.0f, Ratio.create(5,5).getPercentageFloat(), 0.005f, "5/5 => 100");

        // invalid data: numerator > denominator, try to fix the result by returning 100%
        assertEquals(100.0f, Ratio.create(10,5).getPercentageFloat(), 0.005f, "10/5 => 100");
    }

    /**
     * Tests that {@link Ratio#getPercentage()} correctly handles values near
     * whole integers. In particular that 99.99% becomes 99% and not 100%
     */
    @Test
    void testGetPercentage() {
        // truncate to zero
        assertEquals(0,   Ratio.create(   0,1000).getPercentage(), "0/1000    =>   0");
        assertEquals(0,   Ratio.create(   9,1000).getPercentage(), "9/1000    =>   0");

        // truncate to 90
        assertEquals(90,  Ratio.create( 900,1000).getPercentage(), "900/1000  =>  90");
        assertEquals(90,  Ratio.create( 909,1000).getPercentage(), "909/1000  =>  90");

        // truncate to 99
        assertEquals(99,  Ratio.create( 990,1000).getPercentage(), "990/1000  =>  99");
        assertEquals(99,  Ratio.create( 999,1000).getPercentage(), "999/1000  =>  99");

        // still show 100
        assertEquals(100, Ratio.create(1000,1000).getPercentage(), "1000/1000 => 100");
    }

    /**
     * Tests that {@link Ratio#getPcCovered()} correctly handles values near
     * whole integers. This is to make sure that it will match that of
     * getPercentage()
     */
    @Test
    void testGetPcCovered() {
        // Make sure zero is zero
        assertEquals("0%",    Ratio.create(    0, 10000).getPcCovered(), "0/10000     =>    0");
        assertEquals("0%",    Ratio.create(    9, 10000).getPcCovered(), "9/10000     =>    0");

        assertEquals("0.9%",  Ratio.create(   99, 10000).getPcCovered(), "99/10000    =>  0.9");

        assertEquals("90%",   Ratio.create( 9009, 10000).getPcCovered(), "9009/10000  =>   90");
        assertEquals("90.9%", Ratio.create( 9099, 10000).getPcCovered(), "9099/10000  => 90.9");

        assertEquals("99%",   Ratio.create( 9909, 10000).getPcCovered(), "9909/10000  =>   99");
        assertEquals("99.9%", Ratio.create( 9999, 10000).getPcCovered(), "9999/10000  => 99.9");

        // still show 100
        assertEquals("100%",  Ratio.create(10000, 10000).getPcCovered(), "10000/10000 =>  100");
    }

    /**
     * Tests that {@link Ratio#getPcUncovered()} correctly handles values near
     * whole integers. This needs to correctly contrast with getPcCovered()
     */
    @Test
    void testGetPcUncovered() {
        // No coverage should be 100% uncovered
        assertEquals("100%", Ratio.create(    0, 10000).getPcUncovered(),
                     "0/10000     = 0% covered     = 100% uncovered   = 100% rounded up");

        assertEquals("100%", Ratio.create(    9, 10000).getPcUncovered(),
                     "9/10000     = 0.09% covered  = 99.91% uncovered = 100% rounded up");

        assertEquals("99.1%", Ratio.create(   99, 10000).getPcUncovered(),
                     "99/10000    = 0.99% covered  = 99.01% uncovered = 99.1% rounded up");

        assertEquals("10%", Ratio.create( 9009, 10000).getPcUncovered(),
                     "9009/10000  = 90.09% covered = 9.91% uncovered  = 10% rounded up");
        assertEquals("9.1%", Ratio.create( 9099, 10000).getPcUncovered(),
                     "9099/10000  = 90.99% covered = 9.01% uncovered  = 9.1% rounded up");

        assertEquals("1%", Ratio.create( 9909, 10000).getPcUncovered(),
                     "9909/10000  = 99.09% covered = 0.91% uncovered  = 1% rounded up");
        assertEquals("0.1%", Ratio.create( 9999, 10000).getPcUncovered(),
                     "9999/10000  = 99.99% covered = 0.01% uncovered  = 0.1% rounded up");

        // still show 0 when 100% covered
        assertEquals("0%", Ratio.create(10000, 10000).getPcUncovered(),
                     "10000/10000 = 100% covered   = 0% uncovered     = 0% rounded up");
    }

}
