package hudson.plugins.clover;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TouchBuilder;

import java.util.concurrent.TimeUnit;

import static com.gargoylesoftware.htmlunit.WebAssert.assertTextPresent;
import static org.junit.Assert.assertNotNull;

public class CloverBuildActionTest {

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testExpireAfterAccessFreeStyleProject() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("TestCloverBuildAction");
        CloverPublisher cloverPublisher = new CloverPublisher(getClass().getResource("/hudson/plugins/clover/").getPath(), "clover.xml");
        project.getPublishersList().add(cloverPublisher);
        project.getBuildersList().add(new TouchBuilder());
        FreeStyleBuild build = project.scheduleBuild2(0).get(30, TimeUnit.SECONDS);
        checkCloverReports(build, project);
    }

    @Test
    public void testExpireAfterAccessWorkflow() throws Exception {
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
        assertNotNull("CloverBuildAction should be not Null", cloverBuildAction);

        CloverProjectAction cloverProjectAction = project.getAction(CloverProjectAction.class);
        assertNotNull("CloverProjectAction should be not Null", cloverProjectAction);

        //Access clover reports
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getPage(project); // project page
        wc.getPage(build); // build page
        assertTextPresent(wc.getPage(build, "clover"), "Clover Coverage Report");

        //simulate same as reports expire operation (expiredAfterAccess 60mins)
        CloverBuildAction.invalidateReportCache();

        //Access again to trigger rebuilding clover report
        assertTextPresent(wc.getPage(build, "clover"), "Clover Coverage Report");

        //Restore to fresh
        CloverBuildAction.invalidateReportCache();
    }
}
