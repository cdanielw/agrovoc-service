package agrovoc.domain

import agrovoc.dto.ByCodeQuery
import agrovoc.dto.RelationshipQuery
import agrovoc.dto.Term
import agrovoc.dto.TermDescription
import agrovoc.dto.TermLinks
import agrovoc.dto.ByLabelQuery
import agrovoc.port.agrovoc.AgrovocRepository
import agrovoc.port.cron.AgrovocTermPollingJob
import agrovoc.port.event.TermEventPublisher
import agrovoc.port.persistence.TermPersister
import agrovoc.port.persistence.TermRepository
import agrovoc.port.resource.TermProvider

import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author Daniel Wiell
 */
class TermService implements AgrovocTermPollingJob, TermEventPublisher, TermProvider {
    private final TermRepository termRepository
    private final TermPersister termPersister
    private final AgrovocRepository agrovocRepository

    private final List<Closure> createListeners = new CopyOnWriteArrayList<>()
    private final List<Closure> linkListeners = new CopyOnWriteArrayList<>()

    TermService(TermRepository termRepository, TermPersister termPersister, AgrovocRepository agrovocRepository) {
        this.termRepository = termRepository
        this.termPersister = termPersister
        this.agrovocRepository = agrovocRepository
    }

    void pollForChanges() {
        def previousLastChange = termPersister.lastChanged
        def lastChanged = persistTermsChangedSince(previousLastChange)
        persistLinksChangedSince(previousLastChange)
        if (lastChanged && previousLastChange != lastChanged)
            termPersister.lastChanged = lastChanged
    }

    private Date persistTermsChangedSince(Date previousLastChange) {
        Date lastChanged = null
        agrovocRepository.eachTermChangedSince(previousLastChange) { Term term ->
            termPersister.persistTerm(term)
            createListeners.each { it.call(term) }
            if (lastChanged < term.lastChanged)
                lastChanged = term.lastChanged
        }
        return lastChanged
    }

    private void persistLinksChangedSince(Date previousLastChange) {
        agrovocRepository.eachLinkChangedSince(previousLastChange) { TermLinks links ->
            termPersister.persistLinks(links)
            linkListeners.each { it.call(links) }
        }
    }


    void registerCreateListener(Closure listener) {
        createListeners << listener
    }

    void registerLinkListener(Closure listener) {
        linkListeners << listener
    }

    List<TermDescription> findAllByCode(ByCodeQuery query) {
        termRepository.findAllByCode(query)
    }

    List<TermDescription> findAllByLabel(ByLabelQuery query) {
        termRepository.findAllByLabel(query)
    }

    List<TermDescription> findRelationships(RelationshipQuery query) {
        termRepository.findRelatedTerms(query)
    }
}
