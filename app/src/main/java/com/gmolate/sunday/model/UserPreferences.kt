package com.gmolate.sunday.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val clothingLevel: Int = 1, // Default to light clothing
    val skinType: Int = 3, // Default to type 3
    val userAge: Int = 30,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
