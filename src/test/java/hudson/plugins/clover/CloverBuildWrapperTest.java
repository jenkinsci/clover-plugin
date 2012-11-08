package hudson.plugins.clover;

import junit.framework.TestCase;
import hudson.util.LogTaskListener;
import hudson.Launcher;
import hudson.Proc;
import hudson.FilePath;
import hudson.remoting.Channel;
import hudson.model.TaskListener;

import java.util.List;
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

    public void testDecoratinLauncher() throws IOException
    {
        TaskListener listener = new LogTaskListener(Logger.getLogger(getName()), Level.ALL);
        Launcher outer = new Launcher.LocalLauncher(listener);
        CIOptions.Builder options = new CIOptions.Builder();
        CloverBuildWrapper wrapper = new CloverBuildWrapper(true, true, "FOO");
        CloverBuildWrapper.CloverDecoratingLauncher cloverLauncher = new CloverBuildWrapper.CloverDecoratingLauncher(wrapper, outer, options, "MYLICENSESTRING");

        Launcher.ProcStarter starter = new Launcher(cloverLauncher) {
            public Proc launch(ProcStarter starter) throws IOException {
                return null;
            }

            public Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String, String> envVars) throws IOException, InterruptedException {
                return null;
            }


            public void kill(Map<String, String> modelEnvVars) throws IOException, InterruptedException { }
        }.launch();
        
        starter.cmds("cmd.exe", "/C", "\"ant.bat clean test.run    &&  exit %%ERRORLEVEL%%\"");
        starter.pwd("target");
        List<String> cmds = starter.cmds();
        starter.masks(new boolean[cmds.size()]);
        cloverLauncher.decorateArgs(starter);

        cmds = starter.cmds();
        int i = 0;
        assertEquals("cmd.exe", cmds.get(i++));
        assertEquals("/C", cmds.get(i++));
        assertEquals("ant.bat", cmds.get(i++));
        assertEquals("clover.fullclean", cmds.get(i++));
    }

}
