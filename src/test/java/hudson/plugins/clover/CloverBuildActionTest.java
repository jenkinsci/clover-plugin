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

import java.io.File;
import java.util.concurrent.TimeUnit;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.htmlunit.WebAssert.assertTextPresent;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.plugins.clover.targets.CoverageTarget;
import java.util.List;

@WithJenkins
class CloverBuildActionTest {

    private JenkinsRule j;
    
    // Test data constants
    private static final CoverageTarget HEALTHY_TARGET_1 = new CoverageTarget(70, 80, 80);
    private static final CoverageTarget UNHEALTHY_TARGET_1 = new CoverageTarget(50, 60, 60);
    private static final CoverageTarget HEALTHY_TARGET_2 = new CoverageTarget(75, 85, 85);
    private static final CoverageTarget UNHEALTHY_TARGET_2 = new CoverageTarget(55, 65, 65);
    private static final String WORKSPACE_PATH_1 = "/workspace1";
    private static final String WORKSPACE_PATH_2 = "/workspace2";

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
            wc.getPage(project);
            wc.getPage(build); 
            
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
        
        app1Dir.child("clover.xml").copyFrom(requireNonNull(CloverWorkflowTest.class.getResourceAsStream("/hudson/plugins/clover/clover.xml")));
        app2Dir.child("clover.xml").copyFrom(requireNonNull(CloverWorkflowTest.class.getResourceAsStream("/hudson/plugins/clover/clover.xml")));
        
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

        assertThat(cloverActions, containsInAnyOrder(
            allOf(hasProperty("reportId", is(1)), hasProperty("urlName", is("clover-1"))),
            allOf(hasProperty("reportId", is(2)), hasProperty("urlName", is("clover-2")))
        ));

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getPage(project);
            wc.getPage(build);
            for (CloverBuildAction action : cloverActions) {
                assertTextPresent(wc.getPage(build, action.getUrlName()), "Clover Coverage Report");
            }
            CloverBuildAction.invalidateReportCache();
            for (CloverBuildAction action : cloverActions) {
                assertTextPresent(wc.getPage(build, action.getUrlName()), "Clover Coverage Report");
            }
            CloverBuildAction.invalidateReportCache();
        }
    }

    @Test
    void testEqualsMethod() {
        CloverBuildAction action1 = CloverBuildAction.load(WORKSPACE_PATH_1, null, 1, HEALTHY_TARGET_1, UNHEALTHY_TARGET_1);
        CloverBuildAction action2 = CloverBuildAction.load(WORKSPACE_PATH_1, null, 1, HEALTHY_TARGET_1, UNHEALTHY_TARGET_1);
        CloverBuildAction actionDiffReportId = CloverBuildAction.load(WORKSPACE_PATH_1, null, 2, HEALTHY_TARGET_1, UNHEALTHY_TARGET_1);
        CloverBuildAction actionDiffWorkspace = CloverBuildAction.load(WORKSPACE_PATH_2, null, 1, HEALTHY_TARGET_1, UNHEALTHY_TARGET_1);
        CloverBuildAction actionDiffTargets = CloverBuildAction.load(WORKSPACE_PATH_1, null, 1, HEALTHY_TARGET_2, UNHEALTHY_TARGET_2);
        
        // Test reflexivity: object equals itself
        assertThat(action1, equalTo(action1));
        
        // Test null comparison
        assertThat(action1, not(equalTo(null)));
        
        // Test different class comparison
        assertThat(action1, not(equalTo("not a CloverBuildAction")));
        
        // Test equality of equivalent objects
        assertThat(action1, equalTo(action2));
        
        // Test inequality for different reportIds
        assertThat(action1, not(equalTo(actionDiffReportId)));
        
        // Test inequality for different workspace paths
        assertThat(action1, not(equalTo(actionDiffWorkspace)));
        
        // Test inequality for different targets
        assertThat(action1, not(equalTo(actionDiffTargets)));
    }

    @Test
    void testHashCodeMethod() {
        CloverBuildAction action1 = CloverBuildAction.load(WORKSPACE_PATH_1, null, 1, HEALTHY_TARGET_1, UNHEALTHY_TARGET_1);
        CloverBuildAction action2 = CloverBuildAction.load(WORKSPACE_PATH_1, null, 1, HEALTHY_TARGET_1, UNHEALTHY_TARGET_1);
        
        // Test hashCode consistency for equal objects 
        assertThat(action1.hashCode(), equalTo(action2.hashCode()));
    }

    @Test
    void testBackwardCompatibilityLoadMethod() {
        CloverBuildAction action = CloverBuildAction.load(WORKSPACE_PATH_1, null, HEALTHY_TARGET_1, UNHEALTHY_TARGET_1);
        
        assertThat(action.getReportId(), equalTo(0));
    }

    @Test
    void testGetCloverXmlReportMethod() throws Exception {
        // Create a freestyle job for testing the getCloverXmlReport method
        FreeStyleProject project = j.createFreeStyleProject("TestGetCloverXmlReport");
        
        // Create a build
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        
        // Test default method (no reportId)
        File defaultReport = CloverPublisher.getCloverXmlReport(build);
        assertThat(defaultReport.getName(), equalTo("clover.xml"));
        assertThat(defaultReport.getPath(), containsString(build.getRootDir().getPath()));
        
        // Test method with reportId = 0 (should behave like default)
        File reportId0 = CloverPublisher.getCloverXmlReport(build, 0);
        assertThat(reportId0.getName(), equalTo("clover.xml"));
        assertThat(reportId0.getPath(), equalTo(defaultReport.getPath()));
        
        // Test method with negative reportId (should behave like default) 
        File reportIdNegative = CloverPublisher.getCloverXmlReport(build, -1);
        assertThat(reportIdNegative.getName(), equalTo("clover.xml"));
        assertThat(reportIdNegative.getPath(), equalTo(defaultReport.getPath()));
        
        // Test method with positive reportId
        File reportId1 = CloverPublisher.getCloverXmlReport(build, 1);
        assertThat(reportId1.getName(), equalTo("clover-1.xml"));
        assertThat(reportId1.getPath(), containsString(build.getRootDir().getPath()));
        
        // Test method with different positive reportId
        File reportId5 = CloverPublisher.getCloverXmlReport(build, 5);
        assertThat(reportId5.getName(), equalTo("clover-5.xml"));
        assertThat(reportId5.getPath(), containsString(build.getRootDir().getPath()));
        
        // Test that different reportIds generate different file names
        assertThat(reportId1.getName(), not(equalTo(reportId5.getName())));
        assertThat(reportId1.getName(), not(equalTo(defaultReport.getName())));
    }
}