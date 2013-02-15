package agrovoc.port.persistence

import org.neo4j.graphdb.*
import org.neo4j.graphdb.index.Index
import agrovoc.exception.NotFoundException

import static org.neo4j.graphdb.Direction.INCOMING
import static org.neo4j.graphdb.Direction.OUTGOING

/**
 * @author Daniel Wiell
 */
class NodeFinder {
    private static final RelationshipType DESCRIBES = DynamicRelationshipType.withName('DESCRIBES')
    private static final String ENGLISH = 'EN'

    private final GraphDatabaseService graphDb

    NodeFinder(GraphDatabaseService graphDb) {
        this.graphDb = graphDb
    }


    Node findByCode(long code) {
        def terms = accessIndex()
        terms.get('code', code).single
    }


    Node findTerm(Node termDescription) {
        termDescription.getSingleRelationship(DESCRIBES, OUTGOING).endNode
    }

    Node getTermDescription(Node term, String language) {
        def termDescription = findTermDescription(term, language)
        if (!termDescription)
            throw new NotFoundException("Term ${term['code']} has no description in English")
        return termDescription
    }

    Node findTermDescription(Node term, String language) {
        def relationship = term.getRelationships(DESCRIBES, INCOMING).find {
            it.startNode['language'] == language
        } as Relationship
        def termDescription = relationship?.startNode
        if (termDescription) return termDescription
        if (language != ENGLISH)
            termDescription = findTermDescription(term, ENGLISH)
        return termDescription
    }

    Node getTermByCode(long code) {
        def term = findTermByCode(code)
        if (!term)
            throw new NotFoundException("Term ${code} not found")
        return term
    }

    Node findTermByCode(long code) {
        def terms = accessIndex()
        terms.get('code', code).single
    }

    private Index<Node> accessIndex() {
        graphDb.index().forNodes('terms')
    }
}
