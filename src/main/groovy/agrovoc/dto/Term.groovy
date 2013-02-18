package agrovoc.dto

/**
 * @author Daniel Wiell
 */
class Term {
    final long code
    final String scope
    final Date lastChanged
    final Map<String, TermDescription> descriptionByLanguage = [:]

    Term(long code, String scope, Date lastChanged) {
        this.code = code
        this.scope = scope
        this.lastChanged = lastChanged
        assert code
        assert lastChanged
    }

    public java.lang.String toString() {
        "$code: $descriptionByLanguage"
    }
}

class TermDescription {
    final String language
    final int status
    final String label

    TermDescription(String language, int status, String label) {
        this.language = language
        this.status = status
        this.label = label
        assert language
        assert label
    }

    public java.lang.String toString() {
        "$label ($status)"
    }
}

class TermLinks {
    long startTermCode
    private List<List> ends = []

    TermLinks add(long endTermCode, int type) {
        ends << [endTermCode, type]
        return this
    }

    void each(Closure callback) {
        ends.each {
            callback.call(it[0], it[1])
        }
    }

    String toString() {
        "$startTermCode -> ${ends.collect { "${it[0]} (${it[1]})" }.join(', ')}"
    }
}