package agrovoc.dto

/**
 * @author Daniel Wiell
 */
class ByCodeQuery {
    final List<Long> codes
    final String language

    ByCodeQuery(List<Long> codes, String language) {
        this.codes = codes
        this.language = language.toUpperCase()
    }
}
