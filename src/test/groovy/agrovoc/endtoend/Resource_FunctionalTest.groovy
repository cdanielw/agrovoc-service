package agrovoc.endtoend

import agrovoc.dto.Term
import agrovoc.dto.TermLinks
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
        service.createTerm(new Term(code: code, labelByLanguage: [EN: expectedLabel]))

        when:
        def result = client.get(path: "term/$code").data

        then: result.label == expectedLabel
    }

    def 'When finding terms by label, JSON representation is returned'() {
        def query = 'lab exp'
        def expectedLabel = 'Label expected'
        service.createTerms([
                new Term(code: 123, labelByLanguage: [EN: 'Another label']),
                new Term(code: 456, labelByLanguage: [EN: expectedLabel])
        ])

        when:
        def result = client.get(path: "term", query: [q: query]).data

        then:
        result.size() == 1
        result.first().label == expectedLabel
    }

    def 'When finding links, JSON representation is returned'() {
        service.createTerms([
                new Term(code: 123, labelByLanguage: [EN: 'Term 1']),
                new Term(code: 456, labelByLanguage: [EN: 'Term 2']),
                new Term(code: 798, labelByLanguage: [EN: 'Term 2'])], [
                new TermLinks(startTermCode: 123).add(456, 1000),
                new TermLinks(startTermCode: 123).add(798, 2000)
        ])
        when:
        def result = client.get(path: "term/123/links").data

        then:
        result.size() == 2
    }
}
