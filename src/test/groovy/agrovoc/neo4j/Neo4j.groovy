package agrovoc.neo4j

import agrovoc.adapter.persistence.EmbeddedNeo4jFactory
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.test.TestGraphDatabaseFactory

/**
 * @author Daniel Wiell
 */
class Neo4j {
    Neo4j() {
        reset()
    }

    void reset() {
        def graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase()
        EmbeddedNeo4jFactory.configureIndex(graphDb)
        TestNeo4jFactory.SERVICE_PROXY.database = graphDb
    }

    GraphDatabaseService getGraphDb() { TestNeo4jFactory.SERVICE_PROXY.database }

    void stop() {
        TestNeo4jFactory.SERVICE_PROXY.shutdown()
    }
}

