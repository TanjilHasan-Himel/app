import java.util.Properties
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val versionPropsFile = file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
} else {
    versionProps["VERSION_CODE"] = "12"
    versionProps["VERSION_NAME"] = "9.0.0"
}
val vCode = providers.gradleProperty("AUDIA_VERSION_CODE")
    .getOrElse(versionProps["VERSION_CODE"].toString()).toInt()
val vName = providers.gradleProperty("AUDIA_VERSION_NAME")
    .getOrElse(versionProps["VERSION_NAME"].toString())

val keystorePropsFile = file("keystore/keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    keystoreProps.load(FileInputStream(keystorePropsFile))
}

val releaseStorePath = providers.environmentVariable("AUDIA_KEYSTORE_PATH").orNull
    ?: keystoreProps.getProperty("storeFile") ?: "keystore/audia-release.jks"
val releaseStorePassword = providers.environmentVariable("AUDIA_KEYSTORE_PASSWORD").orNull
    ?: keystoreProps.getProperty("storePassword") ?: "audia_release_2026"
val releaseKeyAlias = providers.environmentVariable("AUDIA_KEY_ALIAS").orNull
    ?: keystoreProps.getProperty("keyAlias") ?: "audia"
val releaseKeyPassword = providers.environmentVariable("AUDIA_KEY_PASSWORD").orNull
    ?: keystoreProps.getProperty("keyPassword") ?: "audia_release_2026"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}


android {
    namespace = "com.eplaytime.app"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.eplaytime.app"
        minSdk = 24
        targetSdk = 36
        versionCode = vCode
        versionName = vName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val resolvedStore = file(releaseStorePath)
        if (resolvedStore.exists() && releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null) {
            create("release") {
                storeFile = resolvedStore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            (this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)?.let { output ->
                val suffix = if (variant.buildType.name == "release") "" else "-${variant.buildType.name}"
                output.outputFileName = "Audia-v${variant.versionName}${suffix}.apk"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM & UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Dynamic Colors (Palette)
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Animations (Lottie)
    implementation("com.airbnb.android:lottie-compose:6.0.0")

    // Media3 (ExoPlayer)
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")

    // Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.55")
    ksp("com.google.dagger:hilt-compiler:2.55")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room (Database)
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Drag and Drop (Reorderable)

    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")

    // Accompanist (System UI Controller & Permissions)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")


    // Media Legacy
    implementation("androidx.media:media:1.7.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // WorkManager (Background Tasks)
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Firebase (Analytics & Crashlytics via BoM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
