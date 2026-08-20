plugins {
    id("com.android.application")
}

val releaseStoreFile = providers.environmentVariable("HANKAN_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("HANKAN_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("HANKAN_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("HANKAN_RELEASE_KEY_PASSWORD").orNull
val releaseSigningReady = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.shaterguy.hankan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shaterguy.hankan"
        minSdk = 26
        targetSdk = 35
        versionCode = 10100
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("hankanRelease") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "한칸 DEV")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("hankanRelease")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("unsignedRelease") {
            initWith(getByName("release"))
            signingConfig = null
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.configureEach {
    val releaseDistributionTask = name == "assembleRelease" ||
        name == "bundleRelease" || name.startsWith("packageRelease")
    if (releaseDistributionTask) {
        doFirst {
            check(releaseSigningReady) {
                "Release signing credentials are required. Refusing to create a distributable release with an ephemeral debug key."
            }
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
