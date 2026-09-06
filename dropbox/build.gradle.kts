plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.sharky.dropbox"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.sharky.dropbox"
        minSdk = 28
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    signingConfigs {
        getByName("debug") {
            val key = rootProject.file(".signing/sharky.jks")
            if(key.exists()) { storeFile=key; storePassword="android"; keyAlias="androiddebugkey"; keyPassword="android" }
            else if(System.getenv("CI")=="true") error("Permanent signing key required")
        }
    }
    compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget="17" }
    // Reuse the user's official raster logo without altering it.
    sourceSets["main"].res.srcDir("../app/src/main/res")
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
