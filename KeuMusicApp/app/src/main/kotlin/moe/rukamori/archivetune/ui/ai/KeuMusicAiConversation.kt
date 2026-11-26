/*
 * KeuMusic AI | Experimental hands-free voice assistant
 */
package moe.rukamori.archivetune.ui.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale
import moe.rukamori.archivetune.ai.KeuMusicAiBrain
import moe.rukamori.archivetune.ui.ai.engine.KeuOfflineSpeechEngine
import moe.rukamori.archivetune.ui.ai.engine.KeuOfflineTtsEngine

private data class KeuMusicAiLine(val author: String, val message: String)

@Composable
fun KeuMusicAiConversation(
    onSpeakingChanged: (Boolean) -> Unit,
    onStatusChanged: (String) -> Unit = {},
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPausePlayback: () -> Unit,
    onResumePlayback: () -> Unit,
    onVoiceSearch: (String) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    startWithWelcome: Boolean = false,
) {
    val context = LocalContext.current
    val conversationScope = rememberCoroutineScope()
    var isThinking by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var voiceReady by remember { mutableStateOf(false) }
    var systemTts by remember { mutableStateOf<TextToSpeech?>(null) }
    var systemTtsReady by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf("Preparando a voz masculina do Keu...") }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    // A escuta inicia automaticamente depois da permissão de microfone.
    var handsFreeEnabled by rememberSaveable { mutableStateOf(false) }
    var memoryEnabled by rememberSaveable {
        mutableStateOf(context.getSharedPreferences("keumusic_ai_private_memory", android.content.Context.MODE_PRIVATE).getBoolean("memory_enabled", false))
    }
    var profileName by rememberSaveable {
        mutableStateOf(KeuUserProfileMemory.loadName(context))
    }
    var waitingForCommand by rememberSaveable { mutableStateOf(false) }
    val voiceEngine = remember(context) { KeuOfflineSpeechEngine(context) }
    val ttsEngine = remember(context) { KeuOfflineTtsEngine(context) }

    // Android fallback is allowed only when the installed engine exposes an
    // explicitly male Brazilian Portuguese voice. Never silently switch to a
    // default voice whose gender/timbre is unknown.
    DisposableEffect(context) {
        val mainHandler = Handler(Looper.getMainLooper())
        val fallback = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                mainHandler.post {
                    val fallbackVoice = systemTts
                    if (fallbackVoice == null) {
                        systemTtsReady = false
                        voiceStatus = "A voz de segurança do Android ainda não ficou disponível."
                        return@post
                    }
                    val languageResult = fallbackVoice.setLanguage(Locale("pt", "BR"))
                    val ptBrVoices = fallbackVoice.voices.orEmpty().filter {
                        it.locale.language.equals("pt", ignoreCase = true) &&
                            it.locale.country.equals("BR", ignoreCase = true)
                    }
                    val maleVoice = ptBrVoices.firstOrNull { voice ->
                        val descriptor = (voice.name + " " + voice.features.orEmpty().joinToString(" "))
                            .lowercase(Locale.ROOT)
                        descriptor.contains("male") || descriptor.contains("mascul")
                    }
                    if (languageResult != TextToSpeech.LANG_MISSING_DATA &&
                        languageResult != TextToSpeech.LANG_NOT_SUPPORTED && maleVoice != null
                    ) {
                        fallbackVoice.voice = maleVoice
                        fallbackVoice.setPitch(0.92f)
                        fallbackVoice.setSpeechRate(0.96f)
                        systemTtsReady = true
                        if (!voiceReady) {
                            voiceStatus = "Voz masculina pt-BR do Android pronta • alternativa de segurança."
                        }
                    } else {
                        systemTtsReady = false
                        if (!voiceReady) {
                            voiceStatus = "Não encontrei uma voz masculina pt-BR instalada. O Keu não usará uma voz feminina genérica."
                        }
                    }
                }
            } else {
                systemTtsReady = false
                voiceStatus = "O mecanismo de voz do Android não iniciou."
            }
        }
        systemTts = fallback
        onDispose {
            fallback.stop()
            fallback.shutdown()
            systemTts = null
            systemTtsReady = false
        }
    }
    val conversation = remember {
        val restored = if (memoryEnabled) KeuConversationMemory.load(context) else emptyList()
        mutableStateListOf<KeuMusicAiLine>().apply {
            if (restored.isEmpty()) add(KeuMusicAiLine("KeuMusic", "Olá! Eu sou o KeuMusic AI. Quando você me chamar, vou responder por voz."))
            else restored.forEach { add(KeuMusicAiLine(it.author, it.message)) }
        }
    }

    fun addConversationLine(author: String, message: String) {
        conversation.add(KeuMusicAiLine(author, message))
        while (conversation.size > 12) conversation.removeAt(0)
        if (memoryEnabled) {
            KeuConversationMemory.append(context, KeuConversationMemory.Line(author, message))
        }
    }

    LaunchedEffect(memoryEnabled) {
        context.getSharedPreferences("keumusic_ai_private_memory", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("memory_enabled", memoryEnabled).apply()
        if (memoryEnabled) {
            KeuConversationMemory.save(context, conversation.map { KeuConversationMemory.Line(it.author, it.message) })
            voiceStatus = "Memória local ativada • histórico salvo somente neste aparelho."
        } else {
            KeuConversationMemory.clear(context)
            KeuLongTermMemory.clear(context)
            voiceStatus = "Memória local desativada • histórico e lembranças salvos apagados."
        }
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

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            handsFreeEnabled = true
            voiceStatus = "Microfone autorizado • Keu está iniciando a escuta automaticamente."
        } else {
            voiceStatus = "Solicitando acesso ao microfone para iniciar a conversa por voz."
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(ttsEngine) {
        voiceReady = false
        try {
            ttsEngine.initialize { voiceStatus = it }
            voiceReady = true
            voiceStatus = "Voz Piper Cadu pt-BR pronta • processamento offline"
        } catch (error: Exception) {
            voiceReady = false
            voiceStatus = "Não consegui preparar a voz masculina: ${error.message ?: "erro desconhecido"}"
        }
    }

    fun normalized(value: String): String =
        Normalizer.normalize(value.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

    fun playSystemAnswer(answer: String) {
        val engine = systemTts
        if (engine == null || !systemTtsReady) {
            voiceEngine.setOutputActive(false)
            onSpeakingChanged(false)
            voiceStatus = "Não há voz masculina pt-BR disponível. Instale uma voz masculina brasileira no mecanismo de voz do Android."
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                voiceEngine.setOutputActive(true)
                onSpeakingChanged(true)
            }
            override fun onDone(utteranceId: String?) {
                voiceEngine.setOutputActive(false)
                onSpeakingChanged(false)
            }
            override fun onError(utteranceId: String?) {
                voiceEngine.setOutputActive(false)
                onSpeakingChanged(false)
                voiceStatus = "O mecanismo de voz do Android não conseguiu falar."
            }
        })
        val result = engine.speak(
            answer,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "keu-${System.currentTimeMillis()}",
        )
        if (result == TextToSpeech.ERROR) {
            voiceEngine.setOutputActive(false)
            onSpeakingChanged(false)
            voiceStatus = "Falha ao iniciar a voz do Android."
        }
    }

    fun playAnswer(answer: String) {
        conversationScope.launch {
            if (!voiceReady) {
                playSystemAnswer(answer)
                return@launch
            }
            try {
                ttsEngine.speak(
                    text = answer,
                    onStart = {
                        voiceEngine.setOutputActive(true)
                        onSpeakingChanged(true)
                    },
                    onDone = {
                        voiceEngine.setOutputActive(false)
                        onSpeakingChanged(false)
                    },
                    onError = { message ->
                        voiceEngine.setOutputActive(false)
                        onSpeakingChanged(false)
                        voiceStatus = "A voz Piper falhou ($message). Verificando alternativa masculina pt-BR."
                        playSystemAnswer(answer)
                    },
                )
            } catch (error: Exception) {
                voiceStatus = "A voz Piper falhou. Verificando alternativa masculina pt-BR."
                playSystemAnswer(answer)
            }
        }
    }

    fun speak(answer: String) {
        addConversationLine("KeuMusic", answer)
        if (voiceReady || systemTtsReady) {
            playAnswer(answer)
        } else {
            pendingSpeech = answer
            voiceStatus = "Estou preparando minha voz. Já vou responder assim que estiver pronta."
        }
    }

    LaunchedEffect(startWithWelcome) {
        if (startWithWelcome) {
            speak(
                "Olá! Eu sou o KeuMusic, mas pode me chamar de Keu. Fui criado por Cleudeson Soares. " +
                    "Estou aqui para conversar com você, ajudar a encontrar músicas e controlar a reprodução " +
                    "dentro do KeuMusic. Depois desta primeira ativação, basta falar comigo naturalmente; você não precisa tocar em um botão nem repetir meu nome."
            )
        }
    }

    fun askKeuMusicAi(question: String) {
        if (isThinking) return
        isThinking = true
        voiceStatus = "Keu está pensando na resposta..."
        conversationScope.launch {
            try {
                val history = conversation.dropLast(1).takeLast(8).map { "${it.author}: ${it.message}" }
                val answer = KeuMusicAiBrain.reply(context, question, history)
                isThinking = false
                speak(answer)
            } catch (error: Exception) {
                isThinking = false
                val detail = error.message.orEmpty()
                voiceStatus = if (detail.contains("Configure um provedor", ignoreCase = true)) {
                    "Gemini selecionado, mas falta cadastrar uma chave de API válida em Integração de IA."
                } else {
                    "Falha ao acessar o provedor de IA: ${detail.take(140)}"
                }
                speak(
                    if (detail.contains("Configure um provedor", ignoreCase = true)) {
                        "O Gemini está selecionado, mas ainda falta configurar uma chave de API válida. A voz e os comandos locais podem funcionar sem essa chave; para respostas inteligentes, precisamos configurar o serviço."
                    } else {
                        "Não consegui acessar o serviço de inteligência agora. Verifique sua conexão e a configuração do provedor de IA."
                    },
                )
            }
        }
    }

    fun processCommand(rawCommand: String) {
        val command = normalized(rawCommand)
        waitingForCommand = false
        when {
            command.startsWith("lembre que ") || command.startsWith("guarde que ") || command.startsWith("anote que ") -> {
                if (!memoryEnabled) {
                    speak("A memória está desligada. Ative o botão Memória para eu guardar informações somente neste aparelho.")
                } else {
                    val fact = rawCommand.replace(Regex("(?i)^\\s*(lembre|guarde|anote)\\s+que\\s+"), "").trim()
                    if (KeuLongTermMemory.remember(context, fact)) {
                        speak("Pode deixar! Guardei essa informação na minha memória local.")
                    } else {
                        speak("Não consegui guardar essa informação. Pode dizer de outra forma?")
                    }
                }
            }
            command.startsWith("o que voce lembra") || command.startsWith("listar memorias") ||
                command.startsWith("o que voce sabe sobre mim") -> {
                val topic = command.substringAfter("sobre", "").trim()
                val facts = KeuLongTermMemory.find(context, topic)
                speak(
                    if (facts.isEmpty()) "Ainda não tenho lembranças guardadas sobre isso."
                    else "Estas são as informações que guardei: " + facts.joinToString(". ")
                )
            }
            command.startsWith("esqueca que ") -> {
                val topic = rawCommand.replace(Regex("(?i)^\\s*esqueça?\\s+que\\s+"), "").trim()
                val removed = KeuLongTermMemory.forget(context, topic)
                speak(if (removed > 0) "Pronto! Apaguei essa lembrança da minha memória local." else "Não encontrei essa lembrança para apagar.")
            }
            command.contains("apague minhas memorias") || command.contains("limpe minhas memorias") -> {
                KeuLongTermMemory.clear(context)
                speak("Pronto! Apaguei as lembranças de longo prazo que estavam salvas neste aparelho.")
            }
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
            listOf("embaralha", "embaralhar", "modo aleatorio", "musica aleatoria", "mistura as musicas", "ativar aleatorio").any(command::contains) -> {
                onToggleShuffle()
                speak("Certo! Estou alternando o modo aleatório.")
            }
            listOf("repetir musica", "repetir a faixa", "modo repeticao", "repeticao", "loop da musica", "ativar repeticao").any(command::contains) -> {
                onToggleRepeat()
                speak("Pronto! Estou alternando o modo de repetição.")
            }
            command.contains("roleta") || command.contains("roll") ->
                speak("A roleta musical está no KeuMusic. Você pode abrir a área da roleta e girar para sortear uma música.")
            command.contains("playlist") ->
                speak("As playlists ajudam você a organizar suas músicas favoritas. Abra a área de playlists para acessar suas listas.")
            command.contains("album") || command.contains("albuns") ->
                speak("Você pode explorar seus álbuns na área de álbuns do KeuMusic.")
            command.contains("tudo bem") || command == "oi" || command == "ola" -> {
                val name = KeuUserProfileMemory.loadName(context)
                val greeting = if (name.isNotBlank()) {
                    listOf("Tudo ótimo, $name! E com você?", "Opa, $name! Tudo tranquilo por aqui.", "Estou bem, $name, e pronto para escolher uma música com você.").random()
                } else {
                    listOf("Tudo ótimo! E com você?", "Opa! Tudo tranquilo por aqui. E aí?", "Estou bem e pronto para escolher uma música com você.").random()
                }
                speak(greeting)
            }
            else ->
                askKeuMusicAi(rawCommand)
        }
    }

    fun handleTranscript(transcript: String) {
        if (transcript.isBlank()) {

            return
        }
        addConversationLine("Você", transcript)
        val text = normalized(transcript)
        if (waitingForCommand) {
            processCommand(transcript)
            return
        }

        val wakePhrases = listOf(
            "keu music", "keumusic", "queu music", "q music", "keu musick", "keu musiq",
            "oi keumusic", "ola keumusic", "e ai keumusic", "bom dia keumusic",
            "boa tarde keumusic", "boa noite keumusic", "tudo bem keumusic", "como vai keumusic",
            "oi keu", "ola keu", "e ai keu", "bom dia keu", "boa tarde keu", "boa noite keu",
            "tudo bem keu", "como vai keu", "fala keu", "ei keu", "alo keu",
            "keu", "queu",
        )
        val wakePhrase = wakePhrases.firstOrNull(text::contains)
        // Hands-free mode is explicitly enabled by the user, so accept ordinary speech too.
        // The wake phrase remains optional; this avoids silently discarding natural questions.
        if (wakePhrase == null) {
            processCommand(transcript)
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
            val name = KeuUserProfileMemory.loadName(context)
            val greetings = if (name.isNotBlank()) {
                listOf(
                    "Oi, $name! Estou aqui. O que vamos ouvir?",
                    "E aí, $name! Tudo tranquilo? Pode falar.",
                    "Olá, $name! Estou ouvindo. O que você precisa?",
                    "Opa, $name! Manda ver, estou aqui.",
                )
            } else {
                listOf(
                    "Oi! Estou aqui. O que vamos ouvir?",
                    "E aí! Tudo tranquilo? Pode falar.",
                    "Olá! Estou ouvindo. O que você precisa?",
                    "Opa! Manda ver, estou aqui.",
                    "Bom te ouvir! Qual é o pedido de hoje?",
                )
            }
            speak(greetings.random())
        }
    }

    val latestTranscriptHandler by rememberUpdatedState<(String) -> Unit> { handleTranscript(it) }
    val latestStatusHandler by rememberUpdatedState<(String) -> Unit> { onStatusChanged(it) }
    LaunchedEffect(voiceStatus) { latestStatusHandler(voiceStatus) }

    LaunchedEffect(voiceReady, systemTtsReady, pendingSpeech) {
        val queued = pendingSpeech
        if ((voiceReady || systemTtsReady) && queued != null) {
            pendingSpeech = null
            playAnswer(queued)
        }
    }

    DisposableEffect(ttsEngine) {
        onDispose {
            ttsEngine.release()
            onSpeakingChanged(false)
        }
    }

    DisposableEffect(voiceEngine) {
        onDispose {
            voiceEngine.release()
            onSpeakingChanged(false)
        }
    }

    LaunchedEffect(handsFreeEnabled, permissionGranted, voiceEngine) {
        if (handsFreeEnabled && permissionGranted) {
            voiceEngine.start(
                status = { voiceStatus = it },
                transcript = { latestTranscriptHandler(it) },
            )
        } else {
            voiceEngine.stop()
        }
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
            value = profileName,
            onValueChange = { profileName = it.take(60) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Como posso chamar você?") },
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val safeName = profileName.trim()
                    if (safeName.isNotBlank()) {
                        KeuUserProfileMemory.saveName(context, safeName)
                        profileName = safeName
                        voiceStatus = "Nome salvo neste aparelho. Não é enviado ao provedor de IA."
                    } else {
                        voiceStatus = "Digite um nome antes de salvar."
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = profileName.isNotBlank(),
            ) {
                Text("Salvar meu nome")
            }
            Button(
                onClick = {
                    KeuUserProfileMemory.clear(context)
                    profileName = ""
                    voiceStatus = "Nome salvo apagado do perfil local."
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Apagar nome")
            }
        }
        Text(
            text = "O nome fica salvo somente neste aparelho e é usado nas saudações do Keu. Ele não é enviado ao serviço de IA.",
            color = Color(0xFF8297AA),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "Com a memória ligada, diga “lembre que...” para guardar uma informação, “o que você lembra?” para consultar ou “esqueça que...” para apagar. As lembranças ficam neste aparelho.",
            color = Color(0xFF8297AA),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { memoryEnabled = !memoryEnabled },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (memoryEnabled) "Memória: ligada" else "Memória: desligada")
            }
            Button(
                onClick = {
                    if (!permissionGranted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    else {
                        handsFreeEnabled = !handsFreeEnabled
                        if (handsFreeEnabled) voiceStatus = "Escuta automática ativada."
                        else {
                            waitingForCommand = false
                            voiceEngine.stop()
                            voiceStatus = "Escuta automática pausada."
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (handsFreeEnabled) "Parar escuta" else "Ativar escuta")
            }
        }
        Text(
            text = if (isThinking) "Keu está pensando..." else voiceStatus,
            color = Color(0xFF9AB4C8),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "Reconhecimento de voz offline em português, processado no aparelho. A conversa inteligente usa o provedor configurado em Integração de IA.",
            color = Color(0xFF8297AA),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}
