package hudson.plugins.clover;

import hudson.Extension;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CloverInstallation extends ToolInstallation implements NodeSpecific<CloverInstallation> {

    @DataBoundConstructor
    public CloverInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    public static CloverInstallation forName(String name) {
        for (CloverInstallation cloverInstallation : DESCRIPTOR.getInstallations()) {
            if (name != null && name.equals(cloverInstallation.getName())) {
                return cloverInstallation;
            }
        }
        return null;
    }

    public static List<CloverInstallation> installations() {
        return Arrays.asList(DESCRIPTOR.getInstallations());
    }

    public CloverInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new CloverInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Extension
    public final static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends ToolDescriptor<CloverInstallation> {

        public DescriptorImpl() {
            super.setInstallations();
            load();
        }

        @Override
        public String getDisplayName() {
            return "Clover";
        }

        @Override
        public void setInstallations(CloverInstallation... installations) {
            super.setInstallations(installations);
            save();
        }

        @Override
        public CloverInstallation newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return (CloverInstallation) super.newInstance(req, formData.getJSONObject("cloverInstallation"));
        }
    }

}
