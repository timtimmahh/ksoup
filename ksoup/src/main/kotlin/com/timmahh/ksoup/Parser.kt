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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.timmahh.ksoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.InputStream
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

interface Converter<in V : Any> {
	fun convert(element: Element?, item: V)
}

abstract class BaseConverter<in V : Any, P : Any>(protected open val css: String,
												  protected open val command: (P, V) -> Unit) : Converter<V>

/**
 * A container for each command to extract information from an Element,
 * and stuff it into an object instance field.
 */
internal data class SelectorConverter<in V : Any>(
	override val css: String,
	private val firstOnly: Boolean = true,
	override val command: (Element?, V) -> Unit
) : BaseConverter<V,
		Element>(css, command) {

//    fun convert(doc: Document, item: V): Unit = this.convert(doc.root() as Element, item)
	
	override fun convert(element: Element?, item: V) =
		css.takeUnless(String::isEmpty)?.let { css ->
			if (firstOnly) command(element?.selectFirst(css), item)
			else element?.select(css)?.forEach { command(it,	item) }
		} ?: command(element?.also { it.logE("SelectorConverter") }, item)
	
}

/**
 * A container for each command to extract information from an Element,
 * and stuff it into an object instance field.
 */
internal data class AllSelectorConverter<in V : Any>(
		override val css: String,
		override val command: (Elements?, V) -> Unit) : BaseConverter<V,
		Elements>(css, command) {
	
	override fun convert(element: Element?, item: V) =
			command(element?.select(css), item)
	
}

internal data class CollectionConverter<in V : Any, T : Any>(
	override val css: String,
	private val transform: (Element) -> T,
	override val command: (List<T>?, V) -> Unit
) : BaseConverter<V,
		List<T>>(css, command) {
	override fun convert(element: Element?, item: V) =
			command(element?.select(css)?.map(transform), item)
}

internal data class MapConverter<in V : Any, K : Any, T : Any>(
	override val css: String,
	private val keySelector: (Element) -> K,
	private val valueSelector: (Element) -> T,
	override val command: (Map<K, T>?, V) -> Unit
                                                              ) :
		BaseConverter<V, Map<K, T>>(css, command) {
	override fun convert(element: Element?, item: V) =
			command(element?.select(css)?.associateBy(keySelector, valueSelector), item)
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
	fun parse(root: Element?): V
}

@KSoupDsl
abstract class ParserBase<V : Any>() : Parser<V> {
	
	constructor(instanceGenerator: () -> V) : this() {
		result(instanceGenerator)
	}
	
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
open class SimpleParser<V : Any> : ParserBase<V> {
	
	constructor() : super()
	
	constructor(generator: () -> V) : super(generator)
	
	private var selectorConverters: MutableList<Converter<V>> = mutableListOf()
	
	internal fun parse(element: Element?, instance: V) =
			selectorConverters.forEach { it.convert(element, instance) }
	
	override fun parse(root: Element?): V = instanceGenerator().apply { parse(root, this) }
	
	fun parse(byteStream: InputStream, charset: String = "UTF-8", baseUrl: String = ""): V =
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
	fun select(css: String, convert: (Element?, V) -> Unit) =
			selectorConverters.plusAssign(SelectorConverter(css, command = convert))
	
	/**
	 * If I find a match for your CSS selector, I'll call your extractor function, and pass it an Element.
	 *
	 * ## Usage:
	 * ```kotlin
	 * element(".p-nickname", Element::text, GitHubPage::username)
	 * ```
	 */
	fun <P> select(css: String, toProperty: KMutableProperty1<in V, P>, from: Element.() -> P) =
			selectorConverters.plusAssign(SelectorConverter(css) { e, v -> e?.let { toProperty.set(v, it.from()) } })
	
	fun selections(css: String, convert: (Element?, V) -> Unit) =
			selectorConverters.plusAssign(SelectorConverter(css, false, convert))
	
	fun allElements(css: String, convert: (Elements?, V) -> Unit) =
			selectorConverters.plusAssign(AllSelectorConverter(css, convert))

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
			select(css) { e, v -> convert(e?.text() ?: "", v) }
	
	
	
	/**
	 * If I find a match for your CSS selector, I'll stuff the results into your instance property.
	 *
	 * ## Usage:
	 * ```kotlin
	 * text(".p-name", GitHubPage::fullName)
	 * ```
	 */
	fun text(css: String, property: KMutableProperty1<V, String>,
	         elementText: Element.() -> String = Element::text) =
			select(css, property, elementText)
	
	fun text(css: String, property: KMutableProperty1<V, String>,
	         attr: String) =
			text(css, property) { attr(attr) }
	
