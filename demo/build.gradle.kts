plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.yaqmc.amll.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.yaqmc.amll.demo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }

    buildTypes {
        // Release-equivalent build for on-device frame measurement: R8, non-debuggable Compose,
        // profileable by shell, debug-signed, installs beside the debug demo.
        create("benchmark") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".benchmark"
            isDebuggable = false
            isProfileable = true
            matchingFallbacks += listOf("release")
        }
    }

    sourceSets["main"].java.srcDir("../perf-fixtures/src")

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":amll"))
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
