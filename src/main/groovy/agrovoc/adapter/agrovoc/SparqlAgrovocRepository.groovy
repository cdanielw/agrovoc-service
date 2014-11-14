package agrovoc.adapter.agrovoc

import agrovoc.adapter.persistence.LinkType
import agrovoc.dto.Term
import agrovoc.dto.TermDescription
import agrovoc.dto.TermLinks
import agrovoc.port.agrovoc.AgrovocRepository
import agrovoc.sparql.SparqlEndpoint

/**
 * @author Daniel Wiell
 */
class SparqlAgrovocRepository implements AgrovocRepository {
    def ns = [
            'dcterms': 'http://purl.org/dc/terms/',
            'skos-xl': 'http://www.w3.org/2008/05/skos-xl#',
            'ag': 'http://aims.fao.org/aos/agrontology#'
    ]
    def agrovoc = new SparqlEndpoint('http://202.45.142.113:10035/repositories/agrovoc/', ns)

    void eachTermChangedSince(Date date, Closure callback) {
        def formattedDate = agrovoc.format(date)

        def conceptDescriptions = []
        def currentCode = null as String
        // TODO: Do I reach all codes like this?
        // Perhaps terms have relations to other terms, not being linked from concept
        // Are there terms not being linked to from concept
        agrovoc.query("""
SELECT ?code ?labelType ?label (LANG(?label) AS ?language) ?modified
{
  ?concept      rdf:type            skos:Concept ;
                ?labelType          ?description ;
                dcterms:modified    ?modified .
  ?description  rdf:type            skos-xl:Label ;
                skos-xl:literalForm ?label ;
                ag:hasCodeAgrovoc   ?code .
  #FILTER ( ?modified > "${formattedDate}"^^xsd:dateTime ) .
  FILTER ( ?concept = <http://aims.fao.org/aos/agrovoc/c_3055>  || ?concept = <http://aims.fao.org/aos/agrovoc/c_16086> ) .
}
ORDER BY ?code
            """) {
            if (currentCode != it.code) {
                if (conceptDescriptions) invokeTermCallback(conceptDescriptions, callback)
                currentCode = it.code
            }
            conceptDescriptions << extractDescription(it)
        }
        if (conceptDescriptions) invokeTermCallback(conceptDescriptions, callback)
    }

    private Map extractDescription(result) {
        def prefLabel = result.labelType == agrovoc.uri('skos-xl', 'prefLabel')
        def status = prefLabel ? 20 : 70
        return [
                term: result.term,
                code: agrovoc.value(result.code) as Long,
                status: status,
                label: agrovoc.value(result.label),
                language: agrovoc.value(result.language),
                modified: result.modified
        ]
    }

    private void invokeTermCallback(conceptDescriptions, callback) {
        conceptDescriptions.groupBy { it.code }.each { code, descriptions ->
            def lastChanged = descriptions.collect { agrovoc.date(it.modified) }.max()
            def term = new Term(code, null, lastChanged)
            descriptions.each {
                term.descriptionByLanguage[it.language] = new TermDescription(code, it.status, it.label, it.language)
            }
            callback(term)
        }
        conceptDescriptions.clear()
    }

    void eachLinkChangedSince(Date date, Closure callback) {
        def formattedDate = agrovoc.format(date)

        TermLinks termLinks = null
        agrovoc.query("""
SELECT DISTINCT ?code1 (STR(?p) AS ?relation) ?code2
{
  {
    ?concept1       rdf:type            skos:Concept ;
                    ?p                  ?concept2 ;
                    ?d1                 ?description1 ;
                    dcterms:modified    ?modified .
    ?concept2       rdf:type            skos:Concept ;
                    ?d2                 ?description2 .
    ?description1   ag:hasCodeAgrovoc   ?code1 .
    ?description2   ag:hasCodeAgrovoc   ?code2 .
    #FILTER ( ?modified > "$formattedDate"^^xsd:dateTime ) .
    FILTER ( ?concept1 = <http://aims.fao.org/aos/agrovoc/c_3055> || ?concept1 = <http://aims.fao.org/aos/agrovoc/c_16086> ) .
  }
  UNION
  {
    ?concept1       rdf:type            skos:Concept ;
                    ?d1                  ?description1 ;
                    dcterms:modified    ?modified .
    ?description1   ?p                  ?description2 ;
                    ag:hasCodeAgrovoc   ?code1 .
    ?description2   ag:hasCodeAgrovoc   ?code2 .
    #FILTER ( ?modified > "$formattedDate"^^xsd:dateTime ) .
    FILTER ( ?concept1 = <http://aims.fao.org/aos/agrovoc/c_3055> || ?concept1 = <http://aims.fao.org/aos/agrovoc/c_16086> ) .
  }
}
ORDER BY ?code1
            """) { Map<String, String> r ->
            def startTermCode = agrovoc.value(r.code1) as long
            if (termLinks?.startTermCode != startTermCode) {
                if (termLinks) callback.call(termLinks)
                termLinks = new TermLinks(startTermCode)
            }
            def relation = agrovoc.value(r.relation)
            termLinks.add(agrovoc.value(r.code2) as long, LinkType.id(relation), relation)
        }
        if (termLinks) callback.call(termLinks)
    }
}
