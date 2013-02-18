package agrovoc.port.resource

import agrovoc.dto.LabelQuery

/**
 * @author Daniel Wiell
 */
interface TermProvider {
    Map getByCode(long code, String language)

    Map<String, Object> findByLabel(String label, String language)

    List<Map<String, Object>> findAllWhereLabelStartsWith(LabelQuery query)

    List<Map<String, Object>> findAllWhereWordInLabelStartsWith(LabelQuery query)

    List<Map<String, Object>> getLinksByCode(long code, String language)
}