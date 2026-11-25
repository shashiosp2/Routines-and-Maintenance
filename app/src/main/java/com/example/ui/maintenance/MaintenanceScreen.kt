package com.example.ui.maintenance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DateUtils
import com.example.data.MaintenanceHistoryEntry
import com.example.data.MaintenanceSortColumn
import com.example.data.MaintenanceTask
import com.example.ui.components.AllFilesAccessRequiredCard
import com.example.ui.components.MissingMarkdownFileCard
import com.example.ui.theme.LocalRoutinaryColors

@Composable
fun MaintenanceScreen(
    uiState: MaintenanceUiState,
    showHistorySheet: Boolean,
    onDismissHistorySheet: () -> Unit,
    onRefresh: () -> Unit,
    onSortClick: (MaintenanceSortColumn) -> Unit,
    onUpdatePriority: (MaintenanceTask, Int) -> Unit,
    onArmDone: (MaintenanceTask) -> Unit,
    onCancelDone: () -> Unit,
    onConfirmDone: (MaintenanceTask) -> Unit,
    onAddTask: (priority: Int, group: String, name: String, frequency: Int, customDue: String?) -> Unit,
    onInitializeStarterFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingPriorityTask by remember { mutableStateOf<MaintenanceTask?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 720.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1040.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Status & Source Strip
                MaintenanceHeaderStrip(
                    todayDate = uiState.todayDate,
                    sourceLabel = uiState.activeSourceLabel,
                    taskCount = uiState.tasks.size,
                    overdueCount = uiState.tasks.count { it.daysToDue < 0 }
                )

                // Sort Chips Row
                SortChipBar(
                    selectedColumn = uiState.sortColumn,
                    sortAscending = uiState.sortAscending,
                    onSortClick = onSortClick
                )

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
                            .testTag("maintenance_task_list"),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = uiState.errorMessage,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.tasks.isEmpty() && !uiState.needsAllFilesAccess && uiState.missingFilePath == null) {
                            item(key = "empty_tasks") {
                                EmptyMaintenanceState(onAddClick = { showAddSheet = true })
                            }
                        }

                        if (isWide && uiState.tasks.isNotEmpty()) {
                            item(key = "wide_header") {
                                WideTableHeaderRow()
                            }
                        }

                        items(
                            items = uiState.tasks,
                            key = { "${it.lineIndex}_${it.name}" }
                        ) { task ->
                            val isConfirming = uiState.confirmingLineIndex == task.lineIndex
                            if (isWide) {
                                MaintenanceWideRow(
                                    task = task,
                                    isConfirming = isConfirming,
                                    onPriorityClick = { editingPriorityTask = task },
                                    onArmDone = { onArmDone(task) },
                                    onCancelDone = onCancelDone,
                                    onConfirmDone = { onConfirmDone(task) }
                                )
                            } else {
                                MaintenanceTaskCard(
                                    task = task,
                                    isConfirming = isConfirming,
                                    onPriorityClick = { editingPriorityTask = task },
                                    onArmDone = { onArmDone(task) },
                                    onCancelDone = onCancelDone,
                                    onConfirmDone = { onConfirmDone(task) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_task_fab")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.cd_add_task)
            )
        }
    }

    editingPriorityTask?.let { task ->
        PriorityEditDialog(
            task = task,
            onDismiss = { editingPriorityTask = null },
            onSave = { newPriority ->
                editingPriorityTask = null
                onUpdatePriority(task, newPriority)
            }
        )
    }

    if (showAddSheet) {
        AddTaskBottomSheet(
            todayDate = uiState.todayDate,
            onDismiss = { showAddSheet = false },
            onAdd = { priority, group, name, frequency, customDue ->
                showAddSheet = false
                onAddTask(priority, group, name, frequency, customDue)
            }
        )
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            entries = uiState.historyEntries,
            onDismiss = onDismissHistorySheet
        )
    }
}

