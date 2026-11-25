package com.example.ui.routines

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DateUtils
import com.example.data.Routine
import com.example.data.RoutineMarkdownCodec
import com.example.data.RoutineTask
import com.example.data.StorageMode
import com.example.data.VaultFileKind
import com.example.data.VaultFileStore
import com.example.data.VaultReadResult
import com.example.data.VaultSettings
import com.example.data.VaultSettingsRepository
import com.example.data.VaultWriteResult
import com.example.platform.OvertimeAlertPlayer
import com.example.platform.StoragePermissionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RoutineScreenMode {
    INDEX,
    PLAY,
    SUMMARY
}

data class RoutineUiState(
    val isLoading: Boolean = true,
    val screenMode: RoutineScreenMode = RoutineScreenMode.INDEX,
    val routines: List<Routine> = emptyList(),
    val showAllDays: Boolean = false,
    val currentWeekdayLabel: String = DateUtils.currentWeekdayName(),
    val activeRoutine: Routine? = null,
    val currentTaskIndex: Int = 0,
    val timerSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isOvertime: Boolean = false,
    val progressFraction: Float = 1f,
    val summaryRoutineName: String = "",
    val summaryTaskCount: Int = 0,
    val summaryTotalMinutes: Int = 0,
    val storageMode: StorageMode = StorageMode.INTERNAL_SAMPLE,
    val activeSourceLabel: String = "",
    val needsAllFilesAccess: Boolean = false,
    val missingFilePath: String? = null,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
) {
    val currentTask: RoutineTask?
        get() = activeRoutine?.tasks?.getOrNull(currentTaskIndex)
}

