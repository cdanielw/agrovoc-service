package agrovoc.adapter.persistence

/**
 * @author Daniel Wiell
 */
enum StatusType {
    DELETED(0), ALTERNATIVE_DESCRIPTOR(10), DESCRIPTOR(20), TOP_TERM_DESCRIPTOR(60),
    NON_DESCRIPTOR(70), PROPOSED_DESCRIPTOR(100), NOT_ACCEPTED(120),

    int id

    StatusType(int id) {
        this.id = id
    }

    String getIdString() {
        id as String
    }
}
