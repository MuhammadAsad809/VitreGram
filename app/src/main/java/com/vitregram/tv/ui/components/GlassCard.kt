package com.vitregram.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitregram.tv.ui.theme.GlassBorder
import com.vitregram.tv.ui.theme.GlassBorderFocused
import com.vitregram.tv.ui.theme.ObsidianCardTranslucent

/**
 * Premium Dark Obsidian Liquid Glass Card with D-pad focus elevation,
 * smooth scale-up animation, and luminous glass border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = ObsidianCardTranslucent,
    focusedBorderColor: Color = GlassBorderFocused,
    unfocusedBorderColor: Color = GlassBorder,
    borderWidth: Dp = 1.5.dp,
    focusedScale: Float = 1.04f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "glassCardScale"
    )

    val currentBorderColor = if (isFocused) focusedBorderColor else unfocusedBorderColor
    val currentBorderWidth = if (isFocused) borderWidth * 1.5f else borderWidth

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 16.dp else 4.dp,
                shape = shape,
                spotColor = if (isFocused) Color.White.copy(alpha = 0.25f) else Color.Black
            )
            .clip(shape)
            .background(backgroundColor)
            .border(
                BorderStroke(currentBorderWidth, currentBorderColor),
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        content(isFocused)
    }
}
