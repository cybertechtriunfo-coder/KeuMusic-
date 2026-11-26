/*
 * KeuMusic (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.KeuMusicAiEnabledKey
import moe.rukamori.archivetune.constants.KeuAiChatGptApiKeyKey
import moe.rukamori.archivetune.constants.KeuAiGeminiApiKeyKey
import moe.rukamori.archivetune.constants.KeuAiClaudeApiKeyKey
import moe.rukamori.archivetune.constants.KeuAiOpenRouterApiKeyKey
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.screens.settings.components.YumaSettingsScaffold
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.rememberPreference

@Composable
fun AiIntegrationSettings(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (assistantEnabled, setAssistantEnabled) = rememberPreference(KeuMusicAiEnabledKey, false)
    val preferences by context.dataStore.data.collectAsState(initial = emptyPreferences())
    var showVoiceAssistantDialog by rememberSaveable { mutableStateOf(false) }
    var editingProvider by rememberSaveable { mutableStateOf("") }
    var apiKeyInput by rememberSaveable { mutableStateOf("") }

    fun keyFor(provider: String): Preferences.Key<String> = when (provider) {
        "CHATGPT" -> KeuAiChatGptApiKeyKey
        "GEMINI" -> KeuAiGeminiApiKeyKey
        "CLAUDE" -> KeuAiClaudeApiKeyKey
        else -> KeuAiOpenRouterApiKeyKey
    }

    fun openKeyEditor(provider: String) {
        editingProvider = provider
        apiKeyInput = preferences[keyFor(provider)].orEmpty()
    }

    if (assistantEnabled && showVoiceAssistantDialog) {
        KeuMusicVoiceAssistantDialog(onDismiss = { showVoiceAssistantDialog = false })
    }

    if (editingProvider.isNotBlank()) {
        val providerLabel = when (editingProvider) {
            "CHATGPT" -> "OpenAI"
            "GEMINI" -> "Google Gemini"
            "CLAUDE" -> "Anthropic Claude"
            else -> "OpenRouter"
        }
        AlertDialog(
            onDismissRequest = { editingProvider = "" },
            title = { Text("Conectar $providerLabel") },
            text = {
                Column {
                    Text("Cadastre sua chave de API. O Keu tentará este provedor automaticamente quando os anteriores atingirem limites ou ficarem indisponíveis.")
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Chave de API") },
                        singleLine = true,
                    )
                    Text("A chave é fornecida pelo próprio serviço. Assinaturas de ChatGPT ou Gemini não incluem necessariamente créditos de API.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val key = keyFor(editingProvider)
                        val value = apiKeyInput.trim()
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                if (value.isBlank()) prefs.remove(key) else prefs[key] = value
                            }
                            editingProvider = ""
                        }
                    },
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { editingProvider = "" }) { Text("Cancelar") }
            },
        )
    }

    YumaSettingsScaffold(
        title = { Text("KeuMusic IA") },
        onBackClick = navController::navigateUp,
        onBackLongClick = navController::backToMain,
    ) {
        PreferenceGroup(title = "Assistente virtual") {
            item {
                PreferenceEntry(
                    title = { Text("Ativar integração KeuMusic IA") },
                    description = if (assistantEnabled) {
                        "Keu está ativado. Toque para abrir a conversa por voz."
                    } else {
                        "Ative para habilitar o assistente KeuMusic. A configuração técnica fica automática."
                    },
                    icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
                    trailingContent = {
                        Switch(
                            checked = assistantEnabled,
                            onCheckedChange = { enabled ->
                                setAssistantEnabled(enabled)
                                showVoiceAssistantDialog = enabled
                            },
                        )
                    },
                    onClick = {
                        val enabled = !assistantEnabled
                        setAssistantEnabled(enabled)
                        showVoiceAssistantDialog = enabled
                    },
                )
            }
        }

        PreferenceGroup(title = "Provedores automáticos de IA") {
            item {
                PreferenceEntry(
                    title = { Text("OpenAI") },
                    description = if (preferences[KeuAiChatGptApiKeyKey].isNullOrBlank()) "Adicionar chave para usar como provedor de reserva" else "Chave cadastrada • troca automática ativada",
                    onClick = { openKeyEditor("CHATGPT") },
                )
            }
            item {
                PreferenceEntry(
                    title = { Text("Google Gemini") },
                    description = if (preferences[KeuAiGeminiApiKeyKey].isNullOrBlank()) "Adicionar chave para usar como provedor de reserva" else "Chave cadastrada • troca automática ativada",
                    onClick = { openKeyEditor("GEMINI") },
                )
            }
            item {
                PreferenceEntry(
                    title = { Text("Anthropic Claude") },
                    description = if (preferences[KeuAiClaudeApiKeyKey].isNullOrBlank()) "Adicionar chave para usar como provedor de reserva" else "Chave cadastrada • troca automática ativada",
                    onClick = { openKeyEditor("CLAUDE") },
                )
            }
            item {
                PreferenceEntry(
                    title = { Text("OpenRouter") },
                    description = if (preferences[KeuAiOpenRouterApiKeyKey].isNullOrBlank()) "Adicionar chave para acessar modelos de vários provedores" else "Chave cadastrada • troca automática ativada",
                    onClick = { openKeyEditor("OPENROUTER") },
                )
            }
        }
    }
}
