/*
 * KeuMusic (2026) | Modified work by MuwMix
 * Adapted from Mochy Music (GPL-3.0)
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.component.carousel

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.ui.haptics.YumaHaptics
import moe.rukamori.archivetune.ui.haptics.rememberYumaHaptics
import kotlin.math.roundToInt
import kotlin.random.Random

@Stable
internal class HeroRollController(
    val state: HeroRollState,
    private val haptics: YumaHaptics,
    private val scope: CoroutineScope,
) {
    private var rollJob: Job? = null

    fun syncCandidates(items: List<HeroItem>) {
        val currentIds = items.map { it.id }.toSet()

        val candidateIds = state.candidates.map { it.id }.toSet()
        if (state.candidates.isEmpty() || !currentIds.containsAll(candidateIds) || candidateIds.size < items.size.coerceAtMost(HeroCarouselTokens.GachaCandidates)) {
            cancelRoll()
            state.candidates = items.shuffled().take(HeroCarouselTokens.GachaCandidates)
        } else {
            val itemMap = items.associateBy { it.id }
            state.candidates = state.candidates.mapNotNull { itemMap[it.id] }
        }

        val currentResult = state.result
        if (currentResult != null) {
            val updated = items.find { it.id == currentResult.id }
            if (updated == null) {
                state.result = null
                if (state.phase == HeroRollPhase.Landed) {
                    state.phase = HeroRollPhase.Idle
                }
            } else {
                state.result = updated
            }
        }

        val staleKeys = state.tints.keys.filter { it !in currentIds }
        staleKeys.forEach { state.tints.remove(it) }
    }

    fun onActivityChanged(isCarouselActive: Boolean, animationsDisabled: Boolean) {
        if (!isCarouselActive || animationsDisabled) {
            cancelRoll()
        }
    }

    fun cancelRoll() {
        val activeJob = rollJob
        rollJob = null
        activeJob?.cancel()

        if (state.phase == HeroRollPhase.Rolling) {
            state.phase = HeroRollPhase.Idle
        }
    }

    fun roll(isCarouselActive: Boolean, animationsDisabled: Boolean) {
        if (!isCarouselActive ||
            state.phase == HeroRollPhase.Rolling ||
            state.candidates.size < 2
        ) return

        rollJob?.cancel()
        rollJob = null

        if (animationsDisabled) {
            val winner = state.candidates.random()
            state.result = winner
            state.phase = HeroRollPhase.Landed
            state.completionToken++
            haptics.confirm()
            return
        }

        val candidateList = state.candidates
        val count = candidateList.size

        var currentJob: Job? = null

        // Bloqueia novos toques imediatamente, antes da coroutine começar.
        state.phase = HeroRollPhase.Rolling

        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val winnerIndex = Random.nextInt(count)
                val duration = Random.nextInt(
                    HeroCarouselTokens.GachaMinRollMillis,
                    HeroCarouselTokens.GachaMaxRollMillis + 1,
                )

                val start = state.reel.value.roundToInt()
                state.reel.snapTo(start.toFloat())

                val spins = count * (2 + duration / 2500)
                val landing = start + spins +
                    (winnerIndex - (start + spins)).mod(count)

                state.reel.animateTo(
                    landing.toFloat(),
                    tween(duration, easing = HeroCarouselTokens.GachaRollEasing),
                )

                if (!isActive) return@launch

                val won = candidateList[winnerIndex]
                state.result = won
                state.phase = HeroRollPhase.Landed
                state.completionToken++
                haptics.confirm()

                launch {
                    state.landPop.snapTo(1f)
                    state.landPop.animateTo(1.1f, tween(120))
                    state.landPop.animateTo(
                        1f,
                        spring(dampingRatio = 0.35f, stiffness = 320f)
                    )
                }

                launch {
                    state.burst.snapTo(0.001f)
                    state.burst.animateTo(
                        1f,
                        tween(
                            1100,
                            easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
                        )
                    )
                    state.burst.snapTo(0f)
                }
            } finally {
                // Uma tarefa antiga não pode encerrar uma rolagem mais recente.
                if (rollJob === currentJob) {
                    rollJob = null
                    if (state.phase == HeroRollPhase.Rolling) {
                        state.phase = HeroRollPhase.Idle
                    }
                }
            }
        }

        currentJob = job
        rollJob = job
        job.start()
    }
}

@Composable
internal fun rememberHeroRollController(
    state: HeroRollState = remember { HeroRollState() },
    haptics: YumaHaptics = rememberYumaHaptics(),
    scope: CoroutineScope = rememberCoroutineScope(),
): HeroRollController = remember(state, haptics, scope) {
    HeroRollController(state, haptics, scope)
}

@Composable
internal fun HeroRollEffects(
    controller: HeroRollController,
    items: List<HeroItem>,
    isCarouselActive: Boolean,
    animationsDisabled: Boolean,
    context: Context = LocalContext.current,
    haptics: YumaHaptics = rememberYumaHaptics(),
    scope: CoroutineScope = rememberCoroutineScope(),
) {
    val state = controller.state
    val soundEffects = remember(context.applicationContext) {
        HeroRollSoundEffects(context.applicationContext)
    }

    LaunchedEffect(items) {
        controller.syncCandidates(items)
    }

    LaunchedEffect(isCarouselActive, animationsDisabled) {
        controller.onActivityChanged(isCarouselActive, animationsDisabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.cancelRoll()
        }
    }

    DisposableEffect(soundEffects) {
        onDispose { soundEffects.release() }
    }

    LaunchedEffect(state.phase, soundEffects) {
        when (state.phase) {
            HeroRollPhase.Rolling -> soundEffects.startSpin()
            HeroRollPhase.Landed -> soundEffects.playWin()
            HeroRollPhase.Idle -> soundEffects.stopSpin()
        }
    }

    val candidates = state.candidates
    LaunchedEffect(candidates) {
        candidates.forEach { item ->
            if (item.id in state.tints) return@forEach
            val url = item.thumbnailUrl ?: return@forEach
            HeroPaletteHelper.extractRollTint(context, url)?.let { state.tints[item.id] = it }
        }
    }

    LaunchedEffect(state, isCarouselActive, soundEffects) {
        var lastBuzz = 0L
        snapshotFlow { state.reel.value.roundToInt() }
            .drop(1)
            .collect {
                if (state.phase != HeroRollPhase.Rolling || !isCarouselActive) return@collect
                soundEffects.playTick()
                val now = SystemClock.uptimeMillis()
                if (now - lastBuzz >= 45L) {
                    lastBuzz = now
                    haptics.segmentTick()
                }
                scope.launch {
                    state.pointerKick.snapTo(1f)
                    state.pointerKick.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 900f))
                }
            }
    }
}
