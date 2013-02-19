package agrovoc.adapter.persistence

import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.RelationshipType

/**
 * @author Daniel Wiell
 */
enum LinkType {
    SUBCLASS_OF(100), HAS_SUBCLASS(101),
    INCLUDES(716), INCLUDED_IN(717),
    HAS_SYNONYM(530), HAS_NEAR_SYNONYM(568), HAS_BROARDER_SYNONYM(532), HAS_NARROWER_SYNONYM(531),
    IS_ACRONYM_OF(511), HAS_ACRONYM(510), IS_ABBREVIATION_OF(570), HAS_ABBREVIATION(571),
    HAS_LOCAL_NAME(533)

    final int id
    final RelationshipType type

    LinkType(int id) {
        this.id = id
        this.type = DynamicRelationshipType.withName(id as String);
    }
}
