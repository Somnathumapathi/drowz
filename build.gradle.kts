// Top-level build file. Plugins are declared here (applied in module build files)
// so versions resolve from the single source of truth: gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}
