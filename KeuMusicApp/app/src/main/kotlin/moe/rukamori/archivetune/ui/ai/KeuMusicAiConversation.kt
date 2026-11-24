package moe.rukamori.archivetune.ui.ai

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.Normalizer
import java.util.Locale

private data class KeuMusicAiLine(val author: String, val message: String)

@Composable
fun KeuMusicAiConversation(
    onSpeakingChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var voiceReady by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf("Preparando a voz…") }
    val conversation = remember {
        mutableStateListOf(
            KeuMusicAiLine("KeuMusic", "Olá! Agora você já pode digitar ou tocar no microfone para conversar comigo."),
        )
    }

    val tts = remember(context) {
        TextToSpeech(context) { result ->
            voiceReady = result == TextToSpeech.SUCCESS
            voiceStatus = if (result == TextToSpeech.SUCCESS) {
                "Voz do Android • português brasileiro"
            } else {
                "Mecanismo de voz indisponível neste aparelho"
            }
        }
    }

    LaunchedEffect(voiceReady) {
        if (voiceReady) {
            val brazilianPortuguese = Locale("pt", "BR")
            val languageResult = tts.setLanguage(brazilianPortuguese)
            tts.setSpeechRate(0.96f)
            tts.setPitch(1.04f)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                voiceStatus = "Instale uma voz em português brasileiro nas configurações do Android"
                voiceReady = false
            }
        }
    }

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingChanged(false)
            }

            @Deprecated("Deprecated in Android API")
            override fun onError(utteranceId: String?) {
                onSpeakingChanged(false)
            }
        })
        onDispose {
            onSpeakingChanged(false)
            tts.stop()
            tts.shutdown()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val recognized = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else {
            null
        }
        if (!recognized.isNullOrBlank()) {
            input = recognized
            voiceStatus = "Voz reconhecida • confira e toque em Enviar"
        } else {
            voiceStatus = "Não consegui entender. Tente falar novamente."
        }
    }

    fun answerFor(question: String): String {
        val normalized = Normalizer.normalize(question.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return when {
            listOf("oi", "ola", "bom dia", "boa tarde", "boa noite", "tudo bem").any(normalized::contains) ->
                "Olá! Tudo ótimo por aqui. Sou o KeuMusic AI e estou aprendendo a conversar com você por voz."
            normalized.contains("quem criou") || normalized.contains("seu criador") ->
                "Fui criado por Cleudeson Soares, idealizador do KeuMusic e criador do KeuMusic AI."
            normalized.contains("roleta") || normalized.contains("roll") ->
                "A roleta musical escolhe uma música para você. Abra a área da roleta e toque em Girar roleta. Depois você pode escolher ouvir a música sorteada."
            normalized.contains("playlist") ->
                "As playlists ajudam a organizar suas músicas favoritas. Abra a área de playlists para criar ou acessar suas listas."
            normalized.contains("album") || normalized.contains("albuns") ->
                "Na área de álbuns você pode explorar coleções de músicas e abrir um álbum para ver suas faixas."
            normalized.contains("telegram") || normalized.contains("suporte") || normalized.contains("contato") ->
                "Você pode acompanhar o KeuMusic pelo Instagram qkeumusic e pelo Telegram oficial t.me/KeuMusic."
            normalized.contains("musica") || normalized.contains("tocar") || normalized.contains("artista") ->
                "Essa é uma das funções que vamos conectar: pesquisar músicas e controlar o player por voz. Nesta versão experimental, ainda não estou autorizado a controlar a reprodução."
            else ->
                "Entendi o que você disse. Minha conversa inteligente completa ainda será conectada ao serviço de IA do KeuMusic. Por enquanto, consigo responder algumas dúvidas básicas do aplicativo e falar com você."
        }
    }

    fun sendMessage() {
        val question = input.trim()
        if (question.isEmpty()) return
        conversation.add(KeuMusicAiLine("Você", question))
        val answer = answerFor(question)
        conversation.add(KeuMusicAiLine("KeuMusic", answer))
        while (conversation.size > 8) conversation.removeAt(0)
        input = ""
        if (voiceReady) {
            tts.speak(
                answer,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "keumusic-ai-${System.currentTimeMillis()}",
            )
        } else {
            voiceStatus = "Resposta exibida em texto; a voz ainda não está pronta."
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
                .heightIn(max = 112.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            conversation.takeLast(3).forEach { line ->
                Text(
                    text = "${line.author}: ${line.message}",
                    color = if (line.author == "KeuMusic") Color(0xFFBDEFFF) else Color.White,
                    maxLines = 2,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Digite ou fale com o KeuMusic AI") },
            maxLines = 2,
            singleLine = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale com o KeuMusic AI")
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    try {
                        speechLauncher.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        voiceStatus = "Este aparelho não possui serviço de reconhecimento de voz disponível."
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Microfone")
            }
            Button(
                onClick = { sendMessage() },
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
            text = "Protótipo local: respostas de demonstração. A IA online e o controle do player ainda serão integrados.",
            color = Color(0xFF8297AA),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}
