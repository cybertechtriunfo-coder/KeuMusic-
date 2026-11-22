/*
 * KeuMusic (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.ui.screens.video

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YouTubeVideoScreen(videoId: String) {
    val context = LocalContext.current
    val validId = remember(videoId) {
        videoId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (validId == null) {
            Text(
                text = "Vídeo inválido.",
                color = Color.White,
            )
        } else {
            val webView = remember(validId) {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()

                    val html = """
                        <!doctype html>
                        <html>
                        <head>
                          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                          <style>
                            html, body { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
                            iframe { border:0; width:100%; height:100%; }
                          </style>
                        </head>
                        <body>
                          <iframe
                            src="https://www.youtube.com/embed/$validId?autoplay=1&playsinline=1&rel=0"
                            title="YouTube video player"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                            allowfullscreen>
                          </iframe>
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                }
            }

            DisposableEffect(webView) {
                onDispose {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.destroy()
                }
            }

            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
