/*
 * KeuMusic AI | Conversational brain adapter
 * Uses the AI provider already configured in KeuMusic settings.
 */
package moe.rukamori.archivetune.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.constants.AiApiKeyKey
import moe.rukamori.archivetune.constants.AiCustomEndpointKey
import moe.rukamori.archivetune.constants.AiCustomModelKey
import moe.rukamori.archivetune.constants.AiProvider
import moe.rukamori.archivetune.constants.AiProviderKey
import moe.rukamori.archivetune.constants.AiSelectedModelKey
import moe.rukamori.archivetune.extensions.toEnum
import moe.rukamori.archivetune.utils.dataStore

object KeuMusicAiBrain {
    suspend fun reply(
        context: Context,
        question: String,
        recentHistory: List<String> = emptyList(),
    ): String = withContext(Dispatchers.IO) {
        val preferences = context.dataStore.data.first()
        val provider = preferences[AiProviderKey].toEnum(AiProvider.NONE)
        val model = if (provider == AiProvider.CUSTOM) {
            preferences[AiCustomModelKey].orEmpty()
        } else {
            preferences[AiSelectedModelKey].orEmpty()
        }
        val config = AiServiceConfig(
            provider = provider,
            apiKey = preferences[AiApiKeyKey].orEmpty(),
            customEndpoint = preferences[AiCustomEndpointKey].orEmpty(),
            model = model,
        )
        if (!config.canCallApi) {
            throw AiServiceException("Configure um provedor de IA em Integração de IA.")
        }

        val history = recentHistory.takeLast(8).joinToString("\\n")
        val prompt = buildString {
            append("Conversa recente:\\n")
            if (history.isNotBlank()) append(history).append("\\n")
            append("Usuário: ").append(question)
        }
        AiTextService.complete(
            config = config,
            systemPrompt = """
                Você é KeuMusic, também chamado de Keu, o assistente virtual masculino do aplicativo KeuMusic.
                Você foi criado por Cleudeson Soares, idealizador do KeuMusic e criador do KeuMusic AI.
                Converse naturalmente em português brasileiro, como um assistente prestativo, simpático,
                seguro e objetivo. Responda perguntas gerais, explique assuntos e mantenha o contexto
                recente da conversa. Não diga que é humano. Não invente ações executadas no aplicativo.
                Quando o usuário pedir controle musical, explique ou solicite o comando específico caso
                não tenha sido executado pelo player. Responda em texto simples, sem markdown, emojis
                excessivos ou cabeçalhos, pois sua resposta será falada em voz alta.
            """.trimIndent(),
            userPrompt = prompt,
            temperature = 0.55,
            maxTokens = 700,
        ).trim().ifBlank { throw AiServiceException("A inteligência retornou uma resposta vazia.") }
    }
}
