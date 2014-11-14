package agrovoc.adapter.agrovoc

import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Daniel Wiell
 */
@Ignore
class SparqlAgrovocRepository_Test extends Specification {

    def 'Test terms'() {
        new SparqlAgrovocRepository().eachTermChangedSince(new Date() - 300) {
            println it
        }

        when: true
        then: true
    }

    def 'Test links'() {
        new SparqlAgrovocRepository().eachLinkChangedSince(new Date() - 300) {
            println it
        }

        when: true
        then: true
    }
}
