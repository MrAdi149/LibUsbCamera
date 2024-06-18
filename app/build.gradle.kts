plugins {
    id("com.android.library")
}

android {
    namespace = "com.example.libusbcamera"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34


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

        debug {

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

    implementation ("com.android.support:appcompat-v7:28.0.0")
    implementation ("androidx.appcompat:appcompat:1.7.0")
    implementation ("androidx.appcompat:appcompat-resources:1.7.0")



}