package com.example.data

data class RoutineTask(
    val name: String,
    val durationSeconds: Int,
    val enabledDayColumns: Set<Int>,
    val startTimeEpochMs: Long?,
    val lineIndex: Int
)

data class Routine(
    val name: String,
    val tasks: List<RoutineTask>
) {
    val totalDurationSeconds: Int get() = tasks.sumOf { it.durationSeconds }
    val totalMinutes: Int get() = totalDurationSeconds / 60
    val hasActiveStartTime: Boolean get() = tasks.any { it.startTimeEpochMs != null }
}

object RoutineMarkdownCodec {

    private val HEADER_IGNORE = setOf("title", ":---", "---:", "---")

    fun parseRoutines(
        markdown: String,
        weekdayColIndex: Int = DateUtils.currentWeekdayColumnIndex(),
        filterByWeekday: Boolean = true
    ): List<Routine> {
        val lines = markdown.lines()
        val routines = mutableListOf<Routine>()
        var currentRoutineName: String? = null
        var currentTasks = mutableListOf<RoutineTask>()

        fun flushRoutine() {
            val rName = currentRoutineName
            if (!rName.isNullOrBlank() && currentTasks.isNotEmpty()) {
                routines.add(Routine(name = rName, tasks = currentTasks.toList()))
            }
            currentTasks = mutableListOf()
        }

        for (i in lines.indices) {
            val line = lines[i]
            val trimmed = line.trim()
            if (trimmed.startsWith("## ")) {
                flushRoutine()
                currentRoutineName = trimmed.removePrefix("## ").trim()
                continue
            }

            if (currentRoutineName != null && trimmed.contains("|")) {
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 10) {
                    val name = parts[1]
                    val lowerName = name.lowercase()
                    if (name.isBlank() || lowerName in HEADER_IGNORE || lowerName.startsWith(":--") || lowerName.startsWith("---")) {
                        continue
                    }

                    val duration = parts[2].toIntOrNull() ?: continue
                    val enabledCols = mutableSetOf<Int>()
                    for (col in 3..9) {
                        if (col < parts.size && parts[col].equals("Y", ignoreCase = true)) {
                            enabledCols.add(col)
                        }
                    }

                    val enabledToday = weekdayColIndex in enabledCols
                    if (!filterByWeekday || enabledToday) {
                        val rawStart = if (parts.size > 10) parts[10] else ""
                        val startTime = rawStart.takeIf { it.isNotBlank() }?.toLongOrNull()
                        currentTasks.add(
                            RoutineTask(
                                name = name,
                                durationSeconds = duration.coerceAtLeast(1),
                                enabledDayColumns = enabledCols,
                                startTimeEpochMs = startTime,
                                lineIndex = i
                            )
                        )
                    }
                }
            }
        }
        flushRoutine()
        return routines
    }

    private fun formatRoutineRow(partsInput: List<String>, startTime: Long?): String {
        val cols = partsInput.toMutableList()
        while (cols.size <= 10) {
            cols.add("")
        }
        // Columns 1..10 represent Title..StartTime
        val cells = (1..10).map { idx ->
            if (idx == 10) startTime?.toString() ?: "" else cols[idx]
        }
        return "| ${cells.joinToString(" | ")} |"
    }

    fun updateTaskStartTime(
        markdown: String,
        routineName: String,
        taskName: String,
        timestampMs: Long?
    ): String {
        val lines = markdown.lines().toMutableList()
        var inRoutine = false
        var updated = false

        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("## ")) {
                inRoutine = trimmed.removePrefix("## ").trim() == routineName
                continue
            }
            if (inRoutine && trimmed.contains("|")) {
                val parts = lines[i].split("|").map { it.trim() }
                if (parts.size >= 10 && parts[1] == taskName) {
                    lines[i] = formatRoutineRow(parts, timestampMs)
                    updated = true
                    break
                }
            }
        }
        return if (updated) lines.joinToString("\n") else markdown
    }

    fun resetRoutineTimestamps(markdown: String, routineName: String): String {
        val lines = markdown.lines().toMutableList()
        var inRoutine = false
        var updated = false

        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("## ")) {
                inRoutine = trimmed.removePrefix("## ").trim() == routineName
                continue
            }
            if (inRoutine && trimmed.contains("|")) {
                val parts = lines[i].split("|").map { it.trim() }
                if (parts.size >= 10 &&
                    !parts[1].equals("Title", ignoreCase = true) &&
                    !parts[1].startsWith(":--") &&
                    !parts[1].startsWith("---")
                ) {
                    lines[i] = formatRoutineRow(parts, null)
                    updated = true
                }
            }
        }
        return if (updated) lines.joinToString("\n") else markdown
    }

    fun buildSampleRoutinesMarkdown(): String {
        return """
            # Routines

            ## Morning Launch
            | Title | Duration | Mon | Tue | Wed | Thu | Fri | Sat | Sun | StartTime |
            | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
            | Hydrate & Stretch | 180 | Y | Y | Y | Y | Y | Y | Y |  |
            | Coffee & Daily Review | 300 | Y | Y | Y | Y | Y | Y | Y |  |
            | Top 3 Priorities | 240 | Y | Y | Y | Y | Y | N | N |  |
            | Quick Desk Tidy | 120 | Y | Y | Y | Y | Y | Y | Y |  |

            ## Evening Wind-Down
            | Title | Duration | Mon | Tue | Wed | Thu | Fri | Sat | Sun | StartTime |
            | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
            | Kitchen Counter Reset | 300 | Y | Y | Y | Y | Y | Y | Y |  |
            | Prep Tomorrow Outfit | 180 | Y | Y | Y | Y | N | N | Y |  |
            | Screen-Free Reading | 600 | Y | Y | Y | Y | Y | Y | Y |  |
        """.trimIndent() + "\n"
    }
}
