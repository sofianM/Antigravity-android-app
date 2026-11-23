package com.sofianm.antigravity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sofianm.antigravity.foldable.FoldStateTracker
import com.sofianm.antigravity.ui.AppShell
import com.sofianm.antigravity.ui.theme.AntigravityTheme
import com.sofianm.antigravity.web.WebFileAndMediaHelper
import com.sofianm.antigravity.web.WebViewHostCallbacks
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), WebViewHostCallbacks {

    private val viewModel: AntigravityViewModel by viewModels()
    private val fileAndMediaHelper = WebFileAndMediaHelper()
    private lateinit var foldStateTracker: FoldStateTracker

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileAndMediaHelper.onFileChooserResult(result.resultCode, result.data)
    }

    private val webMediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        fileAndMediaHelper.onRuntimePermissionsResult(grants)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.showMessage("Meldingstoestemming niet verleend")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        foldStateTracker = FoldStateTracker(this)
        viewModel.updateFoldState(foldStateTracker.currentSnapshot())
        viewModel.webSessionHolder.attachHost(this, this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                foldStateTracker.observeFoldState().collect { state ->
                    viewModel.updateFoldState(state)
                }
            }
        }

        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            AntigravityTheme(themeMode = settings.themeMode) {
                AppShell(
                    activity = this,
                    viewModel = viewModel,
                    onRequestNotificationPermission = ::requestNotificationPermissionIfNeeded
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Immediately reflect Pixel 9 Pro Fold cover <-> inner screen switches without recreating Activity
        viewModel.updateFoldState(foldStateTracker.currentSnapshot())
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAppInForeground(true)
        viewModel.webSessionHolder.attachHost(this, this)
    }

    override fun onPause() {
        viewModel.setAppInForeground(false)
        super.onPause()
    }

    override fun onDestroy() {
        if (isFinishing) {
            fileAndMediaHelper.cancelPending()
        }
        viewModel.webSessionHolder.detachHost()
        super.onDestroy()
    }

    override fun onFileChooserRequested(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        val intent = fileAndMediaHelper.createFileChooserIntent(
            context = this,
            filePathCallback = filePathCallback,
            fileChooserParams = fileChooserParams
        )
        return runCatching {
            fileChooserLauncher.launch(intent)
            true
        }.getOrElse {
            fileAndMediaHelper.cancelPending()
            false
        }
    }

    override fun onWebPermissionRequested(request: PermissionRequest) {
        val required = fileAndMediaHelper.holdPermissionRequest(request)
        if (required.isEmpty()) {
            fileAndMediaHelper.onRuntimePermissionsResult(emptyMap())
            return
        }
        val alreadyGranted = required.associateWith { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (alreadyGranted.values.all { it }) {
            fileAndMediaHelper.onRuntimePermissionsResult(alreadyGranted)
        } else {
            webMediaPermissionLauncher.launch(required)
        }
    }

    override fun onDownloadRequested(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        val started = WebFileAndMediaHelper.enqueueDownload(
            context = this,
            url = url,
            userAgent = userAgent,
            contentDisposition = contentDisposition,
            mimeType = mimeType
        )
        if (started) {
            viewModel.showMessage("Download gestart in Downloads")
        } else {
            viewModel.showMessage("Download kon niet worden gestart")
        }
    }

    override fun onExternalSchemeRequested(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            viewModel.showMessage("Geen app gevonden om deze link te openen")
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.onExternalUrlShared(sharedText)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
