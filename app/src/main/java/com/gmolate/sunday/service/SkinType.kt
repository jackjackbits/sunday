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
        "Tipo V - Media oscura",
        0.8,
        850,
        "Muy rara vez se quema, bronceado muy fácil. Ojos y pelo oscuros"
    ),
    TYPE_6(
        "Tipo VI - Muy oscura",
        0.7,
        1100,
        "Nunca se quema, piel naturalmente oscura. Ojos y pelo oscuros"
    );

    companion object {
        fun fromBurnTime(minutes: Int): SkinType {
            return values().minByOrNull { Math.abs(it.burnTime - minutes) } ?: TYPE_3
        }

        fun getDefaultType() = TYPE_3

        fun getDescription(type: SkinType): String {
            return "${type.descriptionText}\n${type.detailedDescription}"
        }
    }
}
