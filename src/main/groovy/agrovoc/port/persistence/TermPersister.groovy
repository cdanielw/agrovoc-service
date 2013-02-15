package agrovoc.port.persistence

import agrovoc.dto.Term
import agrovoc.dto.TermLinks

/**
 * @author Daniel Wiell
 */
interface TermPersister {
    void persistTerm(Term term)

    void persistLinks(TermLinks links)

    Date getLastChanged()

    void setLastChanged(Date lastChanged)
}