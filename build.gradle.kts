// Build de nivel raíz.
//
// AGP 9 trae Kotlin incorporado ("built-in Kotlin"): ya no se aplica el plugin
// org.jetbrains.kotlin.android en los módulos. Acá fijamos la versión de KGP
// para que coincida con el Compose Compiler plugin (2.4.21).
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
