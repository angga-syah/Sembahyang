import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "id.linuwih.sholat"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.linuwih.sembahyang"
        minSdk = 21
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * Penandatanganan rilis dibaca dari keystore.properties yang SENGAJA tidak masuk git — kata
     * sandi keystore tidak boleh tersimpan di repositori. Salin keystore.properties.example,
     * isi nilainya, lalu jalankan ./gradlew assembleRelease
     *
     * Kalau berkasnya belum ada, build rilis tetap berjalan (hanya tidak ditandatangani), bukan
     * gagal dengan pesan yang membingungkan.
     */
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }
    val hasSigningConfig = keystoreProperties.getProperty("storeFile") != null

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // adhan2 relies on kotlinx.datetime -> java.time; minSdk 21 needs desugaring since java.time is API 26+.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Pinned below the latest available releases on purpose: newer androidx.compose /
    // lifecycle / activity artifacts now require compileSdk 36-37 + AGP 9.1+, which would
    // drag in a much bigger toolchain migration (see build history) for no feature benefit
    // at this stage. Revisit when there's an actual reason to bump compileSdk.
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Room: local cache only (bookmarks, last-read, download status) — NOT the bundled Quran text/translation asset DB.
    // Pinned to 2.7.2, the last Room 2.x line still supporting minSdk 21 (2.8.0+ requires
    // minSdk 23, which would break the "runs on old phones" requirement — see plan). 2.6.1
    // (original scaffold pin) has a KSP bug against Kotlin 2.4.0 ("unexpected jvm signature V").
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // Prayer-time astronomical calculations — fully offline, no server dependency (see plan doc).
    implementation("com.batoulapps.adhan:adhan2:0.0.7")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
