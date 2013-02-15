package agrovoc.setup

import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

/**
 * @author Daniel Wiell
 */
class ConfigServletContextListener implements ServletContextListener {
    void contextInitialized(ServletContextEvent servletContextEvent) {
        ConfigurationHolder.init().configure()
    }

    void contextDestroyed(ServletContextEvent servletContextEvent) {
        ConfigurationHolder.configuration.destroy()
    }
}
