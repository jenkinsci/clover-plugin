package hudson.plugins.clover;

import junit.framework.TestCase;
import hudson.util.LogTaskListener;
import hudson.Launcher;
import hudson.Proc;
import hudson.FilePath;
import hudson.remoting.Channel;
import hudson.model.TaskListener;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.io.IOException;
import java.io.OutputStream;

import com.atlassian.clover.api.ci.CIOptions;

/**
 */
public class CloverBuildWrapperTest extends TestCase
{

    private TaskListener listener;
    private Launcher launcher;
    private CIOptions.Builder options;
    private CloverBuildWrapper.CloverDecoratingLauncher cloverLauncher;
    private Launcher.ProcStarter starter;

    public void setUp() {
        listener = new LogTaskListener(Logger.getLogger(getName()), Level.ALL);
        launcher = new Launcher.LocalLauncher(listener);
        options = new CIOptions.Builder();

        cloverLauncher = new CloverBuildWrapper.CloverDecoratingLauncher(launcher, options, "MYLICENSESTRING");
        starter = new Launcher(cloverLauncher) {
            public Proc launch(ProcStarter starter) throws IOException {
                return null;
            }

            public Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String, String> envVars) throws IOException, InterruptedException {
                return null;
            }


            public void kill(Map<String, String> modelEnvVars) throws IOException, InterruptedException { }
        }.launch();
    }

    public void testDecoratinLauncher() throws IOException
    {
        starter.cmds("cmd.exe", "/C", "\"ant.bat clean test.run    &&  exit %%ERRORLEVEL%%\"");
        starter.pwd("target");
        starter.masks(new boolean[starter.cmds().size()]);
        cloverLauncher.decorateArgs(starter);
        int i = 0;
        assertEquals("cmd.exe", starter.cmds().get(i++));
        assertEquals("/C", starter.cmds().get(i++));
        assertEquals("ant.bat", starter.cmds().get(i++));
        assertEquals("clover.fullclean", starter.cmds().get(i++));
    }

    public void testDecoratinLauncherWithSpacesAndQuotes() throws IOException
    {
        starter.cmds("cmd.exe", "/C", "'\"\"c:\\Program Files\\apache-ant-1.8.2\\bin\\ant.bat\" -file build.xml clean test.run && exit %%ERRORLEVEL%%\"'");
        starter.pwd("target");
        starter.masks(new boolean[starter.cmds().size()]);
        cloverLauncher.decorateArgs(starter);
        int i = 0;
        assertEquals("cmd.exe", starter.cmds().get(i++));
        assertEquals("/C", starter.cmds().get(i++));
        assertEquals("\"c:\\Program Files\\apache-ant-1.8.2\\bin\\ant.bat\"", starter.cmds().get(i++));
        assertEquals("clover.fullclean", starter.cmds().get(i++));
    }

}
