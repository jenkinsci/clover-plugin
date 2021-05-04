package hudson.plugins.clover.slave;

import hudson.remoting.VirtualChannel;
import jenkins.SlaveToMasterFileCallable;

import java.io.File;
import java.io.IOException;

public class GetPathFileCallable extends SlaveToMasterFileCallable<String> {
    public String invoke(File file, VirtualChannel virtualChannel) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
