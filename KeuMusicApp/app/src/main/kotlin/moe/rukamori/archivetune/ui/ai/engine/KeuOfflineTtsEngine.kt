/*
 * KeuMusic AI | Offline Brazilian Portuguese male voice
 * Piper Cadu medium via sherpa-onnx. Voice files stay in app-private storage.
 */
package moe.rukamori.archivetune.ui.ai.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class KeuOfflineTtsEngine(private val context: Context) {
    companion object {
        private const val MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-pt_BR-cadu-medium.tar.bz2"
        private const val ESPEAK_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/espeak-ng-data.tar.bz2"
        private const val MODEL_SHA256 = "aba78157d4b89acc17ddef15a70f4b2474f4c189d4de6035002ac7fec9d5d303"
        private const val MODEL_DIR = "piper-pt-br-cadu-medium"
        private const val MODEL_FILE = "pt_BR-cadu-medium.onnx"
        private const val MAX_EXTRACTED_BYTES = 160L * 1024L * 1024L
    }

    private val main = Handler(Looper.getMainLooper())
    private val initializationLock = Mutex()
    @Volatile private var tts: OfflineTts? = null
    @Volatile private var currentTrack: AudioTrack? = null
    @Volatile private var released = false

    suspend fun initialize(onStatus: (String) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            if (released) error("O mecanismo de voz foi encerrado.")
            if (tts != null) return@withContext
            initializationLock.withLock {
                if (released) error("O mecanismo de voz foi encerrado.")
                if (tts != null) return@withLock

                val directory = ensureVoiceFiles { message -> main.post { onStatus(message) } }
                main.post { onStatus("Arquivos de voz instalados • carregando o cérebro de fala do Keu...") }
                val vits = OfflineTtsVitsModelConfig(
                    model = File(directory, MODEL_FILE).absolutePath,
                    tokens = File(directory, "tokens.txt").absolutePath,
                    dataDir = File(directory, "espeak-ng-data").absolutePath,
                    noiseScale = 0.5f,
                    noiseScaleW = 0.333f,
                    lengthScale = 1.0f,
                )
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(vits = vits, numThreads = 2, provider = "cpu"),
                )
                main.post { onStatus("Inicializando a voz neural masculina pt-BR...") }
                tts = OfflineTts(config = config)
                main.post { onStatus("Voz masculina do Keu pronta • preparando a primeira resposta...") }
            }
        }
    }

    suspend fun speak(
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val engine = tts ?: error("A voz masculina ainda está sendo preparada.")
        val spokenText = text.trim().take(1_200)
        if (spokenText.isBlank()) return@withContext
        var track: AudioTrack? = null
        try {
            val generated = engine.generate(spokenText, sid = 0, speed = 0.96f)
            if (generated.samples.isEmpty() || generated.sampleRate <= 0) error("O sintetizador não gerou áudio.")
            val samples = KeuVoiceEffects.process(generated.samples, generated.sampleRate)
            val minBuffer = AudioTrack.getMinBufferSize(
                generated.sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT,
            )
            if (minBuffer <= 0) error("O aparelho não aceita o formato de áudio da voz.")

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(generated.sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(maxOf(minBuffer, 16_384))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            if (track.state != AudioTrack.STATE_INITIALIZED) error("Não foi possível iniciar a saída de áudio.")

            currentTrack = track
            track.play()
            main.post(onStart)
            var offset = 0
            while (offset < samples.size && !released && currentTrack === track) {
                val written = track.write(samples, offset, minOf(2_048, samples.size - offset), AudioTrack.WRITE_BLOCKING)
                if (written < 0) error("Falha ao reproduzir a voz (áudio $written).")
                if (written == 0) Thread.sleep(8) else offset += written
            }
            while (!released && currentTrack === track &&
                track.playState == AudioTrack.PLAYSTATE_PLAYING &&
                track.playbackHeadPosition.toLong() < samples.size.toLong()
            ) Thread.sleep(20)
            if (!released && currentTrack === track) main.post(onDone)
        } catch (error: Throwable) {
            main.post { onError(error.message ?: "Falha ao sintetizar a voz.") }
        } finally {
            if (currentTrack === track) currentTrack = null
            runCatching {
                if (track?.state == AudioTrack.STATE_INITIALIZED) {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                    track.release()
                }
            }
        }
    }

    fun stop() {
        val track = currentTrack
        currentTrack = null
        runCatching {
            if (track?.state == AudioTrack.STATE_INITIALIZED) {
                track.pause()
                track.flush()
                track.stop()
                track.release()
            }
        }
    }

    fun release() {
        released = true
        stop()
        runCatching { tts?.release() }
        tts = null
    }

    private fun ensureVoiceFiles(onStatus: (String) -> Unit): File {
        val root = File(context.filesDir, "keumusic-voice").apply { mkdirs() }
        val finalDir = File(root, MODEL_DIR)
        if (isVoiceReady(finalDir)) return finalDir

        onStatus("Preparando a voz masculina do Keu • download inicial de aproximadamente 75 MB...")
        val staging = File(root, "$MODEL_DIR-staging").apply { deleteRecursively(); mkdirs() }
        val modelArchive = File(root, "cadu-medium.tar.bz2")
        download(MODEL_URL, modelArchive, MODEL_SHA256) { downloaded, total ->
            val totalLabel = if (total > 0L) " de ${total / (1024 * 1024)} MB" else ""
            onStatus("Baixando voz masculina do Keu: ${downloaded / (1024 * 1024)} MB$totalLabel")
        }
        onStatus("Download concluído • extraindo o modelo de voz masculina...")
        extractModel(modelArchive, staging)
        modelArchive.delete()

        onStatus("Instalando a pronúncia em português brasileiro...")
        val espeakArchive = File(root, "espeak-ng-data.tar.bz2")
        download(ESPEAK_URL, espeakArchive, null) { downloaded, total ->
            val totalLabel = if (total > 0L) " de ${total / (1024 * 1024)} MB" else ""
            onStatus("Baixando pronúncia pt-BR: ${downloaded / (1024 * 1024)} MB$totalLabel")
        }
        onStatus("Pronúncia pt-BR baixada • instalando arquivos de pronúncia...")
        extractEspeakData(espeakArchive, File(staging, "espeak-ng-data"))
        espeakArchive.delete()

        onStatus("Finalizando instalação e verificando os arquivos da voz...")
        if (!isVoiceReady(staging)) {
            staging.deleteRecursively()
            error("Os arquivos da voz Cadu ficaram incompletos. Tente novamente com uma conexão estável.")
        }
        finalDir.deleteRecursively()
        if (!staging.renameTo(finalDir)) {
            staging.copyRecursively(finalDir, overwrite = true)
            staging.deleteRecursively()
        }
        return finalDir
    }

    private fun isVoiceReady(dir: File): Boolean =
        File(dir, MODEL_FILE).let { it.isFile && it.length() > 20_000_000L } &&
            File(dir, "tokens.txt").let { it.isFile && it.length() > 1_000L } &&
            File(dir, "espeak-ng-data/phontab").isFile

    private fun download(
        url: String,
        destination: File,
        expectedSha256: String?,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ) {
        val part = File(destination.path + ".part")
        part.delete()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 25_000
            readTimeout = 90_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "KeuMusic-Android")
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) error("Download da voz falhou: HTTP ${connection.responseCode}.")
            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L
            var nextReportAt = 2L * 1024L * 1024L
            connection.inputStream.use { input ->
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (downloadedBytes >= nextReportAt) {
                            onProgress(downloadedBytes, totalBytes)
                            nextReportAt = downloadedBytes + 2L * 1024L * 1024L
                        }
                    }
                }
            }
            onProgress(downloadedBytes, totalBytes)
        } finally {
            connection.disconnect()
        }
        if (!part.isFile || part.length() < 100_000L) {
            part.delete()
            error("O arquivo de voz foi baixado incompleto.")
        }
        if (expectedSha256 != null) {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            part.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    messageDigest.update(buffer, 0, read)
                }
            }
            val digest = messageDigest.digest().joinToString("") { "%02x".format(it) }
            if (!digest.equals(expectedSha256, ignoreCase = true)) {
                part.delete()
                error("A verificação de segurança do modelo de voz falhou.")
            }
        }
        destination.delete()
        if (!part.renameTo(destination)) {
            part.copyTo(destination, overwrite = true)
            part.delete()
        }
    }

    private fun extractModel(archive: File, destination: File) {
        var modelFound = false
        var tokensFound = false
        var extracted = 0L
        TarArchiveInputStream(BufferedInputStream(BZip2CompressorInputStream(archive.inputStream()))).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                if (!entry.isFile) continue
                val name = entry.name.substringAfterLast('/')
                val targetName = when (name) {
                    MODEL_FILE -> MODEL_FILE
                    "tokens.txt" -> "tokens.txt"
                    else -> null
                } ?: continue
                val target = File(destination, targetName)
                extracted += copyEntry(tar, target)
                if (extracted > MAX_EXTRACTED_BYTES) error("Pacote de voz excedeu o limite esperado.")
                if (targetName == MODEL_FILE) modelFound = target.length() > 20_000_000L
                if (targetName == "tokens.txt") tokensFound = target.length() > 1_000L
            }
        }
        if (!modelFound || !tokensFound) error("O pacote Cadu não contém o modelo e os tokens esperados.")
    }

    private fun extractEspeakData(archive: File, destination: File) {
        destination.mkdirs()
        val base = destination.canonicalFile
        var extracted = 0L
        var files = 0
        TarArchiveInputStream(BufferedInputStream(BZip2CompressorInputStream(archive.inputStream()))).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                if (!entry.isFile) continue
                val normalized = entry.name.removePrefix("./").trimStart('/')
                val relative = if (normalized.startsWith("espeak-ng-data/")) normalized.removePrefix("espeak-ng-data/") else normalized
                if (relative.isBlank()) continue
                val target = File(destination, relative)
                if (!target.canonicalFile.path.startsWith(base.path + File.separator)) {
                    error("O pacote de pronúncia contém um caminho inválido.")
                }
                extracted += copyEntry(tar, target)
                files++
                if (extracted > MAX_EXTRACTED_BYTES || files > 20_000) error("Pacote de pronúncia excedeu o limite esperado.")
            }
        }
    }

    private fun copyEntry(input: TarArchiveInputStream, target: File): Long {
        target.parentFile?.mkdirs()
        var total = 0L
        FileOutputStream(target).use { output ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                output.write(buffer, 0, read)
            }
        }
        return total
    }
}
