// AGP 9 has built-in Kotlin support, so the `org.jetbrains.kotlin.android` plugin must NOT be applied.
// AGP embeds KGP 2.2.10 by default; we raise KGP and KSP on the build script classpath so the
// versions match gradle/libs.versions.toml (KSP 2.3.12 is built against Kotlin 2.3.20).
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12")
    }
}

// Top-level build file: declare plugins here so sub-projects can apply them by alias.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
