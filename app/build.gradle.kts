plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "vn.edu.mku.ecoscan"
    compileSdk = 34 // Khuyến nghị dùng bản ổn định hiện tại (hoặc 35 tùy môi trường SDK của bạn)

    defaultConfig {
        applicationId = "vn.edu.mku.ecoscan"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    aaptOptions {
        noCompress("tflite")
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
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // OpenCV
    implementation("org.opencv:opencv:4.9.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Picasso Image Loader
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")

    // --- BỔ SUNG CHO TÍNH NĂNG CCCD ---
    // ML Kit Text Recognition cho OCR tiếng Việt
    // ML Kit Text Recognition (Đọc văn bản/OCR chuẩn của ML Kit)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // ML Kit Barcode Scanning (Quét mã QR trên CCCD)
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    // ----------------------------------

    // Biometric Security
    implementation("androidx.biometric:biometric:1.1.0")

    // WorkManager
    implementation("androidx.work:work-runtime:2.9.0")

}