plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.axelsarassamit.gx12"
    compileSdk = 36
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "com.axelsarassamit.gx12"
        buildConfigField("String", "UPDATE_ASSET", "\"ridedeck-phone-release.apk\"")
        manifestPlaceholders["appLabel"] = "RideDeck"
        minSdk = 26
        targetSdk = 35
        val releaseVersion = (System.getenv("GX12_VERSION_NAME") ?: "0.12.7").removePrefix("v")
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
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.merges += setOf("META-INF/versions/9/OSGI-INF/MANIFEST.MF", "META-INF/LICENSE", "META-INF/NOTICE") }
}

dependencies {
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.camera:camera-video:1.4.2")
    implementation("androidx.core:core:1.16.0")
    testImplementation("junit:junit:4.13.2")
}
