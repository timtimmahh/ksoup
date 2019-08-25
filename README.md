# KSoup

[![](https://jitpack.io/v/timtimmahh/ksoup.svg)](https://jitpack.io/#timtimmahh/ksoup)

A Kotlin DSL for [JSoup](https://jsoup.org/) HTML parsing.

KSoup allows for easily parsing HTML by wrapping the JSoup parser with Kotlin DSL without giving up functionality from the original JSoup library.

KSoup provides helper functions to simplify specifying the CSS selectors and obtaining the desired data. 
Currently, KSoup provides functions for obtaining:

* String
* Float
* Double
* Int
* Long
* Collection's of any type
* Maps with any key and value
* Nested JSoup Element's

## Installation

The latest versions of KSoup are hosted on the maven JitPack repository, so in your project root's `build.gradle` you must add:

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
 ```

Then, you can add KSoup as a dependency to your module:

```gradle
dependencies {
  implementation 'com.github.timtimmahh:ksoup:master-SNAPSHOT'
}
```

Use the version `master-SNAPSHOT` to target the most the latest commit, however you can also use commit tags or release versions. Currently KSoup is on 0.2.1


For additional help, follow the directions as specified on [JitPack's website](https://jitpack.io/#timtimmahh/ksoup).


## Basic Usage

To use KSoup you must first create the model you would like to parse the HTML into where each property is specified with the `var` keyword in order for the values to be updated upon parsing. Properties don't necessarily need to have a default value, however it does make specifying the instance generator for the model easier by using a constructor reference.

```kotlin

data class DetailedClass(
    var title: String = "",
    var status: String = "",
    var reason: String = "",
    var units: Float = 0f,
    var grading: String = "",
    var grade: String = "",
    var classNumber: Int = 0,
    var section: Int = 0,
    var component: String = "",
    var datesAndTimes: MutableList<String> = mutableListOf(),
    var room: MutableList<String> = mutableListOf(),
    var instructor: MutableList<String> = mutableListOf(),
    var startEndDate: MutableList<String> = mutableListOf(),
    var aidEligible: String = ""
) { constructor() : this("") }
```

Now that the model is set up, you must now specify the CSS selectors and parse rules for each property.

The following example uses the abstract class`ParseBuilder<V : Any>` which has an abstract property that you can use to lazily build the DSL. The function `fun <T : Any> buildParser(instanceGenerator: () -> T, builder: SimpleParser<T>.() -> Unit): Lazy<SimpleParser<T>>` is simply a delegate to lazily build the DSL so as not to waste resources when it's not being used. 

```kotlin
object DetailedClassBuilder : ParseBuilder<DetailedClass>() {
    override val build: SimpleParser<DetailedClass> by buildParser(::DetailedClass) {
        text("td.PAGROUPDIVIDER", DetailedClass::title)
        text(
            "table[id^=SSR_DUMMY_RECVW\$scroll] tr[id^=trSSR_DUMMY_RECVW] span[id=STATUS$0]",
            DetailedClass::status
        )
        text(
            "table[id^=SSR_DUMMY_RECVW\$scroll] tr[id^=trSSR_DUMMY_RECVW] span[id=ENRLSTATUSREASON$0]",
            DetailedClass::reason
        )
        float(
            "table[id^=SSR_DUMMY_RECVW\$scroll] tr[id^=trSSR_DUMMY_RECVW] span[id=DERIVED_REGFRM1_UNT_TAKEN$0]",
            DetailedClass::units
        )
        text(
            "table[id^=SSR_DUMMY_RECVW\$scroll] tr[id^=trSSR_DUMMY_RECVW] span[id=GB_DESCR$0]",
            DetailedClass::grading
        )
        text(
            "table[id^=SSR_DUMMY_RECVW\$scroll] tr[id^=trSSR_DUMMY_RECVW] span[id=DERIVED_REGFRM1_CRSE_GRADE_OFF$0]",
            DetailedClass::grade
        )
        int(
            "table[id^=CLASS_MTG_VW\$scroll] tr[id=trCLASS_MTG_VW$0_row1] div[id=win0divDERIVED_CLS_DTL_CLASS_NBR$0] > span",
            DetailedClass::classNumber
        )
        int(
            "table[id^=CLASS_MTG_VW\$scroll] tr[id=trCLASS_MTG_VW$0_row1] div[id=win0divDERIVED_CLS_DTL_CLASS_NBR$0] > span",
            DetailedClass::section
        )
        text(
            "table[id^=CLASS_MTG_VW\$scroll] tr[id=trCLASS_MTG_VW$0_row1] div[id=win0divMTG_COMP$0] > span",
            DetailedClass::component
        )
        collection(
            "table[id^=CLASS_MTG_VW\$scroll] tr[id^=trCLASS_MTG_VW] div[id^=win0divMTG_SCHED] > span",
            DetailedClass::datesAndTimes,
            Element::text
        )
        collection(
            "table[id^=CLASS_MTG_VW\$scroll] tr[id^=trCLASS_MTG_VW] div[id^=win0divMTG_LOC] > span",
            DetailedClass::room,
            Element::text
        )
        collection(
            "table[id^=CLASS_MTG_VW\$scroll] tr[id^=trCLASS_MTG_VW] div[id^=win0divDERIVED_CLS_DTL_SSR_INSTR_LONG] > span",
            DetailedClass::instructor,
            Element::text
        )
        collection(
            "table[id^=CLASS_MTG_VW\$scroll] tr[id^=trCLASS_MTG_VW] div[id^=win0divMTG_DATES] > span",
            DetailedClass::startEndDate,
            Element::text
        )
        text(
            "table[id^=CLASS_MTG_VW\$scroll] tr[id=trCLASS_MTG_VW$0_row1] div[id=win0divMTG_AID$0] > span",
            DetailedClass::aidEligible
        )
    }
}
```

In addition, you can also use the `fun element(css: String, convert: (Element, V) -> Unit): Unit` function to specify parse rules that can't be uptained by using the other helper functions. 

Similarly, the `fun elements(css: String, convert: (Element, V) -> Unit): Unit` works the same way except instead of only selecting the first css selector that was found, it goes through the returned `Elements` object to convert each `Element` into the desired result.

### Using Retrofit

If you'd like to use [Retrofit](https://square.github.io/retrofit/) to perform the requests, this library provides a Retrofit Converter.Factory called `KSoupConverterFactory` that will find the correct ParseBuilder for the return type in the request function. For KSoupConverterFactory to find the correct ParseBuilder you must specify it using the `@ResponseParser` annotation. For example:

```kotlin
@ResponseParser(parser = CurrentUserAdapter::class)
@FormUrlEncoded
@POST("https://canvas.jmu.edu/saml_consume")
fun getCanvasProfileInfo(@Field("SAMLResponse") samlResponse: String): Deferred<Response<CurrentUser>>
```

## Todo:

* Move the Retrofit Converter to different module
* Combine parse models with the DSL builder to simplify creation, operation, as well as remove the kotlin-reflect dependency that is currently required for the Retrofit Converter.

## License
Licensed under [Apache License, Version 2.0](LICENSE)

## Credits

This library is partially based off of [Mikael Gueck's](https://github.com/mikaelhg) [KSoup](https://github.com/mikaelhg/ksoup.git) DSL implementation.
