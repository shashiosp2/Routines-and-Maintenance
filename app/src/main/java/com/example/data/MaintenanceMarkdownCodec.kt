package com.example.data

data class MaintenanceTask(
    val priority: Int,
    val group: String,
    val name: String,
    val frequencyDays: Int,
    val nextDue: String,
    val lastDone: String,
    val lineIndex: Int,
    val daysToDue: Int
)

data class MaintenanceHistoryEntry(
    val priority: Int,
    val group: String,
    val name: String,
    val frequencyDays: Int,
    val nextDue: String,
    val lastDone: String,
    val lineIndex: Int
)

enum class MaintenanceSortColumn(val label: String) {
    PRIORITY("Priority"),
    GROUP("Group"),
    NAME("Task"),
    FREQUENCY("Frequency"),
    DAYS_DUE("Days Due"),
    NEXT_DUE("Next Due"),
    LAST_DONE("Last Done")
}

object MaintenanceMarkdownCodec {

    private data class TableLayout(
        val headerLineIndex: Int,
        val separatorLineIndex: Int,
        val hasPriorityColumn: Boolean
    )

    private fun detectTableLayout(lines: List<String>): TableLayout? {
        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("|") && trimmed.contains("|")) {
                val lower = trimmed.lowercase()
                if (lower.contains("group") && lower.contains("task")) {
                    val hasPriority = lower.contains("priority")
                    val sepIdx = if (i + 1 < lines.size && lines[i + 1].trim().startsWith("|")) i + 1 else i
                    return TableLayout(
                        headerLineIndex = i,
                        separatorLineIndex = sepIdx,
                        hasPriorityColumn = hasPriority
                    )
                }
            }
        }
        return null
    }

    fun parseTasks(markdown: String, today: String = DateUtils.todayDateString()): List<MaintenanceTask> {
        val lines = markdown.lines()
        val layout = detectTableLayout(lines) ?: return emptyList()
        val tasks = mutableListOf<MaintenanceTask>()

        for (i in (layout.separatorLineIndex + 1) until lines.size) {
            val trimmed = lines[i].trim()
            if (!trimmed.startsWith("|") || !trimmed.contains("|")) continue

            val parts = trimmed.split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (layout.hasPriorityColumn && parts.size >= 6) {
                val nextDue = parts[4]
                tasks.add(
                    MaintenanceTask(
                        priority = parts[0].toIntOrNull() ?: 0,
                        group = parts[1],
                        name = parts[2],
                        frequencyDays = parts[3].toIntOrNull() ?: 0,
                        nextDue = nextDue,
                        lastDone = parts[5],
                        lineIndex = i,
                        daysToDue = DateUtils.daysBetween(today, nextDue)
                    )
                )
            } else if (!layout.hasPriorityColumn && parts.size >= 5) {
                val nextDue = parts[3]
                tasks.add(
                    MaintenanceTask(
                        priority = 0,
                        group = parts[0],
                        name = parts[1],
                        frequencyDays = parts[2].toIntOrNull() ?: 0,
                        nextDue = nextDue,
                        lastDone = parts[4],
                        lineIndex = i,
                        daysToDue = DateUtils.daysBetween(today, nextDue)
                    )
                )
            }
        }
        return tasks
    }

    fun parseHistory(markdown: String): List<MaintenanceHistoryEntry> {
        val lines = markdown.lines()
        val layout = detectTableLayout(lines) ?: return emptyList()
        val entries = mutableListOf<MaintenanceHistoryEntry>()

        for (i in (layout.separatorLineIndex + 1) until lines.size) {
            val trimmed = lines[i].trim()
            if (!trimmed.startsWith("|") || !trimmed.contains("|")) continue

            val parts = trimmed.split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (layout.hasPriorityColumn && parts.size >= 6) {
                entries.add(
                    MaintenanceHistoryEntry(
                        priority = parts[0].toIntOrNull() ?: 0,
                        group = parts[1],
                        name = parts[2],
                        frequencyDays = parts[3].toIntOrNull() ?: 0,
                        nextDue = parts[4],
                        lastDone = parts[5],
                        lineIndex = i
                    )
                )
            } else if (!layout.hasPriorityColumn && parts.size >= 5) {
                entries.add(
                    MaintenanceHistoryEntry(
                        priority = 0,
                        group = parts[0],
                        name = parts[1],
                        frequencyDays = parts[2].toIntOrNull() ?: 0,
                        nextDue = parts[3],
                        lastDone = parts[4],
                        lineIndex = i
                    )
                )
            }
        }
        return entries.reversed()
    }

    private fun upgradeToSixColumns(lines: MutableList<String>, layout: TableLayout) {
        if (layout.hasPriorityColumn) return
        lines[layout.headerLineIndex] = "| Priority | Group | Task | Frequency | Next Due | Last Done |"
        if (layout.separatorLineIndex != layout.headerLineIndex) {
            lines[layout.separatorLineIndex] = "| :--- | :--- | :--- | :--- | :--- | :--- |"
        }
        for (i in (layout.separatorLineIndex + 1) until lines.size) {
            val trimmed = lines[i].trim()
            if (!trimmed.startsWith("|") || !trimmed.contains("|")) continue
            val parts = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 5) {
                lines[i] = "| 0 | ${parts[0]} | ${parts[1]} | ${parts[2]} | ${parts[3]} | ${parts[4]} |"
            }
        }
    }

    fun updateTaskPriority(markdown: String, task: MaintenanceTask, newPriority: Int): String {
        val lines = markdown.lines().toMutableList()
        val layout = detectTableLayout(lines) ?: return markdown
        if (!layout.hasPriorityColumn) {
            upgradeToSixColumns(lines, layout)
        }
        if (task.lineIndex in lines.indices) {
            lines[task.lineIndex] =
                "| $newPriority | ${task.group} | ${task.name} | ${task.frequencyDays} | ${task.nextDue} | ${task.lastDone} |"
        }
        return lines.joinToString("\n")
    }

    fun markTaskDone(
        markdown: String,
        task: MaintenanceTask,
        today: String = DateUtils.todayDateString()
    ): Pair<String, String> {
        val lines = markdown.lines().toMutableList()
        val layout = detectTableLayout(lines)
        if (layout != null && !layout.hasPriorityColumn) {
            upgradeToSixColumns(lines, layout)
        }
        val nextDue = DateUtils.addDays(today, task.frequencyDays.coerceAtLeast(0))
        val updatedLine =
            "| ${task.priority} | ${task.group} | ${task.name} | ${task.frequencyDays} | $nextDue | $today |"
        if (task.lineIndex in lines.indices) {
            lines[task.lineIndex] = updatedLine
        }
        return lines.joinToString("\n") to updatedLine
    }

    fun appendNewTask(
        markdown: String,
        priority: Int,
        group: String,
        name: String,
        frequencyDays: Int,
        customNextDue: String?,
        today: String = DateUtils.todayDateString()
    ): String {
        val cleanName = name.trim().replace("|", "/")
        val cleanGroup = group.trim().ifEmpty { "None" }.replace("|", "/")
        val nextDue = when {
            !customNextDue.isNullOrBlank() -> customNextDue.trim()
            frequencyDays > 0 -> DateUtils.addDays(today, frequencyDays)
            else -> DateUtils.addDays(today, 1)
        }

        val lines = markdown.lines().toMutableList()
        val layout = detectTableLayout(lines)
        if (layout == null) {
            val header = buildString {
                if (markdown.isNotBlank()) {
                    append(markdown.trimEnd())
                    append("\n\n")
                } else {
                    append("# Maintenance Tasks\n\n")
                }
                append("| Priority | Group | Task | Frequency | Next Due | Last Done |\n")
                append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")
                append("| $priority | $cleanGroup | $cleanName | $frequencyDays | $nextDue | $today |")
            }
            return header
        }

        if (!layout.hasPriorityColumn) {
            upgradeToSixColumns(lines, layout)
        }
        val newRow = "| $priority | $cleanGroup | $cleanName | $frequencyDays | $nextDue | $today |"
        val base = lines.joinToString("\n").trimEnd()
        return "$base\n$newRow"
    }

    fun appendHistoryEntry(existingHistoryMarkdown: String?, historyRow: String): String {
        val header = "| Priority | Group | Task | Frequency | Next Due | Last Done |\n| :--- | :--- | :--- | :--- | :--- | :--- |\n"
        if (existingHistoryMarkdown.isNullOrBlank()) {
            return "# Maintenance History\n\n$header$historyRow\n"
        }
        val lines = existingHistoryMarkdown.lines().toMutableList()
        val layout = detectTableLayout(lines)
        if (layout == null) {
            return "${existingHistoryMarkdown.trimEnd()}\n\n$header$historyRow\n"
        }
        if (!layout.hasPriorityColumn) {
            upgradeToSixColumns(lines, layout)
        }
        val normalizedRow = historyRow.trim()
        return "${lines.joinToString("\n").trimEnd()}\n$normalizedRow\n"
    }

    fun buildSampleTasksMarkdown(today: String = DateUtils.todayDateString()): String {
        val overdueNext = DateUtils.addDays(today, -2)
        val overdueLast = DateUtils.addDays(today, -9)
        val todayLast = DateUtils.addDays(today, -30)
        val tomorrowNext = DateUtils.addDays(today, 1)
        val tomorrowLast = DateUtils.addDays(today, -13)
        val futureNext = DateUtils.addDays(today, 12)
        val futureLast = DateUtils.addDays(today, -18)
        val hvacNext = DateUtils.addDays(today, 45)
        val hvacLast = DateUtils.addDays(today, -45)

        return """
            # Maintenance Tasks 01

            | Priority | Group | Task | Frequency | Next Due | Last Done |
            | :--- | :--- | :--- | :--- | :--- | :--- |
            | 1 | Appliances | Clean Coffee Machine | 7 | $overdueNext | $overdueLast |
            | 2 | Cleaning | Deep Clean Kitchen | 30 | $today | $todayLast |
            | 3 | Flooring | Vacuum Spare Room | 14 | $tomorrowNext | $tomorrowLast |
            | 4 | Cleaning | Wipe Baseboards | 30 | $futureNext | $futureLast |
            | 5 | HVAC | Replace Air Filter | 90 | $hvacNext | $hvacLast |
        """.trimIndent() + "\n"
    }

    fun buildSampleHistoryMarkdown(today: String = DateUtils.todayDateString()): String {
        val lastWeek = DateUtils.addDays(today, -7)
        val nextWeek = DateUtils.addDays(lastWeek, 7)
        return """
            # Maintenance History

            | Priority | Group | Task | Frequency | Next Due | Last Done |
            | :--- | :--- | :--- | :--- | :--- | :--- |
            | 1 | Appliances | Descale Kettle | 7 | $nextWeek | $lastWeek |
        """.trimIndent() + "\n"
    }
}
