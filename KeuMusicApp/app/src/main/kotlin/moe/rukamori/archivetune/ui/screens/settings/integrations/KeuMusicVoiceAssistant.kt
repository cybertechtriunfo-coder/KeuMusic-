/*
 * KeuMusic (2026) | Modified work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 *
 * First interactive voice bridge: Android speech recognition (pt-BR)
 * -> KeuMusicAiBrain -> Android text-to-speech.
 */
package moe.rukamori.archivetune.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.ai.KeuMusicAiBrain
import java.util.Locale

@Composable
internal fun KeuMusicVoiceAssistantDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conversation = remember { mutableStateListOf<Pair<String, String>>() }
    var status by remember { mutableStateOf("Pronto para conversar em português.") }
    var listening by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var pendingPermission by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    val startListening = {
        val current = recognizer
        if (current == null) {
            status = "Reconhecimento de voz indisponível neste aparelho."
        } else if (!listening && !thinking) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Pode falar com o KeuMusic")
                }
                status = "Estou ouvindo..."
                listening = true
                current.startListening(intent)
            } catch (_: Exception) {
                listening = false
                status = "Não consegui iniciar o microfone. Tente novamente."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            status = "A permissão do microfone é necessária para conversar por voz."
        }
        pendingPermission = false
    }

    fun requestListening() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            startListening()
        } else {
            pendingPermission = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { speech ->
                speech.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        status = "Pode falar..."
                    }
                    override fun onBeginningOfSpeech() { status = "Entendi, continue..." }
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() {
                        listening = false
                        status = "Processando sua fala..."
                    }
                    override fun onError(error: Int) {
                        listening = false
                        status = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "Não consegui entender. Toque no microfone e tente novamente."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "A permissão do microfone não foi concedida."
                            else -> "O reconhecimento de voz falhou. Tente novamente."
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        listening = false
                        val spoken = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (spoken.isBlank()) {
                            status = "Não reconheci a frase. Tente novamente."
                            return
                        }
                        conversation.add("Você" to spoken)
                        thinking = true
                        status = "Keu está pensando..."
                        scope.launch {
                            try {
                                val history = conversation.dropLast(1).takeLast(8).map { (speaker, text) ->
                                    "$speaker: $text"
                                }
                                val answer = withContext(Dispatchers.IO) {
                                    KeuMusicAiBrain.reply(context, spoken, history)
                                }
                                conversation.add("Keu" to answer)
                                status = "Resposta pronta."
                                if (ttsReady) {
                                    tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "keumusic-answer")
                                } else {
                                    status = "Resposta recebida, mas a voz do aparelho ainda não está pronta."
                                }
                            } catch (error: Exception) {
                                status = error.message?.takeIf { it.isNotBlank() }
                                    ?: "Não consegui obter uma resposta da IA."
                            } finally {
                                thinking = false
                            }
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
            }
        } else {
            status = "Este aparelho não possui um serviço de reconhecimento de voz disponível."
        }

        tts = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val languageResult = tts?.setLanguage(Locale.forLanguageTag("pt-BR"))
                tts?.setSpeechRate(0.94f)
                ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!ttsReady) status = "Instale dados de voz em português (Brasil) no Android."
            } else {
                status = "Não foi possível iniciar a voz do Android."
            }
        }

        onDispose {
            recognizer?.destroy()
            recognizer = null
            tts?.stop()
            tts?.shutdown()
            tts = null
            ttsReady = false
        }
    }

    LaunchedEffect(pendingPermission) {
        if (!pendingPermission) return@LaunchedEffect
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("KeuMusic • Assistente de voz") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                if (conversation.isEmpty()) {
                    Text("Toque no microfone e fale normalmente. O Keu vai responder em voz alta.")
                } else {
                    conversation.takeLast(6).forEach { (speaker, message) ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(speaker, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { requestListening() },
                        enabled = !listening && !thinking,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (listening) "Ouvindo..." else "Falar")
                    }
                    OutlinedButton(
                        onClick = {
                            tts?.stop()
                            status = "Fala interrompida."
                        },
                    ) {
                        Text("Parar voz")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
    )
}
