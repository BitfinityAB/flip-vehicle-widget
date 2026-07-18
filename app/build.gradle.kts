import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.flipvehiclewidget.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.flipvehiclewidget.app"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        buildConfigField("String", "TESLA_CLIENT_ID", "\"${localProperties.getProperty("tesla.clientId", "")}\"")
        buildConfigField("String", "PROXY_BASE_URL", "\"${localProperties.getProperty("proxy.baseUrl", "https://example.invalid/")}\"")
        buildConfigField("String", "OAUTH_REDIRECT_URI", "\"${localProperties.getProperty("oauth.redirectUri", "https://example.invalid/oauth/callback")}\"")
        buildConfigField("String", "VEHICLE_BT_NAME", "\"${localProperties.getProperty("vehicle.btName", "")}\"")

        manifestPlaceholders["appAuthRedirectScheme"] = "com.flipvehiclewidget.app"
        manifestPlaceholders["oauthRedirectHost"] = localProperties.getProperty("oauth.redirectHost", "example.invalid")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.startup:startup-runtime:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    // AppAuth's AuthState delegates to org.json.JSONObject, which the AGP unit-test
    // classpath stubs to throw ("not mocked") unless a real implementation is present.
    // Robolectric would also fix this, but this test isn't Robolectric-based, so we
    // put the real org.json implementation on the test classpath instead.
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.work:work-testing:2.9.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
