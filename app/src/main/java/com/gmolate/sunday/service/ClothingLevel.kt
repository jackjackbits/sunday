package com.gmolate.sunday.service

enum class ClothingLevel(val descriptionText: String, val exposureFactor: Double) {
    NONE("Desnudo", 1.0),
    MINIMAL("Mínimo (traje de baño)", 0.80),
    LIGHT("Ligero (shorts y camiseta)", 0.40),
    MODERATE("Moderado (mangas largas)", 0.15),
    HEAVY("Pesado (completamente cubierto)", 0.05);

    companion object {
        fun fromExposureFactor(factor: Double): ClothingLevel {
            return values().minByOrNull { Math.abs(it.exposureFactor - factor) } ?: MODERATE
        }

        fun getDefaultLevel() = LIGHT
    }
}
