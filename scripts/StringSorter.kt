import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.ls.DOMImplementationLS
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun main() {
    val languageCodes =
        listOf("ar", "bn", "cy", "en", "gu", "pa", "pl", "ro", "tr", "ur", "zh", "so")
    for (code in languageCodes) {
        val updated = readUnsorted("./locale/$code.xml")
        updateValues(code, "../app/src/main/res/${getValuesDir(code)}/strings.xml", updated)
    }
}

private fun getValuesDir(languageCode: String): String {
    return if (languageCode == "en") "values" else "values-$languageCode"
}

private fun updateValues(
    languageCode: String,
    filename: String,
    updatedStrings: MutableMap<String, String>
) {
    val doc = parse(filename)

    updatePlurals(doc, updatedStrings)
    updateRegularStrings(doc, updatedStrings)
    doc.xmlStandalone = true

    val outputDirPath = "../app/src/main/res/${getValuesDir(languageCode)}"
    val outputFile = File("$outputDirPath/strings.xml")
    val outputFileWriter = OutputStreamWriter(
        FileOutputStream(outputFile),
        Charset.forName("UTF-8").newEncoder()
    )
    val result = StreamResult(outputFileWriter)

    val transformerFactory = TransformerFactory.newInstance()
//    transformerFactory.setAttribute("indent-number", 10)
    val transformer = transformerFactory.newTransformer()
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
//    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "10")

    val source = DOMSource(doc)

    transformer.transform(source, result)

    val readText = outputFile.readText()
    val updated = readText
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;amp;", "&amp;")
    outputFile.writeText(updated)
}

private fun updateRegularStrings(
    doc: Document,
    updatedStrings: MutableMap<String, String>
) {
    val strings = doc.getElementsByTagName("string")
    val updatedStrings = (updatedStrings.filterKeys { !it.contains("|") }).toMutableMap()

    for (temp in 0 until strings.length) {
        val node = strings.item(temp)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val element = node as Element
            val name = element.getAttribute("name")
            val updatedValue = updatedStrings[name]
            if (updatedValue == null) {
                println("No new value for string name: $name")
            } else {
                element.textContent = updatedValue
                updatedStrings.remove(name)
            }
        }
    }

    val resources = doc.getElementsByTagName("resources").item(0)

    updatedStrings.forEach { (name, value) ->
        if (value != "") {
            val newElement = doc.createElement("string")
            newElement.setAttribute("name", name)
            newElement.textContent = value
            resources.appendChild(newElement)
        }
    }
}

private fun updatePlurals(
    doc: Document,
    updatedStrings: MutableMap<String, String>
) {
    val updatedPlurals = (updatedStrings.filterKeys { it.contains("|") }).toMutableMap()

    val plurals = doc.getElementsByTagName("plurals")

    for (temp in 0 until plurals.length) {
        val node = plurals.item(temp)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val pluralElement = node as Element
            val pluralName = pluralElement.getAttribute("name")

            val items = pluralElement.getElementsByTagName("item")
            for (itemIndex in 0 until items.length) {
                val itemNode = items.item(itemIndex)
                val element = itemNode as Element
                val quantity = element.getAttribute("quantity")
                val updatedValue = updatedPlurals["$pluralName|$quantity"]
                if (updatedValue != null) {
                    element.textContent = updatedValue
                    updatedPlurals.remove("$pluralName|$quantity")
                } else {
                    println("No new value for plural name: $pluralName and quantity: $quantity")
                }
            }
        }
    }

    updatedPlurals.forEach { (name, value) ->
        val (pluralName, quantity) = name.split("|")

        val foundParent = addQuantityToExistingPlural(doc, plurals, pluralName, quantity, value)
        if (!foundParent) {
            val newPlural = doc.createElement("plurals")
            newPlural.setAttribute("name", pluralName)
            doc.documentElement.appendChild(newPlural)
            val added = addQuantityToExistingPlural(doc, plurals, pluralName, quantity, value)
            if (!added) {
                println("Plural was omitted: $name: $value")
            }
        }
    }
}

private fun addQuantityToExistingPlural(
    doc: Document,
    plurals: NodeList,
    pluralName: String,
    quantity: String,
    value: String
): Boolean {
    var foundParent = false
    for (temp in 0 until plurals.length) {
        val node = plurals.item(temp)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val pluralElement = node as Element
            val name = pluralElement.getAttribute("name")
            if (pluralName == name) {
                val newElement = doc.createElement("item")
                newElement.setAttribute("quantity", quantity)
                newElement.textContent = value
                pluralElement.appendChild(newElement)
                foundParent = true
            }
        }
    }
    return foundParent
}

private fun readUnsorted(filename: String): MutableMap<String, String> {
    val doc = parse(filename)

    val strings = doc.getElementsByTagName("string")
    val entries = mutableMapOf<String, String>()

    for (temp in 0 until strings.length) {
        val node = strings.item(temp)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val element = node as Element
            val name = element.getAttribute("name")
            entries[name] = innerXml(element).lines().joinToString(" ") { it.trim() }.trim()
        }
    }

    val plurals = doc.getElementsByTagName("plurals")

    for (temp in 0 until plurals.length) {
        val node = plurals.item(temp)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val pluralElement = node as Element
            val pluralName = pluralElement.getAttribute("name")
            val items = pluralElement.getElementsByTagName("item")
            for (itemIndex in 0 until items.length) {
                val itemNode = items.item(itemIndex)
                val element = itemNode as Element
                val quantity = element.getAttribute("quantity")
                entries["$pluralName|$quantity"] =
                    element.textContent.lines().joinToString(" ") { it.trim() }.trim()
            }
        }
    }

    return entries
}

fun innerXml(node: Node): String {
    val lsImpl = node.ownerDocument.implementation.getFeature("LS", "3.0") as DOMImplementationLS
    val lsSerializer = lsImpl.createLSSerializer()
    lsSerializer.domConfig.setParameter("xml-declaration", false)
    val childNodes = node.childNodes
    val sb = StringBuilder()
    for (i in 0 until childNodes.length) {
        sb.append(lsSerializer.writeToString(childNodes.item(i)))
    }
    return sb.toString()
}

private fun parse(filename: String): Document {
    val fXmlFile = File(filename)
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val doc: Document = dBuilder.parse(fXmlFile)

    doc.documentElement.normalize()
    return doc
}
