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



Credits to [Mikael Gueck](https://github.com/mikaelhg) for the initial [KSoup](https://github.com/mikaelhg/ksoup.git) DSL implementation.
