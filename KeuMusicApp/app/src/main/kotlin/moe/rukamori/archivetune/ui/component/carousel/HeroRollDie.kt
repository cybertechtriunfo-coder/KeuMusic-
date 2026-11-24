/*
 * KeuMusic (2026) | Modified work by MuwMix
 * Adapted from Mochy Music (GPL-3.0)
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.component.carousel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun HeroRollDie(
    spinning: Boolean,
    animationsDisabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(spinning, animationsDisabled) {
        if (animationsDisabled) {
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        if (spinning) {
            while (true) {
                rotation.animateTo(rotation.value + 360f, tween(620, easing = LinearEasing))
            }
        } else {
            val rest = (rotation.value / 90f).roundToInt() * 90f
            if (rotation.value != rest) {
                rotation.animateTo(rest, spring(dampingRatio = 0.45f, stiffness = 300f))
            }
        }
    }

    Canvas(
        modifier = modifier
            .size(20.dp)
            .graphicsLayer { rotationZ = rotation.value }
    ) {
        val wheelColors = listOf(
            Color(0xFFFF5A5F), Color(0xFFFFB84D), Color(0xFFFFE066),
            Color(0xFF65C98A), Color(0xFF45B7D1), Color(0xFF7B78E8),
            Color(0xFFB46BE0), Color(0xFFFF78B7),
        )
        val diameter = size.minDimension * 0.88f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val wheelSize = androidx.compose.ui.geometry.Size(diameter, diameter)
        wheelColors.forEachIndexed { index, color ->
            drawArc(
                color = color,
                startAngle = index * 45f - 90f,
                sweepAngle = 45f,
                useCenter = true,
                topLeft = topLeft,
                size = wheelSize,
            )
        }
        drawCircle(
            color = Color.White,
            radius = diameter * 0.13f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
        drawCircle(
            color = Color(0xFF29233D),
            radius = diameter * 0.065f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}
