package agrovoc.sparql

import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

import java.text.SimpleDateFormat
import java.util.regex.Pattern

import static groovyx.net.http.ContentType.JSON

/**
 * @author Daniel Wiell
 */
class SparqlEndpoint {
    private static final Pattern VALUE_PATTERN = ~/"(.*?)"/
    private final RESTClient client
    private final int limit = 100
    private final Map prefixes

    SparqlEndpoint(String url, Map prefixes = [:]) {
        this.client = new RESTClient(url)
        client.handler.failure = { HttpResponseDecorator resp ->
            throw new IOException("\n" +
                    new InputStreamReader(resp.entity.content).text + "\n\n" +
                    resp.context['http.request'].original.toString())
        }

        this.prefixes = prefixes
    }

    /**
     * Queries the end point. Paging is handled by component, so LIMIT and OFFSET should be excluded
     * @param query SPARQL query
     * @param callback Callback for each row
     * @return
     */
    int query(String query, Closure callback) {
        def prefixedQuery = prefixQuery(query)
        int offset = 0
        int totalCount = 0
        int count = limit
        while (count == limit) {
            def limitedQuery = addLimitAndOffset(prefixedQuery, offset)
            def resp = client.get(query: [query: limitedQuery], contentType: JSON)
            count = processResults(resp, callback)
            offset += limit
            totalCount += count
        }
        return totalCount
    }

    String uri(String prefix, String value) {
        "<${prefixes[prefix]}${value}>"
    }

    String value(String s) {
        def matcher = VALUE_PATTERN.matcher(s)
        assert matcher.find(), "Unable to extract value from $s"
        matcher.group(1)
    }

    Date date(String s) {
        dateFormat.parse(value(s))
    }

    String format(Date date) {
        dateFormat.format(date)
    }

    private SimpleDateFormat getDateFormat() {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    private String prefixQuery(String query) {
        def prefix = prefixes.collect { ['PREFIX', "$it.key:", "<$it.value>"].join(' ') }.join('\n')
        prefix + '\n' + query
    }

    private String addLimitAndOffset(String query, int offset) {
        query + "\nLIMIT $limit\nOFFSET $offset"
    }

    private int processResults(resp, Closure callback) {
        def names = resp.data.names as List
        def count = 0
        resp.data.values.each { List result ->
            def r = [:]
            names.eachWithIndex { name, i ->
                r[name] = result[i]
            }
            callback(r)
            count++
        }
        return count
    }
}
