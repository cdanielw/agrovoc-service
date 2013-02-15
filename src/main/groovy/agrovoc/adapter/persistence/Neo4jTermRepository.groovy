package agrovoc.adapter.persistence

import agrovoc.port.persistence.NodeFinder
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
import static org.neo4j.graphdb.Direction.OUTGOING
import static org.neo4j.kernel.Traversal.traversal
/**
 * @author Daniel Wiell
 */
class Neo4jTermRepository implements TermRepository {
    public static final TraversalDescription RELATED_TRAVERSAL = traversal(Uniqueness.NODE_GLOBAL)
            .relationships(DynamicRelationshipType.withName('50'), OUTGOING)
    private final GraphDatabaseService graphDb
    private final NodeFinder nodes

    Neo4jTermRepository(GraphDatabaseService graphDb) {
        this.graphDb = graphDb
        nodes = new NodeFinder(graphDb)
    }

    Map<String, Object> getByCode(long code, String language) {
        Node termNode = nodes.getTermByCode(code)
        def termDescriptionNode = nodes.getTermDescription(termNode, language)
        toTerm(termNode, termDescriptionNode)
    }

    List<Map<String, Object>> queryByLabel(String label, String language) {
        def termDescriptions = graphDb.index().forNodes('termDescriptions')
        def query = createByLabelQuery(label, language)
        def hits = termDescriptions.query(new QueryContext(query).sort('label'))
        toTerms(hits)
    }

    List<Map<String, Object>> getLinksByCode(long code, String language) {
        def startNode = nodes.getTermByCode(code)
        test(startNode)
        def startTerm = toTerm(startNode, nodes.getTermDescription(startNode, language))
        startNode.getRelationships(OUTGOING).findAll { relationship ->
            nodes.findTermDescription(relationship.endNode, language)
        }.collect { relationship ->
            def endNode = relationship.endNode
            def endTerm = toTerm(endNode, nodes.getTermDescription(endNode, language))
            [type: relationship.type.name(), start: startTerm, end: endTerm]
        }
    }

    private List<Map<String, Object>> toTerms(IndexHits<Node> hits) {
        hits.iterator().take(20).collect { Node termDescriptionNode ->
            def termNode = nodes.findTerm(termDescriptionNode)
            toTerm(termNode, termDescriptionNode)
        }
    }

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


    private Map<String, Object> toTerm(Node termNode, Node termDescriptionNode) {
        def result = termNode.toMap()
        result.putAll([language: termDescriptionNode['language'], label: termDescriptionNode['label']])
        return result
    }

    void test(Node startNode) {
        long time = System.currentTimeMillis()
        RELATED_TRAVERSAL.traverse(startNode).each { path ->
            def last = path.lastRelationship()
            if (!last) return null
            def rel = path.lastRelationship().type?.name()
            def endNode = path.lastRelationship().endNode
            def label = nodes.findTermDescription(endNode, 'EN')?.getProperty('label')
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
        def start = nodes.getTermByCode(startCode)
        def end = nodes.getTermByCode(endCode)

        def pathFinder = GraphAlgoFactory.shortestPath(expander, 100)
        pathFinder.findSinglePath(start, end).nodes().each { node ->
            def label = nodes.findTermDescription(node, 'EN')?.getProperty('label')
            println label
        }
        println 'Time: ' + (System.currentTimeMillis() - time)
    }
}
