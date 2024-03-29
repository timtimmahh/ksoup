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

import okhttp3.HttpUrl
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import kotlin.reflect.KClass


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ResponseParser(val parser: KClass<out ParseBuilder<*>>)

internal class KSoupConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
    ): Converter<ResponseBody, *>? =
            (annotations.find { it is ResponseParser } as? ResponseParser)
		            ?.parser?.objectInstance?.let {
                KSoupConverter(
                    it(),
                    retrofit.baseUrl()
                )
            }
}

class KSoupConverter<T : Any>(private val builder: SimpleParser<T>, private val httpUrl: HttpUrl) :
        Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? =
		    builder.parse(value.byteStream(),
				    value.contentType()?.charset()?.name() ?: "UTF-8",
				    httpUrl.uri().toString()).also {
				    it.logE("KSoup Conversion Result")
		    }
}
