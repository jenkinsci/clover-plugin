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

    public void testDecoratinLauncher() throws IOException
    {
        TaskListener listener = new LogTaskListener(Logger.getLogger(getName()), Level.ALL);
        Launcher outer = new Launcher.LocalLauncher(listener);
        CIOptions.Builder options = new CIOptions.Builder(); 
        CloverBuildWrapper.CloverDecoratingLauncher cloverLauncher = new CloverBuildWrapper.CloverDecoratingLauncher(outer, options, "MYLICENSESTRING");

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
        starter.masks(new boolean[starter.cmds().size()] );
        cloverLauncher.decorateArgs(starter);
        int i = 0;
        assertEquals("cmd.exe", starter.cmds().get(i++));
        assertEquals("/C", starter.cmds().get(i++));
        assertEquals("ant.bat", starter.cmds().get(i++));
        assertEquals("clover.fullclean", starter.cmds().get(i++));
    }

}
