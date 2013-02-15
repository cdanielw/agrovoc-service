package agrovoc.domain

import agrovoc.port.agrovoc.AgrovocRepository
import agrovoc.port.cron.AgrovocTermPollingJob
import agrovoc.port.event.TermEventPublisher
import agrovoc.port.persistence.TermRepository
import agrovoc.port.resource.TermProvider

import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author Daniel Wiell
 */
class TermService implements AgrovocTermPollingJob, TermEventPublisher, TermProvider {
    private final TermRepository termRepository
    private final AgrovocRepository agrovocRepository

    private final List<Closure> createListeners = new CopyOnWriteArrayList<>()
    private final List<Closure> linkListeners = new CopyOnWriteArrayList<>()

    TermService(TermRepository termRepository, AgrovocRepository agrovocRepository) {
        this.termRepository = termRepository
        this.agrovocRepository = agrovocRepository
    }

    void pollForChanges() {
        def time = System.currentTimeMillis()
        def previousLastChange = termRepository.lastChanged
        def lastChanged = persistTermsChangedSince(previousLastChange)
        persistLinksChangedSince(previousLastChange)
        if (lastChanged && previousLastChange != lastChanged)
            termRepository.lastChanged = lastChanged
        println "******** Polling for changes since $previousLastChange took ${(System.currentTimeMillis() - time) / 1000 / 60} minutes"
    }

    void persistLinksChangedSince(Long previousLastChange) {
        agrovocRepository.eachLinkChangedSince(previousLastChange) { link ->
            termRepository.persistLink(link)
            linkListeners.each { it.call(link) }
        }

    }

    private Long persistTermsChangedSince(Long previousLastChange) {
        Long lastChanged = null
        agrovocRepository.eachTermChangedSince(previousLastChange) { term ->
            termRepository.persistTerm(term)
            createListeners.each { it.call(term) }
            if (lastChanged < term.lastChanged)
                lastChanged = term.lastChanged as Long
        }
        return lastChanged
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

    List<Map<String, Object>> query(String query, String language) {
        termRepository.queryByLabel(query, language)
    }

    List<Map<String, Object>> getLinksByCode(long code, String language) {
        termRepository.getLinksByCode(code, language)
    }
}
