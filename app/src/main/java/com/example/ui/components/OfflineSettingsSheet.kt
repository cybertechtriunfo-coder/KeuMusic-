package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cache.OfflineCacheStats
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSettingsSheet(
    isBackgroundCachingEnabled: Boolean,
    cacheStats: OfflineCacheStats,
    isDarkTheme: Boolean,
    onToggleBackgroundCaching: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onClearCache: () -> Unit,
    onToggleTheme: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.testTag("offline_settings_sheet")
    ) {
        OfflineSettingsContent(
            isBackgroundCachingEnabled = isBackgroundCachingEnabled,
            cacheStats = cacheStats,
            isDarkTheme = isDarkTheme,
            onToggleBackgroundCaching = onToggleBackgroundCaching,
            onSyncNow = onSyncNow,
            onClearCache = onClearCache,
            onToggleTheme = onToggleTheme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        )
    }
}

@Composable
fun OfflineSettingsContent(
    isBackgroundCachingEnabled: Boolean,
    cacheStats: OfflineCacheStats,
    isDarkTheme: Boolean,
    onToggleBackgroundCaching: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onClearCache: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Armazenamento local offline e aparência do KEUMUSIC",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Background Offline Caching Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OfflinePin,
                        contentDescription = null,
                        tint = if (isBackgroundCachingEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cache em segundo plano",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Salva até ${cacheStats.maxTracks} músicas (${formatStorageBytes(cacheStats.maxBytes)}) no armazenamento local do app para reprodução offline limitada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isBackgroundCachingEnabled,
                        onCheckedChange = onToggleBackgroundCaching,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("background_cache_switch")
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${cacheStats.cachedCount} de ${cacheStats.maxTracks} músicas salvas • ${formatStorageBytes(cacheStats.totalBytes)} / ${formatStorageBytes(cacheStats.maxBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("offline_cache_usage_text")
                    )
                }

                val progress = if (cacheStats.maxBytes > 0L) {
                    (cacheStats.totalBytes.toFloat() / cacheStats.maxBytes.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                if (cacheStats.isCachingInBackground) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Salvando em segundo plano: ${cacheStats.activeTrackTitle ?: "música"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSyncNow,
                        enabled = isBackgroundCachingEnabled && !cacheStats.isCachingInBackground,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sync_offline_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cached,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cachear agora")
                    }

                    TextButton(
                        onClick = onClearCache,
                        enabled = cacheStats.cachedCount > 0 || cacheStats.totalBytes > 0L,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_offline_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = if (cacheStats.cachedCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Limpar",
                            color = if (cacheStats.cachedCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Theme Preference Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tema escuro",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isDarkTheme) "Modo escuro ativado" else "Modo claro ativado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleTheme() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_theme_switch")
                )
            }
        }

        Text(
            text = "KEUMUSIC • Desenvolvido por Cleudeson Soares",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}
