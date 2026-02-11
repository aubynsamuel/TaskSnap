import com.android.build.gradle.ProguardFiles.getDefaultProguardFile
import org.gradle.kotlin.dsl.android
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.ksp
import org.gradle.kotlin.dsl.libs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.internal.types.error.ErrorModuleDescriptor.platform

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.veivek.taskSnap"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.veivek.taskSnap"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            val minify = false
            isMinifyEnabled = minify
            isShrinkResources = minify
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
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
            freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        }
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Navigation 3 Compose
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Material Icons
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.profileinstaller)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    "baselineProfile"(project(":baselineprofile"))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}