plugins {
    alias(libs.plugins.android.library)
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
            // AGP 9 built-in Kotlin compiles Kotlin from the kotlin source set, not java.srcDir.
            kotlin.srcDir("../third_party/jarvis-source/app/src/main/java")
            java.srcDir("../third_party/jarvis-source/app/src/main/java")
            // Compile the locally adapted Kotlin 2.4-compatible MemoryStore overlay instead.
            kotlin.filter.exclude("**/memory/MemoryStore.kt")
            kotlin.srcDir("src/main/kotlin")
            res.srcDir("../third_party/jarvis-source/app/src/main/res")
            assets.srcDir("../third_party/jarvis-source/app/src/main/assets")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
        getByName("test") {
            java.srcDir("../third_party/jarvis-source/app/src/test/java")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.configureEach {
    if (name.startsWith("compile") || name.startsWith("bundle")) {
        dependsOn(":app:fetchSherpaOnnx")
    }
}
