package hudson.plugins.clover;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.URL;
import java.util.Scanner;

public class DeclarativePipelineTest {
    // BuildWatcher echoes job output to stderr as it arrives
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    private String resourceAsString(String resourcePath) throws Exception {
        String results = "";
        URL cloverXML = getClass().getResource(resourcePath);
        try (Scanner scanner = new Scanner(cloverXML.openStream(), "UTF-8")) {
            scanner.useDelimiter("\\A");
            results = scanner.hasNext() ? scanner.next() : "";
        }
        return results;
    }

    @Test
    public void declarativePipeline() throws Exception {
        final WorkflowRun run = runPipeline(m(
                "pipeline {",
                "  agent any",
                "  stages {",
                "    stage('Hello World') {",
                "      steps {",
                "        echo('hello' + ' from' + ' echo')",
                "        writeFile file: 'clover.xml', ",
                "                  text: '''",
                resourceAsString("/hudson/plugins/clover/clover-declarative.xml"),
                "'''",
                "        clover(cloverReportDir: '.', cloverReportFileName: 'clover.xml',\n\n" +
                "            healthyTarget: [methodCoverage: 20 + 1, conditionalCoverage: 20 + 7, statementCoverage: 20 + 3],\n" +
                "            unhealthyTarget: [methodCoverage: 10 + 1, conditionalCoverage: 10 + 7, statementCoverage: 10 + 3],\n" +
                "            failingTarget: [methodCoverage: 1, conditionalCoverage: 7, statementCoverage: 3])\n" +
                "      }",
                "    }",
                "  }",
                "}"));

        r.assertBuildStatusSuccess(run);
        r.assertLogContains("hello from echo", run); // Trivial check
        r.assertLogContains("Publishing Clover coverage results...", run); // Actual check
    }

    /**
     * Run a pipeline job synchronously.
     *
     * @param definition the pipeline job definition
     * @return the started job
     */
    private WorkflowRun runPipeline(String definition) throws Exception {
        final WorkflowJob project = r.createProject(WorkflowJob.class, "example");
        project.setDefinition(new CpsFlowDefinition(definition, true));
        final WorkflowRun workflowRun = project.scheduleBuild2(0).waitForStart();
        r.waitForCompletion(workflowRun);
        return workflowRun;
    }

    /**
     * Approximates a multiline string in Java.
     *
     * @param lines the lines to concatenate with a newline separator
     * @return the concatenated multiline string
     */
    private static String m(String... lines) {
        return String.join("\n", lines);
    }
}
