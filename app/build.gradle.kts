plugins {
    id("com.android.application")
}

fun ready(vararg values: String?) = values.all { !it.isNullOrBlank() }

val releaseStoreFile = providers.environmentVariable("DDAKHANA_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("DDAKHANA_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("DDAKHANA_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("DDAKHANA_RELEASE_KEY_PASSWORD").orNull
val releaseSigningReady = ready(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)

val devStoreFile = providers.environmentVariable("DDAKHANA_DEV_STORE_FILE").orNull
val devStorePassword = providers.environmentVariable("DDAKHANA_DEV_STORE_PASSWORD").orNull
val devKeyAlias = providers.environmentVariable("DDAKHANA_DEV_KEY_ALIAS").orNull
val devKeyPassword = providers.environmentVariable("DDAKHANA_DEV_KEY_PASSWORD").orNull
val devSigningReady = ready(devStoreFile, devStorePassword, devKeyAlias, devKeyPassword)

android {
    namespace = "com.shaterguy.hankan"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.shaterguy.ddakhana"
        minSdk = 26
        targetSdk = 35
        versionCode = 20000
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (releaseSigningReady) {
            create("ddakhanaRelease") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
        if (devSigningReady) {
            create("ddakhanaDev") {
                storeFile = file(devStoreFile!!)
                storePassword = devStorePassword
                keyAlias = devKeyAlias
                keyPassword = devKeyPassword
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            resValue("string", "app_name", "딱하나 LOCAL")
        }
        create("dev") {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev1"
            resValue("string", "app_name", "딱하나 DEV")
            if (devSigningReady) signingConfig = signingConfigs.getByName("ddakhanaDev")
        }
        create("unsignedDev") {
            initWith(getByName("dev"))
            signingConfig = null
            matchingFallbacks += listOf("dev", "debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("ddakhanaRelease")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    val stableDistribution = name == "assembleRelease" || name == "bundleRelease" || name.startsWith("packageRelease")
    if (stableDistribution) doFirst {
        check(releaseSigningReady) { "Stable release signing credentials are required. Refusing an ephemeral signer." }
    }
    val devDistribution = name == "assembleDev" || name == "bundleDev" || name.startsWith("packageDev")
    if (devDistribution) doFirst {
        check(devSigningReady) { "Persistent DEV signing credentials are required. Refusing an ephemeral signer." }
    }
}

dependencies { testImplementation("junit:junit:4.13.2") }
