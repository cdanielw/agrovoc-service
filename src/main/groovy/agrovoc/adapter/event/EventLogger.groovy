package agrovoc.adapter.event

import org.slf4j.LoggerFactory
import agrovoc.port.event.TermEventPublisher

/**
 * @author Daniel Wiell
 */
class EventLogger {
    void register(TermEventPublisher publisher) {
        def logger = LoggerFactory.getLogger(TermEventPublisher)
        publisher.registerCreateListener {
            logger.info("Created term ($it.code: $it.label)")
        }

        publisher.registerLinkListener {
            logger.info("Linked ($it.type: $it.start to $it.end)")
        }
    }

}
