package agrovoc.dto

/**
 * @author Daniel Wiell
 */
class LabelQuery {
    final String string
    final String language
    final int max

    LabelQuery(String string, String language, int max) {
        this.string = string
        this.language = language
        this.max = max
    }
}
