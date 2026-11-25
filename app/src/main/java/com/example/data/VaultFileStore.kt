package com.example.data

import android.content.Context
import android.net.Uri
import com.example.platform.StoragePermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File

enum class VaultFileKind {
    MAINTENANCE_TASKS,
    MAINTENANCE_HISTORY,
    ROUTINES
}

sealed interface VaultReadResult {
    data class Success(val content: String, val displayLocation: String) : VaultReadResult
    data class MissingAllFilesAccess(val targetPath: String) : VaultReadResult
    data class FileNotFound(val targetPath: String) : VaultReadResult
    data class Error(val message: String) : VaultReadResult
}

sealed interface VaultWriteResult {
    data class Success(val displayLocation: String) : VaultWriteResult
    data class MissingAllFilesAccess(val targetPath: String) : VaultWriteResult
    data class Error(val message: String) : VaultWriteResult
}

class VaultFileStore(private val context: Context) {

    private val _fileUpdatedSignal = MutableSharedFlow<VaultFileKind>(extraBufferCapacity = 8)
    val fileUpdatedSignal: SharedFlow<VaultFileKind> = _fileUpdatedSignal.asSharedFlow()

    private val sampleRootDir: File
        get() = File(context.filesDir, "sample_vault")

    suspend fun ensureSampleVaultSeeded() = withContext(Dispatchers.IO) {
        val tasksFile = resolveSampleFile("notes/maintenance-tasks-01.md")
        val historyFile = resolveSampleFile("notes/maintenance-tasks-01.history.md")
        val routinesFile = resolveSampleFile("notes/routines.md")

        if (!tasksFile.exists()) {
            tasksFile.parentFile?.mkdirs()
            tasksFile.writeText(MaintenanceMarkdownCodec.buildSampleTasksMarkdown())
        }
        if (!historyFile.exists()) {
            historyFile.parentFile?.mkdirs()
            historyFile.writeText(MaintenanceMarkdownCodec.buildSampleHistoryMarkdown())
        }
        if (!routinesFile.exists()) {
            routinesFile.parentFile?.mkdirs()
            routinesFile.writeText(RoutineMarkdownCodec.buildSampleRoutinesMarkdown())
        }
    }

    suspend fun resetSampleVault() = withContext(Dispatchers.IO) {
        val tasksFile = resolveSampleFile("notes/maintenance-tasks-01.md")
        val historyFile = resolveSampleFile("notes/maintenance-tasks-01.history.md")
        val routinesFile = resolveSampleFile("notes/routines.md")

        tasksFile.parentFile?.mkdirs()
        tasksFile.writeText(MaintenanceMarkdownCodec.buildSampleTasksMarkdown())
        historyFile.parentFile?.mkdirs()
        historyFile.writeText(MaintenanceMarkdownCodec.buildSampleHistoryMarkdown())
        routinesFile.parentFile?.mkdirs()
        routinesFile.writeText(RoutineMarkdownCodec.buildSampleRoutinesMarkdown())

        _fileUpdatedSignal.tryEmit(VaultFileKind.MAINTENANCE_TASKS)
        _fileUpdatedSignal.tryEmit(VaultFileKind.MAINTENANCE_HISTORY)
        _fileUpdatedSignal.tryEmit(VaultFileKind.ROUTINES)
    }

    suspend fun readMarkdown(kind: VaultFileKind, settings: VaultSettings): VaultReadResult =
        withContext(Dispatchers.IO) {
            if (settings.storageMode == StorageMode.INTERNAL_SAMPLE) {
                ensureSampleVaultSeeded()
                val file = resolveSampleFile(relativePathForKind(kind, settings))
                return@withContext if (file.exists()) {
                    VaultReadResult.Success(
                        content = file.readText(),
                        displayLocation = "Sample Vault / ${file.name}"
                    )
                } else {
                    VaultReadResult.FileNotFound(file.absolutePath)
                }
            }

            // External Vault Mode
            val uriStr = uriForKind(kind, settings)
            if (!uriStr.isNullOrBlank()) {
                val uri = Uri.parse(uriStr)
                val uriRead = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (uriRead != null) {
                    return@withContext VaultReadResult.Success(
                        content = uriRead,
                        displayLocation = StoragePermissionHelper.resolveDocumentUriToPath(uri) ?: uriStr
                    )
                }
            }

            val resolvedFile = resolveExternalFile(kind, settings)
            if (!StoragePermissionHelper.hasAllFilesAccess(context)) {
                return@withContext VaultReadResult.MissingAllFilesAccess(resolvedFile.absolutePath)
            }

            if (!resolvedFile.exists()) {
                return@withContext VaultReadResult.FileNotFound(resolvedFile.absolutePath)
            }

            return@withContext try {
                VaultReadResult.Success(
                    content = resolvedFile.readText(),
                    displayLocation = resolvedFile.absolutePath
                )
            } catch (e: Exception) {
                VaultReadResult.Error(e.localizedMessage ?: "Unable to read ${resolvedFile.name}")
            }
        }

