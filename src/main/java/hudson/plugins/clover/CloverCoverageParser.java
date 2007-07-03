package hudson.plugins.clover;

import hudson.plugins.clover.results.ClassCoverage;
import hudson.plugins.clover.results.FileCoverage;
import hudson.plugins.clover.results.PackageCoverage;
import hudson.plugins.clover.results.ProjectCoverage;
import hudson.Util;
import hudson.model.Result;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 03-Jul-2007 09:03:30
 */
public class CloverCoverageParser {

    /** Do not instantiate CloverCoverageParser. */
    private CloverCoverageParser() {
    }

    public static ProjectCoverage parse(File inFile) throws IOException {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            fileInputStream = new FileInputStream(inFile);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            CloverCoverageParser parser = new CloverCoverageParser();
            return parse(bufferedInputStream);
        } finally {
            try {
                if (bufferedInputStream != null)
                    bufferedInputStream.close();
                if (fileInputStream != null)
                    fileInputStream.close();
            } catch (IOException e) {
            }
        }
    }

    public static ProjectCoverage parse(InputStream in) throws IOException {
        if (in == null) throw new NullPointerException();
        Digester digester = new Digester();
        digester.setClassLoader(CloverCoverageParser.class.getClassLoader());
        digester.addObjectCreate("coverage/project", ProjectCoverage.class);
        digester.addSetProperties("coverage/project");
        digester.addSetProperties("coverage/project/metrics");

        digester.addObjectCreate("coverage/project/package", PackageCoverage.class);
        digester.addSetProperties("coverage/project/package");
        digester.addSetProperties("coverage/project/package/metrics");
        digester.addSetNext("coverage/project/package", "addPackageCoverage", PackageCoverage.class.getName());

        digester.addObjectCreate("coverage/project/package/file", FileCoverage.class);
        digester.addSetProperties("coverage/project/package/file");
        digester.addSetProperties("coverage/project/package/file/metrics");
        digester.addSetNext("coverage/project/package/file", "addFileCoverage", FileCoverage.class.getName());

        digester.addObjectCreate("coverage/project/package/file/class", ClassCoverage.class);
        digester.addSetProperties("coverage/project/package/file/class");
        digester.addSetProperties("coverage/project/package/file/class/metrics");
        digester.addSetNext("coverage/project/package/file/class", "addClassCoverage", ClassCoverage.class.getName());

        try {
            return (ProjectCoverage) digester.parse(in);
        } catch (SAXException e) {
            throw new IOException("Cannot parse coverage results", e);
        }
    }
}
