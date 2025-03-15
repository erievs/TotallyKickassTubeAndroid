plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.erievs.totallykickasstube"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.erievs.totallykickasstube"
        minSdk = 24  // Supports Android 5.0+ (Lollipop)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("x86")
            abiFilters.add("x86_64")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }

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
}

dependencies {

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ExoPlayer for HLS streaming
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation ("com.github.norulab:android-exoplayer-fullscreen:1.2.1")
    // YouTubeDL-Android library for extracting HLS streams from YouTube
    implementation("io.github.junkfood02.youtubedl-android:library:0.17.2")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.17.2")
    implementation("io.github.junkfood02.youtubedl-android:aria2c:0.17.2")

    implementation("io.reactivex.rxjava2:rxandroid:2.1.0")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")

    implementation ("com.squareup.picasso:picasso:2.71828")

    implementation ("androidx.cardview:cardview:1.0.0")

    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-jackson:2.9.0")
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.12.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
