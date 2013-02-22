package agrovoc.dto

/**
 * @author Daniel Wiell
 */
class RelationshipQuery {
    long code
    int max
    Set<RelationshipType> relationshipTypes
    String language

    RelationshipQuery(long code, int max, Collection<String> relationshipTypes, String language) {
        this.code = code
        this.max = max
        this.relationshipTypes = RelationshipType.createRelationshipTypes(relationshipTypes)
        this.language = language
    }
}