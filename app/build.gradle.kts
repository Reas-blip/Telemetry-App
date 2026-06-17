plugins {
   alias(libs.plugins.android.application)
   alias(libs.plugins.kotlin.compose)
   alias(libs.plugins.kotlin.serialization)
   alias(libs.plugins.secrets.gradle.plugin)
   alias(libs.plugins.ksp)
   alias(libs.plugins.hilt.android)

}

secrets {
   // Optional: Specify the properties file (default is local.properties)
   propertiesFileName = "secrets.properties"

   // Optional: Provide default values to prevent build failures
   defaultPropertiesFileName = "local.defaults.properties"
}
android {
   namespace = "android.learn.telemetryapp"
   compileSdk = 36

   testBuildType = "release"
   defaultConfig {
      applicationId = "android.learn.telemetryapp"
      minSdk = 24
      targetSdk = 35
      versionCode = 1
      versionName = "1.0"

   }


   tasks.withType<Test>().configureEach {
      jvmArgs("-XX:+EnableDynamicAgentLoading")
   }

   buildTypes {
      debug {
         isDebuggable = true
      }
      create("profile") {
         // Initializes it with everything from release (obfuscation, optimizations)
         initWith(getByName("release"))

         // Explicitly enables profiling while keeping debugging off
         isProfileable = true
         isDebuggable = false

         // Optional: Add a suffix so it doesn't overwrite your actual release app
         applicationIdSuffix = ".profile"
         signingConfig = signingConfigs.getByName("debug") // Uses debug key for easy local deployment
      }
      release {
         isMinifyEnabled = true
         isShrinkResources = true

         // 🔥 CRITICAL: Directs the release build to use the auto-generated debug key
         signingConfig = signingConfigs.getByName("debug")

         proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
         )
      }
   }
   testOptions {
      unitTests {
         isIncludeAndroidResources = true
         all {
            it.useJUnitPlatform()
         }
      }
   }
   // Updated to VERSION_17 to match your Kotlin JVM target
   compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
   }

   buildFeatures {
      compose = true
   }
}

// Correct modern syntax for Android + Gradle 9.x
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
   compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
   }
}



dependencies {

   implementation(libs.androidx.runner)
   // Core Koin for Android
   implementation(libs.koin.android)

   // Koin Jetpack Compose support
   implementation(libs.koin.androidx.compose)
   implementation(libs.hilt.android)

   ksp(libs.hilt.android.compiler)
//   implementation(libs.dagger)
//   ksp(libs.dagger.compiler)
   implementation(libs.retrofit)
   implementation(libs.converter.gson)
   implementation(libs.androidx.activity.ktx)
   implementation(libs.androidx.appcompat)
   implementation(libs.androidx.constraintlayout)
   implementation(libs.androidx.lifecycle.viewmodel.compose)
   implementation(libs.androidx.lifecycle.viewmodel.ktx)
   implementation(libs.coil.network.okhttp)
   implementation(libs.androidx.compose.material.icons.extended)
   implementation(libs.coil.compose)
   implementation(libs.kotlinx.serialization.json)
   implementation(libs.ktor.client.content.negotiation)
   implementation(libs.androidx.data.store.preferences)
   implementation(libs.androidx.data.store.core)
   implementation(libs.ktor.serialization.kotlinx.json)
   implementation(libs.ktor)
   implementation(libs.ktor.cio)
   implementation(libs.androidx.core.ktx)
   implementation(libs.androidx.lifecycle.runtime.ktx)
   implementation(libs.kotlinx.coroutines.android)
   implementation(libs.kotlinx.coroutines.core)
   implementation(libs.androidx.activity.compose)
   implementation(platform(libs.androidx.compose.bom))
   implementation(libs.androidx.ui)
   implementation(libs.androidx.ui.graphics)
   implementation(libs.androidx.ui.tooling.preview)
   testImplementation(libs.junit.jupiter.api)
   testRuntimeOnly(libs.junit.jupiter.engine)
   testRuntimeOnly(libs.junit.platform.launcher)
   implementation(libs.androidx.material3)
   implementation(libs.material)
   testImplementation(libs.junit)
   testImplementation(libs.junit.jupiter)
   testImplementation(libs.robolectric)
   testImplementation(libs.androidx.junit)
   testImplementation(platform(libs.androidx.compose.bom))
   testImplementation(libs.androidx.ui.test.junit4)
   testImplementation(libs.mockk)
   androidTestImplementation(libs.mockk)
   androidTestImplementation(libs.androidx.junit)

   androidTestImplementation(libs.androidx.espresso.core)
   androidTestImplementation(platform(libs.androidx.compose.bom))
   androidTestImplementation(libs.hilt.android.testing)
   androidTestImplementation(libs.androidx.ui.test.junit4)
   debugImplementation(libs.androidx.ui.tooling)
   debugImplementation(libs.androidx.ui.test.manifest)
   implementation(libs.androidx.room.runtime)
   implementation(libs.androidx.room.ktx)
   ksp(libs.androidx.room.compiler)

}
