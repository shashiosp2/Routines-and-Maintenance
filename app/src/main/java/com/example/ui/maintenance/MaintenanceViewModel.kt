package com.example.ui.maintenance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DateUtils
import com.example.data.MaintenanceHistoryEntry
import com.example.data.MaintenanceMarkdownCodec
import com.example.data.MaintenanceSortColumn
import com.example.data.MaintenanceTask
import com.example.data.StorageMode
import com.example.data.VaultFileKind
import com.example.data.VaultFileStore
import com.example.data.VaultReadResult
import com.example.data.VaultSettings
import com.example.data.VaultSettingsRepository
import com.example.data.VaultWriteResult
import com.example.platform.StoragePermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaintenanceUiState(
    val isLoading: Boolean = true,
    val todayDate: String = DateUtils.todayDateString(),
    val tasks: List<MaintenanceTask> = emptyList(),
    val historyEntries: List<MaintenanceHistoryEntry> = emptyList(),
    val sortColumn: MaintenanceSortColumn = MaintenanceSortColumn.PRIORITY,
    val sortAscending: Boolean = true,
    val confirmingLineIndex: Int? = null,
    val storageMode: StorageMode = StorageMode.INTERNAL_SAMPLE,
    val activeSourceLabel: String = "",
    val needsAllFilesAccess: Boolean = false,
    val missingFilePath: String? = null,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class MaintenanceViewModel(
    private val appContext: Context,
    private val settingsRepository: VaultSettingsRepository,
    private val vaultFileStore: VaultFileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaintenanceUiState())
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    private var currentSettings = VaultSettings()
    private var rawTasksMarkdown: String = ""

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                loadData()
            }
        }
        viewModelScope.launch {
            vaultFileStore.fileUpdatedSignal.collect { kind ->
                if (kind == VaultFileKind.MAINTENANCE_TASKS || kind == VaultFileKind.MAINTENANCE_HISTORY) {
                    loadData()
                }
            }
        }
    }

    fun onResumeLifecycle() {
        loadData()
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val today = DateUtils.todayDateString()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    todayDate = today,
                    storageMode = currentSettings.storageMode,
                    errorMessage = null,
                    missingFilePath = null
                )
            }

            when (val result = vaultFileStore.readMarkdown(VaultFileKind.MAINTENANCE_TASKS, currentSettings)) {
                is VaultReadResult.Success -> {
                    rawTasksMarkdown = result.content
                    val parsed = MaintenanceMarkdownCodec.parseTasks(result.content, today)
                    val sorted = sortTasks(parsed, _uiState.value.sortColumn, _uiState.value.sortAscending)
                    val history = loadHistoryInternal()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = sorted,
                            historyEntries = history,
                            activeSourceLabel = result.displayLocation,
                            needsAllFilesAccess = false,
                            missingFilePath = null,
                            errorMessage = null,
                            confirmingLineIndex = null
                        )
                    }
                }
                is VaultReadResult.MissingAllFilesAccess -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = emptyList(),
                            activeSourceLabel = result.targetPath,
                            needsAllFilesAccess = true,
                            missingFilePath = null,
                            errorMessage = null
                        )
                    }
                }
                is VaultReadResult.FileNotFound -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = emptyList(),
                            activeSourceLabel = result.targetPath,
                            needsAllFilesAccess = !StoragePermissionHelper.hasAllFilesAccess(appContext) &&
                                currentSettings.storageMode == StorageMode.EXTERNAL_VAULT,
                            missingFilePath = result.targetPath,
                            errorMessage = null
                        )
                    }
                }
                is VaultReadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadHistoryInternal(): List<MaintenanceHistoryEntry> {
        return when (val histResult = vaultFileStore.readMarkdown(VaultFileKind.MAINTENANCE_HISTORY, currentSettings)) {
            is VaultReadResult.Success -> MaintenanceMarkdownCodec.parseHistory(histResult.content)
            else -> emptyList()
        }
    }

    fun onSortColumnClick(column: MaintenanceSortColumn) {
        _uiState.update { state ->
            val ascending = if (state.sortColumn == column) !state.sortAscending else true
            val sorted = sortTasks(state.tasks, column, ascending)
            state.copy(
                sortColumn = column,
                sortAscending = ascending,
                tasks = sorted,
                confirmingLineIndex = null
            )
        }
    }

    private fun sortTasks(
        tasks: List<MaintenanceTask>,
        column: MaintenanceSortColumn,
        ascending: Boolean
    ): List<MaintenanceTask> {
        val comparator = when (column) {
            MaintenanceSortColumn.PRIORITY -> compareBy<MaintenanceTask> { it.priority }.thenBy { it.daysToDue }
            MaintenanceSortColumn.GROUP -> compareBy<MaintenanceTask> { it.group.lowercase() }.thenBy { it.name.lowercase() }
            MaintenanceSortColumn.NAME -> compareBy<MaintenanceTask> { it.name.lowercase() }
            MaintenanceSortColumn.FREQUENCY -> compareBy<MaintenanceTask> { it.frequencyDays }
            MaintenanceSortColumn.DAYS_DUE -> compareBy<MaintenanceTask> { it.daysToDue }.thenBy { it.priority }
            MaintenanceSortColumn.NEXT_DUE -> compareBy<MaintenanceTask> { it.nextDue }
            MaintenanceSortColumn.LAST_DONE -> compareBy<MaintenanceTask> { it.lastDone }
        }
        return if (ascending) tasks.sortedWith(comparator) else tasks.sortedWith(comparator.reversed())
    }

    fun armDoneConfirmation(task: MaintenanceTask) {
        _uiState.update { it.copy(confirmingLineIndex = task.lineIndex) }
    }

    fun cancelDoneConfirmation() {
        _uiState.update { it.copy(confirmingLineIndex = null) }
    }

    fun confirmDone(task: MaintenanceTask) {
        viewModelScope.launch {
            val today = DateUtils.todayDateString()
            val (updatedTasksMd, historyRow) = MaintenanceMarkdownCodec.markTaskDone(
                markdown = rawTasksMarkdown,
                task = task,
                today = today
            )

            when (val writeResult = vaultFileStore.writeMarkdown(VaultFileKind.MAINTENANCE_TASKS, currentSettings, updatedTasksMd)) {
                is VaultWriteResult.Success -> {
                    val existingHistory = when (val hr = vaultFileStore.readMarkdown(VaultFileKind.MAINTENANCE_HISTORY, currentSettings)) {
                        is VaultReadResult.Success -> hr.content
                        else -> null
                    }
                    val updatedHistoryMd = MaintenanceMarkdownCodec.appendHistoryEntry(existingHistory, historyRow)
                    vaultFileStore.writeMarkdown(VaultFileKind.MAINTENANCE_HISTORY, currentSettings, updatedHistoryMd)

                    _uiState.update {
                        it.copy(
                            confirmingLineIndex = null,
                            snackbarMessage = "Marked \"${task.name}\" as done!"
                        )
                    }
                    loadData()
                }
                is VaultWriteResult.MissingAllFilesAccess -> {
                    _uiState.update {
                        it.copy(
                            confirmingLineIndex = null,
                            needsAllFilesAccess = true,
                            snackbarMessage = "All Files Access is needed to update Markdown files."
                        )
                    }
                }
                is VaultWriteResult.Error -> {
                    _uiState.update {
                        it.copy(
                            confirmingLineIndex = null,
                            snackbarMessage = writeResult.message
                        )
                    }
                }
            }
        }
    }

    fun updatePriority(task: MaintenanceTask, newPriority: Int) {
        if (newPriority == task.priority) return
        viewModelScope.launch {
            val updatedMd = MaintenanceMarkdownCodec.updateTaskPriority(rawTasksMarkdown, task, newPriority)
            when (val writeResult = vaultFileStore.writeMarkdown(VaultFileKind.MAINTENANCE_TASKS, currentSettings, updatedMd)) {
                is VaultWriteResult.Success -> {
                    _uiState.update { it.copy(snackbarMessage = "Updated priority for ${task.name}") }
                    loadData()
                }
                is VaultWriteResult.MissingAllFilesAccess -> {
                    _uiState.update {
                        it.copy(
                            needsAllFilesAccess = true,
                            snackbarMessage = "All Files Access is needed to update priority."
                        )
                    }
                }
                is VaultWriteResult.Error -> {
                    _uiState.update { it.copy(snackbarMessage = writeResult.message) }
                }
            }
        }
    }

    fun addNewTask(
        priority: Int,
        group: String,
        name: String,
        frequencyDays: Int,
        customNextDue: String?
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Task name is required.") }
            return
        }
        viewModelScope.launch {
            val today = DateUtils.todayDateString()
            val updatedMd = MaintenanceMarkdownCodec.appendNewTask(
                markdown = rawTasksMarkdown,
                priority = priority,
                group = group,
                name = cleanName,
                frequencyDays = frequencyDays.coerceAtLeast(0),
                customNextDue = customNextDue,
                today = today
            )
            when (val writeResult = vaultFileStore.writeMarkdown(VaultFileKind.MAINTENANCE_TASKS, currentSettings, updatedMd)) {
                is VaultWriteResult.Success -> {
                    _uiState.update { it.copy(snackbarMessage = "Added \"$cleanName\"") }
                    loadData()
                }
                is VaultWriteResult.MissingAllFilesAccess -> {
                    _uiState.update {
                        it.copy(
                            needsAllFilesAccess = true,
                            snackbarMessage = "All Files Access is needed to add tasks."
                        )
                    }
                }
                is VaultWriteResult.Error -> {
                    _uiState.update { it.copy(snackbarMessage = writeResult.message) }
                }
            }
        }
    }

    fun initializeMissingExternalFiles() {
        viewModelScope.launch {
            val result = vaultFileStore.initializeMissingExternalFiles(currentSettings)
            result.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        snackbarMessage = if (count > 0) "Created starter Markdown files." else "Files already exist."
                    )
                }
                loadData()
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = err.localizedMessage ?: "Unable to create files.") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
