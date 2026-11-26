/*
 * KeuMusic AI | Central conversational brain with provider failover
 * Conversation history, identity and speech remain owned by KeuMusic.
 */
package moe.rukamori.archivetune.ai

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.constants.AiApiKeyKey
import moe.rukamori.archivetune.constants.AiCustomEndpointKey
import moe.rukamori.archivetune.constants.AiCustomModelKey
import moe.rukamori.archivetune.constants.AiProvider
import moe.rukamori.archivetune.constants.AiProviderKey
import moe.rukamori.archivetune.constants.AiSelectedModelKey
import moe.rukamori.archivetune.constants.KeuAiChatGptApiKeyKey
import moe.rukamori.archivetune.constants.KeuAiGeminiApiKeyKey
import moe.rukamori.archivetune.constants.KeuAiClaudeApiKeyKey
import moe.rukamori.archivetune.constants.KeuAiOpenRouterApiKeyKey
import moe.rukamori.archivetune.extensions.toEnum
import moe.rukamori.archivetune.utils.dataStore

private data class KeuAiProviderCandidate(
    val provider: AiProvider,
    val apiKey: String,
    val model: String = "",
    val customEndpoint: String = "",
)

object KeuMusicAiBrain {
    private const val FailoverPrefs = "keumusic_ai_provider_failover"

    suspend fun reply(
        context: Context,
        question: String,
        recentHistory: List<String> = emptyList(),
    ): String = withContext(Dispatchers.IO) {
        val preferences = context.dataStore.data.first()
        val selectedProvider = preferences[AiProviderKey].toEnum(AiProvider.NONE)
        val selectedModel = if (selectedProvider == AiProvider.CUSTOM) {
            preferences[AiCustomModelKey].orEmpty()
        } else {
            preferences[AiSelectedModelKey].orEmpty()
        }
        val candidates = buildList {
            val primaryKey = preferences[AiApiKeyKey].orEmpty()
            val primaryEndpoint = preferences[AiCustomEndpointKey].orEmpty()
            if (selectedProvider != AiProvider.NONE && primaryKey.isNotBlank() &&
                (selectedProvider != AiProvider.CUSTOM || primaryEndpoint.isNotBlank())
            ) {
                add(KeuAiProviderCandidate(selectedProvider, primaryKey, selectedModel, primaryEndpoint))
            }
            fun addBackup(provider: AiProvider, key: String?) {
                val value = key.orEmpty().trim()
                if (value.isNotBlank() && none { it.provider == provider }) {
                    add(KeuAiProviderCandidate(provider, value))
                }
            }
            addBackup(AiProvider.CHATGPT, preferences[KeuAiChatGptApiKeyKey])
            addBackup(AiProvider.GEMINI, preferences[KeuAiGeminiApiKeyKey])
            addBackup(AiProvider.CLAUDE, preferences[KeuAiClaudeApiKeyKey])
            addBackup(AiProvider.OPENROUTER, preferences[KeuAiOpenRouterApiKeyKey])
        }

        if (candidates.isEmpty()) return@withContext offlineReply(question)

        val history = recentHistory.takeLast(10).joinToString("\n")
        val systemPrompt = """
            Você é KeuMusic, também chamado de Keu, o assistente virtual masculino do aplicativo KeuMusic.
            Você foi criado por Cleudeson Soares, idealizador do KeuMusic.
            Converse naturalmente em português brasileiro, como um assistente prestativo, simpático,
            seguro e objetivo. Responda perguntas gerais, explique assuntos e mantenha o contexto
            recente da conversa. Não diga que é humano. Não invente ações executadas no aplicativo.
            Quando o usuário pedir controle musical, explique ou solicite o comando específico caso
            não tenha sido executado pelo player. Responda em texto simples, sem markdown, emojis
            excessivos ou cabeçalhos, pois sua resposta será falada em voz alta.
        """.trimIndent()
        val userPrompt = buildString {
            if (history.isNotBlank()) append("Histórico recente da mesma conversa:\n").append(history).append("\n")
            append("Usuário: ").append(question)
        }

        val failures = mutableListOf<String>()
        var lastError: Exception? = null
        for (candidate in candidates) {
            val cooldownKey = "until_${candidate.provider.name}"
            val cooldownUntil = context.getSharedPreferences(FailoverPrefs, Context.MODE_PRIVATE)
                .getLong(cooldownKey, 0L)
            if (cooldownUntil > System.currentTimeMillis()) {
                failures += "${candidate.provider.name}: em pausa temporária"
                continue
            }

            try {
                val answer = AiTextService.complete(
                    config = AiServiceConfig(
                        provider = candidate.provider,
                        apiKey = candidate.apiKey,
                        customEndpoint = candidate.customEndpoint,
                        model = candidate.model,
                    ),
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = 0.55,
                    maxTokens = 900,
                ).trim()
                if (answer.isBlank()) throw AiServiceException("A inteligência retornou uma resposta vazia.")
                context.getSharedPreferences(FailoverPrefs, Context.MODE_PRIVATE)
                    .edit().remove(cooldownKey).apply()
                return@withContext answer
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                lastError = error
                failures += "${candidate.provider.name}: ${error.message.orEmpty().take(100)}"
                val status = (error as? AiServiceException)?.statusCode
                val pauseMillis = when {
                    status == 401 || status == 403 -> 5 * 60 * 1000L
                    status == 429 -> 2 * 60 * 1000L
                    status != null && status >= 500 -> 30 * 1000L
                    else -> 45 * 1000L
                }
                context.getSharedPreferences(FailoverPrefs, Context.MODE_PRIVATE)
                    .edit().putLong(cooldownKey, System.currentTimeMillis() + pauseMillis).apply()
            }
        }

        val summary = failures.take(4).joinToString("; ")
        throw AiServiceException(
            "Não consegui acessar os provedores configurados. O Keu preservou a conversa; confira as chaves e os limites de API. $summary",
            lastError,
        )
    }

    private fun offlineReply(question: String): String {
        val normalized = question.lowercase()
        return when {
            normalized in listOf("oi", "olá", "ola", "e aí", "eai", "bom dia", "boa tarde", "boa noite") ||
                normalized.startsWith("tudo bem") ->
                "Olá! Estou por aqui e pronto para ajudar. Pode falar comigo sobre suas músicas ou pedir um comando de reprodução."
            normalized.contains("quem criou") || normalized.contains("seu criador") ->
                "Fui criado por Cleudeson Soares, idealizador do KeuMusic."
            normalized.contains("o que você faz") || normalized.contains("o que voce faz") ||
                normalized.contains("o que pode fazer") || normalized.contains("ajuda") ->
                "Posso ajudar a controlar sua música: tocar uma faixa ou artista, avançar, voltar, pausar, continuar, embaralhar e repetir. Também posso conversar quando a conexão inteligente estiver disponível."
            normalized.contains("seu nome") || normalized.contains("quem é você") || normalized.contains("quem e voce") ->
                "Sou o KeuMusic, mas pode me chamar de Keu. Fui criado por Cleudeson Soares."
            else ->
                "Estou ativo no modo local. Os comandos de música continuam disponíveis, mas as respostas para perguntas gerais dependem da conexão inteligente central do KeuMusic."
        }
    }
}
