package com.example

import com.example.data.DateUtils
import com.example.data.MaintenanceMarkdownCodec
import com.example.data.RoutineMarkdownCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownCodecTest {

    @Test
    fun `parses both 5-column and 6-column maintenance tables`() {
        val fiveCol = """
            # Maintenance Tasks 01

            | Group | Task | Frequency | Next Due | Last Done |
            | :--- | :--- | :--- | :--- | :--- |
            | Appliances | Clean Coffee Machine | 7 | 2026-11-23 | 2026-11-16 |
            | HVAC | Replace Air Filter | 90 | 2026-11-26 | 2026-08-28 |
        """.trimIndent()

        val parsed = MaintenanceMarkdownCodec.parseTasks(fiveCol, today = "2026-11-25")
        assertEquals(2, parsed.size)
        assertEquals(0, parsed[0].priority)
        assertEquals("Clean Coffee Machine", parsed[0].name)
        assertEquals(-2, parsed[0].daysToDue)
        assertEquals(1, parsed[1].daysToDue)
    }

    @Test
    fun `marking task done updates dates and generates history row`() {
        val md = """
            | Priority | Group | Task | Frequency | Next Due | Last Done |
            | :--- | :--- | :--- | :--- | :--- | :--- |
            | 2 | Cleaning | Deep Clean Kitchen | 30 | 2026-11-25 | 2026-10-26 |
        """.trimIndent()

        val tasks = MaintenanceMarkdownCodec.parseTasks(md, today = "2026-11-25")
        val (updatedMd, historyRow) = MaintenanceMarkdownCodec.markTaskDone(md, tasks.first(), today = "2026-11-25")

        val reparsed = MaintenanceMarkdownCodec.parseTasks(updatedMd, today = "2026-11-25")
        assertEquals("2026-12-25", reparsed.first().nextDue)
        assertEquals("2026-11-25", reparsed.first().lastDone)
        assertEquals(30, reparsed.first().daysToDue)

        val historyMd = MaintenanceMarkdownCodec.appendHistoryEntry(null, historyRow)
        val historyEntries = MaintenanceMarkdownCodec.parseHistory(historyMd)
        assertEquals(1, historyEntries.size)
        assertEquals("Deep Clean Kitchen", historyEntries.first().name)
    }

    @Test
    fun `routine codec filters by weekday and updates start timestamps`() {
        val md = """
            ## Morning Launch
            | Title | Duration | Mon | Tue | Wed | Thu | Fri | Sat | Sun | StartTime |
            | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
            | Hydrate | 180 | Y | Y | Y | Y | Y | Y | Y |  |
            | Weekday Focus | 300 | Y | Y | Y | Y | Y | N | N |  |
        """.trimIndent()

        // Column 3 = Monday (both enabled), Column 9 = Sunday (only Hydrate enabled)
        val mondayRoutines = RoutineMarkdownCodec.parseRoutines(md, weekdayColIndex = 3, filterByWeekday = true)
        assertEquals(2, mondayRoutines.first().tasks.size)

        val sundayRoutines = RoutineMarkdownCodec.parseRoutines(md, weekdayColIndex = 9, filterByWeekday = true)
        assertEquals(1, sundayRoutines.first().tasks.size)
        assertEquals("Hydrate", sundayRoutines.first().tasks.first().name)

        val withStart = RoutineMarkdownCodec.updateTaskStartTime(md, "Morning Launch", "Hydrate", 1700000000000L)
        val parsedStarted = RoutineMarkdownCodec.parseRoutines(withStart, weekdayColIndex = 3, filterByWeekday = true)
        assertTrue(parsedStarted.first().hasActiveStartTime)
        assertEquals(1700000000000L, parsedStarted.first().tasks.first().startTimeEpochMs)

        val reset = RoutineMarkdownCodec.resetRoutineTimestamps(withStart, "Morning Launch")
        val parsedReset = RoutineMarkdownCodec.parseRoutines(reset, weekdayColIndex = 3, filterByWeekday = true)
        assertFalse(parsedReset.first().hasActiveStartTime)
        assertNull(parsedReset.first().tasks.first().startTimeEpochMs)
    }

    @Test
    fun `date utils formats durations and adds days accurately`() {
        assertEquals("03:05", DateUtils.formatDurationMmSs(185))
        assertEquals("2026-12-02", DateUtils.addDays("2026-11-25", 7))
        assertNotNull(DateUtils.startOfDayMillis("2026-11-25"))
    }
}
