package hudson.plugins.clover;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;

public class CloverWorkflowTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    /**
     * Run a workflow job using {@link CloverPublisher} and check for success.
     */
    @Test
    public void cloverPublisherWorkflowStep() throws Exception {
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
        assertNotNull("Build's CloverBuildAction should be not Null", build.getAction(CloverBuildAction.class));
    }
}
