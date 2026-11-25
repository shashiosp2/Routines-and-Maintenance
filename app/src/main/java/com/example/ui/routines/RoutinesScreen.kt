package com.example.ui.routines

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DateUtils
import com.example.data.Routine
import com.example.ui.components.AllFilesAccessRequiredCard
import com.example.ui.components.MissingMarkdownFileCard
import com.example.ui.theme.LocalRoutinaryColors

@Composable
fun RoutinesScreen(
    uiState: RoutineUiState,
    onRefresh: () -> Unit,
    onToggleShowAllDays: () -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onTogglePauseResume: () -> Unit,
    onSkipStep: () -> Unit,
    onCompleteStep: () -> Unit,
    onCloseAndReset: () -> Unit,
    onFinishSummary: () -> Unit,
    onInitializeStarterFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.screenMode == RoutineScreenMode.PLAY) {
        BackHandler { onCloseAndReset() }
    } else if (uiState.screenMode == RoutineScreenMode.SUMMARY) {
        BackHandler { onFinishSummary() }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = uiState.screenMode,
            label = "routine_mode_transition",
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) { mode ->
            when (mode) {
                RoutineScreenMode.INDEX -> RoutineIndexContent(
                    uiState = uiState,
                    onRefresh = onRefresh,
                    onToggleShowAllDays = onToggleShowAllDays,
                    onStartRoutine = onStartRoutine,
                    onInitializeStarterFiles = onInitializeStarterFiles,
                    onOpenSettings = onOpenSettings
                )
                RoutineScreenMode.PLAY -> RoutinePlayContent(
                    uiState = uiState,
                    onTogglePauseResume = onTogglePauseResume,
                    onSkipStep = onSkipStep,
                    onCompleteStep = onCompleteStep,
                    onCloseAndReset = onCloseAndReset
                )
                RoutineScreenMode.SUMMARY -> RoutineSummaryContent(
                    routineName = uiState.summaryRoutineName,
                    taskCount = uiState.summaryTaskCount,
                    totalMinutes = uiState.summaryTotalMinutes,
                    onDone = onFinishSummary
                )
            }
        }
    }
}

@Composable
private fun RoutineIndexContent(
    uiState: RoutineUiState,
    onRefresh: () -> Unit,
    onToggleShowAllDays: () -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onInitializeStarterFiles: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Today,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.currentWeekdayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.activeSourceLabel.isNotBlank()) {
                    Text(
                        text = uiState.activeSourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FilterChip(
                selected = uiState.showAllDays,
                onClick = onToggleShowAllDays,
                label = { Text(if (uiState.showAllDays) "All Days" else "Today Only") },
                modifier = Modifier.testTag("toggle_all_days_chip")
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("routines_index_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.needsAllFilesAccess) {
                    item(key = "perm_banner") {
                        AllFilesAccessRequiredCard(
                            targetPath = uiState.activeSourceLabel,
                            onPermissionReturned = onRefresh,
                            onOpenSettings = onOpenSettings
                        )
                    }
                } else if (uiState.missingFilePath != null) {
                    item(key = "missing_banner") {
                        MissingMarkdownFileCard(
                            targetPath = uiState.missingFilePath,
                            onInitializeStarterFiles = onInitializeStarterFiles,
                            onOpenSettings = onOpenSettings
                        )
                    }
                } else if (uiState.errorMessage != null) {
                    item(key = "error_banner") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                if (uiState.routines.isEmpty() && !uiState.needsAllFilesAccess && uiState.missingFilePath == null) {
                    item(key = "empty_routines") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No routines scheduled for ${uiState.currentWeekdayLabel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Toggle \"All Days\" above or link your routines.md file in Settings.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(
                    items = uiState.routines,
                    key = { it.name }
                ) { routine ->
                    RoutineIndexCard(
                        routine = routine,
                        onPlayClick = { onStartRoutine(routine) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineIndexCard(
    routine: Routine,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
            .testTag("routine_card_${routine.name}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${routine.tasks.size} tasks • ${routine.totalMinutes} mins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = routine.tasks.take(3).joinToString(" → ") { it.name } +
                        if (routine.tasks.size > 3) " …" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("play_routine_button_${routine.name}"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_routine),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun RoutinePlayContent(
    uiState: RoutineUiState,
    onTogglePauseResume: () -> Unit,
    onSkipStep: () -> Unit,
    onCompleteStep: () -> Unit,
    onCloseAndReset: () -> Unit
) {
    val routine = uiState.activeRoutine ?: return
    val task = uiState.currentTask ?: return
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progressFraction,
        animationSpec = tween(durationMillis = 350),
        label = "routine_progress"
    )
    val timerColor = if (uiState.isOvertime) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // Top Play Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onCloseAndReset,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("routine_close_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close_routine)
                )
            }

            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(
                    onClick = onTogglePauseResume,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("routine_pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            if (uiState.isRunning) R.string.cd_pause_routine else R.string.cd_resume_routine
                        )
                    )
                }
                FilledTonalIconButton(
                    onClick = onSkipStep,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("routine_skip_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.cd_skip_step)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Timer Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(
                width = if (uiState.isOvertime) 1.5.dp else 1.dp,
                color = if (uiState.isOvertime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                timerColor.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val scheduledLabel = if (task.durationSeconds >= 60) {
                    "${task.durationSeconds / 60} min scheduled"
                } else {
                    "${task.durationSeconds} sec scheduled"
                }
                Text(
                    text = if (uiState.isOvertime) "$scheduledLabel • OVERTIME" else scheduledLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.isOvertime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = task.name,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("routine_active_task_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                val formattedTimer = if (uiState.isOvertime) {
                    "+${DateUtils.formatDurationMmSs(uiState.timerSeconds)}"
                } else {
                    DateUtils.formatDurationMmSs(uiState.timerSeconds)
                }

                Text(
                    text = formattedTimer,
                    style = MaterialTheme.typography.displayLarge,
                    color = timerColor,
                    modifier = Modifier.testTag("routine_timer_text")
                )

                if (!uiState.isRunning) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = timerColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onCompleteStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("complete_step_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isOvertime) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = if (uiState.currentTaskIndex == routine.tasks.lastIndex) {
                            "FINISH ROUTINE"
                        } else {
                            "COMPLETE STEP"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Step ${uiState.currentTaskIndex + 1} of ${routine.tasks.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Task Queue List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("routine_step_queue"),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(routine.tasks) { index, item ->
                val isPast = index < uiState.currentTaskIndex
                val isCurrent = index == uiState.currentTaskIndex
                val semantic = LocalRoutinaryColors.current

                Surface(
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isPast) 0.45f else 1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPast) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = when {
                                    isPast -> semantic.success
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        Text(
                            text = DateUtils.formatDurationMmSs(item.durationSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        }
    }
}

@Composable
private fun RoutineSummaryContent(
    routineName: String,
    taskCount: Int,
    totalMinutes: Int,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Amazing job!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("routine_summary_title")
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "You have completed $routineName.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TASKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = taskCount.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${totalMinutes}m",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("routine_summary_done_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "DONE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
