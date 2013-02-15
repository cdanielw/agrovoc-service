package agrovoc.util.persistence

import groovy.sql.GroovyResultSet
import groovy.sql.GroovyRowResult

/**
 * @author Daniel Wiell
 */
class RowResult extends HashMap<String, Object> {

    RowResult(GroovyResultSet row) {
        this(row.toRowResult())
    }

    RowResult(GroovyRowResult row) {
        row.each {
            def property = columnNameToProperty(it.key)
            put(property, it.value)
        }
    }

    Object get(Object property) {
        get(property as String)
    }

    Object get(String property) {
        if (!containsKey(property))
            throw new IllegalArgumentException("Non-existing property: ${property}. Valid properties are ${keySet()}")
        super.get(property)
    }

    Object getAt(String property) {
        get(property)
    }

    Object getProperty(String property) {
        get(property)
    }

    private String columnNameToProperty(columnName) {
        StringBuilder property = new StringBuilder()
        boolean nextUpper = false
        columnName.toString().toLowerCase().toCharArray().each {
            if (it == '_' as char) {
                nextUpper = true
            } else if (nextUpper) {
                property.append(it.toUpperCase())
                nextUpper = false
            } else {
                property.append(it)
            }
        }
        return property as String
    }
}