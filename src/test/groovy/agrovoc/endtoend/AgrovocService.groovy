package agrovoc.endtoend

import agrovoc.dto.Term
import agrovoc.dto.TermLinks
import agrovoc.fake.AgrovocDatabase
import agrovoc.fake.WebServer
import agrovoc.neo4j.Neo4j
import agrovoc.neo4j.TestNeo4jFactory
import agrovoc.port.event.TermEventPublisher
import agrovoc.setup.ConfigurationHolder
import agrovoc.setup.SystemConfiguration
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.webapp.WebAppContext
import spock.util.concurrent.BlockingVariable

import javax.naming.InitialContext
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Daniel Wiell
 */
class AgrovocService {
    static final int PORT = 8980
    static final String CONTEXT_PATH = '/agrovoc'
    static final String BASE_URI = "http://localhost:$PORT$CONTEXT_PATH"

    private static AgrovocDatabase agrovocDb
    private static WebServer webServer
    private static Boolean started
    private static Neo4j neo4j = new Neo4j()

    void init() {
        doInit()
    }

    void stop() {
        neo4j.stop()
    }

    Term createTerm(Term term) {
        insertAndWaitUntilCreated([term], [])
        return term
    }

    List<Term> createTerms(List<Term> terms, List<TermLinks> links = []) {
        insertAndWaitUntilCreated(terms, links)
        return terms
    }

    private void insertAndWaitUntilCreated(List<Term> terms, List<TermLinks> links) {
        def created = new BlockingVariable<Boolean>(2, TimeUnit.SECONDS)
        def linked = new BlockingVariable<Boolean>(2, TimeUnit.SECONDS)
        def createCount = new AtomicInteger()
        def linkCount = new AtomicInteger()
        def eventPublisher = ConfigurationHolder.configuration.services[TermEventPublisher]
        eventPublisher.registerCreateListener {
            if (createCount.incrementAndGet() >= terms.size())
                created.set(true)
        }
        eventPublisher.registerLinkListener {
            if (linkCount.incrementAndGet() >= links.size())
                linked.set(true)
        }
        terms.each { agrovocDb.insertTerm(it) }
        links.each { agrovocDb.insertLink(it) }
        if (terms) created.get()
        if (links) linked.get()
    }

    private synchronized void doInit() {
        if (!started) {
            start()
            addShutdownHook()
        }
        agrovocDb.reset()
        neo4j.reset()
    }

    private void start() {
        started = true
        System.setProperty('agrovoc.Neo4jFactory', TestNeo4jFactory.class.name)
        System.setProperty('agrovoc.cron', '* * * * * ?')
        ConfigurationHolder.configuration = new SystemConfiguration()
        agrovocDb = new AgrovocDatabase()
        webServer = new WebServer(PORT, createHandler())
        webServer.start()
    }

    private void addShutdownHook() {
        Runtime.addShutdownHook {
            webServer.stop()
        }
    }

    private Handler createHandler() {
        WebAppContext webapp = new WebAppContext()
        webapp.setWar("src/main/webapp")
        webapp.setContextPath(CONTEXT_PATH)
        webapp.addEventListener(new ServletContextListener() {
            void contextInitialized(ServletContextEvent sce) {
                def ctx = new InitialContext()
                ctx.createSubcontext("java:/comp/env")
                ctx.createSubcontext("java:/comp/env/jdbc")
                ctx.bind(SystemConfiguration.JNDI_AGROVOC_DATA_SOURCE, agrovocDb.dataSource)
            }

            void contextDestroyed(ServletContextEvent sce) {
            }
        } as EventListener)

        return webapp
    }
}
