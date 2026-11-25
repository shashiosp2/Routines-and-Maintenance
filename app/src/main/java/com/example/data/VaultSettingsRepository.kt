package com.example.data

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "routinary_vault_prefs")

enum class StorageMode {
    INTERNAL_SAMPLE,
    EXTERNAL_VAULT
}

data class VaultSettings(
    val storageMode: StorageMode = StorageMode.INTERNAL_SAMPLE,
    val vaultRootPath: String = "${Environment.getExternalStorageDirectory().absolutePath}/Documents/Obsidian",
    val maintenanceFilePath: String = "notes/maintenance-tasks-01.md",
    val historyFilePath: String = "notes/maintenance-tasks-01.history.md",
    val routinesFilePath: String = "notes/routines.md",
    val maintenanceUri: String? = null,
    val historyUri: String? = null,
    val routinesUri: String? = null,
    val overtimeSoundEnabled: Boolean = true,
    val overtimeVibrateEnabled: Boolean = true
)

class VaultSettingsRepository(private val context: Context) {

    private object Keys {
        val STORAGE_MODE = stringPreferencesKey("storage_mode")
        val VAULT_ROOT_PATH = stringPreferencesKey("vault_root_path")
        val MAINTENANCE_PATH = stringPreferencesKey("maintenance_file_path")
        val HISTORY_PATH = stringPreferencesKey("history_file_path")
        val ROUTINES_PATH = stringPreferencesKey("routines_file_path")
        val MAINTENANCE_URI = stringPreferencesKey("maintenance_uri")
        val HISTORY_URI = stringPreferencesKey("history_uri")
        val ROUTINES_URI = stringPreferencesKey("routines_uri")
        val OVERTIME_SOUND = booleanPreferencesKey("overtime_sound_enabled")
        val OVERTIME_VIBRATE = booleanPreferencesKey("overtime_vibrate_enabled")
    }

    val settingsFlow: Flow<VaultSettings> = context.vaultDataStore.data.map { prefs ->
        val defaultSettings = VaultSettings()
        val mode = prefs[Keys.STORAGE_MODE]?.let {
            runCatching { StorageMode.valueOf(it) }.getOrNull()
        } ?: StorageMode.INTERNAL_SAMPLE

        VaultSettings(
            storageMode = mode,
            vaultRootPath = prefs[Keys.VAULT_ROOT_PATH] ?: defaultSettings.vaultRootPath,
            maintenanceFilePath = prefs[Keys.MAINTENANCE_PATH] ?: defaultSettings.maintenanceFilePath,
            historyFilePath = prefs[Keys.HISTORY_PATH] ?: defaultSettings.historyFilePath,
            routinesFilePath = prefs[Keys.ROUTINES_PATH] ?: defaultSettings.routinesFilePath,
            maintenanceUri = prefs[Keys.MAINTENANCE_URI]?.takeIf { it.isNotBlank() },
            historyUri = prefs[Keys.HISTORY_URI]?.takeIf { it.isNotBlank() },
            routinesUri = prefs[Keys.ROUTINES_URI]?.takeIf { it.isNotBlank() },
            overtimeSoundEnabled = prefs[Keys.OVERTIME_SOUND] ?: true,
            overtimeVibrateEnabled = prefs[Keys.OVERTIME_VIBRATE] ?: true
        )
    }

    suspend fun updateSettings(transform: (VaultSettings) -> VaultSettings) {
        context.vaultDataStore.edit { prefs ->
            val currentMode = prefs[Keys.STORAGE_MODE]?.let {
                runCatching { StorageMode.valueOf(it) }.getOrNull()
            } ?: StorageMode.INTERNAL_SAMPLE
            val defaults = VaultSettings()
            val current = VaultSettings(
                storageMode = currentMode,
                vaultRootPath = prefs[Keys.VAULT_ROOT_PATH] ?: defaults.vaultRootPath,
                maintenanceFilePath = prefs[Keys.MAINTENANCE_PATH] ?: defaults.maintenanceFilePath,
                historyFilePath = prefs[Keys.HISTORY_PATH] ?: defaults.historyFilePath,
                routinesFilePath = prefs[Keys.ROUTINES_PATH] ?: defaults.routinesFilePath,
                maintenanceUri = prefs[Keys.MAINTENANCE_URI]?.takeIf { it.isNotBlank() },
                historyUri = prefs[Keys.HISTORY_URI]?.takeIf { it.isNotBlank() },
                routinesUri = prefs[Keys.ROUTINES_URI]?.takeIf { it.isNotBlank() },
                overtimeSoundEnabled = prefs[Keys.OVERTIME_SOUND] ?: true,
                overtimeVibrateEnabled = prefs[Keys.OVERTIME_VIBRATE] ?: true
            )
            val updated = transform(current)
            prefs[Keys.STORAGE_MODE] = updated.storageMode.name
            prefs[Keys.VAULT_ROOT_PATH] = updated.vaultRootPath
            prefs[Keys.MAINTENANCE_PATH] = updated.maintenanceFilePath
            prefs[Keys.HISTORY_PATH] = updated.historyFilePath
            prefs[Keys.ROUTINES_PATH] = updated.routinesFilePath
            if (updated.maintenanceUri != null) prefs[Keys.MAINTENANCE_URI] = updated.maintenanceUri else prefs.remove(Keys.MAINTENANCE_URI)
            if (updated.historyUri != null) prefs[Keys.HISTORY_URI] = updated.historyUri else prefs.remove(Keys.HISTORY_URI)
            if (updated.routinesUri != null) prefs[Keys.ROUTINES_URI] = updated.routinesUri else prefs.remove(Keys.ROUTINES_URI)
            prefs[Keys.OVERTIME_SOUND] = updated.overtimeSoundEnabled
            prefs[Keys.OVERTIME_VIBRATE] = updated.overtimeVibrateEnabled
        }
    }
}
