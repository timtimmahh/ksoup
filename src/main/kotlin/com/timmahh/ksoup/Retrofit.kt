package com.timmahh.ksoup

import okhttp3.HttpUrl
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type


class KSoupConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
    ): Converter<ResponseBody, *>? =
            (annotations.find { it is ResponseParser } as? ResponseParser)
                    ?.parser?.objectInstance?.let { KSoupConverter(it.build, retrofit.baseUrl()) }
}

class KSoupConverter<T : Any>(private val builder: SimpleParser<T>, private val httpUrl: HttpUrl) :
        Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? =
            builder.parse(value.byteStream(),
                    value.contentType()?.charset()?.name() ?: "UTF-8",
                    httpUrl.uri().toString())
}
