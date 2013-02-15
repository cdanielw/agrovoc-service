package agrovoc.adapter.event

import agrovoc.dto.Term
import agrovoc.dto.TermLinks
import agrovoc.port.event.TermEventPublisher
import org.slf4j.LoggerFactory

/**
 * @author Daniel Wiell
 */
class EventLogger {
    void register(TermEventPublisher publisher) {
        def logger = LoggerFactory.getLogger(TermEventPublisher)
        publisher.registerCreateListener { Term term ->
            logger.info("Created term $term")
        }

        publisher.registerLinkListener { TermLinks termLinks ->
            logger.info("Linked $termLinks")
        }
    }
}
