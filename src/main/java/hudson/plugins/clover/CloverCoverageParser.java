package hudson.plugins.clover;

import hudson.plugins.clover.results.ClassCoverage;
import hudson.plugins.clover.results.FileCoverage;
import hudson.plugins.clover.results.PackageCoverage;
import hudson.plugins.clover.results.ProjectCoverage;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 03-Jul-2007 09:03:30
 */
public class CloverCoverageParser {
    public ProjectCoverage parse(InputStream in) throws IOException {
        if (in == null) throw new NullPointerException();
        Digester digester = new Digester();
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
