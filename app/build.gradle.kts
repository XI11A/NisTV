import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { this.load(it) }
    }
}
val tmdbApiKey: String = (System.getenv("TMDB_API_KEY")
    ?: localProperties.getProperty("TMDB_API_KEY")
    ?: (project.findProperty("TMDB_API_KEY") as? String)
    ?: "").toString()

val versionConfigFile = rootProject.file("version.properties")

fun loadVersionProperties(): Properties {
    val props = Properties()
    if (versionConfigFile.exists()) {
        versionConfigFile.inputStream().use { props.load(it) }
    }
    return props
}

val versionProps = loadVersionProperties()
val currentMajor = versionProps.getProperty("VERSION_MAJOR", "1").trim().toInt()
val currentMinor = versionProps.getProperty("VERSION_MINOR", "0").trim().toInt()
val currentPatch = versionProps.getProperty("VERSION_PATCH", "0").trim().toInt()
val currentVersionName = versionProps.getProperty("VERSION_NAME", "$currentMajor.$currentMinor.$currentPatch").trim()
val currentVersionCode = versionProps.getProperty("VERSION_CODE", "${currentMajor * 10000 + currentMinor * 100 + currentPatch}").trim().toInt()

android {
    namespace = "com.nis.nistv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nis.nistv"
        minSdk = 24
        targetSdk = 34
        versionCode = currentVersionCode
        versionName = currentVersionName

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // Media3 ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.exoplayer.smoothstreaming)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.jellyfin.media3.ffmpeg)

    // Coil 3
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Google Play Services Auth & Credential Manager
    implementation(libs.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)

    // Networking & Serialization
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)


    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks.register("printVersion") {
    group = "versioning"
    description = "Prints the current NisTV app version and version code."
    doLast {
        val props = loadVersionProperties()
        val major = props.getProperty("VERSION_MAJOR", "1").trim()
        val minor = props.getProperty("VERSION_MINOR", "0").trim()
        val patch = props.getProperty("VERSION_PATCH", "0").trim()
        val name = props.getProperty("VERSION_NAME", "$major.$minor.$patch").trim()
        val code = props.getProperty("VERSION_CODE", "${major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()}").trim()
        val updated = props.getProperty("LAST_UPDATED", "Unknown").trim()
        val change = props.getProperty("LAST_CHANGE", "None").trim()

        println("--------------------------------------------------")
        println("NisTV Revamped Version Info")
        println("--------------------------------------------------")
        println("Version Name : $name")
        println("Version Code : $code")
        println("Major        : $major")
        println("Minor        : $minor")
        println("Patch        : $patch")
        println("Last Updated : $updated")
        println("Last Change  : $change")
        println("--------------------------------------------------")
    }
}

tasks.register("bumpVersion") {
    group = "versioning"
    description = "Bumps the version (major, minor, patch) and updates version.properties and CHANGELOG.md."
    doLast {
        val bumpType = project.findProperty("bumpType")?.toString()?.trim()?.lowercase()
        if (bumpType.isNullOrEmpty() || bumpType !in listOf("major", "minor", "patch")) {
            throw GradleException(
                "Invalid or missing 'bumpType'. You must specify -PbumpType=major|minor|patch.\n" +
                "Example: ./gradlew bumpVersion -PbumpType=patch -Pmessage=\"Fix episode sort order\""
            )
        }

        val message = project.findProperty("message")?.toString()?.trim().orEmpty()
        val effectiveMessage = if (message.isNotBlank()) message else "Version bump ($bumpType)"

        val props = loadVersionProperties()
        var major = props.getProperty("VERSION_MAJOR", "1").trim().toInt()
        var minor = props.getProperty("VERSION_MINOR", "0").trim().toInt()
        var patch = props.getProperty("VERSION_PATCH", "0").trim().toInt()

        val oldVersion = "$major.$minor.$patch"
        val oldCode = major * 10000 + minor * 100 + patch

        when (bumpType) {
            "major" -> {
                major += 1
                minor = 0
                patch = 0
            }
            "minor" -> {
                minor += 1
                patch = 0
            }
            "patch" -> {
                patch += 1
            }
        }

        val newVersion = "$major.$minor.$patch"
        val newCode = major * 10000 + minor * 100 + patch
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val updatedPropsContent = buildString {
            appendLine("VERSION_MAJOR=$major")
            appendLine("VERSION_MINOR=$minor")
            appendLine("VERSION_PATCH=$patch")
            appendLine("VERSION_NAME=$newVersion")
            appendLine("VERSION_CODE=$newCode")
            appendLine("LAST_UPDATED=$today")
            appendLine("LAST_CHANGE=$effectiveMessage")
        }
        versionConfigFile.writeText(updatedPropsContent)

        val changelogFile = rootProject.file("CHANGELOG.md")
        if (changelogFile.exists()) {
            val changelogContent = changelogFile.readText()
            val sectionName = when (bumpType) {
                "patch" -> "Fixed"
                "minor" -> "Added"
                "major" -> "Changed"
                else -> "Changed"
            }
            val newEntry = """
## [$newVersion] - $today

### $sectionName
- $effectiveMessage
""".trimIndent()

            val unreleasedHeader = "## [Unreleased]"
            val updatedChangelog = if (changelogContent.contains(unreleasedHeader)) {
                changelogContent.replaceFirst(
                    unreleasedHeader,
                    "$unreleasedHeader\n\n$newEntry"
                )
            } else {
                "$newEntry\n\n$changelogContent"
            }
            changelogFile.writeText(updatedChangelog)
        }

        println("Successfully bumped version: $oldVersion ($oldCode) -> $newVersion ($newCode)")
        println("Updated: ${versionConfigFile.absolutePath}")
        if (changelogFile.exists()) {
            println("Updated: ${changelogFile.absolutePath}")
        }
    }
}
