/*
 * KeuMusic | Rádio KeuMusic Top
 * Radio Browser public directory integration.
 */
package moe.rukamori.archivetune.ui.screens.radio

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.main.LocalPlayerConnection
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer

private data class RadioStation(
    val id: String,
    val name: String,
    val stream: String,
    val logo: String?,
    val state: String,
    val city: String?,
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

private val goiasCities = listOf(
    "Todas as cidades", "Goiânia", "Aparecida de Goiânia", "Trindade", "Anápolis"
)

private fun normalizedRadioName(value: String): String =
    Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

private fun radioCity(name: String): String? {
    val value = normalizedRadioName(name)
    return when {
        listOf("trindade fm", "boas novas trindade", "trindade mania", "pop brasil fm", "mova fm trindade", "paulista fm", "de olho no social", "mais oeste", "metamorfose fm", "89 fm a radio rock", "89 fm radio rock").any { value.contains(it) } -> "Trindade"
        listOf("nova bethel", "planalto fm", "primavera fm", "astral mix", "celestial gospel", "cristiano neves", "rap 62", "jesus e o caminho", "nova radio progresso", "cidade polo", "garavelo", "alianca divina", "canal felicidade", "radio aparecida", "roveg").any { value.contains(it) } -> "Aparecida de Goiânia"
        listOf("105 7 fm", "105.7 fm", "96 fm", "amor fm", "gazeta anapolis", "imprensa anapolis", "radio imprensa", "mais 87 9", "manchester fm", "nova fm anapolis", "rede aleluia anapolis", "sao francisco", "voz do coracao imaculado", "cerrado mix", "kairos de anapolis", "jovem news").any { value.contains(it) } -> "Anápolis"
        listOf("interativa", "terra fm", "alpha fm", "positiva", "bandnews", "vinha fm", "jovem pan", "cbn", "bandeirantes", "sucesso fm", "executiva", "paz fm", "99 5 fm", "brahma fm", "bons ventos", "fonte fm", "difusora goiania", "rbc fm", "sara brasil", "metropolitana fm", "moov fm", "mix fm goiania", "serra dourada").any { value.contains(it) } -> "Goiânia"
        else -> null
    }
}

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
            Column(Modifier.align(Alignment.CenterStart).padding(end = 88.dp)) {
                Text("RÁDIO KEUMUSIC TOP", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Emissoras do Brasil • Ao vivo", color = Color.White.copy(alpha = .88f), style = MaterialTheme.typography.bodyMedium)
            }
            Image(
                painter = painterResource(R.drawable.radio_keumusic_top_logo_image),
                contentDescription = "Rádio KeuMusic Top",
                modifier = Modifier.align(Alignment.CenterEnd).size(84.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun KeuTubShortcut(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(112.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF3B145D), Color(0xFFB51F54), Color(0xFF25164F)))
            ).padding(18.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("KeuTube", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Vídeos do YouTube • Pesquise e assista", color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodyMedium)
            }
            Image(
                painter = painterResource(R.drawable.keutube_logo_image),
                contentDescription = "KeuTube",
                modifier = Modifier.align(Alignment.CenterEnd).size(84.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun RadioKeuMusicTopMark(modifier: Modifier = Modifier) {
    Image(painter = painterResource(R.drawable.radio_keumusic_top_logo_image), contentDescription = "Rádio KeuMusic Top", modifier = modifier, contentScale = ContentScale.Fit)
}

@Composable
fun RadioTopScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current
    val player = playerConnection?.player
    val scope = rememberCoroutineScope()
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedState by remember { mutableStateOf("Todos os estados") }
    var selectedCity by remember { mutableStateOf("Todas as cidades") }
    var cityMenu by remember { mutableStateOf(false) }
    var stateMenu by remember { mutableStateOf(false) }
    var popularOnly by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStation by remember { mutableStateOf<RadioStation?>(null) }
    var isPlaying by remember { mutableStateOf(player?.isPlaying == true) }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId?.removePrefix("keumusic-radio:")
                selectedStation = stations.firstOrNull { it.id == id } ?: selectedStation
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    suspend fun loadStations() {
        loading = true
        error = null
        try {
            stations = withContext(Dispatchers.IO) {
                val url = URL("https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/BR?limit=5000&order=clickcount&reverse=true&hidebroken=true")
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
                                city = radioCity(name),
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

    val filtered = remember(stations, selectedState, selectedCity, popularOnly, searchQuery) {
        stations.filter { station ->
            (selectedState == "Todos os estados" || normalizedRadioName(station.state).contains(normalizedRadioName(selectedState)) || (selectedState == "Goiás" && station.city != null)) &&
                (selectedCity == "Todas as cidades" || station.city == selectedCity) &&
                (searchQuery.isBlank() || station.name.contains(searchQuery.trim(), true))
        }.let { list ->
            if (popularOnly) list.sortedByDescending { it.clicks } else list.sortedBy { it.name.lowercase() }
        }
    }

    fun playStation(station: RadioStation) {
        val queue = filtered.map { radio ->
            MediaItem.Builder()
                .setMediaId("keumusic-radio:${radio.id}")
                .setUri(radio.stream)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(radio.name)
                    .setArtist(listOf(radio.city ?: radio.state, radio.genre).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Rádio ao vivo" })
                    .setArtworkUri(radio.logo?.let(Uri::parse))
                    .build())
                .build()
        }
        val index = filtered.indexOfFirst { it.id == station.id }.coerceAtLeast(0)
        player?.setMediaItems(queue, index, 0L)
        player?.prepare()
        player?.play()
        selectedStation = station
        isPlaying = true
    }

    if (selectedStation != null) {
        val station = selectedStation!!
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { selectedStation = null }) { Text("Voltar às rádios") }
                Spacer(Modifier.weight(1f))
                Text("RÁDIO KEUMUSIC TOP", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            if (!station.logo.isNullOrBlank()) {
                AsyncImage(
                    model = station.logo,
                    contentDescription = "Capa de ${station.name}",
                    modifier = Modifier.size(250.dp).clip(RoundedCornerShape(30.dp)),
                )
            } else {
                Box(
                    Modifier.size(250.dp).clip(RoundedCornerShape(30.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF075568), Color(0xFF172A55)))),
                    contentAlignment = Alignment.Center,
                ) { Text("◉", color = Color.White, fontSize = 104.sp) }
            }
            Spacer(Modifier.height(28.dp))
            Text(station.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOf(station.city ?: station.state, station.genre).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Rádio brasileira" },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text("●  AO VIVO", Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { player?.seekToPreviousMediaItem() }, enabled = (player?.mediaItemCount ?: 0) > 1) { Text("|◀") }
                Spacer(Modifier.size(18.dp))
                Button(onClick = {
                    if (player?.isPlaying == true) player.pause() else player?.play()
                    isPlaying = player?.isPlaying == true
                }) { Text(if (isPlaying) "Pausar" else "Reproduzir") }
                Spacer(Modifier.size(18.dp))
                OutlinedButton(onClick = { player?.seekToNextMediaItem() }, enabled = (player?.mediaItemCount ?: 0) > 1) { Text("▶|") }
            }
            Spacer(Modifier.weight(1f))
        }
    } else {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
            ) {
                OutlinedButton(onClick = { navController.navigateUp() }) { Text("Voltar") }
                Text("Rádio KeuMusic Top", Modifier.weight(1f).padding(start = 12.dp),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Text("⌕", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary) },
                placeholder = { Text("Pesquisar rádio pelo nome") },
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { stateMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedState, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = stateMenu, onDismissRequest = { stateMenu = false }) {
                        brazilStates.forEach { state ->
                            DropdownMenuItem(text = { Text(state) }, onClick = { selectedState = state; selectedCity = "Todas as cidades"; stateMenu = false })
                        }
                    }
                }
                Button(onClick = { popularOnly = true }) { Text("Mais ouvidas") }
                OutlinedButton(onClick = { popularOnly = false }) { Text("Todas") }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { cityMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cidade: $selectedCity", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DropdownMenu(expanded = cityMenu, onDismissRequest = { cityMenu = false }) {
                    goiasCities.forEach { city ->
                        DropdownMenuItem(text = { Text(city) }, onClick = {
                            selectedCity = city
                            selectedState = if (city == "Todas as cidades") "Todos os estados" else "Goiás"
                            cityMenu = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            when {
                loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = { scope.launch { loadStations() } }) { Text("Tentar novamente") }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isBlank()) "Nenhuma emissora encontrada para este estado." else "Nenhuma rádio encontrada com esse nome.")
                }
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { station ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { playStation(station) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(22.dp),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!station.logo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = station.logo,
                                        contentDescription = "Logo de ${station.name}",
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(13.dp)),
                                    )
                                } else {
                                    Box(
                                        Modifier.size(54.dp).clip(RoundedCornerShape(13.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) { RadioKeuMusicTopMark(modifier = Modifier.size(54.dp)) }
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(station.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(listOf(station.city ?: station.state, station.genre).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Brasil" },
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                Text("AO VIVO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
