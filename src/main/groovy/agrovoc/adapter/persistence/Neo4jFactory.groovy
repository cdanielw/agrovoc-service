package agrovoc.adapter.persistence

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.index.IndexManager

/**
 * @author Daniel Wiell
 */
interface Neo4jFactory {
    GraphDatabaseService create()
}

class EmbeddedNeo4jFactory implements Neo4jFactory {
    public static final String CONFIGURATION_FILE = '/agrovoc.properties'

    GraphDatabaseService create() {
        def configUrl = getClass().getResource(CONFIGURATION_FILE)
        if (!configUrl) throw new IllegalStateException("Cannot find configuration file in classpath: $CONFIGURATION_FILE")
        def config = new Properties()
        config.load(getClass().getResourceAsStream(CONFIGURATION_FILE))
        def path = config.getProperty('neo4j.path')
        if (!path) throw new IllegalStateException("neo4j.path not configured in $CONFIGURATION_FILE")
        def database = new GraphDatabaseFactory().
                newEmbeddedDatabaseBuilder(path).
                loadPropertiesFromURL(configUrl).
                newGraphDatabase()
        configureIndex(database)
        return database
    }

    static void configureIndex(GraphDatabaseService graphDb) {
        def index = graphDb.index()
        index.forNodes('terms', [(IndexManager.PROVIDER): 'lucene', type: 'exact'])
//        index.forNodes('termDescriptions', [
//                (IndexManager.PROVIDER): 'lucene',
//                type: 'fulltext',
//                analyzer: 'org.apache.lucene.analysis.SimpleAnalyzer'])
        index.forNodes('termDescriptions', [(IndexManager.PROVIDER): 'lucene', type: 'fulltext'])
    }
}