package agrovoc.dto

/**
 * @author Daniel Wiell
 */
class TermDescription {
    static final int PREFERRED_STATUS = 20
    final long code
    final int status
    final String label
    final String language

    TermDescription(long code, int status, String label, String language) {
        assert code
        assert language
        assert label
        this.code = code
        this.status = status
        this.label = label
        this.language = language.toUpperCase()
    }

    boolean isPreferred() {
        status == PREFERRED_STATUS
    }

    public java.lang.String toString() {
        "$label ($status)"
    }
}
