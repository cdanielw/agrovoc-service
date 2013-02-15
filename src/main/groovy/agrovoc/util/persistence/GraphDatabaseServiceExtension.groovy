package agrovoc.util.persistence
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
/**
 * @author Daniel Wiell
 */
class GraphDatabaseServiceExtension {

    static Node createNode(GraphDatabaseService self, Map props) {
        self.createNode()
    }

    static <T> T transact(GraphDatabaseService self, Closure<T> unitOfWork) {
        T result = null
        def tx = self.beginTx()
        try {
            result = unitOfWork.call(tx)
            tx.success()
        } finally {
            tx.finish()
        }
        return result
    }
}
