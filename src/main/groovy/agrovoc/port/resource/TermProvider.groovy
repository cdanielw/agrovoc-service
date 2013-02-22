package agrovoc.port.resource

import agrovoc.dto.ByCodeQuery
import agrovoc.dto.ByLabelQuery
import agrovoc.dto.RelationshipQuery
import agrovoc.dto.TermDescription

/**
 * @author Daniel Wiell
 */
interface TermProvider {
    List<TermDescription> findAllByCode(ByCodeQuery query)

    List<TermDescription> findAllByLabel(ByLabelQuery query)

    List<TermDescription> findRelationships(RelationshipQuery query)
}