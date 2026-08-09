package com.example

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView?.evaluateJavascript("javascript:if(window.onAndroidBack){window.onAndroidBack();}else{false;}") { result ->
                    val handled = result == "true"
                    if (!handled) {
                        if (webView?.canGoBack() == true) {
                            webView?.goBack()
                        } else {
                            finish()
                        }
                    }
                }
            }
        })

        setContent {
            MyApplicationTheme {
                MainWebViewScreen(
                    onWebViewCreated = { wv ->
                        webView = wv
                    }
                )
            }
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun shareFile(base64Data: String, fileName: String, mimeType: String) {
            try {
                val cleanBase64 = if (base64Data.contains(",")) {
                    base64Data.substringAfter(",")
                } else {
                    base64Data
                }
                val fileBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                if (fileBytes.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Sharing failed: File is empty", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val cacheDir = File(cacheDir, "shared_bills")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val file = File(cacheDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(fileBytes)
                }

                val contentUri: Uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    clipData = ClipData.newRawUri(fileName, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Share Bill").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(chooserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Composable
    fun MainWebViewScreen(onWebViewCreated: (WebView) -> Unit) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                        }
                    }

                    addJavascriptInterface(WebAppInterface(), "AndroidInterface")
                    loadUrl("file:///android_asset/dharam_app.html")
                    onWebViewCreated(this)
                }
            }
        )
    }
}
