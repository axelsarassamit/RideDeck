plugins {
    id("com.android.application")
}

android {
    namespace = "com.axelsarassamit.gx12"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.axelsarassamit.gx12"
        minSdk = 26
        targetSdk = 35
        val releaseVersion = (System.getenv("GX12_VERSION_NAME") ?: "0.1.0").removePrefix("v")
        val parts = releaseVersion.split(".")
        require(parts.size >= 2 && parts.take(3).all { it.all(Char::isDigit) }) {
            "GX12_VERSION_NAME must use numeric semver such as 0.1.0"
        }
        val major = parts[0].toInt()
        val minor = parts[1].toInt()
        val patch = if (parts.size > 2) parts[2].toInt() else 0
        versionCode = major * 10000 + minor * 100 + patch
        versionName = releaseVersion
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("GX12_KEYSTORE_PATH")
            val storePasswordValue = System.getenv("GX12_KEYSTORE_PASSWORD")
            val aliasValue = System.getenv("GX12_KEY_ALIAS")
            val keyPasswordValue = System.getenv("GX12_KEY_PASSWORD")
            if (!storePath.isNullOrBlank() && !storePasswordValue.isNullOrBlank() &&
                !aliasValue.isNullOrBlank() && !keyPasswordValue.isNullOrBlank()
            ) {
                storeFile = rootProject.file(storePath)
                storePassword = storePasswordValue
                keyAlias = aliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core:1.16.0")
}
