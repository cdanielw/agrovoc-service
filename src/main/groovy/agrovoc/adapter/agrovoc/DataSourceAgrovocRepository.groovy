package agrovoc.adapter.agrovoc
import agrovoc.port.agrovoc.AgrovocRepository
import agrovoc.util.persistence.RowResult
import groovy.sql.GroovyResultSet
import groovy.sql.Sql

import javax.sql.DataSource
/**
 * @author Daniel Wiell
 */
class DataSourceAgrovocRepository implements AgrovocRepository {
    private final Sql sql

    DataSourceAgrovocRepository(DataSource dataSource) {
        sql = new Sql(dataSource)
    }

    void eachTermChangedSince(Long timestamp, Closure callback) {
        def lastChanged = new Date(timestamp ?: 0)
        sql.eachRow('''
                SELECT termcode code, termspell label, languagecode language, statusid status, scopeid scope, lastupdate last_changed
                FROM agrovocterm
                WHERE lastupdate > ? AND termspell != ''
                ORDER BY termcode
                ''', [lastChanged]) { GroovyResultSet rs ->
            def result = new RowResult(rs)
            result.lastChanged = result.lastChanged.time
            callback.call(result)
        }
    }

    void eachLinkChangedSince(Long timestamp, Closure callback) {
        def lastChanged = new Date(timestamp ?: 0)
        sql.eachRow('''
                SELECT termcode1 start, termcode2 end, newlinktypeid type
                FROM termlink l
                JOIN agrovocterm t ON t.termcode = l.termcode1
                WHERE lastupdate > ? AND termspell != ''
                GROUP BY termcode1, termcode2, newlinktypeid
                ORDER BY termcode1
                ''', [lastChanged]) { GroovyResultSet rs ->
            def result = new RowResult(rs)
            callback.call(result)
        }
    }
}
