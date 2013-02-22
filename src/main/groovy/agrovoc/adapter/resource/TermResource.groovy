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

import static agrovoc.dto.RelationshipType.createRelationshipTypes

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
                  @QueryParam('relationshipType[]') List<String> types,
                  @QueryParam('language') String language) {
        assertParameter('code[] parameter is mandatory') { codes }
        assertParameter('code[] parameter must be number') { allLongs(codes) }
        assertRelationshipTypes(types)
        assertLanguage(language)

        def byCodeQuery = new ByCodeQuery(toLongs(codes), language ?: 'EN')
        try {
            def terms = termProvider.findAllByCode(byCodeQuery)
            termsToJsonp(terms, createRelationshipTypes(types), byCodeQuery.language)
        } catch (NotFoundException e) {
            throw notFoundException(e.message)
        }
    }

    @GET
    @Path("/find")
    @Produces('application/javascript')
    String byLabel(@QueryParam('q') String query,
                   @QueryParam('max') Integer max,
                   @QueryParam('match') String match,
                   @QueryParam('relationshipType[]') List<String> types,
                   @QueryParam('language') String language) {
        assertParameter('q parameter is mandatory') { query }
        assertParameter('match parameter is mandatory') { match }
        assertParameter("match parameter must be one of ${Match.names()}") { match in Match.names() }
        assertRelationshipTypes(types)
        assertLanguage(language)

        def byLabelQuery = new ByLabelQuery(query, max > 0 ? max : 20 , match, types, language ?: 'EN')
        def terms = termProvider.findAllByLabel(byLabelQuery)
        termsToJsonp(terms, byLabelQuery.relationshipTypes, byLabelQuery.language)
    }

    @GET
    @Path("/{code}/relationships")
    @Produces('application/javascript')
    String relationships(@PathParam('code') long code,
                         @QueryParam('max') int max,
                         @QueryParam('relationshipType[]') List<String> types,
                         @QueryParam('language') String language) {
        assertRelationshipTypes(types)
        assertLanguage(language)

        def relationshipQuery = new RelationshipQuery(code, max > 0 ? max : 20 , types, language ?: 'EN')
        def terms = termProvider.findRelationships(relationshipQuery)
        termsToJsonp(terms, [] as Set, relationshipQuery.language)
    }

    private String toJsonp(json) {
        "${callback}($json)"
    }

    private String termsToJsonp(List<TermDescription> terms, Set<RelationshipType> relationshipTypes, String language) {
        def json = [results:
                terms.collect { term ->
                    def termMap = [
                            code: term.code,
                            label: term.label,
                            preferred: term.preferred,
                            language: language
                    ]
                    if (relationshipTypes)
                        termMap.relationships = getRelationshipLink(term, relationshipTypes, language)
                    return termMap
                }]
        return toJsonp(new JsonBuilder(json).toString())
    }

    private String getRelationshipLink(TermDescription term, Set<RelationshipType> types, String language) {
        def typesParams = types.collect { "relationshipType[]=${it.name()}" }.join('&')
        "${ui.baseUri}term/$term.code/relationships?$typesParams&language=$language"
    }

    private String getCallback() {
        ui.queryParameters.callback?.first() ?: 'callback'
    }

    private void assertParameter(String message, Closure assertion) {
        if (!assertion())
            throw new WebApplicationException(
                    Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
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
}
