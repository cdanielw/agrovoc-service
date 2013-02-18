package agrovoc.adapter.persistence

import org.apache.lucene.search.FieldComparator
import org.apache.lucene.search.FieldComparatorSource
import org.apache.lucene.search.SortField

/**
 * @author Daniel Wiell
 */
class CaseInsensitiveSortField extends SortField {
    CaseInsensitiveSortField(String field, String language) {
        super(field, new CaseInsensitiveFieldComparatorSource(field, new Locale(language)))
    }

    static class CaseInsensitiveFieldComparatorSource extends FieldComparatorSource {
        private final String field
        private final Locale locale

        CaseInsensitiveFieldComparatorSource(String field, Locale locale) {
            this.field = field
            this.locale = locale
        }

        FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
//            new FieldComparator.StringComparatorLocale(numHits, fieldname, locale)
            null
        }
    }
}