package com.gmolate.sunday

import kotlinx.serialization.Serializable

@Serializable
data class Quote(
    val text: String,
    val author: String
)
