package agrovoc.neo4j

import agrovoc.adapter.persistence.Neo4jFactory
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.event.KernelEventHandler
import org.neo4j.graphdb.event.TransactionEventHandler
import org.neo4j.graphdb.index.IndexManager
import org.neo4j.graphdb.Node

/**
 * @author Daniel Wiell
 */
@SuppressWarnings("GrDeprecatedAPIUsage")
class TestNeo4jFactory implements Neo4jFactory {
    static final SERVICE_PROXY = new GraphDatabaseServiceProxy()

    GraphDatabaseService create() { SERVICE_PROXY }

    private static class GraphDatabaseServiceProxy implements GraphDatabaseService {
        private GraphDatabaseService graphDb

        synchronized GraphDatabaseService getDatabase() {
            return graphDb
        }

        synchronized void setDatabase(GraphDatabaseService service) {
            this.graphDb = service
        }

        Node createNode() {
            graphDb.createNode()
        }

        Node getNodeById(long id) {
            graphDb.getNodeById(id)
        }

        Relationship getRelationshipById(long id) {
            graphDb.getRelationshipById(id)
        }

        Node getReferenceNode() {
            graphDb.getReferenceNode()
        }

        Iterable<Node> getAllNodes() {
            graphDb.getAllNodes()
        }

        Iterable<RelationshipType> getRelationshipTypes() {
            graphDb.getRelationshipTypes()
        }

        void shutdown() {
            graphDb.shutdown()
        }

        Transaction beginTx() {
            graphDb.beginTx()
        }

        def <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> handler) {
            graphDb.registerTransactionEventHandler(handler)
        }

        def <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> handler) {
            graphDb.unregisterTransactionEventHandler(handler)
        }

        KernelEventHandler registerKernelEventHandler(KernelEventHandler handler) {
            graphDb.registerKernelEventHandler(handler)
        }

        KernelEventHandler unregisterKernelEventHandler(KernelEventHandler handler) {
            graphDb.unregisterKernelEventHandler(handler)
        }

        IndexManager index() {
            graphDb.index()
        }
    }
}
