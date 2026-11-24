/*
 * KeuMusic AI | Experimental hands-free voice assistant
 */
package moe.rukamori.archivetune.ui.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.Normalizer
import java.util.Locale

private data class KeuMusicAiLine(val author: String, val message: String)

@Composable
fun KeuMusicAiConversation(
    onSpeakingChanged: (Boolean) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPausePlayback: () -> Unit,
    onResumePlayback: () -> Unit,
    onVoiceSearch: (String) -> Unit,
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var input by rememberSaveable { mutableStateOf("") }
    var ttsInitialized by remember { mutableStateOf(false) }
    var voiceReady by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf("Ative a escuta para conversar sem tocar no microfone.") }
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var handsFreeEnabled by rememberSaveable { mutableStateOf(false) }
    var waitingForCommand by rememberSaveable { mutableStateOf(false) }
    var restartSignal by remember { mutableIntStateOf(0) }
    val conversation = remember {
        mutableStateListOf(KeuMusicAiLine("KeuMusic", "Olá! Eu sou o KeuMusic AI. Quando você me chamar, vou responder por voz."))
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) {
            handsFreeEnabled = true
            voiceStatus = "Microfone autorizado • iniciando escuta automática"
        } else {
            handsFreeEnabled = false
            voiceStatus = "Permissão de microfone não concedida. Você pode ativá-la nas configurações do app."
        }
    }

    val tts = remember(context) {
        TextToSpeech(context) { result ->
            ttsInitialized = result == TextToSpeech.SUCCESS
            if (result != TextToSpeech.SUCCESS) {
                voiceReady = false
                voiceStatus = "O mecanismo de voz do Android não iniciou. Verifique o mecanismo de texto para fala nas configurações."
            }
        }
    }

    // Só libera a fala depois que o mecanismo e o idioma estiverem realmente prontos.
    LaunchedEffect(ttsInitialized) {
        voiceReady = false
        if (ttsInitialized) {
            val brazilianPortuguese = tts.setLanguage(Locale("pt", "BR"))
            val portuguese = if (
                brazilianPortuguese == TextToSpeech.LANG_MISSING_DATA ||
                brazilianPortuguese == TextToSpeech.LANG_NOT_SUPPORTED
            ) tts.setLanguage(Locale("pt")) else brazilianPortuguese

            if (portuguese >= TextToSpeech.LANG_AVAILABLE) {
                tts.setSpeechRate(0.96f)
                tts.setPitch(1.04f)
                voiceReady = true
                voiceStatus = "Voz pronta • português brasileiro"
            } else {
                voiceStatus = "Não encontrei voz em português. Instale Português (Brasil) no mecanismo de voz do Android."
            }
        }
    }

    val recognizer = remember(context) {
        when {
            !SpeechRecognizer.isRecognitionAvailable(context) -> null
            Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) ->
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else -> SpeechRecognizer.createSpeechRecognizer(context)
        }
    }
    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }

    fun normalized(value: String): String =
        Normalizer.normalize(value.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

    fun speak(answer: String) {
        conversation.add(KeuMusicAiLine("KeuMusic", answer))
        while (conversation.size > 12) conversation.removeAt(0)
        if (voiceReady) {
            val result = tts.speak(
                answer,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "keumusic-ai-${System.currentTimeMillis()}",
            )
            if (result == TextToSpeech.ERROR) {
                voiceStatus = "O mecanismo de voz recusou a reprodução. Confira a voz instalada no Android."
                onSpeakingChanged(false)
            }
        } else {
            voiceStatus = "Resposta exibida, mas a voz ainda não está pronta. " +
                "Verifique o mecanismo de texto para fala e a voz Português (Brasil) nas configurações do Android."
            restartSignal++
        }
    }

    fun processCommand(rawCommand: String) {
        val command = normalized(rawCommand)
        waitingForCommand = false
        when {
            listOf("proxima", "troca de musica", "trocar musica", "outra musica", "passa a musica", "pula a musica", "avanca").any(command::contains) -> {
                onPlayNext()
                speak(listOf("Beleza! Vamos para a próxima.", "Pode deixar, já estou trocando.", "Pronto! Próxima música na área.").random())
            }
            listOf("musica anterior", "volta a musica", "voltar musica", "faixa anterior", "volta uma").any(command::contains) -> {
                onPlayPrevious()
                speak("Claro! Voltando para a música anterior.")
            }
            listOf("pausa", "pausar", "para a musica", "parar musica", "pode parar").any(command::contains) -> {
                onPausePlayback()
                speak("Tudo bem, pausei a música.")
            }
            listOf("continua", "continuar", "retoma", "retomar", "volta a tocar", "pode tocar").any(command::contains) -> {
                onResumePlayback()
                speak("Pronto! A música está tocando novamente.")
            }
            command.startsWith("toca ") || command.startsWith("coloca ") ||
                command.startsWith("quero ouvir ") || command.startsWith("reproduz ") ||
                command.startsWith("toque ") -> {
                val query = command
                    .replace(Regex("^(toca|toque|coloca|reproduz|quero ouvir)\\s+"), "")
                    .replace(Regex("^(uma musica|musica|o album|album)\\s+"), "")
                    .trim()
                if (query.isNotBlank()) {
                    onVoiceSearch(query)
                    speak("Certo! Vou procurar $query para você.")
                } else {
                    waitingForCommand = true
                    speak("Qual música ou artista você quer ouvir?")
                }
            }
            command.contains("quem criou") || command.contains("seu criador") ->
                speak("Fui criado por Cleudeson Soares, idealizador do KeuMusic e criador do KeuMusic AI.")
            command.contains("roleta") || command.contains("roll") ->
                speak("A roleta musical está no KeuMusic. Você pode abrir a área da roleta e girar para sortear uma música.")
            command.contains("playlist") ->
                speak("As playlists ajudam você a organizar suas músicas favoritas. Abra a área de playlists para acessar suas listas.")
            command.contains("album") || command.contains("albuns") ->
                speak("Você pode explorar seus álbuns na área de álbuns do KeuMusic.")
            command.contains("tudo bem") || command == "oi" || command == "ola" ->
                speak(listOf("Tudo ótimo! E com você?", "Opa! Tudo tranquilo por aqui. E aí?", "Estou bem e pronto para escolher uma música com você.").random())
            else ->
                speak("Entendi. Ainda estou aprendendo novas funções, mas já posso ajudar com a reprodução e a busca de músicas.")
        }
    }

    fun handleTranscript(transcript: String) {
        if (transcript.isBlank()) {
            restartSignal++
            return
        }
        conversation.add(KeuMusicAiLine("Você", transcript))
        while (conversation.size > 12) conversation.removeAt(0)
        val text = normalized(transcript)
        if (waitingForCommand) {
            processCommand(transcript)
            return
        }

        val wakePhrases = listOf(
            "keu music", "queu music", "q music", "keu musick", "keu musiq",
            "oi keu", "ola keu", "e ai keu", "bom dia keu", "boa tarde keu", "boa noite keu",
        )
        val wakePhrase = wakePhrases.firstOrNull(text::contains)
        if (wakePhrase == null) {
            voiceStatus = "Aguardando: diga Oi, KeuMusic ou E aí, KeuMusic."
            restartSignal++
            return
        }

        var remainder = text.replace(wakePhrase, " ").trim()
        remainder = remainder.replace(
            Regex("^(oi|ola|e ai|bom dia|boa tarde|boa noite|tudo bem)\\s*"),
            "",
        ).trim()
        if (remainder.isNotBlank()) {
            processCommand(remainder)
        } else {
            waitingForCommand = true
            speak(listOf(
                "Oi! Estou aqui. O que vamos ouvir?",
                "E aí! Tudo tranquilo? Pode falar.",
                "Olá! Estou ouvindo. O que você precisa?",
                "Opa! Manda ver, estou aqui.",
                "Bom te ouvir! Qual é o pedido de hoje?",
            ).random())
        }
    }

    val latestTranscriptHandler by rememberUpdatedState<(String) -> Unit> { handleTranscript(it) }
    val latestHandsFreeEnabled by rememberUpdatedState(handsFreeEnabled)
    val latestPermissionGranted by rememberUpdatedState(permissionGranted)

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { onSpeakingChanged(true) }
            override fun onDone(utteranceId: String?) {
                onSpeakingChanged(false)
                if (latestHandsFreeEnabled) restartSignal++
            }
            @Deprecated("Deprecated in Android API")
            override fun onError(utteranceId: String?) {
                onSpeakingChanged(false)
                voiceStatus = "Falha ao reproduzir a voz. Confira o mecanismo TTS e o idioma Português (Brasil) do Android."
                if (latestHandsFreeEnabled) restartSignal++
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                onSpeakingChanged(false)
                voiceStatus = "Falha no áudio da resposta (código ${errorCode}). Verifique a voz instalada no Android."
                if (latestHandsFreeEnabled) restartSignal++
            }
        })
        onDispose {
            onSpeakingChanged(false)
            tts.stop()
            tts.shutdown()
        }
    }

    DisposableEffect(recognizer) {
        if (recognizer == null) {
            voiceStatus = "Reconhecimento de voz indisponível neste aparelho."
        } else {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    voiceStatus = if (waitingForCommand) "Pode falar, estou ouvindo seu pedido." else "Escutando a chamada KeuMusic."
                }
                override fun onBeginningOfSpeech() { voiceStatus = "Estou ouvindo..." }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { voiceStatus = "Entendi sua voz. Processando..." }
                override fun onError(error: Int) {
                    if (latestHandsFreeEnabled && latestPermissionGranted) {
                        mainHandler.postDelayed({ runCatching { recognizer.startListening(recognitionIntent) } }, 650)
                    }
                }
                override fun onResults(results: Bundle?) {
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!spoken.isNullOrBlank()) latestTranscriptHandler(spoken) else restartSignal++
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
            runCatching { recognizer?.cancel() }
            recognizer?.destroy()
        }
    }

    LaunchedEffect(handsFreeEnabled, permissionGranted, restartSignal, recognizer) {
        if (handsFreeEnabled && permissionGranted && recognizer != null) {
            delay(350)
            runCatching { recognizer.startListening(recognitionIntent) }
                .onFailure { voiceStatus = "Não consegui iniciar a escuta. Tente desativar e ativar novamente." }
        }
    }

    fun sendTypedMessage() {
        val question = input.trim()
        if (question.isEmpty()) return
        conversation.add(KeuMusicAiLine("Você", question))
        input = ""
        processCommand(question)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x66061120), RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 150.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            conversation.takeLast(8).forEach { line ->
                Text(
                    text = "${line.author}: ${line.message}",
                    color = if (line.author == "KeuMusic") Color(0xFFBDEFFF) else Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Digite uma mensagem (opcional)") },
            maxLines = 2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    if (!permissionGranted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    else {
                        handsFreeEnabled = !handsFreeEnabled
                        if (handsFreeEnabled) voiceStatus = "Escuta automática ativada."
                        else {
                            waitingForCommand = false
                            recognizer?.cancel()
                            voiceStatus = "Escuta automática pausada."
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (handsFreeEnabled) "Parar escuta" else "Ativar escuta")
            }
            Button(
                onClick = { sendTypedMessage() },
                enabled = input.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Enviar")
            }
        }
        Text(
            text = voiceStatus,
            color = Color(0xFF9AB4C8),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "Escuta experimental: prioriza reconhecimento local quando disponível. A busca e os comandos básicos usam o player do KeuMusic.",
            color = Color(0xFF8297AA),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}
