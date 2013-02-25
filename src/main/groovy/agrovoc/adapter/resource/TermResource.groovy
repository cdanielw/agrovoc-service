package agrovoc.adapter.resource

import agrovoc.dto.*
import agrovoc.dto.ByLabelQuery.Match
import agrovoc.exception.NotFoundException
import agrovoc.port.resource.TermProvider
import groovy.json.JsonBuilder

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * @author Daniel Wiell
 */
@Path("/term")
class TermResource {
    private final TermProvider termProvider

    @Context
    private UriInfo ui

    TermResource(TermProvider termProvider) {
        this.termProvider = termProvider
    }

    @GET
    @Produces('application/javascript')
    String byCode(@QueryParam('code[]') List<String> codes,
                  @QueryParam('language') String language) {
        assertParameter('code[] parameter is mandatory') { codes }
        assertParameter('code[] parameter must be number') { allLongs(codes) }
        assertLanguage(language)

        def byCodeQuery = new ByCodeQuery(toLongs(codes), language ?: 'EN')
        try {
            def terms = termProvider.findAllByCode(byCodeQuery)
            termsToJsonp(terms, 0, [] as Set, byCodeQuery.language)
        } catch (NotFoundException e) {
            throw notFoundException(e.message)
        }
    }

    @GET
    @Path("/find")
    @Produces('application/javascript')
    String byLabel(@QueryParam('q') String query,
                   @QueryParam('hits') int hits,
                   @QueryParam('match') String match,
                   @QueryParam('suggestions') int suggestions,
                   @QueryParam('relationshipType[]') List<String> types,
                   @QueryParam('language') String language) {
        assertParameter('q parameter is mandatory') { query }
        assertParameter('match parameter is mandatory') { match }
        assertParameter("match parameter must be one of ${Match.names()}") { match in Match.names() }
        assertRelationshipTypes(types)
        assertLanguage(language)

        def byLabelQuery = new ByLabelQuery(query, determineHitsToInclude(hits), match, types, language ?: 'EN')
        def terms = termProvider.findAllByLabel(byLabelQuery)
        termsToJsonp(terms, determineSuggestionsToInclude(suggestions), byLabelQuery.relationshipTypes, byLabelQuery.language)
    }

    @GET
    @Path("/{code}/relationships")
    @Produces('application/javascript')
    String relationships(@PathParam('code') long code,
                         @QueryParam('suggestions') int suggestions,
                         @QueryParam('relationshipType[]') List<String> types,
                         @QueryParam('language') String language) {
        assertRelationshipTypes(types)
        assertLanguage(language)
        def suggestionsToInclude = determineSuggestionsToInclude(suggestions)
        def relationshipQuery = new RelationshipQuery(code, suggestionsToInclude, types, language ?: 'EN')
        def terms = termProvider.findRelationships(relationshipQuery)
        termsToJsonp(terms, 0, [] as Set, relationshipQuery.language)
    }

    private String toJsonp(json) {
        "${callback}($json)"
    }

    private String termsToJsonp(List<TermDescription> terms,
                                int suggestions,
                                Set<RelationshipType> relationshipTypes,
                                String language) {
        def json = [results:
                terms.collect { term ->
                    def termMap = [
                            code: term.code,
                            label: term.label,
                            preferred: term.preferred,
                            language: language
                    ]
                    if (suggestions && relationshipTypes)
                        termMap.relationships = getRelationshipLink(term, suggestions, relationshipTypes, language)
                    return termMap
                }]
        return toJsonp(new JsonBuilder(json).toString())
    }

    private String getRelationshipLink(TermDescription term,
                                       int suggestions,
                                       Set<RelationshipType> types,
                                       String language) {
        def typesParams = types.collect { "relationshipType[]=${it.name()}" }.join('&')
        "${ui.baseUri}term/$term.code/relationships?suggestions=$suggestions&$typesParams&language=$language"
    }

    private String getCallback() {
        ui.queryParameters.callback?.first() ?: 'callback'
    }

    private void assertParameter(String message, Closure assertion) {
        if (!assertion())
            throw new WebApplicationException(
                    Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
                            .type('text/plain')
                            .entity(message)
                            .build()
            )
    }

    private void assertLanguage(String language) {
        assertParameter("Language must be two character iso code. Got $language") {
            !language || language ==~ /\w\w/
        }
    }

    private void assertRelationshipTypes(List<String> relationshipTypes) {
        assertParameter("Relationship types must be one of ${RelationshipType.names()}") {
            relationshipTypes.every { it in RelationshipType.names() }
        }
    }

    private WebApplicationException notFoundException(String message) {
        new WebApplicationException(
                Response.status(HttpURLConnection.HTTP_NOT_FOUND)
                        .entity(message)
                        .type('text/plain')
                        .build()
        )
    }

    private List<Long> toLongs(List<String> strings) {
        strings.collect { it as long }
    }

    private boolean allLongs(List<String> strings) {
        try {
            toLongs(strings)
            return true
        } catch (NumberFormatException ignore) {
            return false
        }
    }

    int determineHitsToInclude(int max) {
        if (max <= 0) return 20
        if (max > 100) return 100
        return max
    }

    int determineSuggestionsToInclude(int max) {
        if (max < 0) return 0
        if (max > 100) return 100
        return max
    }
}
