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
        "$code: $labelByLanguage"
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