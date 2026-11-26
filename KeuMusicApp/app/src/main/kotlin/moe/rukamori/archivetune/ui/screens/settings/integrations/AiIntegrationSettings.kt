/*
 * KeuMusic (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.KeuMusicAiEnabledKey
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.screens.settings.components.YumaSettingsScaffold
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.rememberPreference

@Composable
fun AiIntegrationSettings(navController: NavController) {
    val (assistantEnabled, setAssistantEnabled) = rememberPreference(KeuMusicAiEnabledKey, false)
    var showVoiceAssistantDialog by rememberSaveable { mutableStateOf(false) }

    if (assistantEnabled && showVoiceAssistantDialog) {
        KeuMusicVoiceAssistantDialog(onDismiss = { showVoiceAssistantDialog = false })
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
    }
}
