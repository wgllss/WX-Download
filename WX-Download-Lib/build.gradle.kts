plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.wx.download"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        buildConfig = true
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.okhttp3.okhttp)  // 主库
//    implementation("com.squareup.okio:okio:3.6.0")
//    implementation(libs.kotlinx.coroutines.core)
//    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}