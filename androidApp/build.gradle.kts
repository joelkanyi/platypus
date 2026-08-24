import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.metro)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

android {
    namespace = "com.joelkanyi.platypus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.joelkanyi.platypus"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.gradleProperty("PLATYPUS_KEYSTORE_FILE").orNull
                ?: System.getenv("PLATYPUS_KEYSTORE_FILE")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = providers.gradleProperty("PLATYPUS_KEYSTORE_PASSWORD").orNull
                    ?: System.getenv("PLATYPUS_KEYSTORE_PASSWORD")
                keyAlias = providers.gradleProperty("PLATYPUS_KEY_ALIAS").orNull
                    ?: System.getenv("PLATYPUS_KEY_ALIAS")
                keyPassword = providers.gradleProperty("PLATYPUS_KEY_PASSWORD").orNull
                    ?: System.getenv("PLATYPUS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        val releaseSigning = signingConfigs.getByName("release")
        val hasKeystore = releaseSigning.storeFile != null

        getByName("debug") {
            // Use the same signing identity as release when a keystore is configured, so debug and
            // release builds share one signature; fall back to the auto debug keystore otherwise.
            if (hasKeystore) signingConfig = releaseSigning
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasKeystore) releaseSigning else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
