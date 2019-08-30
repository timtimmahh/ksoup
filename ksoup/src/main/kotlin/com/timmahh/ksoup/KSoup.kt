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

import org.jsoup.nodes.Document

@DslMarker
annotation class KSoupDsl

/**
 * Invoke the methods on this main DSL class to fetch data using JSoup.
 */
@Suppress("MemberVisibilityCanBePrivate")
object KSoup {

    /**
     * Get a web page and extract some content from it.
     *
     * ## Usage:
     *
     * If you want to extract content from GitHub into an instance of the data class GitHubPage:
     *
     * ```kotlin
     * val gh : GitHubPage = KSoup.extract<GitHubPage> {
     *
     *     url = "https://github.com/mikaelhg"
     *
     *     result { GitHubPage() }               // instantiate (or reuse) your result object
     *
     *     userAgent = "Mozilla/5.0 Ksoup/1.0"
     *
     *     headers["Accept-Encoding"] = "gzip"
     *
     *     text(".p-name") { text, page ->       // find all elements for the selector
     *         page.fullName = text              //     then run this code for each
     *     }
     *
     *     text(".p-name", GitHubPage::fullName)
     *
     *     element(".p-nickname") { el, page ->  // find all elements for the selector
     *         page.username = el.text()         //     then run this code for each
     *     }
     *
     *     element(".p-nickname", Element::text, GitHubPage::username)
     * }
     * ```
     */
    fun <V : Any> build(init: SimpleParser<V>.() -> Unit): SimpleParser<V> =
            SimpleParser<V>().apply(init)

    fun <V : Any> build(instanceGenerator: () -> V, init: SimpleParser<V>.() -> Unit): SimpleParser<V> =
            SimpleParser(instanceGenerator).apply(init)

    fun <V : Any> Document.parse(init: SimpleParser<V>.() -> Unit): V =
            build(init).parse(this)

    fun <V : Any> Document.parse(instanceGenerator: () -> V, init: SimpleParser<V>.() -> Unit) =
            build(instanceGenerator, init).parse(this)

}

fun <T : Any> buildParser(
        instanceGenerator: () -> T,
        builder: SimpleParser<T>.() -> Unit
                         ): Lazy<SimpleParser<T>> =
        lazy { SimpleParser(instanceGenerator).apply(builder) }
