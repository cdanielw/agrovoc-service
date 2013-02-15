package agrovoc.adapter.agrovoc

import agrovoc.dto.Term
import agrovoc.dto.TermLinks
import agrovoc.port.agrovoc.AgrovocRepository
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

    void eachTermChangedSince(Date date, Closure callback) {
        if (!date) date = new Date(0)
        Term term = null
        sql.eachRow('''
                SELECT termcode, statusid, scopeid, lastupdate, languagecode, termspell
                FROM agrovocterm
                WHERE lastupdate > ? AND termspell != ''
                ORDER BY termcode, lastupdate DESC
                ''', [date]) { GroovyResultSet rs ->
            if (term?.code != rs.getLong('termcode')) {
                if (term) callback.call(term)
                term = createTerm(rs)
            }
            term.labelByLanguage[rs.getString('languagecode')] = rs.getString('termspell')
        }
        if (term) callback.call(term)
    }

    void eachLinkChangedSince(Date date, Closure callback) {
        println 'Each since changed'
        if (!date) date = new Date(0)
        TermLinks termLinks = null
        sql.eachRow('''
                SELECT termcode1, termcode2 end, newlinktypeid
                FROM termlink l
                JOIN agrovocterm t ON t.termcode = l.termcode1
                WHERE lastupdate > ? AND termspell != ''
                GROUP BY termcode1, termcode2, newlinktypeid
                ORDER BY termcode1
                ''', [date]) { GroovyResultSet rs ->
            def startTermCode = rs.getLong('termcode1')
            if (termLinks?.startTermCode != startTermCode) {
                if (termLinks) callback.call(termLinks)
                termLinks = new TermLinks(startTermCode: startTermCode)
            }
            termLinks.add(rs.getLong('termcode2'), rs.getInt('newlinktypeid'))
        }
        if (termLinks) callback.call(termLinks)
    }

    private Term createTerm(GroovyResultSet rs) {
        new Term(
                code: rs.getLong('termcode'),
                status: rs.getInt('statusid'),
                scope: rs.getString('scopeid'),
                lastChanged: rs.getDate('lastupdate'))
    }
}
