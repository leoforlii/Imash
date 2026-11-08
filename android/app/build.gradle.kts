import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.imash.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.imash.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    // Chiave di firma vera: android/keystore.properties (fuori da git) con storeFile, storePassword,
    // keyAlias, keyPassword. Se manca (per es. su un altro PC) la release esce NON firmata
    // (app-release-unsigned.apk, non si installa): mai firmata di nascosto con la chiave di debug
    // (scansione Vantage, F-001). Per una prova la si può chiedere esplicitamente:
    //   ./gradlew assembleRelease -PfirmaDebug     → versione "0.1-firmadebug"
    val firmaDebug = providers.gradleProperty("firmaDebug").isPresent
    val proprietaFirma = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f ->
        Properties().apply { f.inputStream().use { load(it) } }
    }
    signingConfigs {
        if (proprietaFirma != null) {
            create("imash") {
                storeFile = rootProject.file(proprietaFirma.getProperty("storeFile"))
                storePassword = proprietaFirma.getProperty("storePassword")
                keyAlias = proprietaFirma.getProperty("keyAlias")
                keyPassword = proprietaFirma.getProperty("keyPassword")
            }
        }
    }

    ndkVersion = "30.0.16248370"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }

    buildTypes {
        debug {
            // Anche l'emulatore (x86_64).
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        }
        release {
            // Solo telefoni: arm64 e arm 32 bit.
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            val firmaVera = signingConfigs.findByName("imash")
            signingConfig = firmaVera ?: if (firmaDebug) signingConfigs.getByName("debug") else null
            if (firmaVera == null) {
                if (firmaDebug) versionNameSuffix = "-firmadebug"
                logger.warn(
                    "Imash: manca android/keystore.properties, la release " +
                        if (firmaDebug) "è firmata con la chiave di DEBUG (solo per prove)." else "NON sarà firmata.",
                )
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

    lint {
        // La release è solo per telefoni (arm): niente x86_64 per i Chromebook, scelta voluta (fase 6).
        disable += "ChromeOsAbiSupport"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.reorderable)   // riordino dei brani trascinandoli (fase 5)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
