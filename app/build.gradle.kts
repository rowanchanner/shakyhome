plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { namespace = "com.sharky.home"; compileSdk = 35
    defaultConfig {
        applicationId = "com.sharky.home"
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // GitHub builds receive a higher number each run, allowing Android to install them as updates.
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
    }
    buildFeatures { viewBinding = true; buildConfig = true }
    signingConfigs {
        getByName("debug") {
            val permanentKey = rootProject.file(".signing/sharky.jks")
            if (permanentKey.exists()) {
                storeFile = permanentKey
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            } else if (System.getenv("CI") == "true") {
                error("Permanent Sharky signing key is required for published builds")
            }
        }
    }
    buildTypes { release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}
