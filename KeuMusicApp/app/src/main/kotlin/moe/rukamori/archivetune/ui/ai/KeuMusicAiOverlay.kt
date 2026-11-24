/*
 * KeuMusic AI | Experimental visual assistant
 * The overlay is intentionally local-only in this first milestone.
 */
package moe.rukamori.archivetune.ui.ai

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

private val AiCyan = Color(0xFF55E9FF)
private val AiBlue = Color(0xFF438BFF)
private val AiPurple = Color(0xFF9B72FF)
private val AiPanel = Color(0xE9081020)

@Composable
fun KeuMusicAiOverlay() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var avatarX by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var avatarY by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    // When the user closes the assistant, stop composing its animation loop
    // so the hidden overlay does not keep consuming frames in the background.
    if (dismissed) return

    val transition = rememberInfiniteTransition(label = "keumusicAiPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(animation = tween(1700)),
        label = "keumusicAiPulseAlpha",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val avatarSize = 76.dp
        val margin = 18.dp
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val avatarSizePx = with(density) { avatarSize.toPx() }
        val marginPx = with(density) { margin.toPx() }
        val startX = if (avatarX.isNaN()) (maxWidthPx - avatarSizePx - marginPx).coerceAtLeast(0f) else avatarX
        val startY = if (avatarY.isNaN()) (maxHeightPx - avatarSizePx - with(density) { 22.dp.toPx() }).coerceAtLeast(0f) else avatarY
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(end = 14.dp, bottom = 18.dp)
                    .heightIn(max = 470.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xF20A1428),
                                Color(0xE9101732),
                                Color(0xE90B2030),
                            ),
                        ),
                    )
                    .border(1.dp, AiCyan.copy(alpha = 0.52f), RoundedCornerShape(28.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KEUMUSIC AI",
                            color = AiCyan,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "LABORATÓRIO HOLOGRÁFICO",
                            color = Color.White.copy(alpha = 0.62f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        text = "−",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { expanded = false }
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                    Text(
                        text = "×",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { dismissed = true; expanded = false }
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 210.dp, max = 280.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0x8A06101F)),
                ) {
                    MatrixGrid(modifier = Modifier.matchParentSize(), pulse = pulse)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HologramFace(size = 118.dp, pulse = pulse)
                        Text(
                            text = "Olá! Eu sou o KeuMusic AI",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Seu assistente virtual está ganhando vida.",
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Surface(
                            color = AiBlue.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AiCyan.copy(alpha = 0.45f)),
                        ) {
                            Text(
                                text = "INTERFACE EXPERIMENTAL",
                                color = AiCyan,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Text(
                    text = "A aparência holográfica já está em teste. A conversa inteligente, o microfone e a voz serão conectados na próxima etapa.",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .offset { IntOffset(startX.roundToInt(), startY.roundToInt()) }
                    .size(avatarSize)
                    .pointerInput(maxWidth, maxHeight) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                avatarX = (startX + dragAmount.x).coerceIn(0f, (maxWidthPx - avatarSizePx).coerceAtLeast(0f))
                                avatarY = (startY + dragAmount.y).coerceIn(0f, (maxHeightPx - avatarSizePx).coerceAtLeast(0f))
                            },
                        )
                    }
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center,
            ) {
                // Temporary holographic face; this will be replaced by the final animated avatar asset.
                HologramFace(size = avatarSize, pulse = pulse)
            }
        }
    }
}

@Composable
private fun MatrixGrid(modifier: Modifier = Modifier, pulse: Float) {
    Canvas(modifier = modifier) {
        val gap = 30.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(AiCyan.copy(alpha = 0.10f * pulse), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1.dp.toPx())
            x += gap
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(AiBlue.copy(alpha = 0.08f * pulse), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
            y += gap
        }
        for (index in 0..11) {
            val dotX = (index * 71f + 23f) % size.width
            val dotY = (index * 43f + 17f) % size.height
            drawCircle(AiCyan.copy(alpha = 0.34f * pulse), radius = 1.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(dotX, dotY))
        }
        drawCircle(AiPurple.copy(alpha = 0.18f * pulse), radius = size.minDimension * 0.42f, center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun HologramFace(size: Dp, pulse: Float) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF3A7898), Color(0xFF172A55), Color(0xFF091426))))
            .border(1.2.dp, AiCyan.copy(alpha = pulse), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.58f, size * 0.70f)
                .clip(RoundedCornerShape(42.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFB6EFFF), Color(0xFF4E83B6), Color(0xFF20365F)))),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = size * 0.13f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.size(size * 0.075f).clip(CircleShape).background(Color(0xFF06172B)))
                Box(Modifier.size(size * 0.075f).clip(CircleShape).background(Color(0xFF06172B)))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = size * 0.16f)
                    .width(size * 0.18f)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(AiCyan),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = size * 0.10f)
                .fillMaxWidth(0.62f)
                .height(size * 0.18f)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color(0xFF101B35)),
        )
    }
}
