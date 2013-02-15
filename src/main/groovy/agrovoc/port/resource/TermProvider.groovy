package agrovoc.port.resource
/**
 * @author Daniel Wiell
 */
interface TermProvider {
   Map getByCode(long code, String language)

    List<Map<String, Object>> query(String query, String language)

    List<Map<String, Object>> getLinksByCode(long code, String language)
}