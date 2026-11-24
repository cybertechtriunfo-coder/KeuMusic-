package moe.rukamori.archivetune.ui.component.carousel

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import moe.rukamori.archivetune.R

internal class HeroRollSoundEffects(context: Context) {
    private val loadedSamples = mutableSetOf<Int>()
    private var rollingRequested = false
    private var spinStreamId = 0
    private var released = false

    private var spinSampleId = 0
    private var tickSampleId = 0
    private var winSampleId = 0

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            synchronized(this) {
                if (status == 0 && !released) {
                    loadedSamples.add(sampleId)
                    if (sampleId == spinSampleId && rollingRequested) {
                        startSpinStreamLocked()
                    }
                }
            }
        }

        spinSampleId = pool.load(context, R.raw.hero_roll_spin, 1)
        tickSampleId = pool.load(context, R.raw.hero_roll_tick, 1)
        winSampleId = pool.load(context, R.raw.hero_roll_win, 1)
    }

    private fun startSpinStreamLocked() {
        if (released || !rollingRequested || spinStreamId != 0) return
        if (spinSampleId == 0 || spinSampleId !in loadedSamples) return

        spinStreamId = pool.play(
            spinSampleId,
            0.25f,
            0.25f,
            1,
            -1,
            1f
        )
    }

    fun startSpin() {
        synchronized(this) {
            if (released) return
            rollingRequested = true
            startSpinStreamLocked()
        }
    }

    fun stopSpin() {
        synchronized(this) {
            rollingRequested = false
            if (spinStreamId != 0) {
                pool.stop(spinStreamId)
                spinStreamId = 0
            }
        }
    }

    fun playTick() {
        synchronized(this) {
            if (released || tickSampleId !in loadedSamples) return
            pool.play(tickSampleId, 0.48f, 0.48f, 2, 0, 1f)
        }
    }

    fun playWin() {
        synchronized(this) {
            if (released) return

            rollingRequested = false
            if (spinStreamId != 0) {
                pool.stop(spinStreamId)
                spinStreamId = 0
            }

            if (winSampleId in loadedSamples) {
                pool.play(winSampleId, 0.65f, 0.65f, 3, 0, 1f)
            }
        }
    }

    fun release() {
        synchronized(this) {
            if (released) return
            released = true
            rollingRequested = false
            spinStreamId = 0
            loadedSamples.clear()
            pool.release()
        }
    }
}
