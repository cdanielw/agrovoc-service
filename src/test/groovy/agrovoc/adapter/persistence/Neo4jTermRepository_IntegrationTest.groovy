package agrovoc.adapter.persistence

import agrovoc.dto.*
import agrovoc.exception.NotFoundException
import agrovoc.neo4j.Neo4j
import spock.lang.Specification

import static agrovoc.dto.ByLabelQuery.Match.*
import static agrovoc.dto.RelationshipType.broader
import static agrovoc.dto.RelationshipType.synonym
import static agrovoc.dto.TermDescription.getPREFERRED_STATUS

/**
 * @author Daniel Wiell
 */
@SuppressWarnings("GroovyUnusedAssignment")
class Neo4jTermRepository_IntegrationTest extends Specification {
    int nonPreferredStatus = 10
    int max = 20
    def neo4j = new Neo4j()
    def repository = new Neo4jTermRepository(neo4j.graphDb)
    def persister = new Neo4jTermPersister(neo4j.graphDb)

    def cleanup() { neo4j.stop() }

    def 'Given A, when finding by code of A, then description of A is returned'() {
        def a = persistTerm 'A'

        when:
        def result = repository.findAllByCode(byCodeQuery([a.code]))

        then:
        result?.size() == 1
        result.first().label == 'A'
    }

    def 'Given A and B, when finding by code of A and B, then description of A and B is returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456

        when:
        def result = repository.findAllByCode(byCodeQuery([a.code, b.code]))

