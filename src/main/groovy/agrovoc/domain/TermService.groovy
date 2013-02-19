package agrovoc.domain

import agrovoc.dto.Term
import agrovoc.dto.TermLinks
import agrovoc.dto.LabelQuery
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
        def time = System.currentTimeMillis()
        def previousLastChange = termPersister.lastChanged
        def lastChanged = persistTermsChangedSince(previousLastChange)
        persistLinksChangedSince(previousLastChange)
        if (lastChanged && previousLastChange != lastChanged)
            termPersister.lastChanged = lastChanged
        println "******** Polling for changes since $previousLastChange took ${(System.currentTimeMillis() - time) / 1000 / 60} minutes"
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

    Map getByCode(long code, String language) {
        termRepository.getByCode(code, language)
    }

    Map<String, Object> findByLabel(String label, String language) {
        termRepository.findByLabel(label, language)
    }

    List<Map<String, Object>> findAllWhereLabelStartsWith(LabelQuery query) {
        termRepository.findAllWhereLabelStartsWith(query)
    }

    List<Map<String, Object>> findAllWhereWordInLabelStartsWith(LabelQuery query) {
        termRepository.findAllWhereWordInLabelStartsWith(query)
    }

    List<Map<String, Object>> findAllBroaderTerms(long code, String language) {
        termRepository.findAllBroaderTerms(code, language)
    }

    List<Map<String, Object>> findAllNarrowerTerms(long code, String language) {
        termRepository.findAllNarrowerTerms(code, language)
    }
}
