package hudson.plugins.clover;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Clover {@link Publisher}.
 *
 * @author Stephen Connolly
 */
public class CloverPublisher extends Publisher {

    private final String name;

    /**
     *
     * @param name
     * @stapler-constructor
     */
    public CloverPublisher(String name) {
        this.name = name;
    }

    /** We'll use this from the <tt>config.jelly</tt>. */
    public String getName() {
        return name;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Hello, " + name + "!");
        return true;
    }

    public Descriptor<Publisher> getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return DESCRIPTOR;
    }

    /** Descriptor should be singleton. */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Descriptor for {@link CloverPublisher}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * <p/>
     * <p/>
     * See <tt>views/hudson/plugins/clover/CloverPublisher/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    public static final class DescriptorImpl extends Descriptor<Publisher> {
        DescriptorImpl() {
            super(CloverPublisher.class);
        }

        /** This human readable name is used in the configuration screen. */
        public String getDisplayName() {
            return "Publish Clover Coverage Report";
        }


        public boolean configure(StaplerRequest req) throws FormException {
            req.bindParameters(this, "clover.");            
            save();
            return super.configure(req);    //To change body of overridden methods use File | Settings | File Templates.
        }

        /** Creates a new instance of {@link CloverPublisher} from a submitted form. */
        public CloverPublisher newInstance(StaplerRequest req) throws FormException {
            return req.bindParameters(CloverPublisher.class, "clover.");
        }
    }
}
