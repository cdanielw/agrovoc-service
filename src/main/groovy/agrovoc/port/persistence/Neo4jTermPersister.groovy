package agrovoc.port.persistence

import agrovoc.dto.Term
import agrovoc.dto.TermLinks
import org.neo4j.graphdb.*

import static org.neo4j.graphdb.Direction.INCOMING
import static org.neo4j.graphdb.Direction.OUTGOING

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

    void persistLinks(TermLinks links) {
        validateLinks(links)
        graphDb.transact {
            def startNode = nodeFinder.getTermByCode(links.startTermCode)
            startNode.getRelationships(OUTGOING)*.delete()
            links.each { long endTermCode, int linkType ->
                def endNode = nodeFinder.getTermByCode(endTermCode)
                startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(linkType as String))
            }
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
        nodeFinder.findTermByCode(term.code) ?: graphDb.createNode()
    }

    private void updateDescriptionNodes(Node termNode, Term term) {
        removeDescriptions(termNode)
        term.labelByLanguage.each { language, label ->
            Node descriptionNode = createTermDescriptionNode(termNode, term, language)
            indexTermDescription(descriptionNode, term, language)
        }
    }

    private void removeDescriptions(Node termNode) {
        termNode.getRelationships(DESCRIBES, INCOMING).each { rel ->
            def descriptionNode = rel.startNode
            rel.delete()
            nodeFinder.accessIndex().remove(descriptionNode)
            descriptionNode.delete()
        }
    }

    private Node createTermDescriptionNode(Node termNode, Term term, String language) {
        def termDescriptionNode = graphDb.createNode()
        termDescriptionNode['language'] = language
        termDescriptionNode['label'] = term.labelByLanguage[language]
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

    private void validateLinks(TermLinks links) {
        assert links.startTermCode
        links.each { endTermCode, type ->
            assert endTermCode
            assert type
        }
    }
}
