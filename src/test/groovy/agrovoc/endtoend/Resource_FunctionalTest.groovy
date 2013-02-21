package agrovoc.endtoend

import agrovoc.dto.Term
import agrovoc.dto.TermDescription
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Daniel Wiell
 */
class Resource_FunctionalTest extends Specification {
    @Shared
    def service = new AgrovocService()
    RESTClient client

    def setup() {
        service.init()
        client = new RESTClient("${AgrovocService.BASE_URI}/")
        client.parser.'application/javascript' = client.parser.'text/plain'
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
        def result = getJson("term/$code")

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
        def result = getJson("term/find", [q: query])

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
        def result = getJson("term/find", [q: query, startsWith: true])

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
        def result = getJson("term", ['code[]': [123, 456]])

        then:
        result.size() == 2
    }

    def 'When finding by label, JSON representation is returned'() {
        String label = 'Term label'
        Term term = createTerm(123, [EN: label])
        service.createTerm(term)

        when:
        def result = getJson("term/label/$label")

        then: result.code == term.code
    }

    def getJson(String path, Map query = [:]) {
        query.callback = 'jsonpCallback'
        def jsonp = client.get(path: path,
                query: query).data.text as String
        String json = jsonp.find(~/jsonpCallback\((.*)\)/) { match, json -> json }
        new JsonSlurper().parseText(json)
    }

    private Term createTerm(long code, Map<String, String> labelByLanguage) {
        def term = new Term(code, '', new Date() - 10)
        labelByLanguage.each { language, label ->
            term.descriptionByLanguage[language] = new TermDescription(language, 20, label)
        }
        return term

    }
}
