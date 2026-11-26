/*
 * KeuMusic (2026) | Modified work by MuwMix
 * MSN KeuMusic — embedded msnweb.app Messenger experience.
 */
package moe.rukamori.archivetune.ui.screens.search

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.navigation.NavController
import moe.rukamori.archivetune.R

private const val MSN_MESSENGER_URL = "https://msnweb.app/celular"

@Composable
fun MsnMessengerShortcut(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(112.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF0C4B86), Color(0xFF1977B8), Color(0xFF164C78)))
            ).padding(18.dp)
        ) {
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = 2.dp, end = 124.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("MSN KeuMusic", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Converse com seus amigos", color = Color.White.copy(alpha = .92f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Image(
                painter = painterResource(R.drawable.msn_keumusic_icon),
                contentDescription = "Ícone do MSN KeuMusic",
                modifier = Modifier.align(Alignment.CenterEnd).size(92.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MsnMessengerScreen(navController: NavController) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingWebPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingWebResources by remember { mutableStateOf<Array<String>>(emptyArray()) }
    var pendingAndroidPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileChooserCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        fileChooserCallback = null
    }

    val webPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val request = pendingWebPermission
        if (request != null) {
            val granted = pendingAndroidPermissions.all { permission ->
                result[permission] == true ||
                    ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
            }
            if (granted) request.grant(pendingWebResources) else request.deny()
        }
        pendingWebPermission = null
        pendingWebResources = emptyArray()
        pendingAndroidPermissions = emptyList()
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    DisposableEffect(webView) {
        onDispose {
            pendingWebPermission?.deny()
            fileChooserCallback?.onReceiveValue(null)
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { navController.navigateUp() }) { Text("Voltar") }
            Spacer(Modifier.size(12.dp))
            Image(
                painter = painterResource(R.drawable.msn_keumusic_icon),
                contentDescription = "MSN KeuMusic",
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(
                "MSN KeuMusic",
                Modifier.weight(1f).padding(start = 10.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedButton(onClick = { webView?.reload(); pageProgress = 0 }) { Text("Atualizar") }
        }
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize().padding(bottom = 104.dp),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        webView = this
                        setBackgroundColor(android.graphics.Color.WHITE)
                        overScrollMode = android.view.View.OVER_SCROLL_NEVER
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadsImagesAutomatically = true
                            mediaPlaybackRequiresUserGesture = true
                            allowContentAccess = true
                            allowFileAccess = false
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(false)
                            safeBrowsingEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(this@WebView, true)
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                pageProgress = newProgress
                            }

                            override fun onPermissionRequest(request: PermissionRequest) {
                                val resources = request.resources.toList()
                                val requiredPermissions = buildList {
                                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) add(Manifest.permission.RECORD_AUDIO)
                                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) add(Manifest.permission.CAMERA)
                                }
                                val supportedResources = listOf(
                                    PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                                    PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                                )
                                if (resources.isEmpty() || resources.any { it !in supportedResources }) {
                                    request.deny()
                                    return
                                }
                                val missing = requiredPermissions.filter { permission ->
                                    ContextCompat.checkSelfPermission(context, permission) != PermissionChecker.PERMISSION_GRANTED
                                }
                                if (missing.isEmpty()) {
                                    request.grant(resources.toTypedArray())
                                } else {
                                    pendingWebPermission?.deny()
                                    pendingWebPermission = request
                                    pendingWebResources = resources.toTypedArray()
                                    pendingAndroidPermissions = missing
                                    webPermissionLauncher.launch(missing.toTypedArray())
                                }
                            }

                            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                                if (pendingWebPermission == request) {
                                    pendingWebPermission = null
                                    pendingWebResources = emptyArray()
                                    pendingAndroidPermissions = emptyList()
                                }
                            }

                            override fun onShowFileChooser(
                                view: WebView?,
                                callback: ValueCallback<Array<Uri>>?,
                                params: FileChooserParams?,
                            ): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = callback
                                val acceptType = params?.acceptTypes?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "*/*"
                                filePicker.launch(acceptType)
                                return true
                            }
                        }
                        loadUrl(MSN_MESSENGER_URL)
                    }
                },
                update = { if (webView !== it) webView = it },
            )
            if (pageProgress < 100) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
