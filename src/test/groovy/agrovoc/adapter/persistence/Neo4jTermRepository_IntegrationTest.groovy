package agrovoc.adapter.persistence

import agrovoc.dto.LabelQuery
import agrovoc.dto.Term
import agrovoc.dto.TermDescription
import agrovoc.dto.TermLinks
import agrovoc.exception.NotFoundException
import agrovoc.neo4j.Neo4j
import spock.lang.Specification

/**
 * @author Daniel Wiell
 */
class Neo4jTermRepository_IntegrationTest extends Specification {
    def neo4j = new Neo4j()
    def repository = new Neo4jTermRepository(neo4j.graphDb)
    def persister = new Neo4jTermPersister(neo4j.graphDb)

    def cleanup() { neo4j.stop() }

    def 'Can get by code'() {
        def term = createTerm('Term label')
        persister.persistTerm(term)

        when:
        def result = repository.getByCode(term.code, 'EN')

        then:
        result == toMap(term, 'EN')
    }

    def 'Given no term exists, when getting by code, NotFoundException is thrown'() {
        when: repository.getByCode(123L, 'EN')
        then: thrown NotFoundException
    }

    def 'Can query by label'() {
        def term = createTerm('Term label')
        persister.persistTerm(term)

        when:
        def result = repository.findAllWhereWordInLabelStartsWith(query('Label', 'EN'))

        then:
        result.size() == 1
    }

    def 'Given terms in multiple languages, When querying by label, only term in provided language are returned'() {
        persister.persistTerm(createTerm('Term label', 123, 'EN'))
        persister.persistTerm(createTerm('Term label', 465, 'FR'))

        when:
        def result = repository.findAllWhereWordInLabelStartsWith(query('Label', 'FR'))

        then:
        result.size() == 1
    }

    def 'Given non-descriptor term, when querying by label, term is excluded in result'() {
        def term = createNonDescriptorTerm('Term label')
        persister.persistTerm(term)

        when:
        def result = repository.findAllWhereWordInLabelStartsWith(query('Label', 'EN'))

        then:
        result.empty
    }

    def 'Sorts results by label'() {
        persister.persistTerm(createTerm('c Term label', 1))
        persister.persistTerm(createTerm('A Term label', 2))
        persister.persistTerm(createTerm('b Term label', 3))
        persister.persistTerm(createTerm('E Term label', 4))
        persister.persistTerm(createTerm('d Term label', 5))

        when:
        def result = repository.findAllWhereWordInLabelStartsWith(query('Label', 'EN'))

        then:
        result.collect { it.label.substring(0, 1).toUpperCase() } == ['A', 'B', 'C', 'D', 'E']
    }

    def 'Updating term generates no new node'() {
        persister.persistTerm createTerm('Term label')
        def term = createTerm('Updated label')
        persister.persistTerm term

        when:
        def result = repository.findAllWhereWordInLabelStartsWith(query('label', 'EN'))

        then: result.size() == 1
    }

    def 'Given no description in language, when getting by code, English is used'() {
        def term = createTerm('Term label')
        persister.persistTerm term

        when:
        def result = repository.getByCode(term.code, 'FR')

        then: result.language == 'EN'
    }


    def 'Given no description exist in english, when getting by code, NotFoundException is thrown'() {
        def term = createTerm('Term label', 123, 'FR')
        persister.persistTerm term

        when: repository.getByCode(term.code, 'EN')
        then: thrown NotFoundException
    }

    def 'Can get links by code'() {
        def term1 = createTerm('Term 1', 1)
        def term2 = createTerm('Term 2', 2)
        persister.persistTerm(term1)
        persister.persistTerm(term2)
        persister.persistLinks(new TermLinks(startTermCode: term1.code).add(term2.code, 20))

        when:
        def result = repository.getLinksByCode(term1.code, 'EN')

        then:
        result.size() == 1
        result.first().start.code == term1.code
        result.first().end.code == term2.code
    }

    def 'Given pre-existing link, when updating links, pre-existing ones are removed'() {
        def term1 = createTerm('Term 1', 1)
        def term2 = createTerm('Term 2', 2)
        persister.persistTerm(term1)
        persister.persistTerm(term2)
        persister.persistLinks(new TermLinks(startTermCode: term1.code).add(term2.code, 20))
        assert repository.getLinksByCode(term1.code, 'EN').first().type == '20'

        when:
        persister.persistLinks(new TermLinks(startTermCode: term1.code).add(term2.code, 60))


        then:
        def result = repository.getLinksByCode(term1.code, 'EN')
        result.size() == 1
        result.first().type == '60'
    }

    def 'Given no description in language, when getting links by code, English is used'() {
        def term1 = createTerm('Term 1', 1, 'EN')
        def term2 = createTerm('Term 2', 2, 'EN')
        persister.persistTerm(term1)
        persister.persistTerm(term2)
        persister.persistLinks(new TermLinks(startTermCode: term1.code).add(term2.code, 20))

        when:
        def result = repository.getLinksByCode(term1.code, 'FR')

        then:
        result.size() == 1
        result.first().start.language == 'EN'
        result.first().end.language == 'EN'
    }

    def 'Given no description in English for end term, when getting links by code, link is excluded'() {
        def term1 = createTerm('Term 1', 1, 'EN')
        def term2 = createTerm('Term 2', 2, 'FR')
        persister.persistTerm(term1)
        persister.persistTerm(term2)
        persister.persistLinks(new TermLinks(startTermCode: term1.code).add(term2.code, 20))

        when:
        def result = repository.getLinksByCode(term1.code, 'EN')

        then:
        result.empty
    }

    def 'Given matching term, when finding by label, term is returned'() {
        String label = 'Term'
        def term = createTerm(label, 123, 'EN')
        persister.persistTerm(term)

        when:
        def result = repository.findByLabel(label, 'EN')

        then: result
    }

    def 'Given matching term with different case, when finding by label, term is returned'() {
        def term = createTerm('TeRm', 123, 'EN')
        persister.persistTerm(term)

        when:
        def result = repository.findByLabel('tErM', 'EN')

        then: result
    }

    def 'Given query matching part of term, when finding by label, term is not returned'() {
        def term = createTerm('Term Almost Matching', 123, 'EN')
        persister.persistTerm(term)

        when:
        def result = repository.findByLabel('Term', 'EN')

        then: !result
    }

    def 'Given matching term, when finding by start, term is returned'() {
        def term = createTerm('Term Should Match', 123, 'EN')
        persister.persistTerm(term)

        when:
        def result = repository.findAllWhereLabelStartsWith(query('Term', 'EN'))

        then: result.size() == 1
    }

    def 'Given term not starting but containing query, when finding by start, term is not returned'() {
        def term = createTerm('Should Not Match Term', 123, 'EN')
        persister.persistTerm(term)

        when:
        def result = repository.findAllWhereLabelStartsWith(query('Term', 'EN'))

        then: !result
    }

    private Term createTerm(String label, long code = 123L, String language = 'EN') {
        def term = new Term(code, '', new Date())
        term.descriptionByLanguage[language] = new TermDescription(language, 20, label)
        return term
    }

    private Term createNonDescriptorTerm(String label, long code = 123L, String language = 'EN') {
        def term = new Term(code, '', new Date())
        term.descriptionByLanguage[language] = new TermDescription(language, 60, label)
        return term
    }

    private Map<String, Object> toMap(Term term, String language) {
        def description = term.descriptionByLanguage[language]
        [code: term.code, scope: term.scope, language: language, label: description.label, status: description.status]
    }

    private LabelQuery query(String query, String language) {
        new LabelQuery(query, language, 20)
    }
}
