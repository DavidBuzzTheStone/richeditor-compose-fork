package com.mohamedrejeb.richeditor.ui.material3.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.badge.BadgeModel

@Composable
public fun StatusBadge(
    model: BadgeModel,
    modifier: Modifier = Modifier
) {
    val badgeModifier = modifier
        .then(
            if (model.borderColor != null)
                Modifier.border(1.dp, model.borderColor, CircleShape)
            else
                Modifier.background(model.containerColor, CircleShape)
        )
        .padding(horizontal = 16.dp, vertical = 4.dp)

    Text(
        text = model.text,
        color = model.contentColor,
        fontStyle = FontStyle.Italic,
        style = MaterialTheme.typography.labelSmall,
        modifier = badgeModifier
    )
}
