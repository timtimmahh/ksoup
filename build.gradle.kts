// Top-level build file where you can add configuration options common to all sub-projects/modules.


buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
//        kotlin("gradle.plugin")
        classpath(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = "1.3.50")
//
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        jcenter()
//        maven { url = URI("https://jitpack.io") }
    }
}

//tasks {
//    val clean by registering(Delete::class) {
//        delete(buildDir)
//    }
//}
