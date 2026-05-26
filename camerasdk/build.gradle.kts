plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {

    namespace = "com.cesc.camerasdk"

    compileSdk = 36

    defaultConfig {

        minSdk = 24

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {

        singleVariant("release") {

            withSourcesJar()
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.appcompat)

    implementation(libs.material)

    implementation(libs.androidx.activity)

    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.camera.core)

    implementation(libs.androidx.camera.camera2)

    implementation(libs.androidx.camera.lifecycle)

    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {

    publishing {

        publications {

            register<MavenPublication>("release") {

                groupId = "com.github.thakerram"

                artifactId = "camerasdk"

                version = "1.0.4"

                from(components["release"])
            }
        }
    }
}