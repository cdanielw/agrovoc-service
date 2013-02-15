package agrovoc.adapter.persistence

import agrovoc.dto.Term
import agrovoc.dto.TermLinks
import agrovoc.exception.NotFoundException
import agrovoc.neo4j.Neo4j
import agrovoc.port.persistence.Neo4jTermPersister
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
        def result = repository.queryByLabel('Label', 'EN')

        then:
        result.size() == 1
    }

    def 'Given terms in multiple languages, When querying by label, only term in provided language are returned'() {
        persister.persistTerm(createTerm('Term label', 123, 'EN'))
        persister.persistTerm(createTerm('Term label', 465, 'FR'))

        when:
        def result = repository.queryByLabel('Label', 'FR')

        then:
        result.size() == 1
    }

    def 'Given non-descriptor term, when querying by label, term is excluded in result'() {
        def term = createNonDescriptorTerm('Term label')
        persister.persistTerm(term)

        when:
        def result = repository.queryByLabel('Label', 'EN')

        then:
        result.empty
    }

    def 'Sorts results by label'() {
        persister.persistTerm(createTerm('C Term label', 1))
        persister.persistTerm(createTerm('A Term label', 2))
        persister.persistTerm(createTerm('B Term label', 3))
        persister.persistTerm(createTerm('E Term label', 4))
        persister.persistTerm(createTerm('D Term label', 5))

        when:
        def result = repository.queryByLabel('Label', 'EN')

        then:
        result.collect { it.label.substring(0, 1) } == ['A', 'B', 'C', 'D', 'E']
    }

    def 'Updating term generates no new node'() {
        persister.persistTerm createTerm('Term label')
        def term = createTerm('Updated label')
        persister.persistTerm term

        when:
        def result = repository.queryByLabel('label', 'EN')

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

    private Term createTerm(String label, long code = 123L, String language = 'EN') {
        new Term(code: code, status: 20, scope: '', labelByLanguage: [(language): label])
    }

    private Term createNonDescriptorTerm(String label, long code = 123L, String language = 'EN') {
        new Term(code: code, status: 60, scope: '', labelByLanguage: [(language): label])
    }

    private Map<String, Object> toMap(Term term, String language) {
        [code: term.code, status: term.status, scope: term.scope, language: language, label: term.labelByLanguage[language]]
    }
}
