/*
 * KeuMusic AI | Offline Brazilian Portuguese speech input
 * Uses sherpa-onnx Whisper multilingual locally; no Android/Google SpeechRecognizer.
 */
package moe.rukamori.archivetune.ui.ai.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import androidx.core.content.ContextCompat
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import kotlin.math.sqrt

/**
 * Keeps capture and transcription inside the app. Whisper Tiny multilingual is downloaded once
 * into app-private storage (roughly 100 MB of selected int8 model files).
 */
class KeuOfflineSpeechEngine(private val context: Context) {
    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val FRAME_SAMPLES = 1_600 // 100 ms
        private const val MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.tar.bz2"
        private const val MODEL_DIR = "keumusic-voice/whisper-tiny"
        private const val MIN_MODEL_BYTES = 1_000_000L
    }

    private val main = Handler(Looper.getMainLooper())
    // Adapted from Jarvis (MIT): adaptive microphone gain and noise-floor tracking.
    private val micGain = MicGain()
    @Volatile private var running = false
    @Volatile private var released = false
    @Volatile private var outputActive = false
    @Volatile private var recognizer: OfflineRecognizer? = null
    @Volatile private var worker: Thread? = null
    @Volatile private var onStatus: (String) -> Unit = {}
    @Volatile private var onTranscript: (String) -> Unit = {}

    fun start(status: (String) -> Unit, transcript: (String) -> Unit) {
        onStatus = status
        onTranscript = transcript
        if (released || running) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            postStatus("Permissão de microfone necessária.")
            return
        }
        running = true
        worker = Thread({ loadAndListen() }, "keumusic-offline-voice").also { it.start() }
    }

    fun setOutputActive(active: Boolean) {
        outputActive = active
    }

    fun stop() {
        running = false
        worker?.interrupt()
        runCatching { worker?.join(1_000) }
        worker = null
    }

    fun release() {
        released = true
        stop()
        recognizer?.release()
        recognizer = null
    }

    private fun postStatus(value: String) = main.post { if (!released) onStatus(value) }
    private fun postTranscript(value: String) = main.post { if (!released && running) onTranscript(value) }

    private fun modelFiles(): Triple<File, File, File> {
        val dir = File(context.filesDir, MODEL_DIR)
        return Triple(
            File(dir, "tiny-encoder.int8.onnx"),
            File(dir, "tiny-decoder.int8.onnx"),
            File(dir, "tiny-tokens.txt"),
        )
    }

    private fun ensureModel(): Boolean {
        val (encoder, decoder, tokens) = modelFiles()
        if (encoder.isFile && decoder.isFile && tokens.isFile &&
            encoder.length() > 10_000_000L && decoder.length() > 80_000_000L &&
            tokens.length() > 700_000L
        ) return true

        postStatus("Preparando a voz offline do Keu • baixando modelo uma única vez...")
        val root = File(context.filesDir, "keumusic-voice").apply { mkdirs() }
        val archive = File(root, "whisper-tiny.tar.bz2.part")
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "KeuMusic-Android")
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) error("Download do modelo falhou: HTTP ${connection.responseCode}")
            connection.inputStream.use { input ->
                FileOutputStream(archive).use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }

        postStatus("Instalando o modelo de reconhecimento local...")
        val staging = File(root, "whisper-tiny-staging").apply {
            deleteRecursively()
            mkdirs()
        }
        TarArchiveInputStream(BufferedInputStream(BZip2CompressorInputStream(archive.inputStream()))).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                if (!entry.isFile) continue
                val name = entry.name.substringAfterLast('/')
                val targetName = when {
                    name == "tiny-encoder.int8.onnx" -> "tiny-encoder.int8.onnx"
                    name == "tiny-decoder.int8.onnx" -> "tiny-decoder.int8.onnx"
                    name == "tiny-tokens.txt" -> "tiny-tokens.txt"
                    else -> null
                } ?: continue
                val target = File(staging, targetName)
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = tar.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
        archive.delete()
        val staged = listOf(
            File(staging, "tiny-encoder.int8.onnx"),
            File(staging, "tiny-decoder.int8.onnx"),
            File(staging, "tiny-tokens.txt"),
        )
        if (staged.any { !it.isFile || it.length() < MIN_MODEL_BYTES }) {
            staging.deleteRecursively()
            error("O pacote de voz está incompleto. Tente novamente com uma conexão estável.")
        }
        val finalDir = File(root, "whisper-tiny")
        finalDir.deleteRecursively()
        if (!staging.renameTo(finalDir)) {
            finalDir.mkdirs()
            staged.forEach { it.copyTo(File(finalDir, it.name), overwrite = true) }
            staging.deleteRecursively()
        }
        return true
    }

    private fun createRecognizer(): OfflineRecognizer {
        val (encoder, decoder, tokens) = modelFiles()
        val model = OfflineModelConfig(
            whisper = OfflineWhisperModelConfig(
                encoder = encoder.absolutePath,
                decoder = decoder.absolutePath,
                language = "pt",
                task = "transcribe",
            ),
            tokens = tokens.absolutePath,
            numThreads = 2,
            provider = "cpu",
            modelType = "whisper",
        )
        return OfflineRecognizer(
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = model,
                decodingMethod = "greedy_search",
            ),
        )
    }

    private fun loadAndListen() {
        var retry = 0
        try {
            while (running && !released) {
                try {
                    ensureModel()
                    if (!running || released) break
                    recognizer?.release()
                    recognizer = createRecognizer()
                    postStatus("Voz offline pronta • Keu está escutando dentro do KeuMusic.")
                    captureLoop()
                    if (running && !released) error("A captura do microfone foi encerrada inesperadamente.")
                    retry = 0
                } catch (error: Throwable) {
                    if (!running || released || error is InterruptedException) break
                    runCatching { recognizer?.release() }
                    recognizer = null
                    retry++
                    val waitMs = minOf(30_000L, 2_000L shl (retry - 1).coerceAtMost(4))
                    postStatus(
                        "A voz encontrou um problema: ${error.message ?: "erro desconhecido"}. " +
                            "Vou tentar novamente em ${waitMs / 1_000}s.",
                    )
                    try {
                        Thread.sleep(waitMs)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        } finally {
            runCatching { worker = null }
        }
    }

    private fun captureLoop() {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) error("Microfone incompatível com áudio de 16 kHz.")
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, FRAME_SAMPLES * 4),
        )
        var echo: AcousticEchoCanceler? = null
        try {
            if (record.state != AudioRecord.STATE_INITIALIZED) error("Não foi possível abrir o microfone.")
            if (AcousticEchoCanceler.isAvailable()) {
                echo = runCatching { AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true } }.getOrNull()
            }
            record.startRecording()
            val frame = ShortArray(FRAME_SAMPLES)
            val preRoll = ArrayDeque<FloatArray>()
            val utterance = ArrayList<FloatArray>()
            var speaking = false
            var silenceFrames = 0
            var speechFrames = 0
            while (running && !released) {
                val count = record.read(frame, 0, frame.size)
                if (count <= 0) continue
                if (outputActive) {
                    preRoll.clear()
                    utterance.clear()
                    speaking = false
                    silenceFrames = 0
                    continue
                }
                val samples = FloatArray(count) { frame[it] / 32768.0f }
                // Jarvis-style adaptive gain makes quiet speech easier to detect while limiting
                // amplification of room noise. Keep the raw RMS for voice activity detection.
                val rawRms = micGain.process(samples)
                val voiceThreshold = maxOf(0.0015f, micGain.noiseFloor * 3.2f)
                val voiced = rawRms > voiceThreshold
                if (!speaking) {
                    preRoll.addLast(samples)
                    while (preRoll.size > 5) preRoll.removeFirst()
                    if (voiced) {
                        speaking = true
                        speechFrames = 0
                        silenceFrames = 0
                        preRoll.forEach { utterance.add(it) }
                        preRoll.clear()
                    }
                } else {
                    utterance.add(samples)
                    if (voiced) {
                        speechFrames++
                        silenceFrames = 0
                    } else {
                        silenceFrames++
                    }
                    if (utterance.size >= 150 || (silenceFrames >= 7 && speechFrames >= 2)) {
                        if (speechFrames >= 2) transcribe(utterance)
                        utterance.clear()
                        preRoll.clear()
                        speaking = false
                        silenceFrames = 0
                        speechFrames = 0
                    }
                }
            }
        } finally {
            runCatching { record.stop() }
            record.release()
            echo?.release()
        }
    }

    private fun transcribe(frames: List<FloatArray>) {
        val activeRecognizer = recognizer ?: return
        postStatus("Keu está entendendo o que você falou...")
        val samples = frames.flatMap { it.asIterable() }.toFloatArray()
        val stream = activeRecognizer.createStream()
        try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            activeRecognizer.decode(stream)
            val text = activeRecognizer.getResult(stream).text.trim()
            if (text.isNotBlank()) {
                postTranscript(text)
            } else {
                postStatus("Não entendi. Pode repetir chamando: Keu.")
            }
        } finally {
            stream.release()
        }
    }
}