@Composable
private fun MaintenanceHeaderStrip(
    todayDate: String,
    sourceLabel: String,
    taskCount: Int,
    overdueCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Today: $todayDate",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (overdueCount > 0) {
                    Surface(
                        color = LocalRoutinaryColors.current.overdueBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$overdueCount overdue",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalRoutinaryColors.current.overdueAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$taskCount tasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        if (sourceLabel.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SortChipBar(
    selectedColumn: MaintenanceSortColumn,
    sortAscending: Boolean,
    onSortClick: (MaintenanceSortColumn) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MaintenanceSortColumn.entries.forEach { col ->
            val selected = col == selectedColumn
            FilterChip(
                selected = selected,
                onClick = { onSortClick(col) },
                modifier = Modifier.testTag("sort_chip_${col.name.lowercase()}"),
                label = { Text(col.label) },
                trailingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = if (sortAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = if (sortAscending) "Ascending" else "Descending",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun urgencyColorsFor(daysToDue: Int): Triple<Color, Color, Color> {
    val semantic = LocalRoutinaryColors.current
    return when {
        daysToDue < 0 -> Triple(semantic.overdueBg, semantic.overdueBorder, semantic.overdueAccent)
        daysToDue == 0 -> Triple(semantic.dueTodayBg, semantic.dueTodayBorder, semantic.dueTodayAccent)
        daysToDue == 1 -> Triple(semantic.dueTomorrowBg, semantic.dueTomorrowBorder, semantic.dueTomorrowAccent)
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun daysDueLabel(daysToDue: Int): String = when {
    daysToDue == 999 -> "—"
    daysToDue < 0 -> "${-daysToDue}d overdue"
    daysToDue == 0 -> "Due today"
    daysToDue == 1 -> "Due tomorrow"
    else -> "In ${daysToDue}d"
}

@Composable
private fun MaintenanceTaskCard(
    task: MaintenanceTask,
    isConfirming: Boolean,
    onPriorityClick: () -> Unit,
    onArmDone: () -> Unit,
    onCancelDone: () -> Unit,
    onConfirmDone: () -> Unit
) {
    val (bgColor, borderColor, accentColor) = urgencyColorsFor(task.daysToDue)
    val animatedBg by animateColorAsState(targetValue = bgColor, animationSpec = spring(), label = "cardBg")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("maintenance_task_card_${task.lineIndex}"),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clickable(onClick = onPriorityClick)
                            .testTag("priority_badge_${task.lineIndex}")
                    ) {
                        Text(
                            text = "P${task.priority}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = task.group,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = daysDueLabel(task.daysToDue),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Every ${task.frequencyDays}d  •  Next: ${task.nextDue}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Last done: ${task.lastDone.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DoneConfirmationButtons(
                    lineIndex = task.lineIndex,
                    isConfirming = isConfirming,
                    onArmDone = onArmDone,
                    onCancelDone = onCancelDone,
                    onConfirmDone = onConfirmDone
                )
            }
        }
    }
}

@Composable
private fun WideTableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Prio", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp))
        Text("Group", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(100.dp))
        Text("Task", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("Freq", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp))
        Text("Due", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(96.dp))
        Text("Next Due", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(96.dp))
        Text("Last Done", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(96.dp))
        Text("Action", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(148.dp))
    }
}

@Composable
private fun MaintenanceWideRow(
    task: MaintenanceTask,
    isConfirming: Boolean,
    onPriorityClick: () -> Unit,
    onArmDone: () -> Unit,
    onCancelDone: () -> Unit,
    onConfirmDone: () -> Unit
) {
    val (bgColor, borderColor, accentColor) = urgencyColorsFor(task.daysToDue)
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(48.dp)
                    .clickable(onClick = onPriorityClick)
            ) {
                Text(
                    text = "${task.priority}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.group,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Text(
                text = "${task.frequencyDays}d",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(56.dp)
            )
            Text(
                text = daysDueLabel(task.daysToDue),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.width(96.dp)
            )
            Text(
                text = task.nextDue,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(96.dp)
            )
            Text(
                text = task.lastDone,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(96.dp)
            )
            Box(modifier = Modifier.width(148.dp), contentAlignment = Alignment.CenterEnd) {
                DoneConfirmationButtons(
                    lineIndex = task.lineIndex,
                    isConfirming = isConfirming,
                    onArmDone = onArmDone,
                    onCancelDone = onCancelDone,
                    onConfirmDone = onConfirmDone
                )
            }
        }
    }
}

