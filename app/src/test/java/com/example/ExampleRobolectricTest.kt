package com.example

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import com.example.data.cache.AudioStreamDownloader
import com.example.data.cache.OfflineCacheStats
import com.example.data.cache.OfflineMusicCacheManager
import com.example.data.local.PlaylistWithCount
import com.example.data.local.TrackEntity
import com.example.data.youtube.ParsedYouTubeInput
import com.example.data.youtube.YouTubeUrlParser
import com.example.player.PlaybackUiState
import com.example.ui.components.KeuMusicTopBar
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.OfflineSettingsContent
import com.example.ui.components.buildGenreBuckets
import com.example.ui.theme.PulseTubeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KEUMUSIC", appName)
  }

  @Test
  fun `parse youtube playlist and video urls`() {
    val playlist = YouTubeUrlParser.parse(
      "https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"
    )
    assertTrue(playlist is ParsedYouTubeInput.Playlist)
    assertEquals(
      "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
      (playlist as ParsedYouTubeInput.Playlist).playlistId
    )

    val musicPlaylist = YouTubeUrlParser.parse(
      "https://music.youtube.com/watch?v=jfKfPfyJRdk&list=RDCLAK5uy_kmPRjHDECIcuVwnKsx2Ng7fyNgFKWNJFs"
    )
    assertTrue(musicPlaylist is ParsedYouTubeInput.Playlist)

    val shortVideo = YouTubeUrlParser.parse("https://youtu.be/jfKfPfyJRdk")
    assertEquals(ParsedYouTubeInput.Video("jfKfPfyJRdk"), shortVideo)

    val invalid = YouTubeUrlParser.parse("https://example.com/not-youtube")
    assertEquals(ParsedYouTubeInput.Invalid, invalid)
  }

  @Test
  fun `build genre buckets groups playlists accurately`() {
    val playlists = listOf(
      PlaylistWithCount("p1", "Top Hits & Pop Brasil", "YouTube Mix", "", null, 0L, true, 1L, 1L, 5),
      PlaylistWithCount("p2", "Eletrônica & Festa", "Dance", "", null, 0L, false, 1L, 1L, 5),
      PlaylistWithCount("p3", "Foco & Lo-fi Radio", "Relax", "", null, 0L, true, 1L, 1L, 3)
    )
    val tracks = listOf(
      TrackEntity("t1", "p1", "JVpTp8IHdEg", "Hear Me Now", "Alok", ""),
      TrackEntity("t2", "p3", "jfKfPfyJRdk", "Lofi Radio", "Lofi Girl", "")
    )
    val buckets = buildGenreBuckets(playlists, tracks)
    assertEquals(3, buckets.size)
    assertEquals("pop_brasil", buckets[0].id)
    assertEquals(1, buckets[0].tracks.size)
    assertEquals("eletronica", buckets[1].id)
    assertEquals("lofi", buckets[2].id)
  }

  @Test
  fun `mini player bar renders elapsed and total times and interactive slider`() {
    var seekedTo: Float? = null
    val track = TrackEntity(
      id = "t_alok",
      playlistId = "p1",
      youtubeVideoId = "JVpTp8IHdEg",
      title = "Hear Me Now",
      artist = "Alok, Bruno Martini & Zeeba",
      thumbnailUrl = "",
      durationText = "3:15",
      durationSeconds = 195
    )

    composeTestRule.setContent {
      PulseTubeTheme {
        MiniPlayerBar(
          playbackState = PlaybackUiState(
            currentTrack = track,
            queue = listOf(track),
            currentIndex = 0,
            isPlaying = true,
            currentPositionSec = 45f,
            durationSec = 195f
          ),
          isLiked = false,
          onExpandClick = {},
          onPlayPauseClick = {},
          onPreviousClick = {},
          onNextClick = {},
          onLikeClick = {},
          onSeek = { seekedTo = it },
          onVideoSlotPositioned = {}
        )
      }
    }

    try {
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("mini_player_elapsed_time", useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("0:45")
      composeTestRule.onNodeWithTag("mini_player_total_time", useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("3:15")
    } catch (t: Throwable) {
      throw AssertionError(
        "${t.message}\nUNMERGED TREE:\n${composeTestRule.onRoot(useUnmergedTree = true).printToString()}",
        t
      )
    }
    composeTestRule.onNodeWithTag("mini_player_progress_slider")
      .assertIsDisplayed()
      .performTouchInput {
        swipe(
          start = androidx.compose.ui.geometry.Offset(width * 0.25f, centerY),
          end = androidx.compose.ui.geometry.Offset(width * 0.85f, centerY),
          durationMillis = 300L
        )
      }
    composeTestRule.waitForIdle()
    assertTrue("Expected seekedTo > 45f but was $seekedTo", (seekedTo ?: 0f) > 45f)
  }

  @Test
  fun `theme switch button toggles between dark and light material3 color schemes`() {
    var isDark by mutableStateOf(true)
    var measuredBackgroundLuminance = 0f

    composeTestRule.setContent {
      PulseTubeTheme(darkTheme = isDark) {
        measuredBackgroundLuminance = MaterialTheme.colorScheme.background.luminance()
        KeuMusicTopBar(
          title = "Início",
          isDarkTheme = isDark,
          canNavigateBack = false,
          onBackClick = {},
          onToggleTheme = { isDark = !isDark }
        )
      }
    }

    composeTestRule.waitForIdle()
    assertTrue("Dark theme background should have low luminance", measuredBackgroundLuminance < 0.2f)
    composeTestRule.onNodeWithTag("top_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_switch_button")
      .assertIsDisplayed()
      .assertTextContains("Claro")
      .performClick()

    composeTestRule.waitForIdle()
    assertFalse("isDark should become false after click", isDark)
    assertTrue("Light theme background should have high luminance", measuredBackgroundLuminance > 0.7f)
    composeTestRule.onNodeWithTag("theme_switch_button")
      .assertTextContains("Escuro")
  }

  @Test
  fun `mini player wave animation displays only when track is actively playing`() {
    val track = TrackEntity(
      id = "t_wave",
      playlistId = "p1",
      youtubeVideoId = "JVpTp8IHdEg",
      title = "Hear Me Now",
      artist = "Alok",
      thumbnailUrl = "",
      durationText = "3:15",
      durationSeconds = 195
    )
    var isPlaying by mutableStateOf(true)

    composeTestRule.setContent {
      PulseTubeTheme {
        MiniPlayerBar(
          playbackState = PlaybackUiState(
            currentTrack = track,
            queue = listOf(track),
            currentIndex = 0,
            isPlaying = isPlaying,
            currentPositionSec = 30f,
            durationSec = 195f
          ),
          isLiked = false,
          onExpandClick = {},
          onPlayPauseClick = { isPlaying = !isPlaying },
          onPreviousClick = {},
          onNextClick = {},
          onLikeClick = {},
          onSeek = {},
          onVideoSlotPositioned = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mini_player_wave_animation", useUnmergedTree = true)
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_player_wave_indicator", useUnmergedTree = true)
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag("mini_player_play_pause", useUnmergedTree = true)
      .performClick()
    composeTestRule.waitForIdle()

    assertFalse("isPlaying should become false after clicking pause", isPlaying)
    composeTestRule.onNodeWithTag("mini_player_wave_animation", useUnmergedTree = true)
      .assertDoesNotExist()
    composeTestRule.onNodeWithTag("mini_player_wave_indicator", useUnmergedTree = true)
      .assertDoesNotExist()
  }

  @Test
  fun `offline music cache manager stores bounded tracks in local app storage and clears cleanly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fakeDownloader = AudioStreamDownloader { videoId, dest, _ ->
      dest.writeBytes("audio-$videoId".toByteArray())
      true
    }
    val manager = OfflineMusicCacheManager(
      context = context,
      downloader = fakeDownloader,
      ioDispatcher = Dispatchers.Unconfined,
      maxTracks = 2,
      maxCacheBytes = 1024L * 1024L,
      networkCheck = { true }
    )
    manager.clearCache()

    val tracks = listOf(
      TrackEntity("1", "p1", "vid_1", "Song 1", "Artist 1", "", "3:00", 180),
      TrackEntity("2", "p1", "vid_2", "Song 2", "Artist 2", "", "3:10", 190),
      TrackEntity("3", "p1", "vid_3", "Song 3", "Artist 3", "", "3:20", 200),
      TrackEntity("4", "p1", "vid_live", "Live Radio", "Lofi", "", "AO VIVO", 0)
    )

    manager.cacheCandidatesNow(tracks)
    assertEquals(2, manager.stats.value.cachedCount)
    assertTrue(manager.stats.value.totalBytes > 0L)
    assertTrue(manager.cacheDir.absolutePath.startsWith(context.filesDir.absolutePath))
    assertNotNull(manager.getCachedAudioFile("vid_1"))
    assertNotNull(manager.getCachedAudioFile("vid_2"))
    assertNull(manager.getCachedAudioFile("vid_live"))

    manager.clearCache()
    assertEquals(0, manager.stats.value.cachedCount)
    assertEquals(0L, manager.stats.value.totalBytes)
    assertNull(manager.getCachedAudioFile("vid_1"))
    manager.release()
  }

  @Test
  fun `offline caching setting switch toggles background caching state and clears cache`() {
    var cachingEnabled by mutableStateOf(false)
    var cachedCount by mutableStateOf(3)
    var clearClicked = false

    composeTestRule.setContent {
      PulseTubeTheme {
        OfflineSettingsContent(
          isBackgroundCachingEnabled = cachingEnabled,
          cacheStats = OfflineCacheStats(
            cachedVideoIds = setOf("a", "b", "c"),
            cachedCount = cachedCount,
            totalBytes = 1024L * 1024L * 4L
          ),
          isDarkTheme = true,
          onToggleBackgroundCaching = { cachingEnabled = it },
          onSyncNow = {},
          onClearCache = {
            clearClicked = true
            cachedCount = 0
          },
          onToggleTheme = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("background_cache_switch")
      .assertIsDisplayed()
      .assertIsOff()
      .performClick()

    composeTestRule.waitForIdle()
    assertTrue("Background caching should be enabled after switch tap", cachingEnabled)
    composeTestRule.onNodeWithTag("background_cache_switch").assertIsOn()

    composeTestRule.onNodeWithTag("offline_cache_usage_text")
      .assertTextContains("3 de 12 músicas salvas", substring = true)

    composeTestRule.onNodeWithTag("clear_offline_cache_button")
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()
    assertTrue("Clear cache callback should fire", clearClicked)
  }
}
