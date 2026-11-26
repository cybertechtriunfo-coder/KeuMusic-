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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import moe.rukamori.archivetune.ui.PlayerViewModel

private val AiCyan = Color(0xFF55E9FF)
private val AiBlue = Color(0xFF438BFF)
private val AiPurple = Color(0xFF9B72FF)
private val AiPanel = Color(0xE9081020)

@Composable
fun KeuMusicAiOverlay(playerViewModel: PlayerViewModel) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var showIntro by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val introPreferences = context.getSharedPreferences("keumusic_ai_runtime", android.content.Context.MODE_PRIVATE)
        if (!introPreferences.getBoolean("welcome_presented", false)) {
            introPreferences.edit().putBoolean("welcome_presented", true).apply()
            showIntro = true
            expanded = true
        }
    }
    var aiSpeaking by rememberSaveable { mutableStateOf(false) }
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
        Column(
                modifier = (if (expanded) Modifier
                    .fillMaxWidth(0.90f)
                    .padding(end = 14.dp, bottom = 18.dp)
                    .heightIn(max = (maxHeight - 24.dp).coerceAtLeast(320.dp))
                    .verticalScroll(rememberScrollState())
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
                    .padding(16.dp) else Modifier.size(0.dp).alpha(0f).clipToBounds()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (expanded) {
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { expanded = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "−",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { dismissed = true; expanded = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "×",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
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
                        HologramFace(size = 156.dp, pulse = pulse, speaking = aiSpeaking)
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
                    text = "CONVERSA EXPERIMENTAL • voz em português e reconhecimento de fala do Android.",
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                }
                KeuMusicAiConversation(
                    onSpeakingChanged = { aiSpeaking = it },
                    onPlayNext = playerViewModel::playNext,
                    onPlayPrevious = playerViewModel::playPrevious,
                    onPausePlayback = playerViewModel::pausePlayback,
                    onResumePlayback = playerViewModel::resumePlayback,
                    onVoiceSearch = playerViewModel::playFromVoiceSearch,
                    onToggleShuffle = playerViewModel::toggleShuffle,
                    onToggleRepeat = playerViewModel::toggleRepeat,
                    startWithWelcome = showIntro,
                )
            }
        if (!expanded) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(startX.roundToInt(), startY.roundToInt()) }
                    .size(avatarSize)
                    .pointerInput(maxWidth, maxHeight) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                avatarX = ((if (avatarX.isNaN()) startX else avatarX) + dragAmount.x).coerceIn(0f, (maxWidthPx - avatarSizePx).coerceAtLeast(0f))
                                avatarY = ((if (avatarY.isNaN()) startY else avatarY) + dragAmount.y).coerceIn(0f, (maxHeightPx - avatarSizePx).coerceAtLeast(0f))
                            },
                        )
                    }
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center,
            ) {
                // Temporary holographic face; this will be replaced by the final animated avatar asset.
                HologramFace(size = avatarSize, pulse = pulse, speaking = aiSpeaking)
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
        drawCircle(AiPurple.copy(alpha = 0.18f * pulse), radius = minOf(w, h) * 0.42f, center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun HologramFace(size: Dp, pulse: Float, speaking: Boolean = false) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF164A68), Color(0xFF10233F), Color(0xFF050B18)),
                ),
            )
            .border(1.4.dp, AiCyan.copy(alpha = pulse), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(size * 0.035f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val glow = AiCyan.copy(alpha = 0.55f * pulse)

            // Holographic shoulders and chest plate.
            val torso = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.13f, h * 0.94f)
                lineTo(w * 0.20f, h * 0.77f)
                lineTo(w * 0.37f, h * 0.68f)
                lineTo(w * 0.63f, h * 0.68f)
                lineTo(w * 0.80f, h * 0.77f)
                lineTo(w * 0.87f, h * 0.94f)
                close()
            }
            drawPath(torso, Color(0xFF163A58).copy(alpha = 0.92f))
            drawPath(torso, glow, style = Stroke(width = 2.dp.toPx()))
            drawLine(AiBlue.copy(alpha = 0.8f), Offset(w * 0.30f, h * 0.84f), Offset(w * 0.70f, h * 0.84f), 1.5.dp.toPx())

            // Neck and angular cybernetic helmet.
            drawRoundRect(
                Color(0xFF397A9B),
                topLeft = Offset(w * 0.40f, h * 0.62f),
                size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.17f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
            )
            val helmet = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, h * 0.08f)
                lineTo(w * 0.76f, h * 0.20f)
                lineTo(w * 0.81f, h * 0.49f)
                lineTo(w * 0.68f, h * 0.68f)
                lineTo(w * 0.32f, h * 0.68f)
                lineTo(w * 0.19f, h * 0.49f)
                lineTo(w * 0.24f, h * 0.20f)
                close()
            }
            drawPath(helmet, Brush.linearGradient(listOf(Color(0xFFB8F4FF), Color(0xFF3C789D), Color(0xFF17314F))))
            drawPath(helmet, AiCyan.copy(alpha = pulse), style = Stroke(width = 2.2.dp.toPx()))

            // Forehead visor and side armor.
            drawRoundRect(
                Color(0xFF10243B),
                topLeft = Offset(w * 0.25f, h * 0.25f),
                size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.19f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f),
            )
            drawLine(glow, Offset(w * 0.29f, h * 0.30f), Offset(w * 0.71f, h * 0.30f), 1.dp.toPx())
            drawRoundRect(AiBlue, Offset(w * 0.16f, h * 0.34f), androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.20f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
            drawRoundRect(AiBlue, Offset(w * 0.74f, h * 0.34f), androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.20f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))

            // Expressive cyan eyes.
            val eyeY = h * 0.49f
            drawRoundRect(
                AiCyan.copy(alpha = 0.95f),
                Offset(w * 0.32f, eyeY),
                androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.035f),
                androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            )
            drawRoundRect(
                AiCyan.copy(alpha = 0.95f),
                Offset(w * 0.53f, eyeY),
                androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.035f),
                androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            )
            drawCircle(AiCyan.copy(alpha = 0.25f * pulse), w * 0.13f, Offset(w * 0.395f, eyeY))
            drawCircle(AiCyan.copy(alpha = 0.25f * pulse), w * 0.605f, Offset(w * 0.605f, eyeY))

            // Mouth plate animates only while the voice engine is speaking.
            drawRoundRect(
                Color(0xFF071525),
                Offset(w * 0.39f, h * 0.57f),
                androidx.compose.ui.geometry.Size(w * 0.22f, if (speaking) h * 0.075f else h * 0.025f),
                androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            )
            if (speaking) {
                drawRoundRect(glow, Offset(w * 0.42f, h * 0.59f), androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.025f), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            } else {
                drawLine(glow, Offset(w * 0.43f, h * 0.585f), Offset(w * 0.57f, h * 0.585f), 1.dp.toPx())
            }

            // Antenna, cheek vents and floating scan lines.
            drawLine(glow, Offset(cx, h * 0.02f), Offset(cx, h * 0.085f), 2.dp.toPx())
            drawCircle(AiCyan.copy(alpha = pulse), w * 0.025f, Offset(cx, h * 0.02f))
            repeat(3) { index ->
                val y = h * (0.70f + index * 0.035f)
                drawLine(AiCyan.copy(alpha = 0.28f * pulse), Offset(w * 0.34f, y), Offset(w * 0.66f, y), 1.dp.toPx())
            }
        }
        // Subtle hologram rings make the face legible at the minimized size too.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                AiCyan.copy(alpha = 0.16f * pulse),
                radius = size.minDimension * 0.47f,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}
