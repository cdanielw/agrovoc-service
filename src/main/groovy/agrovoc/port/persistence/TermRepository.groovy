package agrovoc.port.persistence

import agrovoc.dto.ByCodeQuery
import agrovoc.dto.ByLabelQuery
import agrovoc.dto.RelationshipQuery
import agrovoc.dto.TermDescription

/**
 * @author Daniel Wiell
 */
interface TermRepository {
    List<TermDescription> findAllByCode(ByCodeQuery query)

    List<TermDescription> findAllByLabel(ByLabelQuery query)

    List<TermDescription> findRelatedTerms(RelationshipQuery query)
}