plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.mabrouk.core"

    compileSdk = 36

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "API_KEY", "\"SqD712P3E82xnwOAEOkGd5JZH8s9wRR24TqNFzjk\"")
        buildConfigField("String", "BASE_URL", "\"http://api.quran.com/api/v3/\"")
        buildConfigField("String", "BASE_URL_SUNNAH", "\"https://api.sunnah.com/v1/\"")
        buildConfigField("String", "BASE_URL_TAFSEER", "\"http://api.quran-tafseer.com/\"")
        buildConfigField("String", "AUDIO_URL", "\"http://www.everyayah.com/data/\"")
        buildConfigField("String", "AUDIO_URL2", "\"http://verse.mp3quran.net/arabic/\"")
        buildConfigField("String", "PRERY_TIME", "\"http://api.aladhan.com/\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.activity.compose)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.ui.tooling.preview)
    api(libs.androidx.material3)
    api(libs.bundles.retrofitBundle)
    api(libs.bundles.roombundle)
    api(libs.bundles.workbundle)
    api(libs.bundles.coilbundle)
    api(libs.bundles.media3bundle)
    api(libs.navigation.compose)
    api(libs.datastore.preferences)
    api(libs.firebase.config.ktx)
    api(libs.hilt.navigation.compose)
    api(libs.hilt.android)
    api(libs.firebase.config)
    api(libs.androidx.material3.window)
    api(libs.androidx.material3.adaptive.android)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.compiler.android)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

hilt {
    enableAggregatingTask = true
}