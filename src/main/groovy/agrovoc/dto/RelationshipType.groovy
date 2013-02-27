package agrovoc.dto

/**
 * @author Daniel Wiell
 */
enum RelationshipType {
    alternative, broader, narrower

    static Set<RelationshipType> createRelationshipTypes(Collection<String> types) {
        types ? (types.collect { it as RelationshipType } as Set) : new HashSet()
    }

    static List<String> names() {
        values().collect { it.name() }
    }
}
