/*
 * Copyright 2019 Timothy Logan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.timmahh.ksoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.InputStream
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

interface Converter<in V : Any> {
	fun convert(element: Element, item: V)
}

/**
 * A container for each command to extract information from an Element,
 * and stuff it into an object instance field.
 */
internal data class SelectorConverter<in V : Any>(
		private val css: String,
		private val firstOnly: Boolean = true,
		private val command: (Element, V) -> Unit) : Converter<V> {

//    fun convert(doc: Document, item: V): Unit = this.convert(doc.root() as Element, item)
	
	override fun convert(element: Element, item: V) =
			if (firstOnly) command(element.selectFirst(css), item)
			else element.select(css).forEach { command(it, item) }
	
}

internal data class CollectionConverter<in V : Any, T : Any>(
		private val css: String,
		private val transform: (Element) -> T,
		private val command: (List<T>, V) -> Unit) : Converter<V> {
	override fun convert(element: Element, item: V) =
			command(element.select(css).map(transform), item)
}

internal data class MapConverter<in V : Any, K : Any, T : Any>(
		private val css: String,
		private val keySelector: (Element) -> K,
		private val valueSelector: (Element) -> T,
		private val command: (Map<K, T>, V) -> Unit
                                                              ) : Converter<V> {
	override fun convert(element: Element, item: V) =
			command(element.select(css).associateBy(keySelector, valueSelector), item)
}

abstract class ParseBuilder<V : Any> {
	
	operator fun invoke(): SimpleParser<V> = this.build
	
	protected abstract val build: SimpleParser<V>
}

/**
 * Don't know yet if we'll be keeping this, or just using the ExtractorBase.
 * Depends on how the more complicated use cases pan out.
 */
internal interface Parser<out V> {
	fun parse(root: Element): V
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
	
	private var selectorConverters: MutableList<Converter<V>> = mutableListOf()

    internal fun parse(element: Element, instance: V) =
            selectorConverters.forEach { it.convert(element, instance) }
	
	override fun parse(root: Element): V = instanceGenerator().apply { parse(root, this) }

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
		    selectorConverters.plusAssign(SelectorConverter(css, command = convert))

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
		    selectorConverters.plusAssign(SelectorConverter(css) { e, v -> toProperty.set(v, e.from()) })
	
	private fun elements(css: String, convert: (Element, V) -> Unit) =
			selectorConverters.plusAssign(SelectorConverter(css, true, convert))

/*    fun <P : Collection<T>, T : Any> elements(css: String, toProperty: KMutableProperty1<in V, P>, convert: Elements.() -> T) {

    }*/

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
	
	private inline fun <T : Any> multi(css: String,
	                                   builder: SimpleParser<T>,
	                                   crossinline convert: V.(T) -> Unit) =
//        selectorConverters.plusAssign()
			elements(css) { e, v -> v.convert(builder.parse(e)) }
	
	fun <C : MutableCollection<T>, T : Any> collection(css: String,
	                                                   property: KProperty1<V, C>,
	                                                   builder: SimpleParser<T>) =
			multi(css, builder) { e -> property.get(this) += e }
	
	fun <C : MutableCollection<T>, T : Any> collection(css: String,
	                                                   property: KProperty1<V, C>,
	                                                   transform: (Element) -> T) =
			selectorConverters.plusAssign(
					CollectionConverter(css, transform) { list, v ->
						property.get(v).addAll(list)
					}
			                             )
	
	fun <M : MutableMap<K, T>, K : Any, T : Any> map(css: String,
	                                                 property: KProperty1<V, M>,
	                                                 builder: SimpleParser<Pair<K, T>>) =
//        selectorConverters.plusAssign(MapConverter<V, K, T>(css, ))
			multi(css, builder) { e -> property.get(this) += e }
	
	fun <M : MutableMap<K, T>, K : Any, T : Any> map(css: String,
	                                                 property: KProperty1<V, M>,
	                                                 keySelector: (Element) -> K,
	                                                 valueSelector: (Element) -> T) =
			selectorConverters.plusAssign(
					MapConverter(css, keySelector, valueSelector) { map, v ->
						property.get(v).putAll(map)
					}
			                             )

    /**
     * Finds a match for the CSS selector and passes the Element to another SimpleParser in order
     * to nest/reuse selector statements.
     */
    fun parser(css: String,
               parser: NestedParser<V>.() -> Unit) =
        element(css, NestedParser<V>().apply(parser)::parse)

//    fun parser(css: String, parser: NestedParser<V>) = element(css, parser::parse)

    class NestedParser<V : Any> : SimpleParser<V>()

}
