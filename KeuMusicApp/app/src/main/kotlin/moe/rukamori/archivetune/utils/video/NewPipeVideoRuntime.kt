/*
 * KeuMusic video extraction runtime.
 * Uses the GPL-3.0 NewPipe Extractor library.
 */
package moe.rukamori.archivetune.utils.video

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import moe.rukamori.archivetune.utils.potoken.BotGuardTokenGenerator
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult as NewPipePoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import java.util.Locale

/**
 * Supplies the WEB-client PoTokens required by current YouTube integrity checks.
 *
 * The app already owns a WebView-backed BotGuard generator. Reusing it avoids
 * creating a second WebView/token engine just for NewPipe. The visitor data is
 * obtained from the same WEB InnerTube client and is used as the session mint
 * identifier, so the streaming token is paired with the correct visitor data.
 */
private object KeuMusicPoTokenProvider : PoTokenProvider {
    @Volatile
    private var cachedVisitorData: String? = null

    override fun getWebClientPoToken(videoId: String): NewPipePoTokenResult? {
        return try {
            val visitorData = cachedVisitorData ?: synchronized(this) {
                cachedVisitorData ?: run {
                    val requestInfo = InnertubeClientRequestInfo.ofWebClient()
                    requestInfo.clientInfo.clientVersion = YoutubeParsingHelper.getClientVersion()
                    YoutubeParsingHelper.getVisitorDataFromInnertube(
                        requestInfo,
                        NewPipe.getPreferredLocalization(),
                        NewPipe.getPreferredContentCountry(),
                        YoutubeParsingHelper.getYouTubeHeaders(),
                        YoutubeParsingHelper.YOUTUBEI_V1_URL,
                        null,
                        false,
                    ).also { cachedVisitorData = it }
                }
            }

            // NewPipe calls PoTokenProvider synchronously while stream extraction is
            // already running on the resolver's IO dispatcher. BotGuard switches to
            // the main thread only for WebView work and never blocks the UI thread.
            val minted = runBlocking(Dispatchers.IO) {
                BotGuardTokenGenerator.mintToken(videoId, visitorData)
            } ?: return null

            NewPipePoTokenResult(
                visitorData,
                minted.playerToken,
                minted.sessionToken,
            )
        } catch (error: Exception) {
            Log.w("KeuMusicVideo", "Não foi possível gerar PoToken Web; usando fallback do extrator.", error)
            null
        }
    }

    override fun getWebEmbedClientPoToken(videoId: String): NewPipePoTokenResult? = null

    override fun getAndroidClientPoToken(videoId: String): NewPipePoTokenResult? = null

    override fun getIosClientPoToken(videoId: String): NewPipePoTokenResult? = null
}

internal object NewPipeVideoRuntime {
    @Volatile
    private var initialized = false

    fun initialize() {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            val locale = Locale.getDefault()
            val language = locale.language.takeIf(String::isNotBlank) ?: "en"
            val country = locale.country
            val localization = Localization(language, country)
            val contentCountry = if (country.isBlank()) {
                ContentCountry.DEFAULT
            } else {
                ContentCountry(country)
            }

            NewPipe.init(NewPipeVideoDownloader(), localization, contentCountry)
            YoutubeStreamExtractor.setPoTokenProvider(KeuMusicPoTokenProvider)
            initialized = true
        }
    }
}
