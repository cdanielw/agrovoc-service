package agrovoc.setup

import javax.ws.rs.core.Application
/**
 * @author Daniel Wiell
 */
class JaxrsApplication extends Application {
    final Configuration configuration

    JaxrsApplication() {
        configuration = ConfigurationHolder.configuration
    }

    Set<Object> getSingletons() {
        configuration.jaxRsResources.instances
    }
}
