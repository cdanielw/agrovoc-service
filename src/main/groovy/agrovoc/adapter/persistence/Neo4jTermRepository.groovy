package agrovoc.adapter.persistence
import agrovoc.dto.LabelQuery
import agrovoc.port.persistence.NodeFinder
import agrovoc.port.persistence.TermRepository
import org.apache.lucene.index.Term
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.WildcardQuery
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.IndexHits
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.index.lucene.QueryContext
import org.neo4j.kernel.Uniqueness

import static org.apache.lucene.search.BooleanClause.Occur.MUST
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT
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

    List<Map<String, Object>> findAllWhereWordInLabelStartsWith(LabelQuery query) {
        def booleanQuery = new BooleanQuery()
        def queryTerms = query.string.toLowerCase(new Locale(query.language)).split()
        queryTerms.each {
            booleanQuery.add(new WildcardQuery(new Term('label', QueryParser.escape(it) + '*')), MUST)
        }

        execute(query, booleanQuery)
    }

    Map<String, Object> findByLabel(String label, String language) {
        TermQuery query = new TermQuery(new Term('label_e', label.toLowerCase(new Locale(language))))
        def hits = execute(new LabelQuery(label, language, 1), query)
        hits.empty ? null : hits.first()
    }

    List<Map<String, Object>> findAllWhereLabelStartsWith(LabelQuery query) {
        def string = query.string.toLowerCase(new Locale(query.language))
        execute(query, new WildcardQuery(new Term('label_e', QueryParser.escape(string) + '*')))
    }

    private List<Map<String, Object>> execute(LabelQuery query, Query labelRestriction) {
        def booleanQuery = new BooleanQuery()
        booleanQuery.add(new TermQuery(new Term('language_e', query.language.toUpperCase())), MUST)
        booleanQuery.add(new TermQuery(new Term('status_e', '0')), MUST_NOT)
        booleanQuery.add(new TermQuery(new Term('status_e', '60')), MUST_NOT)
        booleanQuery.add(new TermQuery(new Term('status_e', '100')), MUST_NOT)
        booleanQuery.add(new TermQuery(new Term('status_e', '120')), MUST_NOT)
        booleanQuery.add(labelRestriction, MUST)

        def termDescriptions = graphDb.index().forNodes('termDescriptions')
        def hits = termDescriptions.query(new QueryContext(booleanQuery).sort('label_e'))
        toTerms(hits, query.max)
    }

    List<Map<String, Object>> getLinksByCode(long code, String language) {
        def startNode = nodes.getTermByCode(code)
        def startTerm = toTerm(startNode, nodes.getTermDescription(startNode, language))
        startNode.getRelationships(OUTGOING).findAll { relationship ->
            nodes.findTermDescription(relationship.endNode, language)
        }.collect { relationship ->
            def endNode = relationship.endNode
            def endTerm = toTerm(endNode, nodes.getTermDescription(endNode, language))
            [type: relationship.type.name(), start: startTerm, end: endTerm]
        }
    }

    private List<Map<String, Object>> toTerms(IndexHits<Node> hits, int max) {
        hits.iterator().take(max).collect { Node termDescriptionNode ->
            def termNode = nodes.findTerm(termDescriptionNode)
            toTerm(termNode, termDescriptionNode)
        }
    }

    private Map<String, Object> toTerm(Node termNode, Node termDescriptionNode) {
        def result = termNode.toMap()
        result.putAll([language: termDescriptionNode['language'], label: termDescriptionNode['label'], status: termDescriptionNode['status']])
        return result
    }
}
