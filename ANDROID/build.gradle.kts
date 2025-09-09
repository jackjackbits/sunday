plugins {
    // Gradle 8.4.1 es la última versión ESTABLE. 8.12 no existe o es una weá ultra experimental.
    id("com.android.application") version "8.12.0" apply false

    // Kotlin 2.0.0 es la primera versión ESTABLE de Kotlin 2.
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false

    // El compilador de Compose DEBE ser 2.0.0 para ser compatible con Kotlin 2.0.0
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}