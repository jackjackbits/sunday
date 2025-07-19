package com.gmolate.sunday.service

enum class SkinType(
    val descriptionText: String,
    val vitaminDFactor: Double,
    val burnTime: Int, // Tiempo base en minutos para quemadura con UV=1
    val detailedDescription: String
) {
    TYPE_1(
        "Tipo I - Muy clara",
        1.2,
        150,
        "Siempre se quema, nunca se broncea. Ojos claros, pecas, pelo rojizo o rubio"
    ),
    TYPE_2(
        "Tipo II - Clara",
        1.1,
        250,
        "Se quema fácilmente, bronceado mínimo. Ojos claros, pelo rubio o castaño claro"
    ),
    TYPE_3(
        "Tipo III - Media clara",
        1.0,
        425,
        "A veces se quema, bronceado gradual. Ojos cualquier color, pelo castaño"
    ),
    TYPE_4(
        "Tipo IV - Media",
        0.9,
        600,
        "Rara vez se quema, bronceado fácil. Ojos marrones, pelo castaño oscuro"
    ),
    TYPE_5(
        "Tipo V - Oscura",
        0.7,
        900,
        "Muy rara vez se quema, bronceado profundo. Ojos marrones oscuros, pelo negro"
    ),
    TYPE_6(
        "Tipo VI - Muy oscura",
        0.5,
        1200,
        "Nunca se quema, pigmentación profunda. Ojos y pelo muy oscuros"
    );

    companion object {
        fun fromInt(value: Int): SkinType {
            return when (value) {
                1 -> TYPE_1
                2 -> TYPE_2
                3 -> TYPE_3
                4 -> TYPE_4
                5 -> TYPE_5
                6 -> TYPE_6
                else -> TYPE_2 // Default
            }
        }

        fun getDefaultType() = TYPE_2
    }

    fun getSafeExposureMinutes(uvIndex: Double): Int {
        return if (uvIndex > 0) (burnTime / uvIndex * 0.25).toInt() else burnTime
    }
}
