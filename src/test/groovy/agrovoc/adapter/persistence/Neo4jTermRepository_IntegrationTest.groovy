package agrovoc.adapter.persistence

import agrovoc.exception.NotFoundException
import agrovoc.neo4j.Neo4j
import spock.lang.Specification

/**
 * @author Daniel Wiell
 */
class Neo4jTermRepository_IntegrationTest extends Specification {
    Neo4j neo4j = new Neo4j()
    Neo4jTermRepository repository = new Neo4jTermRepository(neo4j.graphDb)

    def cleanup() { neo4j.stop() }

    def 'Can get by code'() {
        def term = createTerm('Term label')
        repository.persistTerm(term)

        when:
        def result = repository.getByCode(term.code as long, term.language as String)

        then:
        result == term
    }

    def 'Given no term exists, when getting by code, NotFoundException is thrown'() {
        when: repository.getByCode(123L, 'EN')
        then: thrown NotFoundException
    }

    def 'Can query by label'() {
        def term = createTerm('Term label')
        repository.persistTerm(term)

        when:
        def result = repository.queryByLabel('Label', term.language as String)

        then:
        result.size() == 1
    }

    def 'Given terms in multiple languages, When querying by label, only term in provided language are returned'() {
        repository.persistTerm(createTerm('Term label', 123, 'EN'))
        repository.persistTerm(createTerm('Term label', 465, 'FR'))

        when:
        def result = repository.queryByLabel('Label', 'FR')

        then:
        result.size() == 1
    }

    def 'Given non-descriptor term, when querying by label, term is excluded in result'() {
        def term = createNonDescriptorTerm('Term label')
        repository.persistTerm(term)

        when:
        def result = repository.queryByLabel('Label', term.language as String)

        then:
        result.empty
    }

    def 'Sorts results by label'() {
        repository.persistTerm(createTerm('C Term label', 1))
        repository.persistTerm(createTerm('A Term label', 2))
        repository.persistTerm(createTerm('B Term label', 3))
        repository.persistTerm(createTerm('E Term label', 4))
        repository.persistTerm(createTerm('D Term label', 5))

        when:
        def result = repository.queryByLabel('Label', 'EN')

        then:
        result.collect { it.label.substring(0, 1) } == ['A', 'B', 'C', 'D', 'E']
    }

    def 'Updating term generates no new node'() {
        repository.persistTerm createTerm('Term label')
        def term = createTerm('Updated label')
        repository.persistTerm term

        when:
        def result = repository.queryByLabel('label', term.language as String)

        then: result.size() == 1
    }

    def 'Given no description in language, when getting by code, English is used'() {
        def term = createTerm('Term label')
        repository.persistTerm term

        when:
        def result = repository.getByCode(term.code as long, 'FR')

        then: result.language == 'EN'
    }


    def 'Given no description exist in english, when getting by code, NotFoundException is thrown'() {
        def term = createTerm('Term label', 123, 'FR')
        repository.persistTerm term

        when: repository.getByCode(term.code as long, 'EN')
        then: thrown NotFoundException
    }

    def 'Can get links by code'() {
        def term1 = createTerm('Term 1', 1)
        def term2 = createTerm('Term 2', 2)
        repository.persistTerm(term1)
        repository.persistTerm(term2)
        repository.persistLink([start: term1.code, end: term2.code, type: 20])

        when:
        def result = repository.getLinksByCode(term1.code as long, 'EN')

        then:
        result.size() == 1
        result.first().start.code == term1.code
        result.first().end.code == term2.code
    }

    def 'Given no description in language, when getting links by code, English is used'() {
        def term1 = createTerm('Term 1', 1, 'EN')
        def term2 = createTerm('Term 2', 2, 'EN')
        repository.persistTerm(term1)
        repository.persistTerm(term2)
        repository.persistLink([start: term1.code, end: term2.code, type: 20])

        when:
        def result = repository.getLinksByCode(term1.code as long, 'FR')

        then:
        result.size() == 1
        result.first().start.language == 'EN'
        result.first().end.language == 'EN'
    }

    def 'Given no description in English for end term, when getting links by code, link is excluded'() {
        def term1 = createTerm('Term 1', 1, 'EN')
        def term2 = createTerm('Term 2', 2, 'FR')
        repository.persistTerm(term1)
        repository.persistTerm(term2)
        repository.persistLink([start: term1.code, end: term2.code, type: 20])

        when:
        def result = repository.getLinksByCode(term1.code as long, 'EN')

        then:
        result.empty
    }

    private Map<String, Object> createTerm(String label, long code = 123L, String language = 'EN') {
        [code: code, label: label, language: language, status: 20, scope: '']
    }

    private Map<String, Object> createNonDescriptorTerm(String label, long code = 123L, String language = 'EN') {
        [code: code, label: label, language: language, status: 60, scope: '']
    }
}
