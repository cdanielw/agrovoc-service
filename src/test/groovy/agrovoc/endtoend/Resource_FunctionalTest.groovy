package agrovoc.endtoend

import agrovoc.dto.Term
import agrovoc.dto.TermDescription
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import spock.lang.Shared
import spock.lang.Specification

import static agrovoc.dto.ByLabelQuery.Match.*
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
        client.handler.failure = { resp ->
            return resp
        }
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
        def document = getJson('term', ['code[]': code])

        then: document.results.first().label == expectedLabel
    }

    def 'When finding terms by label, JSON representation is returned'() {
        def query = 'lab exp'
        def expectedLabel = 'Label expected'
        service.createTerms([
                createTerm(123, [EN: 'Another label']),
                createTerm(456, [EN: expectedLabel])
        ])

        when:
        def json = getJson("term/find", [q: query, match: freeText])

        then:
        json.results.size() == 1
        json.results.first().label == expectedLabel
    }

    def 'When finding terms that starts with, JSON representation is returned'() {
        def query = 'lab'
        def expectedLabel = 'Label expected'
        service.createTerms([
                createTerm(123, [EN: 'Another label']),
                createTerm(456, [EN: expectedLabel])
        ])

        when:
        def json = getJson("term/find", [q: query, match: startsWith])

        then:
        json.results.size() == 1
        json.results.first().label == expectedLabel
    }

    def 'When getting multiple terms by code, JSON representation is returned'() {
        service.createTerms([
                createTerm(123, [EN: 'Term 1']),
                createTerm(456, [EN: 'Term 2'])
        ])

        when:
        def json = getJson("term", ['code[]': [123, 456]])

        then:
        json.results.size() == 2
    }

    def 'When finding by label, JSON representation is returned'() {
        String label = 'Term label'
        Term term = createTerm(123, [EN: label])
        service.createTerm(term)

        when:
        def json = getJson("term/find", [q: label, match: exact])

        then: json.results.first().code == term.code
    }

    def 'When getting term by non-existing code, 404 is returned'() {
        expect: get('term', ['code[]': 123]).status == 404
    }

    def 'Given no code[] parameter, when getting term by code, 400 is returned'() {
        expect: get('term').status == 400
    }

    def 'Given invalid code[] parameter, when getting term by code, 400 is returned'() {
        expect: get('term', ['code[]': 'invalid']).status == 400
    }

    def 'Given invalid ralationshipType[] parameter, when getting term by code, 400 is returned'() {
        expect: get('term', ['code[]': 123, 'relationshipType[]': 'invalid']).status == 400
    }

    def 'Given invalid language parameter, when getting term by code, 400 is returned'() {
        expect: get('term', ['code[]': '213', 'language': 'invalid']).status == 400
    }

    def 'Given missing q parameter, when getting term by label, 400 is returned'() {
        expect: get('term').status == 400
    }

    def 'Given invalid match parameter, when getting term by label, 400 is returned'() {
        expect: get('term', [match: 'invalid']).status == 400
    }

    private getJson(String path, Map query = [:]) {
        Object response = get(path, query)
        assert response.status == 200
        def jsonp = response.data.text as String
        String json = jsonp.find(~/jsonpCallback\((.*)\)/) { match, json -> json }
        new JsonSlurper().parseText(json)
    }

    private Object get(String path, Map query = [:]) {
        query.callback = 'jsonpCallback'
        client.get(path: path,
                query: query)
    }

    private Term createTerm(long code, Map<String, String> labelByLanguage) {
        def term = new Term(code, '', new Date() - 10)
        labelByLanguage.each { language, label ->
            term.descriptionByLanguage[language] = new TermDescription(term.code, 20, label, language)
        }
        return term

    }
}
