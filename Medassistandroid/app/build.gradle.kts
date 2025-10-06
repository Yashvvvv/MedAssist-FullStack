import java.util.Properties
import java.io.FileInputStream
import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.medassist_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.medassist_android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read the API key from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        val mapsApiKey = localProperties.getProperty("maps.apiKey", "YOUR_API_KEY_HERE")

        // Use manifestPlaceholders to provide the key to the AndroidManifest
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = mapsApiKey

        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
        buildConfigField("String", "AUTH_URL", "\"http://10.0.2.2:8080/\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM - This ensures all Compose dependencies are compatible
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended-android:1.7.8")

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.ext.compiler)

    // Room Database
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Networking - Updated to use proper KSP configuration
    implementation(libs.bundles.networking)
    implementation(libs.retrofit.converter.gson)

    // Moshi KSP configuration - ensure we're using the correct processor
    ksp(libs.moshi.codegen)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // CameraX
    implementation(libs.bundles.camerax)

    // Google Maps
    implementation(libs.bundles.maps)

    // Image Loading
    implementation(libs.coil.compose)

    // Data Storage
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    // Work Manager
    implementation(libs.work.runtime.ktx)

    // Permissions
    implementation(libs.permissions.compose)

    // UI/UX
    implementation(libs.lottie.compose)

    // Logging
    implementation(libs.timber)

    // Debug
    debugImplementation(libs.leakcanary.android)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.bundles.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.navigation.testing)
    androidTestImplementation(libs.work.runtime.ktx)
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest(libs.hilt.compiler)
}


// TODO: Add the following to your project's `build.gradle.kts` file if you are using Hilt for dependency injection.
//afterEvaluate {
//    if (project.extensions.findByType<ApplicationExtension>() != null) {
//        project.extensions.configure<ApplicationExtension> {
//            if (buildTypes.findByName("release")?.isMinifyEnabled == true) {
//                tasks.named("kspReleaseKotlin") {
//                    dependsOn(tasks.named("minifyReleaseWithR8"))
//                }
//            }
//        }
//    }
//}
