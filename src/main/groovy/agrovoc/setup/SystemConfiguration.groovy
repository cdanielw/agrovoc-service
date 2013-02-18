package agrovoc.setup

import agrovoc.adapter.agrovoc.DataSourceAgrovocRepository
import agrovoc.adapter.cron.AgrovocTermCron
import agrovoc.adapter.event.EventLogger
import agrovoc.adapter.persistence.EmbeddedNeo4jFactory
import agrovoc.adapter.persistence.Neo4jFactory
import agrovoc.adapter.persistence.Neo4jTermRepository
import agrovoc.adapter.resource.TermResource
import agrovoc.domain.TermService
import agrovoc.port.agrovoc.AgrovocRepository
import agrovoc.port.cron.AgrovocTermPollingJob
import agrovoc.port.event.TermEventPublisher
import agrovoc.adapter.persistence.Neo4jTermPersister
import agrovoc.port.persistence.TermPersister
import agrovoc.port.persistence.TermRepository
import agrovoc.port.resource.TermProvider
import agrovoc.util.config.Resources
import org.neo4j.graphdb.GraphDatabaseService

import javax.sql.DataSource

/**
 * @author Daniel Wiell
 */
class SystemConfiguration implements Configuration {
    static final JNDI_AGROVOC_DATA_SOURCE = "java:comp/env/jdbc/AgrovocDB"
    static final String NEO4J_FACTORY_SYSTEM_PROPERTY = 'agrovoc.Neo4jFactory'
    static final String AGROVOC_CRON_SYSTEM_PROPERTY = 'agrovoc.cron'
    final Resources services = new Resources()
    final Resources jaxRsResources = new Resources()

    void configure() {
        services[DataSource] = lookupAgrovocDataSource()
        services[AgrovocRepository] = new DataSourceAgrovocRepository(services[DataSource])
        services[GraphDatabaseService] = setupGraphDatabaseService()
        services[TermPersister] = new Neo4jTermPersister(services[GraphDatabaseService])
        services[TermRepository] = new Neo4jTermRepository(services[GraphDatabaseService])
        services[[TermEventPublisher, TermProvider, AgrovocTermPollingJob]] =
            new TermService(services[TermRepository], services[TermPersister], services[AgrovocRepository])
        services[TermProvider] = services[TermService]
        services[AgrovocTermCron] = setupAgrovocTermCron()
        services[EventLogger] = setupEventLogger()
        addJaxRsResources()
    }

    private AgrovocTermCron setupAgrovocTermCron() {
        def cron = new AgrovocTermCron(services[AgrovocTermPollingJob])
        cron.cronExpression = System.getProperty(AGROVOC_CRON_SYSTEM_PROPERTY, '0 0 * * * ?')
//        cron.cronExpression = System.getProperty(AGROVOC_CRON_SYSTEM_PROPERTY, '0 * * * * ?')
        cron.start()
        return cron
    }

    GraphDatabaseService setupGraphDatabaseService() {
        def factoryType = System.getProperty(NEO4J_FACTORY_SYSTEM_PROPERTY, EmbeddedNeo4jFactory.name) as Class<Neo4jFactory>
        factoryType.newInstance().create()
    }

    private EventLogger setupEventLogger() {
        def logger = new EventLogger()
        logger.register(services[TermEventPublisher])
        return logger
    }

    private DataSource lookupAgrovocDataSource() {
        def ctx = new javax.naming.InitialContext()
        ctx.lookup(JNDI_AGROVOC_DATA_SOURCE) as DataSource
    }

    private void addJaxRsResources() {
        jaxRsResources.addAll([
                new TermResource(services[TermProvider])
        ])
    }

    void destroy() {
        services[AgrovocTermCron].stop()
        services[GraphDatabaseService].shutdown()
    }
}
