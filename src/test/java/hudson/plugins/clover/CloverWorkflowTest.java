package hudson.plugins.clover;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class CloverWorkflowTest {

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
    }
    /**
     * Run a workflow job using {@link CloverPublisher} and check for success.
     */
    @Test
    void cloverPublisherWorkflowStep() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "cloverPublisherWorkflowStep");
        FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(job);
        FilePath mavenSettings = workspace.child("target").child("site").child("clover.xml");
        mavenSettings.copyFrom(CloverWorkflowTest.class.getResourceAsStream("/hudson/plugins/clover/clover.xml"));

        job.setDefinition(new CpsFlowDefinition(
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
        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        assertNotNull(build.getAction(CloverBuildAction.class), "Build's CloverBuildAction should be not Null");
    }

    /**
     * Run a scripted Pipeline using {@link CloverPublisher} and the clover keyword.
     */
    @Test
    void cloverPublisherKeywordCloverStep() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "cloverPublisherKeywordCloverStep");
        FilePath workspace = jenkinsRule.jenkins.getWorkspaceFor(job);
        FilePath mavenSettings = workspace.child("target").child("site").child("clover.xml");
        mavenSettings.copyFrom(CloverWorkflowTest.class.getResourceAsStream("/hudson/plugins/clover/clover.xml"));

        job.setDefinition(new CpsFlowDefinition(
                        "node {\n" +
                        "    stage('Clover report') {\n" +
                        "        clover(cloverReportDir: 'target/site', cloverReportFileName: 'clover.xml',\n" +
                        "            healthyTarget:   [methodCoverage: 21, conditionalCoverage: 27, statementCoverage: 23],\n" +
                        "            unhealthyTarget: [methodCoverage: 11, conditionalCoverage: 17, statementCoverage: 13],\n" +
                        "            failingTarget:   [methodCoverage:  1, conditionalCoverage:  7, statementCoverage:  3])\n" +
                        "    }\n" +
                        "}\n", true)
        );
        WorkflowRun build = jenkinsRule.buildAndAssertSuccess(job);
        assertNotNull(build.getAction(CloverBuildAction.class), "Build's CloverBuildAction should be not null");
    }
}
