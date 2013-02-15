package agrovoc.endtoend
import groovyx.net.http.RESTClient
import spock.lang.Specification
/**
 * @author Daniel Wiell
 */
class Resource_FunctionalTest extends Specification {
    def service = new AgrovocService()
    def client = new RESTClient("${AgrovocService.BASE_URI}/")

    def setup() {
        service.init()
    }

    def cleanup() {
        service.stop()
    }

    def 'When getting term by code, JSON representation is returned'() {
        def code = 123
        def expectedLabel = 'Expected label'
        service.createTerm(code: code, label: expectedLabel)

        when:
        def result = client.get(path: "term/$code").data

        then: result.label == expectedLabel
    }

    def 'When finding terms by label, JSON representation is returned'() {
        def query = 'lab exp'
        def expectedLabel = 'Label expected'
        service.createTerms([
                [code: 123, label: 'Another label'],
                [code: 456, label: expectedLabel]
        ])

        when:
        def result = client.get(path: "term", query: [q: query]).data

        then:
        result.size() == 1
        result.first().label == expectedLabel
    }

    def 'When finding links, JSON representation is returned'() {
        service.createTerms([
                [code: 123, label: 'Term 1'],
                [code: 456, label: 'Term 2'],
                [code: 798, label: 'Term 2']], [
                [start: 123, end: 456, type: 1000],
                [start: 123, end: 798, type: 2000]
        ])
        when:
        def result = client.get(path: "term/123/links").data

        then:
        result.size() == 2
    }
}
