import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
//    id("com.android.library")
//    kotlin("android")
//    kotlin("android.extensions")
    kotlin("jvm")
}

apply(from = "maven-push.gradle")

/*android {
    compileSdkVersion(29)
    buildToolsVersion = "29.0.2"
    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin",
            "${buildDir.absolutePath}/tmp/kapt/main/kotlinGenerated/")
        getByName("test").java.srcDirs("src/main/kotlin")
        getByName("androidTest").java.srcDirs("src/main/kotlin")
        getByName("debug").java.srcDirs("src/main/kotlin")
    }
}*/
sourceSets {
    getByName("main").java.srcDirs("src/main/kotlin",
        "${buildDir.absolutePath}/tmp/kapt/main/kotlinGenerated/")
    getByName("test").java.srcDirs("src/main/kotlin")
//    getByName("androidTest").java.srcDirs("src/main/kotlin")
//    getByName("debug").java.srcDirs("src/main/kotlin")
}
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = KotlinCompilerVersion.VERSION)

    implementation(group = "com.squareup.retrofit2", name = "retrofit", version = "2.6.1")
    implementation(group = "org.jsoup", name = "jsoup", version = "1.12.1")

    testImplementation("junit:junit:4.12")
//    androidTestImplementation("androidx.test.ext:junit:1.1.1")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.languageVersion = "1.3"
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    languageVersion = "1.3"
}
