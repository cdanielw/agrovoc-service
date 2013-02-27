package agrovoc.adapter.persistence

import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.RelationshipType

import static agrovoc.dto.RelationshipType.*

/**
 * @author Daniel Wiell
 */
enum LinkType {
    SUBCLASS_OF(100, broader), HAS_BROARDER_SYNONYM(532, broader),

    HAS_SUBCLASS(101, narrower), INCLUDES(716, narrower), INCLUDED_IN(717, broader),
    HAS_NARROWER_SYNONYM(531, narrower),

    HAS_SYNONYM(530, alternative), HAS_NEAR_SYNONYM(568, alternative), IS_ACRONYM_OF(511, alternative), HAS_ACRONYM(510, alternative),
    IS_ABBREVIATION_OF(570, alternative), HAS_ABBREVIATION(571, alternative), HAS_LOCAL_NAME(533, alternative)

    final int id
    final agrovoc.dto.RelationshipType relationshipType
    final RelationshipType neo4jType

    LinkType(int id, agrovoc.dto.RelationshipType relationshipType) {
        this.id = id
        this.relationshipType = relationshipType
        this.neo4jType = DynamicRelationshipType.withName(id as String);
    }

    static RelationshipType[] neo4jTypesFor(Collection<agrovoc.dto.RelationshipType> types) {
        values().findAll { linkType ->
            linkType.relationshipType in types
        }.collect {
            it.neo4jType
        } as RelationshipType[]
    }
}
