/*
 * KeuMusic | Rádio KeuMusic Top
 * Radio Browser public directory integration.
 */
package moe.rukamori.archivetune.ui.screens.radio

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.ui.main.LocalPlayerConnection
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class RadioStation(
    val id: String,
    val name: String,
    val stream: String,
    val logo: String?,
    val state: String,
    val genre: String,
    val clicks: Long,
)

private val brazilStates = listOf(
    "Todos os estados", "Acre", "Alagoas", "Amapá", "Amazonas", "Bahia", "Ceará",
    "Distrito Federal", "Espírito Santo", "Goiás", "Maranhão", "Mato Grosso",
    "Mato Grosso do Sul", "Minas Gerais", "Pará", "Paraíba", "Paraná",
    "Pernambuco", "Piauí", "Rio de Janeiro", "Rio Grande do Norte",
    "Rio Grande do Sul", "Rondônia", "Roraima", "Santa Catarina", "São Paulo",
    "Sergipe", "Tocantins"
)

@Composable
fun RadioTopShortcut(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(112.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF063D4B), Color(0xFF087E91), Color(0xFF172A55)))
            ).padding(18.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("RÁDIO KEUMUSIC TOP", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Emissoras do Brasil • Ao vivo", color = Color.White.copy(alpha = .88f), style = MaterialTheme.typography.bodyMedium)
            }
            Text("◉", Modifier.align(Alignment.CenterEnd), color = Color.White, style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
fun RadioTopScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedState by remember { mutableStateOf("Todos os estados") }
    var stateMenu by remember { mutableStateOf(false) }
    var popularOnly by remember { mutableStateOf(true) }

    suspend fun loadStations() {
        loading = true
        error = null
        try {
            stations = withContext(Dispatchers.IO) {
                val url = URL("https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/BR?limit=500&order=clickcount&reverse=true&hidebroken=true")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "KeuMusic/1.0")
                }
                try {
                    val json = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                    buildList {
                        for (i in 0 until json.length()) {
                            val item = json.optJSONObject(i) ?: continue
                            val stream = item.optString("url_resolved").ifBlank { item.optString("url") }
                            val name = item.optString("name").trim()
                            if (name.isBlank() || !(stream.startsWith("http://") || stream.startsWith("https://"))) continue
                            add(RadioStation(
                                id = item.optString("stationuuid").ifBlank { stream },
                                name = name,
                                stream = stream,
                                logo = item.optString("favicon").takeIf { it.startsWith("http") },
                                state = item.optString("state").trim(),
                                genre = item.optString("tags").split(",").firstOrNull()?.trim().orEmpty(),
                                clicks = item.optLong("clickcount"),
                            ))
                        }
                    }.distinctBy { it.id }
                } finally { connection.disconnect() }
            }
        } catch (t: Throwable) {
            error = t.message ?: "Não foi possível carregar as rádios agora."
        } finally { loading = false }
    }

    LaunchedEffect(Unit) { loadStations() }

    val filtered = remember(stations, selectedState, popularOnly) {
        stations.filter { selectedState == "Todos os estados" || it.state.contains(selectedState, true) }
            .let { list -> if (popularOnly) list.sortedByDescending { it.clicks } else list.sortedBy { it.name.lowercase() } }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            OutlinedButton(onClick = { navController.navigateUp() }) { Text("Voltar") }
            Text("Rádio KeuMusic Top", Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(onClick = { stateMenu = true }) { Text(selectedState) }
                DropdownMenu(expanded = stateMenu, onDismissRequest = { stateMenu = false }) {
                    brazilStates.forEach { state ->
                        DropdownMenuItem(text = { Text(state) }, onClick = { selectedState = state; stateMenu = false })
                    }
                }
            }
            Button(onClick = { popularOnly = true }) { Text("Mais ouvidas") }
            OutlinedButton(onClick = { popularOnly = false }) { Text("Todas") }
        }
        Spacer(Modifier.height(8.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = { scope.launch { loadStations() } }) { Text("Tentar novamente") }
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Nenhuma emissora encontrada para este estado.")
            }
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { station ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val mediaItems = filtered.map { radio ->
                                MediaItem.Builder()
                                    .setMediaId("keumusic-radio:${radio.id}")
                                    .setUri(radio.stream)
                                    .setMediaMetadata(MediaMetadata.Builder()
                                        .setTitle(radio.name)
                                        .setArtist(listOf(radio.state, radio.genre).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Rádio ao vivo" })
                                        .setArtworkUri(radio.logo?.let(Uri::parse))
                                        .build())
                                    .build()
                            }
                            playerConnection?.player?.setMediaItems(mediaItems, filtered.indexOf(station), 0L)
                            playerConnection?.player?.prepare()
                            playerConnection?.player?.play()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("◉", Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(station.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOf(station.state, station.genre).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Brasil • Ao vivo" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Text("AO VIVO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
