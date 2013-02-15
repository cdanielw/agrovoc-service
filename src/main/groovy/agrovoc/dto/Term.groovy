package agrovoc.dto

/**
 * @author Daniel Wiell
 */
class Term {
    long code
    int status
    String scope
    Date lastChanged
    Map<String, String> labelByLanguage = [:]

    public java.lang.String toString() {
        return "$code: $labelByLanguage"
    }
}
