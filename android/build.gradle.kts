// Root build file — plugin versions are pinned here and applied per module.
//
// Pins (OQ-4): AGP 8.5.2, Kotlin 1.9.24, Gradle 8.7, JDK 17.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
}
