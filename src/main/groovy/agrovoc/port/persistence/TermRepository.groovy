package agrovoc.port.persistence

import agrovoc.dto.LabelQuery

/**
 * @author Daniel Wiell
 */
interface TermRepository {
    Map<String, Object> getByCode(long code, String language)

    Map<String, Object> findByLabel(String label, String language)

    List<Map<String, Object>> findAllWhereLabelStartsWith(LabelQuery query)

    List<Map<String, Object>> findAllWhereWordInLabelStartsWith(LabelQuery query)

    List<Map<String, Object>> getLinksByCode(long code, String language)
}