class RoutineViewModel(
    private val appContext: Context,
    private val settingsRepository: VaultSettingsRepository,
    private val vaultFileStore: VaultFileStore,
    private val overtimeAlertPlayer: OvertimeAlertPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState())
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    private var currentSettings = VaultSettings()
    private var rawRoutinesMarkdown: String = ""
    private var tickerJob: Job? = null
    private var pausedElapsedSeconds: Int = 0
    private var overtimeAlertFiredStepKey: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                loadRoutines()
            }
        }
        viewModelScope.launch {
            vaultFileStore.fileUpdatedSignal.collect { kind ->
                if (kind == VaultFileKind.ROUTINES) {
                    loadRoutines()
                }
            }
        }
    }

    fun onResumeLifecycle() {
        if (_uiState.value.screenMode == RoutineScreenMode.PLAY && _uiState.value.isRunning) {
            recomputeTimerFromStartTime(triggerAlertIfNewlyOvertime = true)
        } else {
            loadRoutines()
        }
    }

    fun refresh() {
        loadRoutines()
    }

    fun toggleShowAllDays() {
        _uiState.update { it.copy(showAllDays = !it.showAllDays) }
        if (rawRoutinesMarkdown.isNotEmpty()) {
            applyParsedRoutines(rawRoutinesMarkdown, _uiState.value.activeSourceLabel)
        }
    }

    private fun loadRoutines() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    currentWeekdayLabel = DateUtils.currentWeekdayName(),
                    storageMode = currentSettings.storageMode,
                    errorMessage = null,
                    missingFilePath = null
                )
            }

            when (val result = vaultFileStore.readMarkdown(VaultFileKind.ROUTINES, currentSettings)) {
                is VaultReadResult.Success -> {
                    rawRoutinesMarkdown = result.content
                    applyParsedRoutines(result.content, result.displayLocation)
                }
                is VaultReadResult.MissingAllFilesAccess -> {
                    stopTicker()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            routines = emptyList(),
                            activeSourceLabel = result.targetPath,
                            needsAllFilesAccess = true,
                            missingFilePath = null,
                            errorMessage = null
                        )
                    }
                }
                is VaultReadResult.FileNotFound -> {
                    stopTicker()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            routines = emptyList(),
                            activeSourceLabel = result.targetPath,
                            needsAllFilesAccess = !StoragePermissionHelper.hasAllFilesAccess(appContext) &&
                                currentSettings.storageMode == StorageMode.EXTERNAL_VAULT,
                            missingFilePath = result.targetPath,
                            errorMessage = null
                        )
                    }
                }
                is VaultReadResult.Error -> {
                    stopTicker()
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

    private fun applyParsedRoutines(markdown: String, displayLocation: String) {
        val todayRoutines = RoutineMarkdownCodec.parseRoutines(markdown, filterByWeekday = true)
        val displayRoutines = if (_uiState.value.showAllDays) {
            RoutineMarkdownCodec.parseRoutines(markdown, filterByWeekday = false)
        } else {
            todayRoutines
        }

        // Check if any routine already has an active startTime persisted in Markdown
        val activeRoutine = todayRoutines.find { it.hasActiveStartTime }
            ?: displayRoutines.find { it.hasActiveStartTime }

        if (activeRoutine != null) {
            val lastStartedIndex = activeRoutine.tasks.indexOfLast { it.startTimeEpochMs != null }
                .coerceAtLeast(0)
            val wasPausedOnSameTask = !_uiState.value.isRunning &&
                _uiState.value.screenMode == RoutineScreenMode.PLAY &&
                _uiState.value.activeRoutine?.name == activeRoutine.name &&
                _uiState.value.currentTaskIndex == lastStartedIndex

            _uiState.update {
                it.copy(
                    isLoading = false,
                    routines = displayRoutines,
                    activeSourceLabel = displayLocation,
                    needsAllFilesAccess = false,
                    missingFilePath = null,
                    activeRoutine = activeRoutine,
                    currentTaskIndex = lastStartedIndex,
                    screenMode = RoutineScreenMode.PLAY,
                    isRunning = !wasPausedOnSameTask
                )
            }
            if (!wasPausedOnSameTask) {
                recomputeTimerFromStartTime(triggerAlertIfNewlyOvertime = false)
                startTicker()
            }
        } else {
            stopTicker()
            val nextMode = if (_uiState.value.screenMode == RoutineScreenMode.SUMMARY) {
                RoutineScreenMode.SUMMARY
            } else {
                RoutineScreenMode.INDEX
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    routines = displayRoutines,
                    activeSourceLabel = displayLocation,
                    needsAllFilesAccess = false,
                    missingFilePath = null,
                    activeRoutine = if (nextMode == RoutineScreenMode.INDEX) null else it.activeRoutine,
                    screenMode = nextMode,
                    isRunning = false
                )
            }
        }
    }

    fun startRoutine(routine: Routine) {
        // Always run today's active steps if available, otherwise fall back to routine's tasks
        val todayEquivalent = RoutineMarkdownCodec.parseRoutines(rawRoutinesMarkdown, filterByWeekday = true)
            .find { it.name == routine.name } ?: routine
        if (todayEquivalent.tasks.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "No tasks scheduled for today in ${routine.name}.") }
            return
        }

        viewModelScope.launch {
            overtimeAlertFiredStepKey = null
            pausedElapsedSeconds = 0
            val now = System.currentTimeMillis()
            val cleared = RoutineMarkdownCodec.resetRoutineTimestamps(rawRoutinesMarkdown, todayEquivalent.name)
            val updated = RoutineMarkdownCodec.updateTaskStartTime(
                markdown = cleared,
                routineName = todayEquivalent.name,
                taskName = todayEquivalent.tasks.first().name,
                timestampMs = now
            )
            writeRoutinesMarkdown(updated) {
                loadRoutines()
            }
        }
    }

    fun togglePauseResume() {
        val state = _uiState.value
        val routine = state.activeRoutine ?: return
        val task = state.currentTask ?: return

        if (state.isRunning) {
            // Pause: capture elapsed seconds so far
            val start = task.startTimeEpochMs
            pausedElapsedSeconds = if (start != null) {
                ((System.currentTimeMillis() - start) / 1000L).toInt().coerceAtLeast(0)
            } else {
                if (state.isOvertime) task.durationSeconds + state.timerSeconds
                else (task.durationSeconds - state.timerSeconds).coerceAtLeast(0)
            }
            stopTicker()
            _uiState.update { it.copy(isRunning = false) }
        } else {
            // Resume: shift startTime backwards by pausedElapsedSeconds and persist
            val adjustedStartTime = System.currentTimeMillis() - (pausedElapsedSeconds * 1000L)
            viewModelScope.launch {
                val updated = RoutineMarkdownCodec.updateTaskStartTime(
                    markdown = rawRoutinesMarkdown,
                    routineName = routine.name,
                    taskName = task.name,
                    timestampMs = adjustedStartTime
                )
                _uiState.update { it.copy(isRunning = true) }
                writeRoutinesMarkdown(updated) {
                    recomputeTimerFromStartTime(triggerAlertIfNewlyOvertime = false)
                    startTicker()
                }
            }
        }
    }

    fun completeCurrentStep() {
        advanceToNextStep(showSummaryOnFinish = true)
    }

    fun skipCurrentStep() {
        advanceToNextStep(showSummaryOnFinish = true)
    }

    private fun advanceToNextStep(showSummaryOnFinish: Boolean) {
        val state = _uiState.value
        val routine = state.activeRoutine ?: return
        val nextIndex = state.currentTaskIndex + 1

        viewModelScope.launch {
            overtimeAlertFiredStepKey = null
            pausedElapsedSeconds = 0
            if (nextIndex < routine.tasks.size) {
                val nextTask = routine.tasks[nextIndex]
                val updated = RoutineMarkdownCodec.updateTaskStartTime(
                    markdown = rawRoutinesMarkdown,
                    routineName = routine.name,
                    taskName = nextTask.name,
                    timestampMs = System.currentTimeMillis()
                )
                writeRoutinesMarkdown(updated) {
                    loadRoutines()
                }
            } else {
                stopTicker()
                val resetMd = RoutineMarkdownCodec.resetRoutineTimestamps(rawRoutinesMarkdown, routine.name)
                writeRoutinesMarkdown(resetMd) {
                    _uiState.update {
                        it.copy(
                            screenMode = if (showSummaryOnFinish) RoutineScreenMode.SUMMARY else RoutineScreenMode.INDEX,
                            isRunning = false,
                            summaryRoutineName = routine.name,
                            summaryTaskCount = routine.tasks.size,
                            summaryTotalMinutes = routine.totalMinutes
                        )
                    }
                }
            }
        }
    }

    fun closeAndResetRoutine() {
        val routine = _uiState.value.activeRoutine ?: return
        viewModelScope.launch {
            stopTicker()
            overtimeAlertFiredStepKey = null
            pausedElapsedSeconds = 0
            val resetMd = RoutineMarkdownCodec.resetRoutineTimestamps(rawRoutinesMarkdown, routine.name)
            writeRoutinesMarkdown(resetMd) {
                _uiState.update {
                    it.copy(
                        screenMode = RoutineScreenMode.INDEX,
                        activeRoutine = null,
                        isRunning = false
                    )
                }
                loadRoutines()
            }
        }
    }

    fun finishSummary() {
        _uiState.update {
            it.copy(
                screenMode = RoutineScreenMode.INDEX,
                activeRoutine = null,
                isRunning = false
            )
        }
        loadRoutines()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                recomputeTimerFromStartTime(triggerAlertIfNewlyOvertime = true)
                delay(1000L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun recomputeTimerFromStartTime(triggerAlertIfNewlyOvertime: Boolean) {
        val state = _uiState.value
        if (state.screenMode != RoutineScreenMode.PLAY || !state.isRunning) return
        val routine = state.activeRoutine ?: return
        val task = state.currentTask ?: return
        val start = task.startTimeEpochMs ?: return

        val elapsedSec = ((System.currentTimeMillis() - start) / 1000L).toInt().coerceAtLeast(0)
        val isOvertime = elapsedSec > task.durationSeconds
        val displaySeconds = if (isOvertime) {
            elapsedSec - task.durationSeconds
        } else {
            (task.durationSeconds - elapsedSec).coerceAtLeast(0)
        }
        val progress = if (isOvertime || task.durationSeconds <= 0) {
            0f
        } else {
            (displaySeconds.toFloat() / task.durationSeconds.toFloat()).coerceIn(0f, 1f)
        }

        val stepKey = "${routine.name}::${state.currentTaskIndex}::${task.name}"
        if (triggerAlertIfNewlyOvertime && isOvertime && overtimeAlertFiredStepKey != stepKey) {
            overtimeAlertFiredStepKey = stepKey
            overtimeAlertPlayer.triggerOvertimeAlert(
                playSound = currentSettings.overtimeSoundEnabled,
                vibrate = currentSettings.overtimeVibrateEnabled
            )
        }

        _uiState.update {
            it.copy(
                timerSeconds = displaySeconds,
                isOvertime = isOvertime,
                progressFraction = progress
            )
        }
    }

    private suspend fun writeRoutinesMarkdown(updatedContent: String, onSuccess: () -> Unit) {
        when (val res = vaultFileStore.writeMarkdown(VaultFileKind.ROUTINES, currentSettings, updatedContent)) {
            is VaultWriteResult.Success -> {
                rawRoutinesMarkdown = updatedContent
                onSuccess()
            }
            is VaultWriteResult.MissingAllFilesAccess -> {
                _uiState.update {
                    it.copy(
                        needsAllFilesAccess = true,
                        snackbarMessage = "All Files Access is needed to update routines.md."
                    )
                }
            }
            is VaultWriteResult.Error -> {
                _uiState.update { it.copy(snackbarMessage = res.message) }
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
                loadRoutines()
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = err.localizedMessage ?: "Unable to create files.") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }
}
