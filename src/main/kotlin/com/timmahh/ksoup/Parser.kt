package com.timmahh.ksoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.InputStream
import kotlin.reflect.KMutableProperty1

/**
 * A container for each command to extract information from an Element,
 * and stuff it into an object instance field.
 */
internal data class SelectorConverter<in V : Any>(private val css: String,
                                         private val multi: Boolean = false,
                                         private val command: (Element, V) -> Unit) {

    fun convert(doc: Document, item: V): Unit = this.convert(doc.root() as Element, item)

    fun convert(element: Element, item: V) =
            if (multi) element.select(css).forEach { command(it, item) }
            else command(element.selectFirst(css), item)

}

abstract class ParseBuilder<V : Any> {
    abstract val build: SimpleParser<V>
}

/**
 * Don't know yet if we'll be keeping this, or just using the ExtractorBase.
 * Depends on how the more complicated use cases pan out.
 */
internal interface Parser<out V> {
    fun parse(html: Document): V
}

@KSoupDsl
abstract class ParserBase<V : Any>() : Parser<V> {

    constructor(instanceGenerator: () -> V) : this() { result(instanceGenerator) }

    internal lateinit var instanceGenerator: () -> V

    /**
     * Pass me a generator function for your result type.
     * I'll make you one of these, so you can stuff it with information from the page.
     */
    private fun result(generator: () -> V) {
        this.instanceGenerator = generator
    }

    /**
     * Pass me an instance, and I'll fill it in with data from the page.
     */
    private fun result(instance: V) = result { instance }

}

/**
 * Hit one page, get some data.
 */
open class SimpleParser<V: Any> : ParserBase<V> {

    constructor(): super()

    constructor(generator: () -> V): super(generator)

    private var selectorConverters: MutableList<SelectorConverter<V>> = mutableListOf()

    internal fun parse(element: Element, instance: V) =
            selectorConverters.forEach { it.convert(element, instance) }

    override fun parse(html: Document): V = instanceGenerator().apply { parse(html.root() as Element, this) }

    internal fun parse(byteStream: InputStream, charset: String = "UTF-8", baseUrl: String = ""): V =
            parse(Jsoup.parse(byteStream, charset, baseUrl))

    /**
     * If I find a match for your CSS selector, I'll call your extractor function, and pass it an Element.
     *
     * ## Usage:
     * ```kotlin
     * element(".p-nickname") { element, page ->
     *     page.username = element.text()
     * }
     * ```
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun element(css: String, convert: (Element, V) -> Unit) =
        selectorConverters.add(SelectorConverter(css, command = convert))

    /**
     * If I find a match for your CSS selector, I'll call your extractor function, and pass it an Element.
     *
     * ## Usage:
     * ```kotlin
     * element(".p-nickname", Element::text, GitHubPage::username)
     * ```
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun <P> element(css: String, toProperty: KMutableProperty1<in V, P>, from: Element.() -> P) =
        selectorConverters.add(SelectorConverter(css) { e, v -> toProperty.set(v, e.from()) })

    fun elements(css: String, convert: (Element, V) -> Unit) =
        selectorConverters.add(SelectorConverter(css, true, convert))

    /**
     * If I find a match for your CSS selector, I'll call your extractor function, and pass it a String.
     *
     * ## Usage:
     * ```kotlin
     * text(".p-name") { text, page ->
     *     page.fullName = text
     * }
     * ```
     */
    fun text(css: String, convert: (String, V) -> Unit) =
        element(css) { e, v -> convert(e.text(), v) }

    /**
     * If I find a match for your CSS selector, I'll stuff the results into your instance property.
     *
     * ## Usage:
     * ```kotlin
     * text(".p-name", GitHubPage::fullName)
     * ```
     */
    fun text(css: String, property: KMutableProperty1<V, String>) =
        element(css, property, Element::text)
//        extractionCommands.add(ExtractionCommand(css) { e, v -> property.set(v, e.text()) })

    fun float(css: String, convert: (Float, V) -> Unit) =
        element(css) { e, v -> convert(e.text().toFloatOrNull() ?: 0f, v) }
//        extractionCommands.add(ExtractionCommand(css) { e, v -> convert(e.text().toFloatOrNull() ?: 0f, v) })

    fun float(css: String, property: KMutableProperty1<V, Float>) =
        element(css, property) { text().toFloatOrNull() ?: 0f }
//        extractionCommands.add(ExtractionCommand(css) { e, v -> property.set(v, e.text().toFloatOrNull() ?: 0f)})

    fun double(css: String, convert: (Double, V) -> Unit) =
        element(css) { e, v -> convert(e.text().toDoubleOrNull() ?: 0.0, v) }
//        extractionCommands.add(ExtractionCommand(css) { e, v -> convert(e.text().toDoubleOrNull() ?: 0.0, v)})

    fun double(css: String, property: KMutableProperty1<V, Double>) =
        element(css, property) { text().toDoubleOrNull() ?: 0.0 }

    fun int(css: String, convert: (Int, V) -> Unit) =
        element(css) { e, v -> convert(e.text().toIntOrNull() ?: 0, v) }

    fun int(css: String, property: KMutableProperty1<V, Int>) =
        element(css, property) { text().toIntOrNull() ?: 0 }

    fun long(css: String, convert: (Long, V) -> Unit) =
        element(css) { e, v -> convert(e.text().toLongOrNull() ?: 0L, v) }

    fun long(css: String, property: KMutableProperty1<V, Long>) =
        element(css, property) { text().toLongOrNull() ?: 0L }

    /**
     * Finds a match for the CSS selector and passes the Element to another SimpleParser in order
     * to nest/reuse selector statements.
     */
    fun parser(css: String, parser: NestedParser<V>.() -> Unit) =
        element(css, NestedParser<V>().apply(parser)::parse)

    class NestedParser<V : Any> : SimpleParser<V>()

}
