package agrovoc.port.persistence

/**
 * @author Daniel Wiell
 */
interface TermRepository {
    Map<String, Object> getByCode(long code, String language)

    List<Map<String, Object>> queryByLabel(String label, String language)

    List<Map<String, Object>> getLinksByCode(long code, String language)

    void persistTerm(Map<String, Object> term)

    void persistLink(Map<String, Object> link)

    Long getLastChanged()

    void setLastChanged(Long timestamp)
}