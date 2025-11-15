package hudson.plugins.clover;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.FilePath;
import java.io.File;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class MultipleReportsTest {

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
            dir.child("clover.xml")
                    .copyFrom(requireNonNull(
                            MultipleReportsTest.class.getResourceAsStream("/hudson/plugins/clover/clover.xml")));
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
                """
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
                                    reportId: (appName == 'app1' ? 'app1' : 'app2'),
                                    healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],
                                    unhealthyTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0],
                                    failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]
                                )
                            }
                        }
                    }
                }
                """,
                true));

        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));

        // Verify 2 separate CloverBuildAction instances
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(2, cloverActions.size(), "Should have exactly 2 CloverBuildAction instances (one for each app)");

        // Verify actions have correct properties
        assertThat(
                cloverActions,
                containsInAnyOrder(
                        allOf(hasProperty("reportId", is("app1")), hasProperty("urlName", is("clover-app1"))),
                        allOf(hasProperty("reportId", is("app2")), hasProperty("urlName", is("clover-app2")))));

        CloverBuildAction app1Action = cloverActions.stream()
                .filter(a -> "app1".equals(a.getReportId()))
                .findFirst()
                .get();
        CloverBuildAction app2Action = cloverActions.stream()
                .filter(a -> "app2".equals(a.getReportId()))
                .findFirst()
                .get();

        // Verify separate XML files created
        File buildDir = build.getRootDir();
        File app1XmlFile = new File(buildDir, "clover-app1.xml");
        File app2XmlFile = new File(buildDir, "clover-app2.xml");

        assertThat(app1XmlFile.exists(), is(true));
        assertThat(app2XmlFile.exists(), is(true));
        assertThat(app1XmlFile.getAbsolutePath(), not(equalTo(app2XmlFile.getAbsolutePath())));

        // Verify both actions have results
        assertThat(app1Action.getResult(), is(notNullValue()));
        assertThat(app2Action.getResult(), is(notNullValue()));

        // Verify build log shows both reports
        String buildLog = JenkinsRule.getLog(build);
        assertThat(buildLog, containsString("[COVERAGE] Publishing Clover report for app1"));
        assertThat(buildLog, containsString("[COVERAGE] Publishing Clover report for app2"));
        assertThat(buildLog, containsString("Publishing Clover coverage results for app1"));
        assertThat(buildLog, containsString("Publishing Clover coverage results for app2"));
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
                """
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
                """,
                true));

        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));

        // Verify 2 separate actions with auto-generated IDs
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(2, cloverActions.size(), "Should have exactly 2 CloverBuildAction instances");

        // Verify both have unique auto-generated reportIds (base36 encoded ~8 chars)
        CloverBuildAction action1 = cloverActions.get(0);
        CloverBuildAction action2 = cloverActions.get(1);

        assertThat(action1.getReportId(), not(equalTo("")));
        assertThat(action2.getReportId(), not(equalTo("")));
        assertThat(action1.getReportId(), not(equalTo(action2.getReportId())));
        assertThat(action1.getReportId().length(), is(8)); // base36 encoded ~8 chars
        assertThat(action2.getReportId().length(), is(8));

        // Verify different URLs
        assertThat(action1.getUrlName(), containsString("clover-"));
        assertThat(action2.getUrlName(), containsString("clover-"));
        assertThat(action1.getUrlName(), not(equalTo(action2.getUrlName())));

        // Verify separate XML files created
        File buildDir = build.getRootDir();
        File xml1 = new File(buildDir, "clover-" + action1.getReportId() + ".xml");
        File xml2 = new File(buildDir, "clover-" + action2.getReportId() + ".xml");
        assertThat(xml1.exists(), is(true));
        assertThat(xml2.exists(), is(true));
    }

    // Test backward compatibility - single report without reportId gets auto-generated ID
    @Test
    void testSingleReportWithoutExplicitId() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "singleReport");
        FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(job);
        FilePath targetDir = workspace.child("target").child("site");
        setupCloverXmlFiles(targetDir);

        job.setDefinition(new CpsFlowDefinition(
                """
                node {
                    clover(
                        cloverReportDir: 'target/site',
                        cloverReportFileName: 'clover.xml',
                        healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80],
                        unhealthyTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0],
                        failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]
                    )
                }
                """,
                true));

        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));

        // Verify single action created
        List<CloverBuildAction> cloverActions = build.getActions(CloverBuildAction.class);
        assertEquals(1, cloverActions.size(), "Should have exactly 1 CloverBuildAction instance");

        CloverBuildAction action = cloverActions.get(0);
        // Even single reports now get auto-generated IDs
        assertThat(action.getReportId(), not(equalTo("")));
        assertThat(action.getReportId().length(), is(8));
        assertThat(action.getUrlName(), containsString("clover-"));
    }
}
