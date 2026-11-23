package com.sofianm.antigravity.web

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Coordinates WebView file uploads (documents, gallery & camera capture via FileProvider),
 * downloads (via Android DownloadManager), and WebRTC camera/microphone permission requests.
 */
class WebFileAndMediaHelper {

    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingCameraOutputUri: Uri? = null
    private var pendingWebPermissionRequest: PermissionRequest? = null

    fun createFileChooserIntent(
        context: Context,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Intent {
        // Cancel any lingering previous callback cleanly
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = filePathCallback
        pendingCameraOutputUri = null

        val acceptTypes = fileChooserParams?.acceptTypes
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val allowMultiple = fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE

        val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (acceptTypes.size == 1) acceptTypes.first() else "*/*"
            if (acceptTypes.size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes.toTypedArray())
            }
            if (allowMultiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

        val cameraIntents = mutableListOf<Intent>()
        val wantsImageOrAny = acceptTypes.isEmpty() || acceptTypes.any {
            it.startsWith("image/") || it == "*/*" || it.contains("jpg") || it.contains("png")
        }
        if (wantsImageOrAny) {
            createCameraImageUri(context)?.let { photoUri ->
                pendingCameraOutputUri = photoUri
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                cameraIntents.add(captureIntent)
            }
        }

        return Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, contentIntent)
            putExtra(Intent.EXTRA_TITLE, "Bestand kiezen of foto maken")
            if (cameraIntents.isNotEmpty()) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toTypedArray())
            }
        }
    }

    fun onFileChooserResult(resultCode: Int, data: Intent?) {
        val callback = pendingFileCallback ?: return
        pendingFileCallback = null

        if (resultCode != Activity.RESULT_OK) {
            callback.onReceiveValue(null)
            pendingCameraOutputUri = null
            return
        }

        val uris = mutableListOf<Uri>()
        val clipData = data?.clipData
        if (clipData != null && clipData.itemCount > 0) {
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).uri?.let { uris.add(it) }
            }
        } else if (data?.data != null) {
            data.data?.let { uris.add(it) }
        } else {
            pendingCameraOutputUri?.let { uris.add(it) }
        }

        pendingCameraOutputUri = null
        callback.onReceiveValue(if (uris.isEmpty()) null else uris.toTypedArray())
    }

    fun holdPermissionRequest(request: PermissionRequest): Array<String> {
        pendingWebPermissionRequest?.deny()
        pendingWebPermissionRequest = request

        val needed = mutableListOf<String>()
        request.resources.forEach { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> needed.add(android.Manifest.permission.CAMERA)
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> needed.add(android.Manifest.permission.RECORD_AUDIO)
            }
        }
        return needed.distinct().toTypedArray()
    }

    fun onRuntimePermissionsResult(grants: Map<String, Boolean>) {
        val request = pendingWebPermissionRequest ?: return
        pendingWebPermissionRequest = null

        val allowedResources = request.resources.filter { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> grants[android.Manifest.permission.CAMERA] == true
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> grants[android.Manifest.permission.RECORD_AUDIO] == true
                else -> false
            }
        }.toTypedArray()

        if (allowedResources.isNotEmpty()) {
            runCatching { request.grant(allowedResources) }
        } else {
            runCatching { request.deny() }
        }
    }

    fun cancelPending() {
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        pendingCameraOutputUri = null
        pendingWebPermissionRequest?.deny()
        pendingWebPermissionRequest = null
    }

    private fun createCameraImageUri(context: Context): Uri? {
        return runCatching {
            val dir = File(context.cacheDir, "camera_uploads").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "IMG_$stamp.jpg")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    companion object {
        fun enqueueDownload(
            context: Context,
            url: String,
            userAgent: String?,
            contentDisposition: String?,
            mimeType: String?
        ): Boolean {
            return runCatching {
                val uri = Uri.parse(url)
                if (uri.scheme != "http" && uri.scheme != "https") return false

                val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: run {
                    val ext = MimeTypeMap.getFileExtensionFromUrl(url)
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                }
                val fileName = URLUtil.guessFileName(url, contentDisposition, resolvedMime)
                val cookies = CookieManager.getInstance().getCookie(url)

                val request = DownloadManager.Request(uri).apply {
                    setMimeType(resolvedMime)
                    setTitle(fileName)
                    setDescription("Downloaden via Antigravity")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    if (!cookies.isNullOrBlank()) addRequestHeader("Cookie", cookies)
                    if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
                }

                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                true
            }.getOrDefault(false)
        }
    }
}
