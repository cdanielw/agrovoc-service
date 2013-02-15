package agrovoc.port.persistence

import agrovoc.dto.Term

/**
 * @author Daniel Wiell
 */
interface TermPersister {
    void persistTerm(Term term)

    void persistLink(Map<String, Object> link)

    Date getLastChanged()

    void setLastChanged(Date lastChanged)
}