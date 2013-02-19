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
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.IndexHits
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.index.lucene.QueryContext
import org.neo4j.kernel.Uniqueness

import static agrovoc.adapter.persistence.StatusType.*
import static org.apache.lucene.search.BooleanClause.Occur.MUST
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT
import static org.neo4j.graphdb.Direction.OUTGOING
import static org.neo4j.kernel.Traversal.traversal

/**
 * @author Daniel Wiell
 */
class Neo4jTermRepository implements TermRepository {
    public static final TraversalDescription BROADER_TRAVERSAL = traversal(Uniqueness.NODE_GLOBAL)
            .relationships(LinkType.SUBCLASS_OF.type, OUTGOING)
            .relationships(LinkType.INCLUDED_IN.type, OUTGOING)
            .relationships(LinkType.HAS_SYNONYM.type, OUTGOING)
            .relationships(LinkType.HAS_NEAR_SYNONYM.type, OUTGOING)
            .relationships(LinkType.HAS_BROARDER_SYNONYM.type, OUTGOING)
            .relationships(LinkType.IS_ACRONYM_OF.type, OUTGOING)
            .relationships(LinkType.HAS_ACRONYM.type, OUTGOING)
            .relationships(LinkType.IS_ABBREVIATION_OF.type, OUTGOING)
            .relationships(LinkType.HAS_ABBREVIATION.type, OUTGOING)

    public static final TraversalDescription NARROWER_TRAVERSAL = traversal(Uniqueness.NODE_GLOBAL)
            .relationships(LinkType.HAS_SUBCLASS.type, OUTGOING)
            .relationships(LinkType.INCLUDES.type, OUTGOING)
            .relationships(LinkType.HAS_SYNONYM.type, OUTGOING)
            .relationships(LinkType.HAS_NEAR_SYNONYM.type, OUTGOING)
            .relationships(LinkType.HAS_BROARDER_SYNONYM.type, OUTGOING)
            .relationships(LinkType.IS_ACRONYM_OF.type, OUTGOING)
            .relationships(LinkType.HAS_ACRONYM.type, OUTGOING)
            .relationships(LinkType.IS_ABBREVIATION_OF.type, OUTGOING)
            .relationships(LinkType.HAS_ABBREVIATION.type, OUTGOING)

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
        excludeStatusTypes(booleanQuery, DELETED, TOP_TERM_DESCRIPTOR, PROPOSED_DESCRIPTOR, NOT_ACCEPTED)
        booleanQuery.add(labelRestriction, MUST)

        def termDescriptions = graphDb.index().forNodes('termDescriptions')
        def hits = termDescriptions.query(new QueryContext(booleanQuery).sort('label_e'))
        toTerms(hits, query.max)
    }

    private void excludeStatusTypes(BooleanQuery booleanQuery, StatusType... statusTypes) {
        statusTypes.each {
            booleanQuery.add(new TermQuery(new Term('status_e', it.idString)), MUST_NOT)
        }
    }

    List<Map<String, Object>> findAllBroaderTerms(long code, String language) {
        return findRelatedTerms(code, language, BROADER_TRAVERSAL)
    }

    private ArrayList findRelatedTerms(long code, String language, TraversalDescription traversal) {
        def terms = []
        def startTerm = nodes.getTermByCode(code)

        traversal.traverse(startTerm).each { path ->
            def last = path.lastRelationship()
            if (!last) return null
            def endTerm = path.lastRelationship().endNode
            def endTermDescription = nodes.findTermDescription(endTerm, language)
            if (endTermDescription && endTermDescription['status'] == DESCRIPTOR.id)
                terms << toTerm(endTerm, endTermDescription)
        }
        return terms
    }

    List<Map<String, Object>> findAllNarrowerTerms(long code, String language) {
        return findRelatedTerms(code, language, NARROWER_TRAVERSAL)
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
