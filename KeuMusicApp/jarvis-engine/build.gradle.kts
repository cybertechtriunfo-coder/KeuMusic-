plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.jarvis.assistant"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    sourceSets {
        getByName("main") {
            java.srcDir("../third_party/jarvis-source/app/src/main/java")
            res.srcDir("../third_party/jarvis-source/app/src/main/res")
            assets.srcDir("../third_party/jarvis-source/app/src/main/assets")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.anthropic:anthropic-java:2.34.0")
    implementation("org.apache.commons:commons-compress:1.27.1")

    // Reuse the exact, SHA-256-pinned sherpa-onnx AAR already downloaded by the KeuMusic app.
    implementation(files(rootProject.file("app/libs/sherpa-onnx-1.13.8.aar")))
}

tasks.configureEach {
    if (name.startsWith("compile") || name.startsWith("bundle")) {
        dependsOn(":app:fetchSherpaOnnx")
    }
}
