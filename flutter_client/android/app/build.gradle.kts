import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.qatra.app.flutter"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.qatra.app.flutter"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            val keystorePropertiesFile = rootProject.file("../../qatra-key.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties()
                properties.load(FileInputStream(keystorePropertiesFile))
                
                signingConfigs.create("release") {
                    keyAlias = properties.getProperty("keyAlias")
                    keyPassword = properties.getProperty("keyPassword")
                    storeFile = file(properties.getProperty("storeFile"))
                    storePassword = properties.getProperty("storePassword")
                }
                signingConfig = signingConfigs.getByName("release")
            } else {
                val storeFileEnv = System.getenv("QATRA_STORE_FILE")
                if (storeFileEnv != null) {
                    signingConfigs.create("release") {
                        keyAlias = System.getenv("QATRA_KEY_ALIAS")
                        keyPassword = System.getenv("QATRA_KEY_PASSWORD")
                        storeFile = file(storeFileEnv)
                        storePassword = System.getenv("QATRA_STORE_PASSWORD")
                    }
                    signingConfig = signingConfigs.getByName("release")
                } else {
                    signingConfig = signingConfigs.getByName("debug")
                }
            }
        }
    }
}

flutter {
    source = "../.."
}
