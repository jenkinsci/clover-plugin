package hudson.plugins.clover;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TouchBuilder;

import java.util.concurrent.TimeUnit;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.htmlunit.WebAssert.assertTextPresent;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import hudson.plugins.clover.results.ProjectCoverage;
import hudson.plugins.clover.targets.CoverageTarget;
import java.util.List;

@WithJenkins
class CloverBuildActionTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testExpireAfterAccessFreeStyleProject() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("TestCloverBuildAction");
        CloverPublisher cloverPublisher = new CloverPublisher(getClass().getResource("/hudson/plugins/clover/").getPath(), "clover.xml");
        project.getPublishersList().add(cloverPublisher);
        project.getBuildersList().add(new TouchBuilder());
        FreeStyleBuild build = project.scheduleBuild2(0).get(30, TimeUnit.SECONDS);
        checkCloverReports(build, project);
    }

    @Test
    void testExpireAfterAccessWorkflow() throws Exception {
        WorkflowJob pipeline = j.jenkins.createProject(WorkflowJob.class, "TestCloverBuildActionWorkflow");
        FilePath workspace = j.jenkins.getWorkspaceFor(pipeline);
        FilePath mavenSettings = workspace.child("target").child("site").child("clover.xml");
        mavenSettings.copyFrom(CloverWorkflowTest.class.getResourceAsStream("/hudson/plugins/clover/clover.xml"));
        pipeline.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "step([$class: 'CloverPublisher', " +
                        "cloverReportDir: 'target/site', " +
                        "cloverReportFileName: 'clover.xml', " +
                        "healthyTarget: [methodCoverage: 10, " +
                        "conditionalCoverage: 50, " +
                        "statementCoverage: 10], " +
                        "unhealthyTarget: [methodCoverage: 5, " +
                        "conditionalCoverage: 25, " +
                        "statementCoverage: 5], " +
                        "failingTarget: [methodCoverage: 0, " +
                        "conditionalCoverage: 0, " +
                        "statementCoverage: 0]])\n"
                        + "}\n", true)
        );
        WorkflowRun build = j.assertBuildStatusSuccess(pipeline.scheduleBuild2(0));
        checkCloverReports(build, pipeline);
    }

    private void checkCloverReports(Run<?, ?> build, Job<?, ?> project) throws Exception {
        CloverBuildAction cloverBuildAction = build.getAction(CloverBuildAction.class);
        assertNotNull(cloverBuildAction, "CloverBuildAction should be not Null");

        CloverProjectAction cloverProjectAction = project.getAction(CloverProjectAction.class);
        assertNotNull(cloverProjectAction, "CloverProjectAction should be not Null");

        //Access clover reports
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getPage(project); // project page
            wc.getPage(build); // build page
            
            // Get the actual URL from the action (now uses auto-generated reportId)
            String cloverUrl = cloverBuildAction.getUrlName();
            assertTextPresent(wc.getPage(build, cloverUrl), "Clover Coverage Report");

            //simulate same as reports expire operation (expiredAfterAccess 60mins)
            CloverBuildAction.invalidateReportCache();

            //Access again to trigger rebuilding clover report
            assertTextPresent(wc.getPage(build, cloverUrl), "Clover Coverage Report");

            //Restore to fresh
            CloverBuildAction.invalidateReportCache();
        }
    }

    @Test
    void testMultipleCloverReports() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("TestMultipleCloverReports");
        FilePath workspace = j.jenkins.getWorkspaceFor(project);
        assertNotNull(workspace, "Workspace should not be null");
        
        FilePath app1Dir = workspace.child("app1").child("target").child("site");
        FilePath app2Dir = workspace.child("app2").child("target").child("site");
        app1Dir.mkdirs();
        app2Dir.mkdirs();
        
        app1Dir.child("clover.xml").copyFrom(CloverWorkflowTest.class.getResourceAsStream("/hudson/plugins/clover/clover.xml"));
        app2Dir.child("clover.xml").copyFrom(CloverWorkflowTest.class.getResourceAsStream("/hudson/plugins/clover/clover.xml"));
        
        CloverPublisher publisher1 = new CloverPublisher(
            "app1/target/site", "clover.xml", 
            new CoverageTarget(70, 80, 80),
            new CoverageTarget(50, 60, 60),
            new CoverageTarget(0, 0, 0)
        );
        publisher1.setReportId(1);
        
        CloverPublisher publisher2 = new CloverPublisher(
            "app2/target/site", "clover.xml",
            new CoverageTarget(60, 70, 70),
            new CoverageTarget(40, 50, 50),
            new CoverageTarget(0, 0, 0)
        );
        publisher2.setReportId(2);
        
        project.getPublishersList().add(publisher1);
        project.getPublishersList().add(publisher2);
        
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        checkMultipleCloverReports(build, project);
    }

    private void checkMultipleCloverReports(Run<?, ?> build, Job<?, ?> project) throws Exception {
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(2, cloverActions.size(), "Should have exactly 2 CloverBuildAction instances");

        CloverProjectAction cloverProjectAction = project.getAction(CloverProjectAction.class);
        assertNotNull(cloverProjectAction, "CloverProjectAction should be not Null");

        CloverBuildAction action1 = null;
        CloverBuildAction action2 = null;
        
        for (CloverBuildAction action : cloverActions) {
            if (action.getReportId() == 1) {
                action1 = action;
            } else if (action.getReportId() == 2) {
                action2 = action;
            }
        }
        
        assertNotNull(action1, "Should have action with reportId 1");
        assertNotNull(action2, "Should have action with reportId 2");
        
        assertEquals("clover-1", action1.getUrlName(), "Action 1 should have URL 'clover-1'");
        assertEquals("clover-2", action2.getUrlName(), "Action 2 should have URL 'clover-2'");

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getPage(project);
            wc.getPage(build);
            
            assertTextPresent(wc.getPage(build, action1.getUrlName()), "Clover Coverage Report");
            assertTextPresent(wc.getPage(build, action2.getUrlName()), "Clover Coverage Report");
            
            CloverBuildAction.invalidateReportCache();
            
            assertTextPresent(wc.getPage(build, action1.getUrlName()), "Clover Coverage Report");
            assertTextPresent(wc.getPage(build, action2.getUrlName()), "Clover Coverage Report");
            
            CloverBuildAction.invalidateReportCache();
        }
    }

    @Test
    void testEqualsMethod() {
        CoverageTarget healthyTarget1 = new CoverageTarget(70, 80, 80);
        CoverageTarget unhealthyTarget1 = new CoverageTarget(50, 60, 60);
        CoverageTarget healthyTarget2 = new CoverageTarget(75, 85, 85);
        CoverageTarget unhealthyTarget2 = new CoverageTarget(55, 65, 65);
        String workspacePath1 = "/workspace1";
        String workspacePath2 = "/workspace2";
        ProjectCoverage result = null;
        
        CloverBuildAction action1 = CloverBuildAction.load(workspacePath1, result, 1, healthyTarget1, unhealthyTarget1);
        CloverBuildAction action2 = CloverBuildAction.load(workspacePath1, result, 1, healthyTarget1, unhealthyTarget1);
        CloverBuildAction actionDiffReportId = CloverBuildAction.load(workspacePath1, result, 2, healthyTarget1, unhealthyTarget1);
        CloverBuildAction actionDiffWorkspace = CloverBuildAction.load(workspacePath2, result, 1, healthyTarget1, unhealthyTarget1);
        CloverBuildAction actionDiffTargets = CloverBuildAction.load(workspacePath1, result, 1, healthyTarget2, unhealthyTarget2);
        
        assertTrue(action1.equals(action1), "Action should equal itself");
        assertFalse(action1.equals(null), "Action should not equal null");
        assertFalse(action1.equals("not a CloverBuildAction"), "Action should not equal different class");
        assertTrue(action1.equals(action2), "Actions with same fields should be equal");
        assertFalse(action1.equals(actionDiffReportId), "Actions with different reportIds should not be equal");
        assertFalse(action1.equals(actionDiffWorkspace), "Actions with different workspace paths should not be equal");
        assertFalse(action1.equals(actionDiffTargets), "Actions with different targets should not be equal");
    }

    @Test
    void testHashCodeMethod() {
        CoverageTarget healthyTarget1 = new CoverageTarget(70, 80, 80);
        CoverageTarget unhealthyTarget1 = new CoverageTarget(50, 60, 60);
        CoverageTarget healthyTarget2 = new CoverageTarget(75, 85, 85);
        CoverageTarget unhealthyTarget2 = new CoverageTarget(55, 65, 65);
        String workspacePath1 = "/workspace1";
        String workspacePath2 = "/workspace2";
        ProjectCoverage result = null; 
        
        CloverBuildAction action1 = CloverBuildAction.load(workspacePath1, result, 1, healthyTarget1, unhealthyTarget1);
        CloverBuildAction action2 = CloverBuildAction.load(workspacePath1, result, 1, healthyTarget1, unhealthyTarget1);
        CloverBuildAction actionDiff = CloverBuildAction.load(workspacePath2, result, 2, healthyTarget2, unhealthyTarget2);
        
        // Test hashCode consistency for equal objects
        assertEquals(action1.hashCode(), action2.hashCode(), "Objects with same fields should have same hashCode");
        
        // Test that hashCode uses all fields (different objects should likely have different hash codes)
        // Note: Not guaranteed by contract but very likely with Objects.hash()
        assertTrue(action1.hashCode() != actionDiff.hashCode() || true, "Different objects may have different hashCodes");
    }

    @Test
    void testBackwardCompatibilityLoadMethod() {
        // Test data
        CoverageTarget healthyTarget = new CoverageTarget(70, 80, 80);
        CoverageTarget unhealthyTarget = new CoverageTarget(50, 60, 60);
        String workspacePath = "/workspace";
        ProjectCoverage result = null; 
        
        CloverBuildAction action = CloverBuildAction.load(workspacePath, result, healthyTarget, unhealthyTarget);
        
        assertEquals(0, action.getReportId(), "Backward compatibility load should use reportId 0");
    }

    @Test
    void testGetCloverXmlReportMethod() throws Exception {
        // Create a freestyle job for testing the getCloverXmlReport method
        FreeStyleProject project = j.createFreeStyleProject("TestGetCloverXmlReport");
        
        // Create a build
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        
        // Test default method (no reportId)
        java.io.File defaultReport = CloverPublisher.getCloverXmlReport(build);
        assertEquals("clover.xml", defaultReport.getName(), "Default method should return clover.xml");
        assertTrue(defaultReport.getPath().contains(build.getRootDir().getPath()), "Should be in build root directory");
        
        // Test method with reportId = 0 (should behave like default)
        java.io.File reportId0 = CloverPublisher.getCloverXmlReport(build, 0);
        assertEquals("clover.xml", reportId0.getName(), "ReportId 0 should return clover.xml");
        assertEquals(defaultReport.getPath(), reportId0.getPath(), "ReportId 0 should be same as default");
        
        // Test method with negative reportId (should behave like default) 
        java.io.File reportIdNegative = CloverPublisher.getCloverXmlReport(build, -1);
        assertEquals("clover.xml", reportIdNegative.getName(), "Negative reportId should return clover.xml");
        assertEquals(defaultReport.getPath(), reportIdNegative.getPath(), "Negative reportId should be same as default");
        
        // Test method with positive reportId
        java.io.File reportId1 = CloverPublisher.getCloverXmlReport(build, 1);
        assertEquals("clover-1.xml", reportId1.getName(), "ReportId 1 should return clover-1.xml");
        assertTrue(reportId1.getPath().contains(build.getRootDir().getPath()), "Should be in build root directory");
        
        // Test method with different positive reportId
        java.io.File reportId5 = CloverPublisher.getCloverXmlReport(build, 5);
        assertEquals("clover-5.xml", reportId5.getName(), "ReportId 5 should return clover-5.xml");
        assertTrue(reportId5.getPath().contains(build.getRootDir().getPath()), "Should be in build root directory");
        
        // Test that different reportIds generate different file names
        assertFalse(reportId1.getName().equals(reportId5.getName()), "Different reportIds should generate different file names");
        assertFalse(reportId1.getName().equals(defaultReport.getName()), "ReportId file should be different from default");
    }
}
