plugins {
    id("com.android.application")
}

android {
    namespace = "com.kento.pitchvideoplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kento.pitchvideoplayer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Media3 ExoPlayer：動画/音声再生とピッチ・速度変更に使用
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
}
