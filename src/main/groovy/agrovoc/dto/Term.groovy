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
        assert code
        assert lastChanged
        this.code = code
        this.scope = scope
        this.lastChanged = lastChanged
    }

    public java.lang.String toString() {
        "$code: $descriptionByLanguage"
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