package agrovoc.endtoend
import agrovoc.dto.Term
import agrovoc.dto.TermDescription
import groovyx.net.http.RESTClient
import spock.lang.Shared
import spock.lang.Specification
/**
 * @author Daniel Wiell
 */
class Resource_FunctionalTest extends Specification {
    @Shared def service = new AgrovocService()
    def client = new RESTClient("${AgrovocService.BASE_URI}/")

    def setup() {
        service.init()
    }

    def cleanup() {
        service.stop()
    }

    def cleanupSpec() {
        service.destroy()
    }

    def 'When getting term by code, JSON representation is returned'() {
        def code = 123
        def expectedLabel = 'Expected label'
        service.createTerm(createTerm(code, [EN: expectedLabel]))

        when:
        def result = client.get(path: "term/$code").data

        then: result.label == expectedLabel
    }

    def 'When finding terms by label, JSON representation is returned'() {
        def query = 'lab exp'
        def expectedLabel = 'Label expected'
        service.createTerms([
                createTerm(123, [EN: 'Another label']),
                createTerm(456, [EN: expectedLabel])
        ])

        when:
        def result = client.get(path: "term/find", query: [q: query]).data

        then:
        result.size() == 1
        result.first().label == expectedLabel
    }

    def 'When finding terms that starts with, JSON representation is returned'() {
        def query = 'lab'
        def expectedLabel = 'Label expected'
        service.createTerms([
                createTerm(123, [EN: 'Another label']),
                createTerm(456, [EN: expectedLabel])
        ])

        when:
        def result = client.get(path: "term/find", query: [q: query, startsWith: true]).data

        then:
        result.size() == 1
        result.first().label == expectedLabel
    }

    def 'When getting multiple terms by code, JSON representation is returned'() {
        service.createTerms([
                createTerm(123, [EN: 'Term 1']),
                createTerm(456, [EN: 'Term 2'])
        ])

        when:
        def result = client.get(path: "term", query: ['code[]': [123, 456]]).data

        then:
        result.size() == 2
    }

    def 'When finding by label, JSON representation is returned'() {
        String label = 'Term label'
        Term term = createTerm(123, [EN: label])
        service.createTerm(term)

        when:
        def result = client.get(path: "term/label/$label").data

        then: result.code == term.code
    }

    private Term createTerm(long code, Map<String, String> labelByLanguage) {
        def term = new Term(code, '', new Date() - 10)
        labelByLanguage.each { language, label ->
            term.descriptionByLanguage[language] = new TermDescription(language, 20, label)
        }
        return term

    }
}
