package agrovoc.port.agrovoc

/**
 * @author Daniel Wiell
 */
interface AgrovocRepository {
    void eachTermChangedSince(Long timestamp, Closure callback)

    void eachLinkChangedSince(Long timestamp, Closure callback)
}