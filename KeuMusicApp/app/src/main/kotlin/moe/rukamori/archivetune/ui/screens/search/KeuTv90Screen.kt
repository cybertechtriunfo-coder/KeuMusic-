/*
 * KeuMusic | KeuTV90
 * Retro television channels opened inside KeuMusic.
 */
package moe.rukamori.archivetune.ui.screens.search

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController

private const val KEUTV90_URL = "https://tv90.com.br/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KeuTv90Screen(navController: NavController) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("KeuTV90 • Clássicos dos anos 90") }
    var fullScreenView by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    fun exitFullScreen() {
        val callback = fullScreenCallback
        fullScreenCallback = null
        fullScreenView = null
        callback?.onCustomViewHidden()
    }

    BackHandler(enabled = fullScreenView != null) {
        exitFullScreen()
    }
    BackHandler(enabled = fullScreenView == null && canGoBack) {
        webView?.goBack()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = {
                if (fullScreenView != null) exitFullScreen()
                else if (webView?.canGoBack() == true) webView?.goBack()
                else navController.navigateUp()
            }) {
                Text("Voltar")
            }
            Column(Modifier.weight(1f)) {
                Text("KeuTV90", style = MaterialTheme.typography.titleMedium)
                Text(pageTitle, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportMultipleWindows(false)
                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                            fullScreenCallback?.onCustomViewHidden()
                            (view.parent as? ViewGroup)?.removeView(view)
                            fullScreenView = view
                            fullScreenCallback = callback
                        }

                        override fun onHideCustomView() {
                            val callback = fullScreenCallback
                            fullScreenCallback = null
                            fullScreenView = null
                            callback?.onCustomViewHidden()
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            return false
                        }

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            canGoBack = view.canGoBack()
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            canGoBack = view.canGoBack()
                            pageTitle = view.title?.takeIf { it.isNotBlank() } ?: "Canais clássicos"
                        }
                    }
                    // Load exactly once; never restart navigation during Compose recomposition.
                    loadUrl(KEUTV90_URL)
                    webView = this
                }
            },
            update = { view ->
                webView = view
            },
        )
    }

    if (fullScreenView != null) {
        Dialog(
            onDismissRequest = { exitFullScreen() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { FrameLayout(context) },
                    update = { container ->
                        val videoView = fullScreenView
                        if (container.childCount != 1 || container.getChildAt(0) !== videoView) {
                            container.removeAllViews()
                            (videoView?.parent as? ViewGroup)?.removeView(videoView)
                            if (videoView != null) {
                                container.addView(
                                    videoView,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fullScreenCallback?.onCustomViewHidden()
            fullScreenCallback = null
            fullScreenView = null
            webView?.stopLoading()
            webView?.webChromeClient = null
            webView?.destroy()
            webView = null
        }
    }
}