        then:
        result?.size() == 2
        result.find { it.label == 'A' }
        result.find { it.label == 'B' }
    }

    def 'Given A and B, when finding by code of A, then description of B is not returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456

        when:
        def result = repository.findAllByCode(byCodeQuery([a.code]))

        then:
        result?.size() == 1
        result.first().label != 'B'
    }

    def 'Given code of non-existing term, when finding by code, then NotFoundException is thrown'() {
        def nonExistingCode = 123

        when: repository.findAllByCode(byCodeQuery([nonExistingCode]))
        then: thrown NotFoundException
    }

    def 'Given A, when finding by exact label of A, then description of A is returned'() {
        def a = persistTerm 'A', 123

        when:
        def result = repository.findAllByLabel(byLabelQuery('A', exact))

        then:
        result?.size() == 1
        result.first().code == a.code
    }

    def 'Given A is labeled "Label A", when finding terms starting "Lab", then description of A is returned'() {
        def a = persistTerm 'Label A', 123

        when:
        def result = repository.findAllByLabel(byLabelQuery('Lab', startsWith))

        then:
        result?.size() == 1
        result.first().code == a.code
    }

    def 'Given A is labeled "A Label", when finding terms starting "Lab", then description of A is not returned'() {
        def a = persistTerm 'A Label', 123

        when:
        def result = repository.findAllByLabel(byLabelQuery('Lab', startsWith))

        then:
        result?.size() == 0
    }

    def 'Given A is labeled "A Label", when doing free text search for "Lab", then description of A is returned'() {
        def a = persistTerm 'A Label', 123

        when:
        def result = repository.findAllByLabel(byLabelQuery('Lab', freeText))

        then:
        result?.size() == 1
        result.first().code == a.code
    }

    def 'When finding terms by label start, then returned descriptions are sorted by title'() {
        persistTerm 'Term c label', 1
        persistTerm 'Term A label', 2
        persistTerm 'Term b label', 3
        persistTerm 'Term E label', 4
        persistTerm 'Term d label', 5

        when:
        def result = repository.findAllByLabel(byLabelQuery('Ter', startsWith))

        then:
        result.collect { it.label.substring(5, 6).toUpperCase() } == ['A', 'B', 'C', 'D', 'E']
    }

    def 'When doing free text search, then returned descriptions are sorted by title'() {
        persistTerm 'Term c label', 1
        persistTerm 'Term A label', 2
        persistTerm 'Term b label', 3
        persistTerm 'Term E label', 4
        persistTerm 'Term d label', 5

        when:
        def result = repository.findAllByLabel(byLabelQuery('Label', freeText))

        then:
        result.collect { it.label.substring(5, 6).toUpperCase() } == ['A', 'B', 'C', 'D', 'E']
    }

    def 'Given term with label "AbC", when searching for exact label "aBc" , description of term is returned'() {
        persistTerm 'AbC', 123

        when:
        def result = repository.findAllByLabel(byLabelQuery('aBc', exact))

        then:
        !result.empty
    }

    def 'Given term with label "AbC", when finding terms starting with "aBc" , description of term is returned'() {
        persistTerm 'AbC', 123

        when:
        def result = repository.findAllByLabel(byLabelQuery('aBc', startsWith))

        then:
        !result.empty
    }

    def 'Given term with label "AbC", when doing free text search with "aBc" , description of term is returned'() {
        persistTerm 'AbC', 123

        when:
        def result = repository.findAllByLabel(byLabelQuery('aBc', freeText))

        then:
        !result.empty
    }

    def 'Given A is non-preferred, when finding by label without relations, then A is not returned'() {
        persistTerm 'A', 123, 'EN', nonPreferredStatus

        when:
        def result = repository.findAllByLabel(byLabelQuery('A', exact, [], max))

        then:
        result?.size() == 0
    }

    def 'Given A is non-preferred and synonym to B, when finding by label, including synonyms, then A is returned'() {
        def a = persistTerm 'A', 123, 'EN', nonPreferredStatus
        def b = persistTerm 'B', 456
        persistLink a, b, synonym

        when:
        def result = repository.findAllByLabel(byLabelQuery('A', exact, [synonym], max))

        then:
        result?.size() == 1
        result.first().code == a.code
    }

    def 'Given A, B with labels starting with "Label", when finding max 1 labels starting with "Label", then A is returned'() {
        def a = persistTerm 'Label A', 123
        def b = persistTerm 'Label B', 456
        def c = persistTerm 'Label C', 789

        when:
        def result = repository.findAllByLabel(byLabelQuery('Label', startsWith, [], 1))

        then:
        result?.size() == 1
        result.first().code == a.code
    }

    def 'Given A, B with labels starting with "Label", when finding max 1 labels containing text "Label", then A is returned'() {
        def a = persistTerm 'Label A', 123
        def b = persistTerm 'Label B', 456
        def c = persistTerm 'Label C', 789

        when:
        def result = repository.findAllByLabel(byLabelQuery('Label', freeText, [], 1))

        then:
        result?.size() == 1
        result.first().code == a.code
    }

    def 'Given A, B with labels starting with "Label" and A is non-preferred, when finding max 1 labels starting with "Label", then B is returned'() {
        def a = persistTerm 'Label A', 123, 'EN', nonPreferredStatus
        def b = persistTerm 'Label B', 456
        def c = persistTerm 'Label C', 789

        when:
        def result = repository.findAllByLabel(byLabelQuery('Label', startsWith, [], 1))

        then:
        result?.size() == 1
        result.first().code == b.code
    }

    def 'Given term A in FR and B in EN, both with label "Label", when finding by exact label in FR, only A'() {
        def a = persistTerm 'label', 123, 'FR'
        def b = persistTerm 'Label', 456, 'EN'

        when:
        def result = repository.findAllByLabel(byLabelQuery('Label', exact, [], max, 'FR'))

        then:
        result?.size() == 1
        result.first().code == a.code
    }

    def 'Given A in EN and not in FR, when finding by code in FR, then description of A in EN is returned'() {
        def a = persistTerm 'label', 123, 'EN'

        when:
        def result = repository.findAllByCode(byCodeQuery([a.code], 'FR'))

        then:
        result?.size() == 1
        result.first().language == 'EN'
    }

    def 'Given A in AR and not in FR, when finding by code in FR, then description of A in AR is returned'() {
        def a = persistTerm 'label', 123, 'AR'

        when:
        def result = repository.findAllByCode(byCodeQuery([a.code], 'FR'))

        then:
        result?.size() == 1
        result.first().language == 'AR'
    }

    def 'Given A in ES and AR, when finding by code in FR, then description of A in ES is returned'() {
        def a = persistTerm 'label in AR', 123, 'AR'
        a.descriptionByLanguage['ES'] = new TermDescription(123, PREFERRED_STATUS, 'label in ES', 'ES')
        persister.persistTerm(a)

        when:
        def result = repository.findAllByCode(byCodeQuery([a.code], 'FR'))

        then:
        result?.size() == 1
        result.first().language == 'ES'
        result.first().label == 'label in ES'
    }

    def 'Given A synonym with B, when finding synonyms for A, description of B is returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456
        persistLink(a, b, synonym)

        when:
        def result = repository.findRelatedTerms(relationshipQuery(a.code, [synonym]))

        then:
        result?.size() == 1
        result.find { it.label == 'B' }
    }

    def 'Given A synonym with B, when finding broader terms for A, description of B is not returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456
        persistLink(a, b, synonym)

        when:
        def result = repository.findRelatedTerms(relationshipQuery(a.code, [broader]))

        then:
        result?.size() == 0
    }

    def 'Given A synonym with B, and B only exists in FR, when finding synonyms for A in EN, description of B is not returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456, 'FR'
        persistLink(a, b, synonym)

        when:
        def result = repository.findRelatedTerms(relationshipQuery(a.code, [synonym]))

        then:
        result?.size() == 0
    }

    def 'Given A synonym with B and B synonym with C, when finding synonyms for A, description of B and C is returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456
        def c = persistTerm 'C', 789
        persistLink(a, b, synonym)
        persistLink(b, c, synonym)

        when:
        def result = repository.findRelatedTerms(relationshipQuery(a.code, [synonym]))

        then:
        result?.size() == 2
        result.find { it.label == 'B' }
        result.find { it.label == 'C' }
    }

    def 'Given A synonym with B and B synonym with C, when finding max one synonym for A, description of B is returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456
        def c = persistTerm 'C', 789
        persistLink(a, b, synonym)
        persistLink(b, c, synonym)

        when:
        def result = repository.findRelatedTerms(relationshipQuery(a.code, [synonym], 1))

        then:
        result?.size() == 1
        result.find { it.label == 'B' }
    }

    def 'Given A synonym with B and B is broader then C, when finding synonyms for A, description of B is returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456
        def c = persistTerm 'C', 789
        persistLink(a, b, synonym)
        persistLink(b, c, broader)

        when:
        def result = repository.findRelatedTerms(relationshipQuery(a.code, [synonym]))

        then:
        result?.size() == 1
        result.find { it.label == 'B' }
    }

    def 'Given A synonym with B and B is broader then C, when finding synonyms and broaders terms for A, description of B and C is returned'() {
        def a = persistTerm 'A', 123
        def b = persistTerm 'B', 456
        def c = persistTerm 'C', 789
        persistLink(a, b, synonym)
        persistLink(b, c, broader)

        when:
        def result = repository.findRelatedTerms(relationshipQuery(a.code, [synonym, broader]))

        then:
        result?.size() == 2
        result.find { it.label == 'B' }
        result.find { it.label == 'C' }
    }

    def 'Updating term generates no new node'() {
        def term = persistTerm 'Term label'
        persistTerm 'Updated label', term.code
        persister.persistTerm term

        when:
        def result = repository.findAllByLabel(byLabelQuery('label', freeText))

        then: result.size() == 1
    }

    private Term persistTerm(String label, long code = 123L, String language = 'EN', int status = PREFERRED_STATUS) {
        def term = new Term(code, '', new Date())
        term.descriptionByLanguage[language] = new TermDescription(code, status, label, language)
        persister.persistTerm(term)
        return term
    }


    private void persistLink(Term term1, Term term2, RelationshipType relationshipType) {
        def linkType = LinkType.values().find { it.relationshipType == relationshipType }
        persister.persistLinks(new TermLinks(startTermCode: term1.code).add(term2.code, linkType.id))
    }

    private ByCodeQuery byCodeQuery(List<Long> codes, String language = 'EN') {
        new ByCodeQuery(codes, language)
    }

    private ByLabelQuery byLabelQuery(String query,
                                      ByLabelQuery.Match match,
                                      Collection<RelationshipType> relationshipTypes = [],
                                      int max = 20,
                                      String language = 'EN') {
        new ByLabelQuery(query, max, match.name(), relationshipTypes.collect { it.name() }, language)
    }

    private RelationshipQuery relationshipQuery(long code,
                                                Collection<RelationshipType> relationshipTypes = [],
                                                int max = 20,
                                                String language = 'EN') {
        new RelationshipQuery(code, max, relationshipTypes.collect { it.name() }, language)
    }
}
