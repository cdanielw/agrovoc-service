package agrovoc.dto

/**
 * @author Daniel Wiell
 */
class ByLabelQuery {
    enum Match {
        exact, startsWith, freeText


        static List<String> names() {
            values().collect { it.name() }
        }
    }

    final String string
    final int max
    final Match match
    final Set<RelationshipType> relationshipTypes
    final String language
    private final Locale locale

    ByLabelQuery(String string,
                 int max,
                 String match,
                 Collection<String> relationshipTypes,
                 String language) {
        this.string = string
        this.max = max
        this.match = match as Match
        this.relationshipTypes = RelationshipType.createRelationshipTypes(relationshipTypes)
        this.language = language.toUpperCase()
        this.locale = new Locale(language)
    }

    String toLowerCase() {
        string.toLowerCase(locale)
    }
}
