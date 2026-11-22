/*
 * KeuMusic video extraction adapter.
 * Adapted from the downloader approach used by Plyvanta (GPL-3.0):
 * https://github.com/Plyvanta/Plyvanta
 */
package moe.rukamori.archivetune.utils.video

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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build(),
) : Downloader() {

    override fun execute(request: NewPipeRequest): NewPipeResponse {
        val builder = OkHttpRequest.Builder().url(request.url())
        var hasUserAgent = false

        request.headers().forEach { (name, values) ->
            if (name.equals("User-Agent", ignoreCase = true)) {
                hasUserAgent = true
            }
            values.forEach { value -> builder.addHeader(name, value) }
        }

        if (!hasUserAgent) {
            builder.header("User-Agent", USER_AGENT)
        }

        val method = request.httpMethod()
        val data = request.dataToSend()
        when {
            method.equals("GET", ignoreCase = true) -> builder.get()
            method.equals("HEAD", ignoreCase = true) -> builder.head()
            else -> {
                val contentType = request.headers().entries
                    .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                    ?.value
                    ?.firstOrNull()
                    ?.toMediaTypeOrNull()
                val body = (data ?: ByteArray(0)).toRequestBody(contentType)
                builder.method(method, body)
            }
        }

        client.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            return NewPipeResponse(
                response.code,
                response.message,
                response.headers.toMultimap(),
                body,
                response.request.url.toString(),
            )
        }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}
