package agrovoc.port.persistence

import agrovoc.dto.Term
import org.neo4j.graphdb.*

import static org.neo4j.graphdb.Direction.INCOMING

/**
 * @author Daniel Wiell
 */
class Neo4jTermPersister implements TermPersister {
    private static final RelationshipType DESCRIBES = DynamicRelationshipType.withName('DESCRIBES')
    public static final String ENGLISH = 'EN'
    private final GraphDatabaseService graphDb
    private final NodeFinder nodeFinder

    Neo4jTermPersister(GraphDatabaseService graphDb) {
        this.graphDb = graphDb
        nodeFinder = new NodeFinder(graphDb)
    }

    void persistTerm(Term term) {
        validateTerm(term)
        graphDb.transact {
            def termNode = findOrCreateTermNode(term)
            updateTerm(termNode, term)
            updateDescriptionNodes(termNode, term)
        }
    }

    void persistLink(Map<String, Object> link) {
        validateLink(link)
        graphDb.transact {
            def startNode = nodeFinder.findByCode(link.start as Long)
            def endNode = nodeFinder.findByCode(link.end as Long)
            // TODO: Need to remove existing links
            startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(link.type as String))
        }
    }

    Date getLastChanged() {
        def node = findLastChangedNode()
        node ? new Date(node['lastChanged'] as long) : null

    }


    void setLastChanged(Date lastChange) {
        assert lastChange != null
        graphDb.transact {
            def node = findLastChangedNode()
            if (!node)
                node = graphDb.createNode()
            node['type'] = 'timestamp'
            node['lastChanged'] = lastChange.time
            graphDb.index().forNodes('timestamp').add(node, 'type', 'timestamp')
        }
    }

    private Node findOrCreateTermNode(Term term) {
        nodeFinder.findByCode(term.code) ?: graphDb.createNode()
    }

    private void updateDescriptionNodes(Node termNode, Term term) {
        // TODO: Make sure languages can be removed
        term.labelByLanguage.each { language, label ->
            Node termDescriptionNode = findOrCreateTermDescriptionNode(termNode, language)
            termDescriptionNode['label'] = label
            indexTermDescription(termDescriptionNode, term, language)
        }
    }

    private Node findOrCreateTermDescriptionNode(Node termNode, String language) {
        def termDescriptionNode = findTermDescriptionNode(termNode, language) ?:
            createTermDescriptionNode(termNode, language)
        return termDescriptionNode
    }

    private Node createTermDescriptionNode(Node termNode, String language) {
        def termDescriptionNode = graphDb.createNode()
        termDescriptionNode['language'] = language
        termDescriptionNode.createRelationshipTo(termNode, DESCRIBES)
        return termDescriptionNode
    }

    private void indexTermDescription(Node termDescriptionNode, Term term, String language) {
        def termDescriptions = graphDb.index().forNodes('termDescriptions')
        termDescriptions.with {
            remove(termDescriptionNode)
            add(termDescriptionNode, 'code', term.code)
            add(termDescriptionNode, 'status', term.status)
            add(termDescriptionNode, 'scope', term.scope)
            add(termDescriptionNode, 'language', language)
            add(termDescriptionNode, 'label', term.labelByLanguage[language])
        }
    }

    private void updateTerm(Node termNode, Term term) {
        termNode['code'] = term.code
        termNode['status'] = term.status
        termNode['scope'] = term.scope
        def terms = graphDb.index().forNodes('terms')
        terms.add(termNode, 'code', term.code)
    }

    private Node findTermDescriptionNode(Node termNode, String language) {
        def relationship = termNode.getRelationships(DESCRIBES, INCOMING).find {
            it.startNode['language'] == language
        } as Relationship
        def node = relationship?.startNode
        if (node) return node
        if (language != ENGLISH)
            node = findTermDescriptionNode(termNode, ENGLISH)
        return node
    }

    private Node findLastChangedNode() {
        def hits = graphDb.index().forNodes('timestamp').get('type', 'timestamp')
        if (hits.size() == 0) return null
        def node = hits.next()
        hits.close()
        return node
    }

    private void validateTerm(Term term) {
        assert term.code
        assert term.labelByLanguage
        assert term.status
        assert term.scope != null
        assert term.labelByLanguage.each { language, label ->
            assert language
            assert label
        }
    }

    private void validateLink(Map<String, Object> link) {
        assert link.start
        assert link.end
        assert link.type
    }
}
