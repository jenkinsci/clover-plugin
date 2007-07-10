package hudson.plugins.clover;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import hudson.plugins.clover.results.ProjectCoverage;
import hudson.plugins.clover.results.PackageCoverage;

import java.io.File;

/**
 * CloverCoverageParser Tester.
 *
 * @author Stephen Connolly
 * @version 1.0
 */
public class CloverCoverageParserTest extends TestCase {
    public CloverCoverageParserTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFailureMode1() throws Exception {
        try {
            CloverCoverageParser.parse(null, "");
        } catch (NullPointerException e) {
            assertTrue("Expected exception thrown", true);
        }
    }

    public void testParse() throws Exception {
        ProjectCoverage result = CloverCoverageParser.parse(getClass().getResourceAsStream("clover.xml"));
        assertNotNull(result);
        assertEquals(ProjectCoverage.class, result.getClass());
        assertEquals("Maven Cloverreport", result.getName());
        assertEquals(10, result.getMethods());
        assertEquals(1, result.getPackageCoverages().size());
        PackageCoverage subResult = result.getPackageCoverages().get(0);
        assertEquals("hudson.plugins.clover", subResult.getName());
        assertEquals(70, subResult.getNcloc());
    }

    public void testParseMultiPackage() throws Exception {
        ProjectCoverage result = CloverCoverageParser.parse(getClass().getResourceAsStream("clover-two-packages.xml"));
        result = CloverCoverageParser.trimPaths(result, "C:\\local\\maven\\helpers\\hudson\\clover\\");
        assertNotNull(result);
        assertEquals(ProjectCoverage.class, result.getClass());
        assertEquals("Maven Cloverreport", result.getName());
        assertEquals(40, result.getMethods());
        assertEquals(2, result.getPackageCoverages().size());
        assertEquals(14, result.findClassCoverage("hudson.plugins.clover.results.AbstractCloverMetrics").getCoveredmethods());
    }

    public static Test suite() {
        return new TestSuite(CloverCoverageParserTest.class);
    }
}
