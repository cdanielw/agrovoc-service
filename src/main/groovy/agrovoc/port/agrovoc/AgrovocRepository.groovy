package agrovoc.port.agrovoc

/**
 * @author Daniel Wiell
 */
interface AgrovocRepository {
    void eachTermChangedSince(Date date, Closure callback)

    void eachLinkChangedSince(Date date, Closure callback)
}