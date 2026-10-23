import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Kotlin viene incorporado en AGP 9; solo aplicamos el compilador de Compose.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

// La firma del release: las claves viven fuera del repositorio (`keystore.properties` y el
// keystore estan en el .gitignore). Sin el archivo, el release se construye sin firmar.
val releaseKeystore = Properties()
val releaseKeystoreFile = rootProject.file("keystore.properties")
if (releaseKeystoreFile.exists()) {
    releaseKeystoreFile.inputStream().use { releaseKeystore.load(it) }
}
val hasReleaseKey = releaseKeystore.size > 0

android {
    namespace = "com.foco.app"
    // Android 16 (API 36) estable. Compose 1.12+ exige 37, aún no estable.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.foco.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(releaseKeystore.getProperty("storeFile"))
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    sourceSets {
        // `MigrationTestHelper` reads the exported schemas as assets.
        getByName("androidTest").assets.srcDir("$projectDir/src/androidTest/schemas")
    }
}

ksp {
    // The exported schemas are the migration baseline: committed under `app/src/androidTest/schemas/`.
    arg("room.schemaLocation", "$projectDir/src/androidTest/schemas")
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (!name.contains("AndroidTest")) {
        compilerOptions {
            allWarningsAsErrors.set(true)
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    // Room 2.8's schema bundles are compiled against kotlinx.serialization 1.8; the lifecycle
    // resolves 1.7.3, which breaks them at runtime (`typeParametersSerializers`).
    implementation(platform(libs.kotlinx.serialization.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
