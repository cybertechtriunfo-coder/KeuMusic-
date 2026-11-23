/*
 * KeuMusic video extraction adapter.
 * Adapted from the downloader approach used by Plyvanta (GPL-3.0):
 * https://github.com/Plyvanta/Plyvanta
 */
package moe.rukamori.archivetune.utils.video

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response as NewPipeResponse
import java.util.concurrent.TimeUnit

internal class NewPipeVideoDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build(),
) : Downloader() {

    override fun execute(request: NewPipeRequest): NewPipeResponse {
        val builder = OkHttpRequest.Builder().url(request.url())
        var hasUserAgent = false

        request.headers().forEach { (name, values) ->
            if (name.equals("User-Agent", ignoreCase = true)) hasUserAgent = true
            values.forEach { value -> builder.addHeader(name, value) }
        }
        if (!hasUserAgent) builder.header("User-Agent", USER_AGENT)

        val method = request.httpMethod()
        val data = request.dataToSend()
        when {
            method.equals("GET", ignoreCase = true) -> builder.get()
            method.equals("HEAD", ignoreCase = true) -> builder.head()
            else -> {
                val contentType = request.headers().entries
                    .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                    ?.value?.firstOrNull()?.toMediaTypeOrNull()
                builder.method(method, (data ?: ByteArray(0)).toRequestBody(contentType))
            }
        }

        client.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val url = response.request.url.toString()
            val contentType = response.header("Content-Type").orEmpty()
            val isYoutubeJsonEndpoint = url.contains("youtubei.googleapis.com", ignoreCase = true)
            val looksLikeHtml = contentType.contains("text/html", ignoreCase = true) ||
                body.trimStart().startsWith("<!doctype html", ignoreCase = true) ||
                body.trimStart().startsWith("<html", ignoreCase = true)

            // YouTube sometimes returns an HTML challenge/block page for an API call.
            // Passing that page to NewPipe's JSON parser produces the misleading
            // "Got HTML document, expected JSON response" error.
            if (isYoutubeJsonEndpoint && looksLikeHtml) {
                val reason = when (response.code) {
                    403 -> "YouTube recusou a solicitação (HTTP 403)."
                    429 -> "YouTube limitou temporariamente as solicitações (HTTP 429)."
                    else -> "YouTube retornou uma página HTML em vez de JSON (HTTP ${response.code})."
                }
                throw IOException("$reason URL: $url")
            }

            return NewPipeResponse(
                response.code,
                response.message,
                response.headers.toMultimap(),
                body,
                url,
            )
        }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}
