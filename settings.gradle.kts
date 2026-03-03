rootProject.name = "compose-richeditor"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }
}


include(
    ":richeditor-compose",
    ":richeditor-compose-coil3",

//    ":sample:android",
//    ":sample:desktop",
//    ":sample:web",
//    ":sample:common",
)
