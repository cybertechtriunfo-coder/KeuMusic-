/*
 * KeuMusic AI | Subtle spatial voice processing
 *
 * This DSP stage is intentionally independent from any TTS provider.
 * It can be placed after Sherpa/Piper PCM generation without tying the
 * KeuMusic voice identity to a particular synthesis engine.
 */
package moe.rukamori.archivetune.ui.ai.engine

data class KeuVoiceFxProfile(
    val enabled: Boolean = true,
    val dryLevel: Float = 0.96f,
    val earlyReflection: Float = 0.13f,
    val lateReflection: Float = 0.055f,
    val firstDelayMs: Int = 92,
    val secondDelayMs: Int = 158,
    val tailMs: Int = 360,
) {
    init {
        require(dryLevel in 0f..1f)
        require(earlyReflection in 0f..0.35f)
        require(lateReflection in 0f..0.25f)
        require(firstDelayMs in 35..250)
        require(secondDelayMs in (firstDelayMs + 10)..400)
        require(tailMs in 100..800)
    }

    companion object {
        /** Clean, confident voice with a restrained futuristic room reflection. */
        val FUTURISTIC_LIGHT = KeuVoiceFxProfile()

        /** Dry profile for accessibility and quiet environments. */
        val CLEAN = KeuVoiceFxProfile(
            enabled = false,
            dryLevel = 1f,
            earlyReflection = 0f,
            lateReflection = 0f,
        )
    }
}

/**
 * Adds a subtle, mono spatial reflection to normalized PCM samples.
 *
 * This does not synthesize speech or change pitch. The TTS engine supplies
 * the Brazilian Portuguese voice; this class only adds the optional effect.
 * The delayed signal is fed back at a low level to create a short, controlled
 * tail. Output is kept within the normalized PCM range.
 */
object KeuVoiceEffects {
    fun process(
        input: FloatArray,
        sampleRate: Int,
        profile: KeuVoiceFxProfile = KeuVoiceFxProfile.FUTURISTIC_LIGHT,
    ): FloatArray {
        if (input.isEmpty()) return input
        require(sampleRate in 8_000..96_000) { "Unsupported sample rate: $sampleRate" }

        if (!profile.enabled) return input.copyOf()

        val firstDelay = (sampleRate * profile.firstDelayMs / 1_000).coerceAtLeast(1)
        val secondDelay = (sampleRate * profile.secondDelayMs / 1_000).coerceAtLeast(firstDelay + 1)
        val tail = sampleRate * profile.tailMs / 1_000
        val output = FloatArray(input.size + tail)

        for (i in output.indices) {
            val dry = if (i < input.size) input[i] else 0f
            val early = if (i >= firstDelay) output[i - firstDelay] else 0f
            val late = if (i >= secondDelay) output[i - secondDelay] else 0f
            val reflected = early * profile.earlyReflection + late * profile.lateReflection
            output[i] = (dry * profile.dryLevel + reflected).coerceIn(-0.98f, 0.98f)
        }
        return output
    }
}
