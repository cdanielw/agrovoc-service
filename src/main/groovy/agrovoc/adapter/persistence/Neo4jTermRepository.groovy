package agrovoc.adapter.persistence

import agrovoc.exception.NotFoundException
import agrovoc.port.persistence.TermRepository
import org.apache.lucene.index.Term
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.WildcardQuery
import org.neo4j.graphalgo.GraphAlgoFactory
import org.neo4j.graphdb.*
import org.neo4j.graphdb.index.IndexHits
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.helpers.Predicate
import org.neo4j.index.lucene.QueryContext
import org.neo4j.kernel.Uniqueness

import static org.apache.lucene.search.BooleanClause.Occur.MUST
import static org.neo4j.graphdb.Direction.INCOMING
import static org.neo4j.graphdb.Direction.OUTGOING
import static org.neo4j.kernel.Traversal.traversal

/**
 * @author Daniel Wiell
 */
class Neo4jTermRepository implements TermRepository {
    private static final RelationshipType DESCRIBES = DynamicRelationshipType.withName('DESCRIBES')
    public static final String ENGLISH = 'EN'
    public static final TraversalDescription RELATED_TRAVERSAL = traversal(Uniqueness.NODE_GLOBAL)
            .relationships(DynamicRelationshipType.withName('50'), OUTGOING)
    private final GraphDatabaseService graphDb

    Neo4jTermRepository(GraphDatabaseService graphDb) {
        this.graphDb = graphDb
    }

    Map<String, Object> getByCode(long code, String language) {
        Node termNode = getTermNodeByCode(code)
        def termDescriptionNode = getTermDescriptionNode(termNode, language)
        toTerm(termNode, termDescriptionNode)
    }

    List<Map<String, Object>> queryByLabel(String label, String language) {
        def termDescriptions = graphDb.index().forNodes('termDescriptions')
        def query = createByLabelQuery(label, language)
        def hits = termDescriptions.query(new QueryContext(query).sort('label'))
        toTerms(hits)
    }

    List<Map<String, Object>> getLinksByCode(long code, String language) {
        def startNode = getTermNodeByCode(code)
        test(startNode)
        def startTerm = toTerm(startNode, getTermDescriptionNode(startNode, language))
        startNode.getRelationships(OUTGOING).findAll { relationship ->
            findTermDescriptionNode(relationship.endNode, language)
        }.collect { relationship ->
            def endNode = relationship.endNode
            def endTerm = toTerm(endNode, getTermDescriptionNode(endNode, language))
            [type: relationship.type.name(), start: startTerm, end: endTerm]
        }
    }

    private List<Map<String, Object>> toTerms(IndexHits<Node> hits) {
        hits.iterator().take(20).collect { Node termDescriptionNode ->
            def termNode = findTermNode(termDescriptionNode)
            toTerm(termNode, termDescriptionNode)
        }
    }

//    private String createByLabelQuery(String label, String language) {
//        def queryTerms = label.toLowerCase().split()
//        def query = new StringBuilder()
//        query.append('+status:20 +language:').append(QueryParser.escape(language.toLowerCase()))
//        queryTerms.each {
//            query.append(' +label:').append(QueryParser.escape(it)).append('*')
//        }
//        return query as String
//    }

    private Query createByLabelQuery(String label, String language) {
        def queryTerms = label.toLowerCase().split()
        def query = new BooleanQuery()
        query.add(new TermQuery(new Term('language', language.toLowerCase())), MUST)
        query.add(new TermQuery(new Term('status', '20')), MUST)
        queryTerms.each {
            query.add(new WildcardQuery(new Term('label', QueryParser.escape(it) + '*')), MUST)
        }
        return query
    }


    void persistTerm(Map<String, Object> term) {
        validateTerm(term)
        graphDb.transact {
            def termNode = findOrCreateTermNode(term)
            updateTerm(termNode, term)
            def termDescriptionNode = findOrCreateTermDescriptionNode(termNode, term)
            updateTermDescription(termDescriptionNode, term)
        }
    }

