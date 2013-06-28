package agrovoc.adapter.persistence

import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.RelationshipType

import static agrovoc.dto.RelationshipType.*

/**
 * @author Daniel Wiell
 */
enum LinkType {
    SUBCLASS_OF(100, broader, 'http://www.w3.org/2004/02/skos/core#broader'),
    HAS_BROARDER_SYNONYM(532, broader, 'http://aims.fao.org/aos/agrontology#hasBroaderSynonym'),

    HAS_SUBCLASS(101, narrower, 'http://www.w3.org/2004/02/skos/core#narrower'),
    INCLUDES(716, narrower, 'http://aims.fao.org/aos/agrontology#includes'),
    INCLUDED_IN(717, broader, 'http://aims.fao.org/aos/agrontology#isIncludedIn'),
    HAS_NARROWER_SYNONYM(531, narrower, 'http://aims.fao.org/aos/agrontology#hasNarrowerSynonym'),

    HAS_SYNONYM(530, alternative, 'http://aims.fao.org/aos/agrontology#hasSynonym'),
    HAS_NEAR_SYNONYM(568, alternative, 'http://aims.fao.org/aos/agrontology#hasNearSynonym'),
    IS_ACRONYM_OF(511, alternative, 'http://aims.fao.org/aos/agrontology#isAcronymOf'),
    HAS_ACRONYM(510, alternative, 'http://aims.fao.org/aos/agrontology#hasAcronym'),
    IS_ABBREVIATION_OF(570, alternative, 'http://aims.fao.org/aos/agrontology#isAbbreviationOf'),
    HAS_ABBREVIATION(571, alternative, 'http://aims.fao.org/aos/agrontology#hasAbbreviation'),
    HAS_LOCAL_NAME(533, alternative, 'http://aims.fao.org/aos/agrontology#hasLocalName')

    final int id
    final agrovoc.dto.RelationshipType relationshipType
    final String uri
    final RelationshipType neo4jType

    LinkType(int id, agrovoc.dto.RelationshipType relationshipType, String uri) {
        this.id = id
        this.relationshipType = relationshipType
        this.neo4jType = DynamicRelationshipType.withName(id as String);
        this.uri = uri
    }

    static int id(String uri) {
        values().find { it.uri == uri }?.id ?: -1
    }

    static RelationshipType[] neo4jTypesFor(Collection<agrovoc.dto.RelationshipType> types) {
        values().findAll { linkType ->
            linkType.relationshipType in types
        }.collect {
            it.neo4jType
        } as RelationshipType[]
    }
}
