plugins {
    id("com.google.devtools.ksp")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.serialization)
}

android {
    namespace = "xyz.sevive.arcaeaoffline.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.kotlinx.serialization)

    // android & androidx
    implementation(androidx.room.runtime)
    implementation(androidx.room.ktx)
    ksp(androidx.room.compiler)

    implementation(androidx.exifinterface)

    // 3rd party
    implementation(libs.apache.commons.io)
    implementation(libs.github.requery.sqlite.android)
    implementation(libs.threetenabp)

    // test & debug
    testImplementation(libs.junit)
    androidTestImplementation(androidx.test.ext.junit)
    androidTestImplementation(androidx.test.espresso.core)
}
