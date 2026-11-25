package com.example.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.core.content.ContextCompat

object StoragePermissionHelper {

    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            read && write
        }
    }

    fun createAllFilesAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun createFallbackAllFilesAccessIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * Resolves a standard Android external storage SAF tree URI (e.g. primary:Documents/Vault)
     * into a direct filesystem path (/storage/emulated/0/Documents/Vault) when possible.
     */
    fun resolveTreeUriToPath(uri: Uri): String? {
        return try {
            val treeDocId = DocumentsContract.getTreeDocumentId(uri)
            decodePrimaryStorageDocId(treeDocId)
        } catch (_: Exception) {
            null
        }
    }

    fun resolveDocumentUriToPath(uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            decodePrimaryStorageDocId(docId)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodePrimaryStorageDocId(docId: String): String? {
        val parts = docId.split(":", limit = 2)
        if (parts.isEmpty()) return null
        val volume = parts[0]
        val rel = if (parts.size > 1) parts[1] else ""
        return if (volume.equals("primary", ignoreCase = true)) {
            val root = Environment.getExternalStorageDirectory().absolutePath
            if (rel.isEmpty()) root else "$root/$rel"
        } else if (volume.equals("raw", ignoreCase = true) && rel.startsWith("/")) {
            rel
        } else {
            null
        }
    }
}
