package hudson.plugins.clover;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.util.LogTaskListener;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.openclover.ci.AntIntegrationListener;
import org.openclover.ci.CIOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hudson.plugins.clover.CloverBuildWrapper.CloverDecoratingLauncher.isAnt;
import static hudson.plugins.clover.CloverBuildWrapper.CloverDecoratingLauncher.isAntBat;
import static hudson.plugins.clover.CloverBuildWrapper.CloverDecoratingLauncher.isCmdExe;
import static hudson.plugins.clover.CloverBuildWrapper.CloverDecoratingLauncher.splitArgumentsIntoPreUserPost;
import static hudson.plugins.clover.CloverBuildWrapper.CloverDecoratingLauncher.trimDoubleQuotes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CloverBuildWrapperTest {

    @Test
    public void testTrimDoubleQuotes() {
        assertThat(trimDoubleQuotes.apply(null), nullValue());
        assertThat(trimDoubleQuotes.apply(""), equalTo(""));
        assertThat(trimDoubleQuotes.apply("\"abc"), equalTo("abc"));
        assertThat(trimDoubleQuotes.apply("abc\""), equalTo("abc"));
        assertThat(trimDoubleQuotes.apply("abc"), equalTo("abc"));
        assertThat(trimDoubleQuotes.apply("\"abc\"def\""), equalTo("abc\"def"));
    }

    @Test
    public void testIsCmdExe() {
        assertThat(isCmdExe(Arrays.asList("c:\\windows\\cmd.exe", "echo")), is(false));
        assertThat(isCmdExe(Arrays.asList("c:\\windows\\cmd.exe", "/c")), is(false));
        assertThat(isCmdExe(Arrays.asList("c:\\windows\\cmd.exe", "/C")), is(true));
        assertThat(isCmdExe(Arrays.asList("c:\\windows\\cmd.exe", "/C", "echo")), is(true));
        assertThat(isCmdExe(Arrays.asList("cmd.exe", "/C", "echo")), is(true));
    }

    @Test
    public void testIsAntBat() {
        assertThat(isAntBat(Arrays.asList("c:\\ant\\ant.bat", "echo")), is(true));
        assertThat(isAntBat(Arrays.asList("ant.bat", "echo")), is(true));
        assertThat(isAntBat(Arrays.asList("/usr/bin/ant", "echo")), is(false));
        assertThat(isAntBat(Arrays.asList("ant", "echo")), is(false));
    }

    @Test
    public void testIsAnt() {
        assertThat(isAnt(Arrays.asList("c:\\ant\\ant.bat", "echo")), is(false));
        assertThat(isAnt(Arrays.asList("ant.bat", "echo")), is(false));
        assertThat(isAnt(Arrays.asList("/usr/bin/ant", "echo")), is(true));
        assertThat(isAnt(Arrays.asList("ant", "echo")), is(true));
    }

    @Test
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

    @Test
    public void testDecoratingLauncherOnWindowsOldJenkins() throws IOException {
        TaskListener listener = new LogTaskListener(Logger.getLogger("testDecoratingLauncherOnWindowsOldJenkins"), Level.ALL);
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

    @Test
    public void testDecoratingLauncherOnWindowsNewJenkins() throws IOException {
        TaskListener listener = new LogTaskListener(Logger.getLogger("testDecoratingLauncherOnWindowsNewJenkins"), Level.ALL);
        Launcher outer = new Launcher.LocalLauncher(listener);
        CIOptions.Builder options = new CIOptions.Builder();
        CloverBuildWrapper wrapper = new CloverBuildWrapper(true, true, null, false);
        CloverBuildWrapper.CloverDecoratingLauncher cloverLauncher = new CloverBuildWrapper.CloverDecoratingLauncher(wrapper, null, outer, options);

        Launcher.ProcStarter starter = new DummyLauncher(cloverLauncher).launch();

        starter.cmds("cmd.exe", "/C", "\"ant.bat", "clean", "test", "&&", "exit", "%%ERRORLEVEL%%\"");
        starter.pwd("target");
        List<String> commandArgs = starter.cmds();
        starter.masks(new boolean[commandArgs.size()]);
        cloverLauncher.decorateArgs(starter);

        commandArgs = starter.cmds();

        assertThat(commandArgs.get(0), equalTo("cmd.exe"));
        assertThat(commandArgs.get(1), equalTo("/C"));
        assertThat(commandArgs.get(2), equalTo("ant.bat"));
        assertThat(commandArgs.get(3), equalTo("clean"));
        assertThat(commandArgs.get(4), equalTo("test"));

        assertThat(commandArgs.get(6), equalTo("-listener"));
        assertThat(commandArgs.get(7), equalTo(AntIntegrationListener.class.getName()));
        assertThat(commandArgs.get(8), equalTo("-lib"));
        assertThat(commandArgs.get(9), containsString("clover.jar"));

        assertThat(commandArgs.get(10), equalTo("&&"));
        assertThat(commandArgs.get(11), equalTo("exit"));
        assertThat(commandArgs.get(12), equalTo("%%ERRORLEVEL%%"));
    }

    @Test
    public void testDecoratingLauncherOnLinux() throws IOException {
        TaskListener listener = new LogTaskListener(Logger.getLogger("testDecoratingLauncherOnLinux"), Level.ALL);
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

        public Proc launch(@NotNull Launcher.ProcStarter starter) {
            return null;
        }

        public Channel launchChannel(@NotNull String[] cmd, @NotNull OutputStream out,
                                     FilePath workDir, @NotNull Map<String, String> envVars) {
            return null;
        }

        public void kill(Map<String, String> modelEnvVars) {
        }
    }
}
