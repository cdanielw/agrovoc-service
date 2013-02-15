package agrovoc.fake

import groovy.sql.Sql
import org.h2.jdbcx.JdbcDataSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.sql.DataSource

/**
 * @author Daniel Wiell
 */
class AgrovocDatabase {
    private static final Logger LOG = LoggerFactory.getLogger(this.class)
    private static final File SCHEMA = new File('src/main/db', 'agrovoc-schema.sql')
    private static final File RESET_SCRIPT = new File('src/main/db', 'agrovoc-reset.sql')
    private static final String URL = "jdbc:h2:mem:agrovoc;MODE=MYSQL;DB_CLOSE_DELAY=-1"

    private static boolean initiated
    private static DataSource dataSource

    AgrovocDatabase() {
        initDatabase()
    }

    synchronized DataSource getDataSource() { dataSource }

    private synchronized void initDatabase() {
        if (!initiated) {
            initiated = true
            long time = System.currentTimeMillis()
            dataSource = new JdbcDataSource(url: AgrovocDatabase.URL,
                    user: 'sa', password: 'sa')
            setupSchema()
            LOG.info("Setup database in ${System.currentTimeMillis() - time} millis.")
        }
    }

    private void setupSchema() {
        def schema = SCHEMA.getText('UTF-8')
        new Sql(dataSource).execute(schema)
    }

    void reset() {
        long time = System.currentTimeMillis()
        def resetScript = RESET_SCRIPT.getText('UTF-8')
        new Sql(dataSource).execute(resetScript)
        LOG.info("Reset database in ${System.currentTimeMillis() - time} millis.")
    }

    void insertTerm(term) {
        new Sql(dataSource).executeInsert('INSERT INTO agrovocterm(termcode, termspell, statusid, languagecode, scopeid, lastupdate) VALUES(?, ?, ?, ?, ?, ?)', [
                term.code,
                term.label,
                term.status ?: 20,
                term.language ?: 'EN',
                term.scope ?: '',
                term.lastChange ?: new Date()])
    }

    void insertLink(link) {
        new Sql(dataSource).executeInsert('INSERT INTO termlink(termcode1, termcode2, newlinktypeid) VALUES(?, ?, ?)', [
                link.start,
                link.end,
                link.type])
    }
}
