
import com.android.build.gradle.tasks.AidlCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "xyz.ggorg.easyspot.hiddenapi"
    compileSdk = 36

    defaultConfig {
        minSdk = 30

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }
    buildFeatures { aidl = true }

    afterEvaluate {
        tasks.withType<AidlCompile>().configureEach {
            doLast {
                outputs.files.forEach { fileOrFolder ->
                    fileOrFolder.walkTopDown().forEach { file ->
                        file
                            .takeIf { file.extension == "java" }
                            ?.apply {
                                val sanitized = readText().replace("\\", "\\\\")
                                writeText(sanitized)
                            }
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    annotationProcessor(libs.refine.processor)
    implementation(libs.refine.annotation)
}
