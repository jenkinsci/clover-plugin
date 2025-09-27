package hudson.plugins.clover;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


// Test for JENKINS-76093: Cannot use Clover for multiple apps in project
@WithJenkins
class JENKINS76093Test {

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }
    
    /**
     * Helper method to set up clover.xml files in the specified directories
     */
    private static void setupCloverXmlFiles(FilePath... directories) throws Exception {
        for (FilePath dir : directories) {
            dir.mkdirs();
            dir.child("clover.xml").copyFrom(JENKINS76093Test.class.getResourceAsStream("/hudson/plugins/clover/clover.xml"));
        }
    }

    // Test multiple clover() calls with reportId create separate reports
    @Test
    void testJENKINS76093_MultipleAppsInProject() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "multipleAppsProject");
        FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(job);
        
        // Create test directory structure and clover.xml files
        FilePath app1Dir = workspace.child("coverage").child("apps").child("app1");
        FilePath app2Dir = workspace.child("coverage").child("apps").child("app2");
        setupCloverXmlFiles(app1Dir, app2Dir);

        // Pipeline with reportId parameter
        job.setDefinition(new CpsFlowDefinition("""
                node {
                    stage('Report') {
                        script {
                            def apps = ['app1', 'app2']
                            apps.each { appName ->
                                def reportDir = "coverage/apps/${appName}"
                                def cloverFilePath = "${reportDir}/clover.xml"
                                echo "[COVERAGE] Publishing Clover report for ${appName} (${cloverFilePath})"
                                clover(
                                    cloverReportDir: reportDir,
                                    cloverReportFileName: 'clover.xml',
                                    reportId: (appName == 'app1' ? 1 : 2),
                                    healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],
                                    unhealthyTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0],
                                    failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]
                                )
                            }
                        }
                    }
                }
                """, true)
        );

        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        
        // Verify 2 separate CloverBuildAction instances
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(2, cloverActions.size(), "Should have exactly 2 CloverBuildAction instances (one for each app)");
        
        // Verify actions have correct properties
        assertThat(cloverActions, containsInAnyOrder(
            allOf(
                hasProperty("reportId", is(1)),
                hasProperty("urlName", is("clover-1")),
                hasProperty("displayName", containsString("1"))
            ),
            allOf(
                hasProperty("reportId", is(2)),
                hasProperty("urlName", is("clover-2")),
                hasProperty("displayName", containsString("2"))
            )
        ));

        CloverBuildAction app1Action = cloverActions.stream().filter(a -> a.getReportId() == 1).findFirst().get();
        CloverBuildAction app2Action = cloverActions.stream().filter(a -> a.getReportId() == 2).findFirst().get();
        
        // Verify separate XML files created
        File buildDir = build.getRootDir();
        File app1XmlFile = new File(buildDir, "clover-1.xml");
        File app2XmlFile = new File(buildDir, "clover-2.xml");
        
        assertThat(app1XmlFile.exists(), is(true));
        assertThat(app2XmlFile.exists(), is(true));
        assertThat(app1XmlFile.getAbsolutePath(), not(equalTo(app2XmlFile.getAbsolutePath())));
        
        // Verify both actions have results
        assertThat(app1Action.getResult(), is(notNullValue()));
        assertThat(app2Action.getResult(), is(notNullValue()));
        
        // Verify build log shows both reports
        String buildLog = jenkinsRule.getLog(build);
        assertThat(buildLog, containsString("[COVERAGE] Publishing Clover report for app1"));
        assertThat(buildLog, containsString("[COVERAGE] Publishing Clover report for app2"));
        assertThat(buildLog, containsString("Publishing Clover coverage results for 1"));
        assertThat(buildLog, containsString("Publishing Clover coverage results for 2"));
    }

    // Test auto-generated reportId when not provided
    @Test
    void testAutoGeneratedReportId_MultipleReports() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "autoGeneratedIds");
        FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(job);
        
        // Create test files
        FilePath app1Dir = workspace.child("app1");
        FilePath app2Dir = workspace.child("app2");
        setupCloverXmlFiles(app1Dir, app2Dir);

        // Multiple clover() calls WITHOUT reportId - should auto-generate unique IDs
        job.setDefinition(new CpsFlowDefinition("""
                node {
                    ['app1', 'app2'].each { app ->
                        clover(
                            cloverReportDir: app,
                            cloverReportFileName: 'clover.xml',
                            healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],
                            unhealthyTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0],
                            failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]
                        )
                    }
                }
                """, true)
        );

        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        
        // Verify 2 separate actions with auto-generated IDs
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(2, cloverActions.size(), "Should have exactly 2 CloverBuildAction instances");
        
        // Verify auto-generated reportIds - first is 0 (backward compatible), second is numbered
        CloverBuildAction action1 = cloverActions.get(0);
        CloverBuildAction action2 = cloverActions.get(1);
        
        assertEquals(0, action1.getReportId(), "First action should have reportId 0 (backward compatible)");
        assertEquals(1, action2.getReportId(), "Second action should have reportId 1");
        
        // Verify different URLs
        assertEquals("clover", action1.getUrlName(), "First action should use 'clover' URL");
        assertEquals("clover-1", action2.getUrlName(), "Second action should use 'clover-1' URL");
        
        // Verify separate XML files created
        File buildDir = build.getRootDir();
        File xml1 = new File(buildDir, "clover.xml"); 
        File xml2 = new File(buildDir, "clover-1.xml"); 
        assertThat(xml1.exists(), is(true));
        assertThat(xml2.exists(), is(true));
    }

    // Test backward compatibility - single report without reportId
    @Test
    void testBackwardCompatibility_SingleReport() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "backwardCompatibility");
        FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(job);
        FilePath targetDir = workspace.child("target").child("site");
        setupCloverXmlFiles(targetDir);

        job.setDefinition(new CpsFlowDefinition("""
                node {
                    clover(
                        cloverReportDir: 'target/site',
                        cloverReportFileName: 'clover.xml',
                        healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],
                        unhealthyTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0],
                        failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]
                    )
                }
                """, true)
        );

        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        
        // Verify single action created
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(1, cloverActions.size(), "Should have exactly 1 CloverBuildAction instance");
        
        CloverBuildAction action = cloverActions.get(0);
        assertEquals(0, action.getReportId(), "First action should have reportId 0 for backward compatibility");
        assertEquals("clover", action.getUrlName(), "URL should be 'clover' for backward compatibility");
    }
}