package hudson.plugins.clover;

import hudson.FilePath;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

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

        job.setDefinition(new CpsFlowDefinition(""
                        + "node {\n"
                        + "  step([$class: 'CloverPublisher', cloverReportDir: 'target/site/clover', cloverReportFileName: 'clover.xml', healthyTarget: [methodCoverage: 80, conditionalCoverage: 80, statementCoverage: 80], unhealthyTarget: [methodCoverage: 60, conditionalCoverage: 60, statementCoverage: 60], failingTarget: [methodCoverage: 40, conditionalCoverage: 40, statementCoverage: 40]])\n"
                        + "}\n", true)
        );
        jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }
}
