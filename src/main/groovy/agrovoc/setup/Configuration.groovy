package agrovoc.setup

import agrovoc.util.config.Resources

/**
 * @author Daniel Wiell
 */
interface Configuration {
    void configure()

    Resources getServices()

    Resources getJaxRsResources()

    void destroy()
}

class ConfigurationHolder {
    private static Configuration config

    static synchronized Configuration getConfiguration() {
        return config
    }

    static synchronized Configuration init() {
        if (config == null)
            configuration = new SystemConfiguration()
        return config
    }

    static synchronized void setConfiguration(Configuration configuration) {
        config = configuration
    }

}


