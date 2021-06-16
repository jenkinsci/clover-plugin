package hudson.plugins.clover;

import hudson.plugins.clover.results.ClassCoverage;
import hudson.plugins.clover.results.FileCoverage;
import hudson.plugins.clover.results.PackageCoverage;
import hudson.plugins.clover.results.ProjectCoverage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;


public class CloverCoverageParser {

    /** Do not instantiate CloverCoverageParser. */
    private CloverCoverageParser() {
    }

    public static ProjectCoverage trimPaths(ProjectCoverage result, String pathPrefix) {
        if (result == null) throw new NullPointerException();
        if (pathPrefix == null) return result;
        for (PackageCoverage p: result.getPackageCoverages()) {
            for (FileCoverage f: p.getFileCoverages()) {
                if (f.getName().startsWith(pathPrefix)) {
                    f.setName(f.getName().substring(pathPrefix.length()));
                }
                f.setName(f.getName().replace('\\', '/'));
                for (ClassCoverage c: f.getClassCoverages()) {
                    c.setName(p.getName() + "." + c.getName());
                }
            }
        }
        return result;
    }

    public static ProjectCoverage parse(File inFile, String pathPrefix) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(inFile);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            return trimPaths(parse(bufferedInputStream), pathPrefix);
        }
    }

    private static Digester createDigester(boolean secure) throws SAXException {
        Digester digester = new Digester();
        if (secure) {
            digester.setXIncludeAware(false);
            try {
                digester.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                digester.setFeature("http://xml.org/sax/features/external-general-entities", false);
                digester.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                digester.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException ex) {
                throw new SAXException("Failed to securely configure xml digester parser", ex);
            }
        }
        return digester;
    }

    public static ProjectCoverage parse(InputStream in) throws IOException {
        if (in == null) throw new NullPointerException();
        try {
            boolean secure = (!Boolean.getBoolean(CloverCoverageParser.class.getName() + ".UNSAFE"));
            Digester digester = createDigester(secure);
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

            return (ProjectCoverage) digester.parse(in);
        } catch (SAXException e) {
            throw new IOException("Cannot parse coverage results", e);
        }
    }
}