	fun float(css: String, convert: (Float, V) -> Unit) =
			select(css) { e, v -> convert(e?.text()?.toFloatOrNull() ?: 0f, v) }
	
	fun float(css: String, property: KMutableProperty1<V, Float>,
	          elementText: Element.() -> String = Element::text) =
			select(css, property) { elementText().toFloatOrNull() ?: 0f }
	
	fun float(css: String, property: KMutableProperty1<V, Float>,
	          attr: String) =
			float(css, property) { attr(attr) }
	
	fun double(css: String, convert: (Double, V) -> Unit) =
			select(css) { e, v -> convert(e?.text()?.toDoubleOrNull() ?: 0.0, v) }
	
	fun double(css: String, property: KMutableProperty1<V, Double>,
	           elementText: Element.() -> String = Element::text) =
			select(css, property) { elementText().toDoubleOrNull() ?: 0.0 }
	
	fun double(css: String, property: KMutableProperty1<V, Double>,
	          attr: String) =
			double(css, property) { attr(attr) }
	
	fun int(css: String, convert: (Int, V) -> Unit) =
			select(css) { e, v -> convert(e?.text()?.toIntOrNull() ?: 0, v) }
	
	fun int(css: String, property: KMutableProperty1<V, Int>,
	        elementText: Element.() -> String = Element::text) =
			select(css, property) { elementText().toIntOrNull() ?: 0 }
	
	fun int(css: String, property: KMutableProperty1<V, Int>,
	          attr: String) =
			int(css, property) { attr(attr) }
	
	fun long(css: String, convert: (Long, V) -> Unit) =
			select(css) { e, v -> convert(e?.text()?.toLongOrNull() ?: 0L, v) }
	
	fun long(css: String, property: KMutableProperty1<V, Long>,
	         elementText: Element.() -> String = Element::text) =
			select(css, property) { elementText().toLongOrNull() ?: 0L }
	
	fun long(css: String, property: KMutableProperty1<V, Long>,
	          attr: String) =
			long(css, property) { attr(attr) }
	
	private inline fun <T : Any> multi(css: String,
	                                   builder: SimpleParser<T>,
	                                   crossinline convert: V.(T) -> Unit) =
			selections(css) { e, v -> v.convert(builder.parse(e)) }
	
	fun <C : MutableCollection<T>, T : Any> collection(css: String,
	                                                   property: KProperty1<V, C>,
	                                                   builder: SimpleParser<T>) =
			multi(css, builder) { e -> property.get(this) += e }
	
	fun <C : MutableCollection<T>, T : Any> collection(css: String,
	                                                   property:
	                                                   KProperty1<V, C>,
	                                                   generator: () -> T,
	                                                   builder:
	                                                   SimpleParser<T>.() ->
	                                                   Unit) =
			collection(css, property, SimpleParser(generator).apply(builder))
	
	fun <C : MutableCollection<T>, T : Any> collection(css: String,
	                                                   property: KProperty1<V, C>,
	                                                   transform: (Element?) -> T) =
			selectorConverters.plusAssign(CollectionConverter(css, transform) { list, v ->
				property.get(v).addAll(list ?: emptyList())
			})
	
	fun <M : MutableMap<K, T>, K : Any, T : Any> map(css: String,
	                                                 property: KProperty1<V, M>,
	                                                 builder: SimpleParser<Pair<K, T>>) =
			multi(css, builder) { e -> property.get(this) += e }
	
	fun <C : MutableMap<K, T>, K : Any, T : Any> map(css: String,
	                                                 property:
	                                                 KProperty1<V, C>,
	                                                 builder:
	                                                 SimpleParser<Pair<K, T>>.
	                                                 () ->
	                                                 Unit): Unit =
			map(css, property, SimpleParser<Pair<K, T>>().apply(builder))
	
	fun <M : MutableMap<K, T>, K : Any, T : Any> map(css: String,
	                                                 property: KProperty1<V, M>,
	                                                 keySelector: (Element?) -> K,
	                                                 valueSelector: (Element?)
	                                                 -> T) =
		selectorConverters.plusAssign(MapConverter(css, keySelector, valueSelector) { map, v ->
			property.get(v).putAll(map ?: emptyMap())
		})
	
	/**
	 * Finds a match for the CSS selector and passes the Element to another SimpleParser in order
	 * to nest/reuse selector statements.
	 */
	fun parser(css: String,
	           parser: NestedParser<V>.() -> Unit) =
			select(css, NestedParser<V>().apply(parser)::parse)

//    fun parser(css: String, parser: NestedParser<V>) = element(css, parser::parse)
	
	class NestedParser<V : Any> : SimpleParser<V>()
	
}
