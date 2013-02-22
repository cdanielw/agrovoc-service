package agrovoc.adapter.persistence

/**
 * @author Daniel Wiell
 */
class LanguageFallback {
    private final List descriptions = []

    Long getFallbackLinkId() {
        return null
    }

    void add(linkId, String language, int status) {
        descriptions << [linkId, language, status]
    }
}
