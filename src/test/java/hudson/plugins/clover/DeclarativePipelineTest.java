package hudson.plugins.clover;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

public class DeclarativePipelineTest {
    // BuildWatcher echoes job output to stderr as it arrives
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void smokes() throws Exception {
        final WorkflowRun run = runPipeline(m(
                "pipeline {",
                "  agent any",
                "  stages {",
                "    stage('Hello World') {",
                "      steps {",
                "        echo('hello' + ' from' + ' echo')",
                "        writeFile file: 'clover.xml', text: '''",
                "<coverage generated=\"1183450300322\" clover=\"1.3.13\">",
                "   <project timestamp=\"1183450141125\" name=\"Maven Cloverreport\">",
                "      <metrics classes=\"5\" methods=\"10\" conditionals=\"2\" files=\"4\" packages=\"1\" coveredstatements=\"2\" loc=\"137\" ncloc=\"70\" coveredmethods=\"1\" coveredconditionals=\"1\" statements=\"18\" coveredelements=\"4\" elements=\"30\"/>",
                "      <package name=\"hudson.plugins.clover\">",
                "         <metrics conditionals=\"2\" methods=\"10\" classes=\"5\" files=\"4\" ncloc=\"70\" coveredstatements=\"2\" coveredmethods=\"1\" coveredconditionals=\"1\" statements=\"18\" loc=\"137\" coveredelements=\"4\" elements=\"30\"/>",
                "         <file name=\"CloverPublisher.java\">",
                "            <class name=\"CloverPublisher\">",
                "               <metrics conditionals=\"0\" methods=\"4\" coveredstatements=\"0\" coveredmethods=\"0\" coveredconditionals=\"0\" statements=\"5\" coveredelements=\"0\" elements=\"9\"/>",
                "            </class>",
                "            <class name=\"CloverPublisher.DescriptorImpl\">",
                "               <metrics conditionals=\"0\" methods=\"4\" coveredstatements=\"0\" coveredmethods=\"0\" coveredconditionals=\"0\" statements=\"6\" coveredelements=\"0\" elements=\"10\"/>",
                "            </class>",
                "            <metrics conditionals=\"0\" methods=\"8\" classes=\"2\" ncloc=\"42\" coveredstatements=\"0\" coveredmethods=\"0\" coveredconditionals=\"0\" statements=\"11\" loc=\"79\" coveredelements=\"0\" elements=\"19\"/>",
                "            <line num=\"27\" count=\"0\" type=\"method\"/>",
                "         </file>",
                "      </package>",
                "   </project>",
                "</coverage>",
                "'''",
                "        clover(cloverReportDir: '.', cloverReportFileName: 'clover.xml',\n" +
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
