plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Chave de assinatura FIXA (vem dos segredos do GitHub Actions).
// Com a mesma chave, cada APK novo instala por cima do anterior e o Android
// preserva o modelo baixado e as conversas.
val rudKeystore: File? = System.getenv("RUD_KEYSTORE_FILE")
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it) }
    ?.takeIf { it.exists() }

android {
    namespace = "com.rudia.chatbot"
    compileSdk = 35

    defaultConfig {
        // Identificador definitivo. Não mude mais: trocar o applicationId cria
        // um "outro app" para o Android, com dados separados.
        applicationId = "com.rudia.ia"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "2.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                // Sempre otimizado, inclusive no build de teste.
                arguments += listOf("-DCMAKE_BUILD_TYPE=Release")
            }
        }
    }

    signingConfigs {
        if (rudKeystore != null) {
            create("rud") {
                storeFile = rudKeystore
                storePassword = System.getenv("RUD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RUD_KEY_ALIAS")
                keyPassword = System.getenv("RUD_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (rudKeystore != null) {
                signingConfigs.getByName("rud")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            if (rudKeystore != null) signingConfig = signingConfigs.getByName("rud")
        }
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
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.google.android.material:material:1.12.0")
    // Removido: com.google.mediapipe:tasks-genai (não era usado e aumentava o APK).

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
