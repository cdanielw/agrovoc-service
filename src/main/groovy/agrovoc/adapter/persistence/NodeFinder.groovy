package agrovoc.adapter.persistence

import agrovoc.exception.NotFoundException
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery
import org.neo4j.graphdb.*
import org.neo4j.graphdb.index.Index
import org.neo4j.index.lucene.QueryContext

import static org.neo4j.graphdb.Direction.INCOMING
import static org.neo4j.graphdb.Direction.OUTGOING

/**
 * @author Daniel Wiell
 */
class NodeFinder {
    static final RelationshipType DESCRIBES = DynamicRelationshipType.withName('DESCRIBES')
    private static final String ENGLISH = 'EN'
    private static final String FRENCH = 'FR'
    private static final String SPANISH = 'ES'

    private final GraphDatabaseService graphDb

    NodeFinder(GraphDatabaseService graphDb) {
        this.graphDb = graphDb
    }

    Node findTerm(Node termDescription) {
        termDescription.getSingleRelationship(DESCRIBES, OUTGOING).endNode
    }

    Node getTermDescription(Node term, String language) {
        def termDescription = findTermDescription(term, language)
        if (!termDescription)
            throw new NotFoundException("No description found for term with code ${term['code']}")
        return termDescription
    }

    Node findTermDescription(Node term, String language) {
        def relationship = term.getRelationships(DESCRIBES, INCOMING).find {
            it.startNode['language'] == language
        } as Relationship
        return relationship?.startNode ?: findFallbackTermDescription(term)
    }

    private Node findFallbackTermDescription(Node term) {
        term.getRelationships(DESCRIBES, INCOMING)
                .collect { it.startNode }.sort { getLanguageSort(it) }.first()
    }

    private String getLanguageSort(Node termDescription) {
        def language = termDescription['language']
        if (language == ENGLISH) return "1"
        if (language == FRENCH) return "2"
        if (language == SPANISH) return "3"
        return language
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

    List<Node> findTermsByCode(Collection<Long> codes) {
        def terms = accessIndex()
        def query = new BooleanQuery()
        codes.each { code ->
            query.add(new TermQuery(new Term('code', code as String)), BooleanClause.Occur.SHOULD)
        }
        terms.query(new QueryContext(query)).collect { it }
    }

    Index<Node> accessIndex() {
        graphDb.index().forNodes('terms')
    }
}
