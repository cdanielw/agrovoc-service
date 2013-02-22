package agrovoc.adapter.persistence
import agrovoc.dto.ByCodeQuery
import agrovoc.dto.ByLabelQuery
import agrovoc.dto.RelationshipQuery
import agrovoc.dto.TermDescription
import agrovoc.exception.NotFoundException
import agrovoc.port.persistence.TermRepository
import org.apache.lucene.index.Term
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.WildcardQuery
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Path
import org.neo4j.graphdb.index.IndexHits
import org.neo4j.graphdb.traversal.Evaluation
import org.neo4j.graphdb.traversal.Evaluator
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.index.lucene.QueryContext
import org.neo4j.kernel.Uniqueness

import static agrovoc.adapter.persistence.StatusType.*
import static agrovoc.dto.ByLabelQuery.Match.*
import static org.apache.lucene.search.BooleanClause.Occur.MUST
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT
import static org.neo4j.graphdb.traversal.Evaluation.EXCLUDE_AND_PRUNE
import static org.neo4j.graphdb.traversal.Evaluation.INCLUDE_AND_CONTINUE
import static org.neo4j.kernel.Traversal.traversal
/**
 * @author Daniel Wiell
 */
class Neo4jTermRepository implements TermRepository {
    private final GraphDatabaseService graphDb
    private final NodeFinder nodeFinder

    Neo4jTermRepository(GraphDatabaseService graphDb) {
        this.graphDb = graphDb
        nodeFinder = new NodeFinder(graphDb)
    }

    List<TermDescription> findAllByCode(ByCodeQuery query) {
        List<Node> nodes = nodeFinder.findTermsByCode(query.codes)
        if (nodes.size() != query.codes.size())
            throw new NotFoundException("Could not find terms with codes ${findMissingCodes(query.codes, nodes)}")
        nodes.collect { Node node ->
            def termDescriptionNode = nodeFinder.getTermDescription(node, query.language)
            toTermDescription(node, termDescriptionNode)
        }
    }

    List<TermDescription> findAllByLabel(ByLabelQuery query) {
        Query labelRestriction = createLabelRestriction(query)
        def booleanQuery = new BooleanQuery()
        booleanQuery.add(new TermQuery(new Term('language_e', query.language)), MUST)
        excludeStatusTypesFromQuery(booleanQuery, DELETED, TOP_TERM_DESCRIPTOR, PROPOSED_DESCRIPTOR, NOT_ACCEPTED)
        booleanQuery.add(labelRestriction, MUST)

        def termDescriptions = graphDb.index().forNodes('termDescriptions')
        def hits = termDescriptions.query(new QueryContext(booleanQuery).sort('label_e'))
        def descriptionNodes = extractDescriptionNodes(query, hits)
        return toTermDescriptions(descriptionNodes)
    }

    List<TermDescription> findRelatedTerms(RelationshipQuery query) {
        def relatedTerms = [] as List<TermDescription>
        TraversalDescription traversal = createRelationshipsTraversal(relatedTerms, query)
        collectRelatedTerms(relatedTerms, query, traversal)
        return relatedTerms
    }

    private void collectRelatedTerms(List<TermDescription> collectedRelatedTerms, RelationshipQuery query, TraversalDescription traversal) {
        def startTerm = nodeFinder.getTermByCode(query.code)
        traversal.traverse(startTerm).each { path ->
            def last = path.lastRelationship()
            if (!last) return null
            def endTerm = path.lastRelationship().endNode
            def endTermDescription = nodeFinder.findTermDescription(endTerm, query.language)
            if (shouldTermDescriptionBeIncludedInRelatedTerms(endTermDescription, query))
                collectedRelatedTerms << toTermDescription(endTerm, endTermDescription)
        }
    }

    private boolean shouldTermDescriptionBeIncludedInRelatedTerms(Node endTermDescription, RelationshipQuery query) {
        endTermDescription &&
                endTermDescription['status'] == DESCRIPTOR.id &&
                endTermDescription['language'] == query.language
    }

    private TraversalDescription createRelationshipsTraversal(List<TermDescription> collectedRelatedTerms, RelationshipQuery query) {
        def traversal = traversal(Uniqueness.NODE_GLOBAL).breadthFirst().evaluator(
                new Evaluator() {
                    Evaluation evaluate(Path path) {
                        collectedRelatedTerms.size() < query.max ? INCLUDE_AND_CONTINUE : EXCLUDE_AND_PRUNE
                    }
                })
        LinkType.neo4jTypesFor(query.relationshipTypes).each {
            traversal = traversal.relationships(it, Direction.OUTGOING)
        }
        return traversal
    }

    private Query createLabelRestriction(ByLabelQuery query) {
        switch (query.match) {
            case exact: return createExactLabelRestriction(query)
            case startsWith: return createStartsWithLabelRestriction(query)
            case freeText: return createFreeTextLabelRestriction(query)
        }
    }

    private Query createExactLabelRestriction(ByLabelQuery query) {
        new TermQuery(new Term('label_e', query.toLowerCase()))
    }

    private Query createStartsWithLabelRestriction(ByLabelQuery query) {
        new WildcardQuery(new Term('label_e', QueryParser.escape(query.toLowerCase()) + '*'))
    }

    private Query createFreeTextLabelRestriction(ByLabelQuery query) {
        def booleanQuery = new BooleanQuery()
        def queryTerms = query.toLowerCase().split()
        queryTerms.each {
            booleanQuery.add(new WildcardQuery(new Term('label', QueryParser.escape(it) + '*')), MUST)
        }
        return booleanQuery
    }

    private List<Node> extractDescriptionNodes(ByLabelQuery query, IndexHits<Node> hits) {
        Iterator<Node> it = hits.iterator()
        def nodes = [] as List<Node>
        while (it.hasNext() && nodes.size() < query.max) {
            def descriptionNode = it.next();
            if (shouldIncludeInResult(descriptionNode, query))
                nodes << descriptionNode
        }
        return nodes
    }

    private boolean shouldIncludeInResult(Node descriptionNode, ByLabelQuery query) {
        if (descriptionNode['status'] == TermDescription.PREFERRED_STATUS) return true
        def termNode = nodeFinder.findTerm(descriptionNode)
        def neo4jTypes = LinkType.neo4jTypesFor(query.relationshipTypes)
        termNode.getRelationships(neo4jTypes).any {
            def endNode = it.endNode
            endNode.getRelationships(Direction.INCOMING, NodeFinder.DESCRIBES).any {
                it.startNode['status'] == TermDescription.PREFERRED_STATUS
            }
        }
    }

    private void excludeStatusTypesFromQuery(BooleanQuery booleanQuery, StatusType... statusTypes) {
        statusTypes.each {
            booleanQuery.add(new TermQuery(new Term('status_e', it.idString)), MUST_NOT)
        }
    }

    private List<Long> findMissingCodes(List<Long> expectedCodes, List<Node> termNodes) {
        def codes = termNodes.collect { it['code'] as Long }
        expectedCodes.findAll { !codes.contains(it) }
    }

    private List<TermDescription> toTermDescriptions(List<Node> termDescriptionNodes) {
        termDescriptionNodes.collect { Node termDescriptionNode ->
            def termNode = nodeFinder.findTerm(termDescriptionNode)
            toTermDescription(termNode, termDescriptionNode)
        }
    }

    private TermDescription toTermDescription(Node termNode, Node termDescriptionNode) {
        new TermDescription(
                termNode['code'] as long,
                termDescriptionNode['status'] as int, termDescriptionNode['label'] as String,
                termDescriptionNode['language'] as String
        )
    }
}
