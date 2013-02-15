package agrovoc.fake

import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server

/**
 * @author Daniel Wiell
 */
class WebServer {

    private final Server server

    WebServer(int port, Handler handler) {
        server = new Server(port)
        server.stopAtShutdown = true
        server.setHandler(handler)
    }

    void start() { server.start() }

    void stop() { server.stop() }
}
