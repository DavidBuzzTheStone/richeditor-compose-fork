package com.mohamedrejeb.richeditor.model.badge

import androidx.compose.ui.graphics.Color

public data class BadgeModel(
    val id: String,
    val text: String,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color? = null
)
