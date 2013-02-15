package agrovoc.port.event

/**
 * Domain object publishing term events.
 * @author Daniel Wiell
 */
interface TermEventPublisher {
    void registerCreateListener(Closure callback)

    void registerLinkListener(Closure callback)
}
