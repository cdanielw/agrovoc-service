package agrovoc.adapter.resource

import agrovoc.dto.LabelQuery
import agrovoc.port.resource.TermProvider
import groovy.json.JsonOutput

import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.UriInfo
import javax.xml.ws.WebServiceException

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
    @Produces('application/javascript')
    String termByCode(@PathParam('code') long code,
                      @QueryParam('language') String language) {
        def term = termProvider.getByCode(code, language ?: 'EN')
        termToJsonp(term)
    }

    @GET
    @Path("/label/{label}")
    @Produces('application/javascript')
    String termByLabel(@PathParam('label') String label,
                       @QueryParam('language') String language) {
        def term = termProvider.findByLabel(label, language ?: 'EN')
        if (!term) return "${callback}({});" // TODO: Handle this better
        termToJsonp(term)
    }

    @GET
    @Produces('application/javascript')
    String terms(@QueryParam('code[]') List<Long> codes,
                 @QueryParam('language') String language) {
        // TODO: Make more efficient
        def terms = codes.collect {
            termProvider.getByCode(it, language ?: 'EN')
        }
        termsToJsonp(terms)
    }

    @GET
    @Path("/{code}/broader")
    @Produces('application/javascript')
    String broader(@PathParam('code') long code,
                   @QueryParam('language') String language) {
        def terms = termProvider.findAllBroaderTerms(code, language ?: 'EN')
        termsToJsonp(terms)
    }

    @GET
    @Path("/{code}/narrower")
    @Produces('application/javascript')
    String narrower(@PathParam('code') long code,
                    @QueryParam('language') String language) {
        def terms = termProvider.findAllNarrowerTerms(code, language ?: 'EN')
        termsToJsonp(terms)
    }

    @GET
    @Path("/find")
    @Produces('application/javascript')
    String find(@QueryParam('startsWith') boolean startsWith,
                @QueryParam('q') String query,
                @QueryParam('language') String language,
                @QueryParam('max') Integer max) {
        def labelQuery = new LabelQuery(query, language ?: 'EN', max ?: 20)
        def terms = startsWith ?
            termProvider.findAllWhereLabelStartsWith(labelQuery) :
            termProvider.findAllWhereWordInLabelStartsWith(labelQuery)
        termsToJsonp(terms)
    }

    private String termToJsonp(term) {
        addLinks(term)
        "${callback}(${JsonOutput.toJson(term)});";
    }

    private String termsToJsonp(terms) {
        terms.each { addLinks(it) }
        "${callback}(${JsonOutput.toJson(terms)})";
    }

    private String getCallback() {
        def callback = ui.queryParameters.callback?.first()
        if (!callback)
            throw new WebServiceException('callback parameter missing') // TODO: Handle this with a 400
        return callback
    }

    private void addLinks(term) {
        def links = [
                self: "${ui.baseUri}term/$term.code?language=$term.language",
                broader: "${ui.baseUri}term/$term.code/broader?language=$term.language",
                narrower: "${ui.baseUri}term/$term.code/narrower?language=$term.language"
        ]
        term.links = links
    }
}