    void persistLink(Map<String, Object> link) {
        validateLink(link)
        graphDb.transact {
            def startNode = findTermNodeByCode(link.start as Long)
            def endNode = findTermNodeByCode(link.end as Long)
            // TODO: Need to remove existing links
//            startNode.getRelationships(OUTGOING).each {
//                it.delete()
//            }
            startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(link.type as String))
        }
    }

    private void updateTermDescription(Node termDescriptionNode, Map<String, Object> term) {
        termDescriptionNode['label'] = term.label
        indexTermDescription(termDescriptionNode, term)
    }

    private Node findOrCreateTermNode(Map<String, Object> term) {
        findTermNodeByCode(term.code as Long) ?: graphDb.createNode()
    }

    private Node findOrCreateTermDescriptionNode(Node termNode, Map<String, Object> term) {
        findTermDescriptionNode(termNode, term.language as String) ?:
            createTermDescriptionNode(termNode, term)
    }

    private Node createTermDescriptionNode(Node termNode, Map<String, Object> term) {
        def termDescriptionNode = graphDb.createNode()
        termDescriptionNode['language'] = term.language
        termDescriptionNode.createRelationshipTo(termNode, DESCRIBES)
        return termDescriptionNode
    }

    private void indexTermDescription(Node termDescriptionNode, Map<String, Object> term) {
        def termDescriptions = graphDb.index().forNodes('termDescriptions')
        termDescriptions.with {
            remove(termDescriptionNode)
            add(termDescriptionNode, 'code', term.code)
            add(termDescriptionNode, 'language', term.language)
            add(termDescriptionNode, 'status', term.status)
            add(termDescriptionNode, 'scope', term.scope)
            add(termDescriptionNode, 'label', term.label)
        }
    }

    Long getLastChanged() {
        def node = findLastChangedNode()
        node ? node['lastChanged'] as long : null

    }

    void setLastChanged(Long lastChange) {
        assert lastChange != null
        graphDb.transact {
            def node = findLastChangedNode()
            if (!node)
                node = graphDb.createNode()
            node['type'] = 'timestamp'
            node['lastChanged'] = lastChange
            graphDb.index().forNodes('timestamp').add(node, 'type', 'timestamp')
        }
    }


    private Node findTermNode(Node termDescriptionNode) {
        termDescriptionNode.getSingleRelationship(DESCRIBES, OUTGOING).endNode
    }

    private Map<String, Object> toTerm(Node termNode, Node termDescriptionNode) {
        def result = termNode.toMap()
        result.putAll([language: termDescriptionNode['language'], label: termDescriptionNode['label']])
        return result
    }

    private Node getTermDescriptionNode(Node termNode, String language) {
        def node = findTermDescriptionNode(termNode, language)
        if (!node)
            throw new NotFoundException("Term ${termNode['code']} has no description in English")
        return node
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

    private void updateTerm(Node termNode, Map<String, Object> term) {
        termNode['code'] = term.code
        termNode['status'] = term.status
        termNode['scope'] = term.scope
        def terms = graphDb.index().forNodes('terms')
        terms.add(termNode, 'code', term.code)
    }

    private Node getTermNodeByCode(long code) {
        def node = findTermNodeByCode(code)
        if (!node)
            throw new NotFoundException("Term ${code} not found")
        return node
    }

    private Node findTermNodeByCode(long code) {
        def terms = graphDb.index().forNodes('terms')
        terms.get('code', code).single
    }

    private Node findLastChangedNode() {
        def hits = graphDb.index().forNodes('timestamp').get('type', 'timestamp')
        if (hits.size() == 0) return null
        def node = hits.next()
        hits.close()
        return node
    }

    private void validateTerm(Map<String, Object> term) {
        assert term.code
        assert term.language
        assert term.status
        assert term.scope != null
        assert term.label
    }


    private void validateLink(Map<String, Object> link) {
        assert link.start
        assert link.end
        assert link.type
    }

    void test(Node startNode) {
        long time = System.currentTimeMillis()
        RELATED_TRAVERSAL.traverse(startNode).each { path ->
            def last = path.lastRelationship()
            if (!last) return null
            def rel = path.lastRelationship().type?.name()
            def endNode = path.lastRelationship().endNode
            def label = findTermDescriptionNode(endNode, 'EN')?.getProperty('label')
            println "${' ' * path.length()}$rel: $label"
        }
        println 'Time: ' + (System.currentTimeMillis() - time)
    }


    def expander = org.neo4j.kernel.Traversal.expanderForTypes(
            DynamicRelationshipType.withName('90'), Direction.OUTGOING,
            DynamicRelationshipType.withName('60'), Direction.OUTGOING,
            DynamicRelationshipType.withName('50'), Direction.OUTGOING,
            DynamicRelationshipType.withName('70'), Direction.OUTGOING,
            DynamicRelationshipType.withName('20'), Direction.OUTGOING)
            .addNodeFilter(new Predicate<Node>() {
        boolean accept(Node item) {
            item['status'] == 20
        }
    })

    void shortestPath(long startCode, long endCode) {
        long time = System.currentTimeMillis()
        def start = getTermNodeByCode(startCode)
        def end = getTermNodeByCode(endCode)

        def pathFinder = GraphAlgoFactory.shortestPath(expander, 100)
        pathFinder.findSinglePath(start, end).nodes().each { node ->
            def label = findTermDescriptionNode(node, 'EN')?.getProperty('label')
            println label
        }
        println 'Time: ' + (System.currentTimeMillis() - time)
    }
}
