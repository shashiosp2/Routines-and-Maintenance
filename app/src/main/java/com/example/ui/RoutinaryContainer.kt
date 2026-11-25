package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.VaultFileStore
import com.example.data.VaultSettingsRepository
import com.example.platform.OvertimeAlertPlayer
import com.example.ui.maintenance.MaintenanceViewModel
import com.example.ui.routines.RoutineViewModel
import com.example.ui.settings.SettingsViewModel

class RoutinaryContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settingsRepository = VaultSettingsRepository(appContext)
    val vaultFileStore = VaultFileStore(appContext)
    val overtimeAlertPlayer = OvertimeAlertPlayer(appContext)

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when {
                modelClass.isAssignableFrom(MaintenanceViewModel::class.java) ->
                    MaintenanceViewModel(appContext, settingsRepository, vaultFileStore) as T
                modelClass.isAssignableFrom(RoutineViewModel::class.java) ->
                    RoutineViewModel(appContext, settingsRepository, vaultFileStore, overtimeAlertPlayer) as T
                modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                    SettingsViewModel(appContext, settingsRepository, vaultFileStore) as T
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
