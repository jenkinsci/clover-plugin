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

import static org.junit.jupiter.api.Assertions.*;

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
    private void setupCloverXmlFiles(FilePath... directories) throws Exception {
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
        job.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "    stage('Report') {\n" +
                "        script {\n" +
                "            def apps = ['app1', 'app2']\n" +
                "            apps.each { appName ->\n" +
                "                def reportDir = \"coverage/apps/${appName}\"\n" +
                "                def cloverFilePath = \"${reportDir}/clover.xml\"\n" +
                "                echo \"[COVERAGE] Publishing Clover report for ${appName} (${cloverFilePath})\"\n" +
                "                clover(\n" +
                "                    cloverReportDir: reportDir,\n" +
                "                    cloverReportFileName: 'clover.xml',\n" +
                "                    reportId: (appName == 'app1' ? 1 : 2),\n" +
                "                    healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],\n" +
                "                    unhealthyTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0],\n" +
                "                    failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]\n" +
                "                )\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n", true)
        );

        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        
        // Verify 2 separate CloverBuildAction instances
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(2, cloverActions.size(), "Should have exactly 2 CloverBuildAction instances (one for each app)");
        
        // Find actions by reportId
        CloverBuildAction app1Action = null;
        CloverBuildAction app2Action = null;
        
        for (CloverBuildAction action : cloverActions) {
            if (1 == action.getReportId()) {
                app1Action = action;
            } else if (2 == action.getReportId()) {
                app2Action = action;
            }
        }
        
        assertNotNull(app1Action, "Should have a CloverBuildAction for reportId 1");
        assertNotNull(app2Action, "Should have a CloverBuildAction for reportId 2");
        
        // Verify different URLs
        assertNotEquals(app1Action.getUrlName(), app2Action.getUrlName(), 
            "Actions should have different URLs to avoid conflicts");
        assertEquals("clover-1", app1Action.getUrlName(), "first action should have URL 'clover-1'");
        assertEquals("clover-2", app2Action.getUrlName(), "second action should have URL 'clover-2'");
        
        // Verify different display names
        assertNotEquals(app1Action.getDisplayName(), app2Action.getDisplayName(), 
            "Actions should have different display names");
        assertTrue(app1Action.getDisplayName().contains("1"), 
            "first action display name should contain '1'");
        assertTrue(app2Action.getDisplayName().contains("2"), 
            "second action display name should contain '2'");
        
        // Verify separate XML files created
        File buildDir = build.getRootDir();
        File app1XmlFile = new File(buildDir, "clover-1.xml");
        File app2XmlFile = new File(buildDir, "clover-2.xml");
        
        assertTrue(app1XmlFile.exists(), "clover-1.xml should exist in build directory");
        assertTrue(app2XmlFile.exists(), "clover-2.xml should exist in build directory");
        assertNotEquals(app1XmlFile.getAbsolutePath(), app2XmlFile.getAbsolutePath(), 
            "XML files should be separate files");
        
        // Verify both actions have results
        assertNotNull(app1Action.getResult(), "app1 action should have coverage results");
        assertNotNull(app2Action.getResult(), "app2 action should have coverage results");
        
        // Verify build log shows both reports
        String buildLog = jenkinsRule.getLog(build);
        assertTrue(buildLog.contains("[COVERAGE] Publishing Clover report for app1"), 
            "Build log should show app1 report being published");
        assertTrue(buildLog.contains("[COVERAGE] Publishing Clover report for app2"), 
            "Build log should show app2 report being published");
        assertTrue(buildLog.contains("Publishing Clover coverage results for 1"), 
            "Build log should show reportId 1 results being published");
        assertTrue(buildLog.contains("Publishing Clover coverage results for 2"), 
            "Build log should show reportId 2 results being published");
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
        job.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "    ['app1', 'app2'].each { app ->\n" +
                "        clover(\n" +
                "            cloverReportDir: app,\n" +
                "            cloverReportFileName: 'clover.xml',\n" +
                "            healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],\n" +
                "            unhealthyTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0],\n" +
                "            failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]\n" +
                "        )\n" +
                "    }\n" +
                "}\n", true)
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
        File xml1 = new File(buildDir, "clover.xml"); // First report uses standard name
        File xml2 = new File(buildDir, "clover-1.xml"); // Second report uses numbered name
        assertTrue(xml1.exists(), "First XML file should exist as clover.xml");
        assertTrue(xml2.exists(), "Second XML file should exist as clover-1.xml");
    }

    // Test backward compatibility - single report without reportId
    @Test
    void testBackwardCompatibility_SingleReport() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "backwardCompatibility");
        FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(job);
        FilePath targetDir = workspace.child("target").child("site");
        setupCloverXmlFiles(targetDir);

        job.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                "    clover(\n" +
                "        cloverReportDir: 'target/site',\n" +
                "        cloverReportFileName: 'clover.xml',\n" +
                "        healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],\n" +
                "        unhealthyTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0],\n" +
                "        failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]\n" +
                "    )\n" +
                "}\n", true)
        );

        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        
        // Verify single action created - should maintain backward compatibility
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(1, cloverActions.size(), "Should have exactly 1 CloverBuildAction instance");
        
        CloverBuildAction action = cloverActions.get(0);
        assertEquals(0, action.getReportId(), "First action should have reportId 0 for backward compatibility");
        assertEquals("clover", action.getUrlName(), "URL should be 'clover' for backward compatibility");
    }
}