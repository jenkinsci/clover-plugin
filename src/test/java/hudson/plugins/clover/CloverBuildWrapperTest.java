package hudson.plugins.clover;

import org.openclover.ci.AntIntegrationListener;
import junit.framework.TestCase;
import hudson.util.LogTaskListener;
import hudson.Launcher;
import hudson.Proc;
import hudson.FilePath;
import hudson.remoting.Channel;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.io.IOException;
import java.io.OutputStream;

import com.atlassian.clover.api.ci.CIOptions;

import static hudson.plugins.clover.CloverBuildWrapper.CloverDecoratingLauncher.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CloverBuildWrapperTest extends TestCase {

    public void testTrimDoubleQuotes() {
        assertThat(trimDoubleQuotes.apply(null), nullValue());
        assertThat(trimDoubleQuotes.apply(""), equalTo(""));
        assertThat(trimDoubleQuotes.apply("\"abc"), equalTo("abc"));
        assertThat(trimDoubleQuotes.apply("abc\""), equalTo("abc"));
        assertThat(trimDoubleQuotes.apply("abc"), equalTo("abc"));
        assertThat(trimDoubleQuotes.apply("\"abc\"def\""), equalTo("abc\"def"));
    }

    public void testIsCmdExe() {
        assertThat(isCmdExe(Arrays.asList("c:\\windows\\cmd.exe", "echo")), is(false));
        assertThat(isCmdExe(Arrays.asList("c:\\windows\\cmd.exe", "/c")), is(false));
        assertThat(isCmdExe(Arrays.asList("c:\\windows\\cmd.exe", "/C")), is(true));
        assertThat(isCmdExe(Arrays.asList("c:\\windows\\cmd.exe", "/C", "echo")), is(true));
        assertThat(isCmdExe(Arrays.asList("cmd.exe", "/C", "echo")), is(true));
    }

    public void testIsAntBat() {
        assertThat(isAntBat(Arrays.asList("c:\\ant\\ant.bat", "echo")), is(true));
        assertThat(isAntBat(Arrays.asList("ant.bat", "echo")), is(true));
        assertThat(isAntBat(Arrays.asList("/usr/bin/ant", "echo")), is(false));
        assertThat(isAntBat(Arrays.asList("ant", "echo")), is(false));
    }

    public void testIsAnt() {
        assertThat(isAnt(Arrays.asList("c:\\ant\\ant.bat", "echo")), is(false));
        assertThat(isAnt(Arrays.asList("ant.bat", "echo")), is(false));
        assertThat(isAnt(Arrays.asList("/usr/bin/ant", "echo")), is(true));
        assertThat(isAnt(Arrays.asList("ant", "echo")), is(true));
    }

    public void testSplitArgumentsIntoPreUserPost() {
        List<String> pre = new ArrayList<>();
        List<String> user = new ArrayList<>();
        List<String> post = new ArrayList<>();

        splitArgumentsIntoPreUserPost(Arrays.asList("ant", "clean", "test", "&&", "exit", "1"),
                pre, user, post, true);
        
        assertThat(pre, contains("ant"));
        assertThat(user, contains("clean", "test"));
        assertThat(post, contains("&&", "exit", "1"));
    }

    public void testDecoratingLauncherOnWindowsOldJenkins() throws IOException {
        TaskListener listener = new LogTaskListener(Logger.getLogger(getName()), Level.ALL);
        Launcher outer = new Launcher.LocalLauncher(listener);
        CIOptions.Builder options = new CIOptions.Builder();
        CloverBuildWrapper wrapper = new CloverBuildWrapper(true, true, null, false);
        CloverBuildWrapper.CloverDecoratingLauncher cloverLauncher = new CloverBuildWrapper.CloverDecoratingLauncher(wrapper, null, outer, options);

        Launcher.ProcStarter starter = new DummyLauncher(cloverLauncher).launch();

        starter.cmds("cmd.exe", "/C", "\"ant.bat clean test  && exit %%ERRORLEVEL%%\"");
        starter.pwd("target");
        List<String> cmds = starter.cmds();
        starter.masks(new boolean[cmds.size()]);
        cloverLauncher.decorateArgs(starter);

        cmds = starter.cmds();

        assertThat(cmds.get(0), equalTo("cmd.exe"));
        assertThat(cmds.get(1), equalTo("/C"));
        assertThat(cmds.get(2), equalTo("ant.bat"));
        assertThat(cmds.get(3), equalTo("clean"));
        assertThat(cmds.get(4), equalTo("test"));

        assertThat(cmds.get(7), equalTo("-listener"));
        assertThat(cmds.get(8), equalTo(AntIntegrationListener.class.getName()));
        assertThat(cmds.get(9), equalTo("-lib"));
        assertThat(cmds.get(10), containsString("clover.jar"));

        assertThat(cmds.get(11), equalTo("&&"));
        assertThat(cmds.get(12), equalTo("exit"));
        assertThat(cmds.get(13), equalTo("%%ERRORLEVEL%%"));
    }

    public void testDecoratingLauncherOnWindowsNewJenkins() throws IOException {
        TaskListener listener = new LogTaskListener(Logger.getLogger(getName()), Level.ALL);
        Launcher outer = new Launcher.LocalLauncher(listener);
        CIOptions.Builder options = new CIOptions.Builder();
        CloverBuildWrapper wrapper = new CloverBuildWrapper(true, true, null, false);
        CloverBuildWrapper.CloverDecoratingLauncher cloverLauncher = new CloverBuildWrapper.CloverDecoratingLauncher(wrapper, null, outer, options);

        Launcher.ProcStarter starter = new DummyLauncher(cloverLauncher).launch();

        starter.cmds("cmd.exe", "/C", "\"ant.bat", "clean", "test", "&&", "exit", "%%ERRORLEVEL%%\"");
        starter.pwd("target");
        List<String> cmds = starter.cmds();
        starter.masks(new boolean[cmds.size()]);
        cloverLauncher.decorateArgs(starter);

        cmds = starter.cmds();

        assertThat(cmds.get(0), equalTo("cmd.exe"));
        assertThat(cmds.get(1), equalTo("/C"));
        assertThat(cmds.get(2), equalTo("ant.bat"));
        assertThat(cmds.get(3), equalTo("clean"));
        assertThat(cmds.get(4), equalTo("test"));

        assertThat(cmds.get(6), equalTo("-listener"));
        assertThat(cmds.get(7), equalTo(AntIntegrationListener.class.getName()));
        assertThat(cmds.get(8), equalTo("-lib"));
        assertThat(cmds.get(9), containsString("clover.jar"));

        assertThat(cmds.get(10), equalTo("&&"));
        assertThat(cmds.get(11), equalTo("exit"));
        assertThat(cmds.get(12), equalTo("%%ERRORLEVEL%%"));
    }

    public void testDecoratingLauncherOnLinux() throws IOException {
        TaskListener listener = new LogTaskListener(Logger.getLogger(getName()), Level.ALL);
        Launcher outer = new Launcher.LocalLauncher(listener);
        CIOptions.Builder options = new CIOptions.Builder();
        CloverBuildWrapper wrapper = new CloverBuildWrapper(true, true, null, false);
        CloverBuildWrapper.CloverDecoratingLauncher cloverLauncher = new CloverBuildWrapper.CloverDecoratingLauncher(wrapper, null, outer, options);

        Launcher.ProcStarter starter = new DummyLauncher(cloverLauncher).launch();

        starter.cmds("/usr/bin/ant", "clean", "test");
        starter.pwd("target");
        List<String> cmds = starter.cmds();
        starter.masks(new boolean[cmds.size()]);
        cloverLauncher.decorateArgs(starter);

        cmds = starter.cmds();

        assertThat(cmds.get(0), equalTo("/usr/bin/ant"));
        assertThat(cmds.get(1), equalTo("clean"));
        assertThat(cmds.get(2), equalTo("test"));

        assertThat(cmds.get(4), equalTo("-listener"));
        assertThat(cmds.get(5), equalTo(AntIntegrationListener.class.getName()));
        assertThat(cmds.get(6), equalTo("-lib"));
        assertThat(cmds.get(7), containsString("clover.jar"));
    }

    private static class DummyLauncher extends Launcher {
        DummyLauncher(Launcher launcher) {
            super(launcher);
        }

        public Proc launch(Launcher.ProcStarter starter) {
            return null;
        }

        public Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String, String> envVars) {
            return null;
        }

        public void kill(Map<String, String> modelEnvVars) {
        }
    }
}
