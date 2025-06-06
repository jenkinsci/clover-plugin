package hudson.plugins.clover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.plugins.clover.results.PackageCoverage;
import hudson.plugins.clover.results.ProjectCoverage;

import org.junit.jupiter.api.Test;

/**
 * CloverCoverageParser Tester.
 */
class CloverCoverageParserTest {

    @Test
    void testFailureMode1() {
        assertThrows(NullPointerException.class, () -> CloverCoverageParser.parse(null, ""));
    }

    @Test
    void testParse() throws Exception {
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

    @Test
    void testParseMultiPackage() throws Exception {
        ProjectCoverage result = CloverCoverageParser.parse(getClass().getResourceAsStream("clover-two-packages.xml"));
        result = CloverCoverageParser.trimPaths(result, "C:\\local\\maven\\helpers\\hudson\\clover\\");
        assertNotNull(result);
        assertEquals(ProjectCoverage.class, result.getClass());
        assertEquals("Maven Cloverreport", result.getName());
        assertEquals(40, result.getMethods());
        assertEquals(2, result.getPackageCoverages().size());
        assertEquals(14, result.findClassCoverage("hudson.plugins.clover.results.AbstractCloverMetrics").getCoveredmethods());
    }
}
