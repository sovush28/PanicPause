plugins {
    alias(libs.plugins.android.application)

    ///////id("com.android.application") IS NOT NEEDED. TRIGGERS AN ERROR

    // Adding the Google services Gradle plugin
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.panicpause"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.panicpause"
        minSdk = 29
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Importing the Firebase BoM (Bill of Materials)
    implementation (platform("com.google.firebase:firebase-bom:33.1.0"))

    implementation (libs.material.v100)


    // dependencies for Firebase products:
    // https://firebase.google.com/docs/android/setup#available-libraries

    // from FireBase: When using the BoM, don't specify versions in Firebase dependencies

    implementation(libs.firebase.analytics)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    //adding the material design components dependency (MDC)
    implementation (libs.material.v1130)

    //библиотека glide для загрузки изображений из интернета
    implementation (libs.glide)

    implementation (libs.okhttp)

}