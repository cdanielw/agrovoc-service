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

    public String toString() {
        "$code: $descriptionByLanguage"
    }
}


class TermLinks {
    final long startTermCode
    private List<List> ends = []

    TermLinks(long startTermCode) {
        this.startTermCode = startTermCode
    }

    TermLinks add(long endTermCode, int type) {
        ends << [endTermCode, type, null]
        return this
    }

    TermLinks add(long endTermCode, int type, String uri) {
        ends << [endTermCode, type, uri]
        return this
    }

    void each(Closure callback) {
        ends.each {
            callback.call(it[0], it[1])
        }
    }

    String toString() {
        "$startTermCode -> ${ends.collect { "${it[0]} (${it[1]}${it[2] ? ": ${it[2]}" : ''})" }.join(', ')}"
    }
}