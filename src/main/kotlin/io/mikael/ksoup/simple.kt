package io.mikael.ksoup

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


internal data class ElementExtraction<in V: Any>(val css: String, val extract: (Element, V) -> Unit) {

    fun extract(doc: Document, item: V) = doc.select(css).forEach { element -> extract(element, item) }

}

/**
 * Hit one page, get some data.
 */
class SimpleExtractor<V: Any>(url: String = "") : ExtractorBase<V>() {

    init {
        this.urlGenerator = { url }
    }

    internal val elementExtractions: MutableList<ElementExtraction<V>> = mutableListOf()

    override fun extract(): V {
        val instance = instanceGenerator()
        val doc = document()
        elementExtractions.forEach { it.extract(doc, instance) }
        return instance
    }

    /**
     * If I element a match for your CSS selector, I'll call your extractor function, and pass it an Element.
     */
    fun element(css: String, extract: (Element, V) -> Unit) =
            elementExtractions.add(ElementExtraction(css, extract))

    /**
     * If I element a match for your CSS selector, I'll call your extractor function, and pass it a String.
     */
    fun text(css: String, extract: (String, V) -> Unit) =
            elementExtractions.add(ElementExtraction(css, { e, v -> extract(e.text(), v) }))

}