    suspend fun writeMarkdown(
        kind: VaultFileKind,
        settings: VaultSettings,
        content: String
    ): VaultWriteResult = withContext(Dispatchers.IO) {
        if (settings.storageMode == StorageMode.INTERNAL_SAMPLE) {
            ensureSampleVaultSeeded()
            val file = resolveSampleFile(relativePathForKind(kind, settings))
            return@withContext try {
                file.parentFile?.mkdirs()
                file.writeText(content)
                _fileUpdatedSignal.tryEmit(kind)
                VaultWriteResult.Success("Sample Vault / ${file.name}")
            } catch (e: Exception) {
                VaultWriteResult.Error(e.localizedMessage ?: "Failed to write sample file")
            }
        }

        // Prefer direct filesystem path when All Files Access is granted; fallback to SAF URI if set
        val resolvedFile = resolveExternalFile(kind, settings)
        if (StoragePermissionHelper.hasAllFilesAccess(context)) {
            return@withContext try {
                resolvedFile.parentFile?.mkdirs()
                resolvedFile.writeText(content)
                _fileUpdatedSignal.tryEmit(kind)
                VaultWriteResult.Success(resolvedFile.absolutePath)
            } catch (e: Exception) {
                VaultWriteResult.Error(e.localizedMessage ?: "Failed to write ${resolvedFile.name}")
            }
        }

        val uriStr = uriForKind(kind, settings)
        if (!uriStr.isNullOrBlank()) {
            val uri = Uri.parse(uriStr)
            return@withContext try {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                    it.write(content)
                } ?: return@withContext VaultWriteResult.Error("Cannot open output stream for selected file")
                _fileUpdatedSignal.tryEmit(kind)
                VaultWriteResult.Success(StoragePermissionHelper.resolveDocumentUriToPath(uri) ?: uriStr)
            } catch (e: Exception) {
                VaultWriteResult.Error(e.localizedMessage ?: "Failed to write selected document")
            }
        }

        VaultWriteResult.MissingAllFilesAccess(resolvedFile.absolutePath)
    }

    suspend fun initializeMissingExternalFiles(settings: VaultSettings): Result<Int> =
        withContext(Dispatchers.IO) {
            if (!StoragePermissionHelper.hasAllFilesAccess(context)) {
                return@withContext Result.failure(IllegalStateException("All Files Access permission is required."))
            }
            return@withContext try {
                var createdCount = 0
                val tasksFile = resolveExternalFile(VaultFileKind.MAINTENANCE_TASKS, settings)
                if (!tasksFile.exists()) {
                    tasksFile.parentFile?.mkdirs()
                    tasksFile.writeText(MaintenanceMarkdownCodec.buildSampleTasksMarkdown())
                    createdCount++
                }
                val historyFile = resolveExternalFile(VaultFileKind.MAINTENANCE_HISTORY, settings)
                if (!historyFile.exists()) {
                    historyFile.parentFile?.mkdirs()
                    historyFile.writeText(MaintenanceMarkdownCodec.buildSampleHistoryMarkdown())
                    createdCount++
                }
                val routinesFile = resolveExternalFile(VaultFileKind.ROUTINES, settings)
                if (!routinesFile.exists()) {
                    routinesFile.parentFile?.mkdirs()
                    routinesFile.writeText(RoutineMarkdownCodec.buildSampleRoutinesMarkdown())
                    createdCount++
                }
                if (createdCount > 0) {
                    _fileUpdatedSignal.tryEmit(VaultFileKind.MAINTENANCE_TASKS)
                    _fileUpdatedSignal.tryEmit(VaultFileKind.MAINTENANCE_HISTORY)
                    _fileUpdatedSignal.tryEmit(VaultFileKind.ROUTINES)
                }
                Result.success(createdCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun resolveExternalFile(kind: VaultFileKind, settings: VaultSettings): File {
        val rawPath = relativePathForKind(kind, settings).trim()
        return if (rawPath.startsWith("/")) {
            File(rawPath)
        } else {
            val cleanedRel = rawPath.removePrefix("./").removePrefix("/")
            File(settings.vaultRootPath.trimEnd('/'), cleanedRel)
        }
    }

    private fun resolveSampleFile(rawPath: String): File {
        val fileName = rawPath.trim().substringAfterLast('/').ifEmpty { "notes.md" }
        return File(sampleRootDir, fileName)
    }

    private fun relativePathForKind(kind: VaultFileKind, settings: VaultSettings): String = when (kind) {
        VaultFileKind.MAINTENANCE_TASKS -> settings.maintenanceFilePath
        VaultFileKind.MAINTENANCE_HISTORY -> settings.historyFilePath
        VaultFileKind.ROUTINES -> settings.routinesFilePath
    }

    private fun uriForKind(kind: VaultFileKind, settings: VaultSettings): String? = when (kind) {
        VaultFileKind.MAINTENANCE_TASKS -> settings.maintenanceUri
        VaultFileKind.MAINTENANCE_HISTORY -> settings.historyUri
        VaultFileKind.ROUTINES -> settings.routinesUri
    }
}
