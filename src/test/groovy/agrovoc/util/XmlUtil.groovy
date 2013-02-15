package agrovoc.util

import org.w3c.dom.Element

import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * @author Daniel Wiell
 */
class XmlUtil {

    static String serialize(Element element) {
        StringWriter sw = new StringWriter()
        def source = new DOMSource(element)
        def target = new StreamResult(sw)
        TransformerFactory factory = TransformerFactory.newInstance()
        setIndent(factory, 2)
        try {
            Transformer transformer = factory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.METHOD, "xml")
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml")
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            transformer.transform(source, target)
        }
        catch (TransformerException e) {
            throw new GroovyRuntimeException(e.getMessage())
        }

        return sw.toString()
    }


    private static void setIndent(TransformerFactory factory, int indent) {
        try {
            factory.setAttribute("indent-number", indent)
        } catch (IllegalArgumentException ignore) {
            // ignore for factories that don't support this
        }
    }
}
