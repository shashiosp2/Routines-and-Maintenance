package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StorageMode
import com.example.data.VaultFileKind
import com.example.data.VaultFileStore
import com.example.data.VaultSettings
import com.example.data.VaultSettingsRepository
import com.example.platform.StoragePermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: VaultSettings = VaultSettings(),
    val hasAllFilesAccess: Boolean = false,
    val snackbarMessage: String? = null
)

class SettingsViewModel(
    private val appContext: Context,
    private val settingsRepository: VaultSettingsRepository,
    private val vaultFileStore: VaultFileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(hasAllFilesAccess = StoragePermissionHelper.hasAllFilesAccess(appContext))
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        hasAllFilesAccess = StoragePermissionHelper.hasAllFilesAccess(appContext)
                    )
                }
            }
        }
    }

    fun refreshPermissionStatus() {
        _uiState.update {
            it.copy(hasAllFilesAccess = StoragePermissionHelper.hasAllFilesAccess(appContext))
        }
    }

    fun setStorageMode(mode: StorageMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(storageMode = mode) }
        }
    }

    fun updateVaultRootPath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(vaultRootPath = path) }
        }
    }

    fun updateMaintenancePath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(maintenanceFilePath = path, maintenanceUri = null)
            }
        }
    }

    fun updateHistoryPath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(historyFilePath = path, historyUri = null)
            }
        }
    }

    fun updateRoutinesPath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(routinesFilePath = path, routinesUri = null)
            }
        }
    }

    fun onVaultFolderPicked(uri: Uri) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val resolvedPath = StoragePermissionHelper.resolveTreeUriToPath(uri)
        viewModelScope.launch {
            if (resolvedPath != null) {
                settingsRepository.updateSettings {
                    it.copy(
                        storageMode = StorageMode.EXTERNAL_VAULT,
                        vaultRootPath = resolvedPath
                    )
                }
                _uiState.update { it.copy(snackbarMessage = "Vault folder set to $resolvedPath") }
            } else {
                _uiState.update {
                    it.copy(snackbarMessage = "Could not resolve direct path from folder picker. Enter the full path below.")
                }
            }
        }
    }

    fun onMarkdownFilePicked(kind: VaultFileKind, uri: Uri) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val resolvedPath = StoragePermissionHelper.resolveDocumentUriToPath(uri)
        val uriString = uri.toString()

        viewModelScope.launch {
            settingsRepository.updateSettings { current ->
                when (kind) {
                    VaultFileKind.MAINTENANCE_TASKS -> current.copy(
                        storageMode = StorageMode.EXTERNAL_VAULT,
                        maintenanceFilePath = resolvedPath ?: current.maintenanceFilePath,
                        maintenanceUri = uriString
                    )
                    VaultFileKind.MAINTENANCE_HISTORY -> current.copy(
                        storageMode = StorageMode.EXTERNAL_VAULT,
                        historyFilePath = resolvedPath ?: current.historyFilePath,
                        historyUri = uriString
                    )
                    VaultFileKind.ROUTINES -> current.copy(
                        storageMode = StorageMode.EXTERNAL_VAULT,
                        routinesFilePath = resolvedPath ?: current.routinesFilePath,
                        routinesUri = uriString
                    )
                }
            }
            _uiState.update {
                it.copy(
                    snackbarMessage = if (resolvedPath != null) {
                        "Selected $resolvedPath"
                    } else {
                        "Linked document via system picker"
                    }
                )
            }
        }
    }

    fun setOvertimeSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(overtimeSoundEnabled = enabled) }
        }
    }

    fun setOvertimeVibrate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(overtimeVibrateEnabled = enabled) }
        }
    }

    fun initializeExternalFiles() {
        viewModelScope.launch {
            val res = vaultFileStore.initializeMissingExternalFiles(_uiState.value.settings)
            res.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        snackbarMessage = if (count > 0) {
                            "Created $count starter Markdown file(s) in external vault."
                        } else {
                            "All configured Markdown files already exist."
                        }
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(snackbarMessage = err.localizedMessage ?: "Failed to create files.")
                }
            }
        }
    }

    fun resetSampleVault() {
        viewModelScope.launch {
            vaultFileStore.resetSampleVault()
            _uiState.update { it.copy(snackbarMessage = "Built-in sample Markdown files reset.") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
