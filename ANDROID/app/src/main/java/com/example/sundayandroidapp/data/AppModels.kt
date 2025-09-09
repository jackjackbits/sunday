package com.example.sundayandroidapp.data

enum class SkinType(val type: Int, val description: String) {
    TYPE_1(1, "Very fair, Always burns, never tans"),
    TYPE_2(2, "Fair, Usually burns, tans minimally"),
    TYPE_3(3, "Light, Sometimes burns, tans uniformly"),
    TYPE_4(4, "Medium, Burns minimally, tans well"),
    TYPE_5(5, "Dark, Rarely burns, tans profusely"),
    TYPE_6(6, "Very dark, Never burns, deeply pigmented");

    companion object {
        fun fromType(type: Int): SkinType? = values().find { it.type == type }
    }
}

enum class ClothingLevel(val level: String) {
    NUDE("Nude!"),
    MINIMAL("Minimal (swimwear)"),
    LIGHT("Light (shorts & t-shirt)"),
    MODERATE("Moderate (long sleeves)"),
    HEAVY("Heavy (fully covered)");

    companion object {
        fun fromLevel(level: String): ClothingLevel? = values().find { it.level == level }
    }
}
