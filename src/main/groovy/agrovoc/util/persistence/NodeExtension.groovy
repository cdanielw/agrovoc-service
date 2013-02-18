package agrovoc.util.persistence

import org.neo4j.graphdb.Node

/**
 * @author Daniel Wiell
 */
class NodeExtension {
    static Node putAt(Node node, String key, Object value) {
        if (value == null)
            node.removeProperty(key)
        else
            node.setProperty(key, value)
        return node
    }

    static Object getAt(Node node, String key) {
        node.getProperty(key)
    }

    static Map toMap(Node node) {
        def map = [:]
        node.getPropertyKeys().each {
            map[it] = node.getProperty(it)
        }
        return map
    }
}