@Composable
private fun DoneConfirmationButtons(
    lineIndex: Int,
    isConfirming: Boolean,
    onArmDone: () -> Unit,
    onCancelDone: () -> Unit,
    onConfirmDone: () -> Unit
) {
    if (isConfirming) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onCancelDone,
                modifier = Modifier.testTag("cancel_done_button_$lineIndex")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel completion"
                )
            }
            Button(
                onClick = onConfirmDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.testTag("sure_button_$lineIndex")
            ) {
                Text("Sure?")
            }
        }
    } else {
        OutlinedButton(
            onClick = onArmDone,
            modifier = Modifier.testTag("done_button_$lineIndex")
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Done")
        }
    }
}

@Composable
private fun EmptyMaintenanceState(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.TaskAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No maintenance tasks yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add your first recurring task or link an existing Obsidian tasks Markdown file in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddClick) {
                Text("Add Task")
            }
        }
    }
}

@Composable
private fun PriorityEditDialog(
    task: MaintenanceTask,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var priorityText by remember(task) { mutableStateOf(task.priority.toString()) }
    val currentVal = priorityText.toIntOrNull() ?: task.priority

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Priority") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(task.name, style = MaterialTheme.typography.bodyMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { priorityText = (currentVal - 1).coerceAtLeast(0).toString() },
                        modifier = Modifier.testTag("priority_decrement_button")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease priority")
                    }
                    OutlinedTextField(
                        value = priorityText,
                        onValueChange = { priorityText = it.filter { ch -> ch.isDigit() || ch == '-' } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Priority") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("priority_input")
                    )
                    IconButton(
                        onClick = { priorityText = (currentVal + 1).toString() },
                        modifier = Modifier.testTag("priority_increment_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Increase priority")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(priorityText.toIntOrNull() ?: task.priority) },
                modifier = Modifier.testTag("save_priority_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskBottomSheet(
    todayDate: String,
    onDismiss: () -> Unit,
    onAdd: (priority: Int, group: String, name: String, frequency: Int, customDue: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var priorityText by remember { mutableStateOf("1") }
    var groupText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var frequencyText by remember { mutableStateOf("7") }
    var customNextDue by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "New Maintenance Task",
                style = MaterialTheme.typography.headlineMedium
            )
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Task Name *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_task_name_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = groupText,
                    onValueChange = { groupText = it },
                    label = { Text("Group") },
                    placeholder = { Text("Cleaning") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("new_task_group_input")
                )
                OutlinedTextField(
                    value = priorityText,
                    onValueChange = { priorityText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Priority") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("new_task_priority_input")
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = frequencyText,
                    onValueChange = { frequencyText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Frequency (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_task_frequency_input")
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("new_task_due_date_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customNextDue ?: "Auto Next Due",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val freq = frequencyText.toIntOrNull() ?: 0
            val previewDue = customNextDue ?: DateUtils.addDays(todayDate, if (freq > 0) freq else 1)
            Text(
                text = "Next due will be set to $previewDue • Last done: $todayDate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    onAdd(
                        priorityText.toIntOrNull() ?: 0,
                        groupText,
                        nameText,
                        frequencyText.toIntOrNull() ?: 0,
                        customNextDue
                    )
                },
                enabled = nameText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_new_task_button")
            ) {
                Text("Add to Markdown")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        customNextDue = DateUtils.millisToDateString(millis)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryBottomSheet(
    entries: List<MaintenanceHistoryEntry>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Completion History",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Text(
                    text = "No completed maintenance entries recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .testTag("history_entry_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${entry.group} • Every ${entry.frequencyDays}d",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = entry.lastDone,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Next: ${entry.nextDue}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}
