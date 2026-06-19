plugins {
    id("com.android.application")
    kotlin("android") // <-- Sintaxe clássica que evita o erro de resolução do "org"
}

android {
    namespace = "com.robsonsmartins.androidmidisynth"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.robsonsmartins.androidmidisynth"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(setOf("arm64-v8a"))
        }

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_STL=c++_shared")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt") // Verifique se este é o caminho real do seu arquivo CMakeLists.txt
            version = "3.22.1" // Alinhe com a versão do CMake instalada no seu Android Studio
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }

    // CORREÇÃO DO ALVO JAVA (MUDANDO DE 21 PARA 17 OU 1.8)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Adicione este bloco caso ele não exista, para alinhar o compilador Kotlin com o Java 17
    kotlinOptions {
        this.jvmTarget = "1.8"
    }

    dependencies {
        // ... suas dependências originais do fluidsynth ...

        // Versões estáveis e perfeitamente compatíveis com o compilador 1.5.2
        implementation("androidx.activity:activity-compose:1.8.2")
        implementation("androidx.compose.ui:ui:1.5.4")
        implementation("androidx.compose.material3:material3:1.1.2")
        implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
        debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")

        // Biblioteca oficial para habilitar serviços de áudio em segundo plano
        implementation("androidx.media:media:1.7.0")
    }
}