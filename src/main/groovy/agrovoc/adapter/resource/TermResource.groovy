package agrovoc.adapter.resource

import agrovoc.port.resource.TermProvider
import groovy.json.JsonOutput

import javax.ws.rs.*
import javax.ws.rs.core.Context
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
    @Path("/{code}")
    @Produces('application/json')
    String get(@PathParam('code') long code,
               @QueryParam('language') String language) {
        def term = termProvider.getByCode(code, language ?: 'EN')
        termToJson(term)
    }

    @GET
    @Path("/{code}/links")
    @Produces('application/json')
    String links(@PathParam('code') long code,
                 @QueryParam('language') String language) {
        def links = termProvider.getLinksByCode(code, language ?: 'EN')
        links.each {
            addTermLinks(it.start)
            addTermLinks(it.end)
        }
        JsonOutput.toJson(links)
    }

    @GET
    @Produces('application/json')
    String find(@QueryParam('q') String query,
                @QueryParam('language') String language) {
        def terms = termProvider.query(query, language ?: 'EN')
        termsToJson(terms)
    }

    private String termToJson(term) {
        addTermLinks(term)
        JsonOutput.toJson(term)
    }

    private String termsToJson(terms) {
        terms.each { addTermLinks(it) }
        JsonOutput.toJson(terms)
    }

    private void addTermLinks(term) {
        def links = [
                self: "${ui.baseUri}term/$term.code?language=$term.language",
                links: "${ui.baseUri}term/$term.code/links?language=$term.language"
        ]
        term.links = links
    }
}
