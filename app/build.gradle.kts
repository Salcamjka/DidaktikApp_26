plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("org.jetbrains.dokka")
}

android {
    namespace = "com.salca.didaktikapp"
    // Usamos la versión 35 (Android 15) que es la actual estable
    compileSdk = 35

    defaultConfig {
        applicationId = "com.salca.didaktikapp"
        minSdk = 24
        // Target 35 para coincidir con el compileSdk
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // --- LIBRERÍAS CORE (Versiones manuales estables para evitar el error del SDK 36) ---
    // Usamos versiones específicas en lugar de "libs.xxx" para asegurar compatibilidad
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // --- GOOGLE MAPS ---
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // --- GLIDE (PARA EL GIF) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- TESTING ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}