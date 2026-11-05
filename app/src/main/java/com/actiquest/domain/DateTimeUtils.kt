package com.actiquest.domain

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class WeekDayInfo(
    val dayName: String, // "Mon", "Tue", etc.
    val dayNumber: Int,  // 31
    val isToday: Boolean,
    val dayIndex: Int    // 0 = Mon, 6 = Sun
)

data class CalendarDay(
    val dayNumber: Int,
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean = false,
    val hasQuests: Boolean = false,
    val hasWorkouts: Boolean = false,
    val hasStreak: Boolean = false,
    val pointsEarned: Int = 0
)

data class StreakResult(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalActiveDays: Int
)

object DateTimeUtils {

    fun getGreeting(firstName: String = ""): String {
        val now = try {
            LocalTime.now(ZoneId.of("Asia/Manila"))
        } catch (e: Exception) {
            LocalTime.of(12, 0)
        }
        val hour = now.hour
        val minute = now.minute

        // Time slot mapping based on specific hours of the day
        val slotGreetings = when (hour) {
            // 12:00 AM – 3:59 AM (The Deep Night / Midnight)
            in 0..3 -> listOf(
                "Up late?",
                "Burning the midnight oil?",
                "Hello, night owl!",
                "Still awake?",
                "Quiet hours."
            )
            // 4:00 AM – 5:59 AM (The Twilight / Early Dawn)
            in 4..5 -> listOf(
                "Good early morning!",
                "Hello, early bird!",
                "Rise and shine!",
                "Before the sun.",
                "Quiet mornings."
            )
            // 6:00 AM – 8:59 AM (Early Morning / Breakfast)
            in 6..8 -> listOf(
                "Top of the morning!",
                "Fresh start!",
                "Ready for today?",
                "Good morning!"
            )
            // 9:00 AM – 11:59 AM (Late Morning / Mid-Day)
            in 9..11 -> listOf(
                "Happy mid-morning!",
                "Productive day!",
                "Day in full swing.",
                "Hello there!"
            )
            // 12:00 PM – 1:59 PM (Noon / Lunchbreak)
            in 12..13 -> listOf(
                "Happy lunchtime!",
                "Midday check-in.",
                "Good afternoon!"
            )
            // 2:00 PM – 4:59 PM (Late Afternoon / Post-Lunch)
            in 14..16 -> listOf(
                "Powering through?",
                "Afternoon slump?",
                "Good late afternoon!"
            )
            // 5:00 PM – 7:59 PM (Twilight / Golden Hour)
            in 17..19 -> listOf(
                "Good evening!",
                "Unwinding?",
                "Sun is setting.",
                "Day is wrapping up."
            )
            // 8:00 PM – 11:59 PM (Late Evening / Bedtime)
            else -> listOf(
                "Time to unwind.",
                "Good night!",
                "Rest well.",
                "Ending the day."
            )
        }

        // Pick greeting deterministically per ~15 minute window so it stays consistent yet lively
        val greetingIndex = ((hour * 4 + minute / 15) % slotGreetings.size).coerceAtLeast(0)
        val baseGreeting = slotGreetings[greetingIndex]

        if (firstName.isBlank()) return baseGreeting

        val cleanName = firstName.trim()
        return when {
            baseGreeting.endsWith("?") -> {
                val stem = baseGreeting.removeSuffix("?")
                "$stem, $cleanName?"
            }
            baseGreeting.endsWith("!") -> {
                val stem = baseGreeting.removeSuffix("!")
                "$stem, $cleanName!"
            }
            baseGreeting.endsWith(".") -> {
                val stem = baseGreeting.removeSuffix(".")
                "$stem, $cleanName."
            }
            else -> "$baseGreeting, $cleanName!"
        }
    }

    fun getTimeSlotDescription(): String {
        val hour = try { LocalTime.now(ZoneId.of("Asia/Manila")).hour } catch (e: Exception) { 12 }
        return when (hour) {
            in 0..3 -> "Midnight & Deep Night"
            in 4..5 -> "Twilight & Early Dawn"
            in 6..8 -> "Early Morning"
            in 9..11 -> "Late Morning"
            in 12..13 -> "Noon & Lunchbreak"
            in 14..16 -> "Late Afternoon"
            in 17..19 -> "Twilight & Golden Hour"
            else -> "Late Evening & Night"
        }
    }

    fun getFormattedCurrentDate(): String {
        return try {
            val now = LocalDate.now(ZoneId.of("Asia/Manila"))
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
            now.format(formatter)
        } catch (e: Exception) {
            "Today"
        }
    }

    fun getFormattedShortDate(): String {
        return try {
            val now = LocalDate.now(ZoneId.of("Asia/Manila"))
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
            now.format(formatter)
        } catch (e: Exception) {
            "Today"
        }
    }

    fun getCurrentWeekDays(): List<WeekDayInfo> {
        return try {
            val today = LocalDate.now(ZoneId.of("Asia/Manila"))
            // Adjust to Monday as first day of week
            val dayOfWeekValue = today.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            val monday = today.minusDays((dayOfWeekValue - 1).toLong())

            (0..6).map { i ->
                val day = monday.plusDays(i.toLong())
                val name = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                WeekDayInfo(
                    dayName = name,
                    dayNumber = day.dayOfMonth,
                    isToday = day == today,
                    dayIndex = i
                )
            }
        } catch (e: Exception) {
            listOf(
                WeekDayInfo("Mon", 1, false, 0),
                WeekDayInfo("Tue", 2, false, 1),
                WeekDayInfo("Wed", 3, false, 2),
                WeekDayInfo("Thu", 4, false, 3),
                WeekDayInfo("Fri", 5, true, 4),
                WeekDayInfo("Sat", 6, false, 5),
                WeekDayInfo("Sun", 7, false, 6)
            )
        }
    }

    fun getTodayDayOfWeekIndex(): Int {
        return try {
            LocalDate.now(ZoneId.of("Asia/Manila")).dayOfWeek.value - 1 // 0 (Mon) to 6 (Sun)
        } catch (e: Exception) {
            0
        }
    }

    fun getEpochLocalDate(epochMs: Long): LocalDate {
        return try {
            Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("Asia/Manila")).toLocalDate()
        } catch (e: Exception) {
            LocalDate.now(ZoneId.of("Asia/Manila"))
        }
    }

    fun calculateStreaks(submissions: List<Submission>, userId: String? = null): StreakResult {
        val today = LocalDate.now(ZoneId.of("Asia/Manila"))
        val activeDates = submissions
            .asSequence()
            .filter { it.submittedAt > 0L }
            .filter { userId.isNullOrBlank() || it.userId == userId }
            .mapNotNull { parseSubmissionDate(it.submittedAt) }
            .filter { !it.isAfter(today) }
            .distinct()
            .sorted()
            .toList()

        if (activeDates.isEmpty()) {
            return StreakResult(0, 0, 0)
        }

        val totalActiveDays = activeDates.size
        var bestStreak = 0
        var currentRun = 0
        var prevDate: LocalDate? = null

        for (date in activeDates) {
            if (prevDate == null) {
                currentRun = 1
            } else {
                if (prevDate.plusDays(1) == date) {
                    currentRun++
                } else if (prevDate == date) {
                    // duplicate
                } else {
                    currentRun = 1
                }
            }
            prevDate = date
            if (currentRun > bestStreak) {
                bestStreak = currentRun
            }
        }

        val latestDate = activeDates.last()
        val diffFromToday = java.time.temporal.ChronoUnit.DAYS.between(latestDate, today)

        val currentStreak = if (diffFromToday == 0L || diffFromToday == 1L) {
            var streak = 0
            var expectedDate = latestDate
            val dateSet = activeDates.toSet()
            while (dateSet.contains(expectedDate)) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            }
            streak
        } else {
            0
        }

        return StreakResult(
            currentStreak = currentStreak,
            bestStreak = maxOf(bestStreak, currentStreak),
            totalActiveDays = totalActiveDays
        )
    }

    fun getPhilippineCurrentTimestamp(): String {
        return try {
            val zdt = ZonedDateTime.now(ZoneId.of("Asia/Manila"))
            zdt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a", Locale.ENGLISH))
        } catch (e: Exception) {
            "October 25, 2026, 12:00 PM"
        }
    }

    fun formatPhilippineTimestamp(millis: Long): String {
        if (millis <= 0) return ""
        return try {
            val instant = Instant.ofEpochMilli(millis)
            val zdt = instant.atZone(ZoneId.of("Asia/Manila"))
            zdt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a", Locale.ENGLISH))
        } catch (e: Exception) {
            millis.toString()
        }
    }

    fun formatPhilippineFullTimestamp(millis: Long): String {
        val effectiveMillis = if (millis <= 0) System.currentTimeMillis() else millis
        return try {
            val instant = Instant.ofEpochMilli(effectiveMillis)
            val zdt = instant.atZone(ZoneId.of("Asia/Manila"))
            zdt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm:ss a 'UTC+8'", Locale.ENGLISH))
        } catch (e: Exception) {
            effectiveMillis.toString()
        }
    }

    fun parseSubmissionDate(dateObj: Any?): LocalDate? {
        val millis = parseToEpochMillis(dateObj)
        if (millis <= 0L) return null
        return try {
            Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Manila")).toLocalDate()
        } catch (e: Exception) {
            null
        }
    }

    fun parseToEpochMillis(timestampInput: Any?): Long {
        if (timestampInput == null) return 0L
        if (timestampInput is Number) {
            val num = timestampInput.toLong()
            return if (num > 0L && num < 10_000_000_000L) num * 1000L else num
        }
        if (timestampInput is java.util.Date) return timestampInput.time
        try {
            if (timestampInput::class.java.simpleName.contains("Timestamp") ||
                timestampInput.javaClass.name.contains("Timestamp")) {
                val method = timestampInput::class.java.getMethod("toDate")
                val d = method.invoke(timestampInput) as? java.util.Date
                if (d != null) return d.time
            }
        } catch (_: Exception) {}

        if (timestampInput is String) {
            val trimmed = timestampInput.trim()
            if (trimmed.isEmpty()) return 0L

            val parsedLong = trimmed.toLongOrNull()
            if (parsedLong != null) {
                return if (parsedLong > 0L && parsedLong < 10_000_000_000L) parsedLong * 1000L else parsedLong
            }

            try { return Instant.parse(trimmed).toEpochMilli() } catch (_: Exception) {}
            try { return ZonedDateTime.parse(trimmed).toInstant().toEpochMilli() } catch (_: Exception) {}
            try { return OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() } catch (_: Exception) {}

            val patterns = listOf(
                "MMMM d, yyyy 'at' h:mm:ss a 'UTC+8'",
                "MMMM d, yyyy 'at' h:mm:ss a",
                "MMMM d, yyyy, h:mm:ss a",
                "MMMM d, yyyy, h:mm a",
                "MMM d, yyyy, h:mm a",
                "MMMM d, yyyy",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd"
            )

            for (pattern in patterns) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                    if (pattern.contains("HH:mm") || pattern.contains("h:mm")) {
                        val parsed = LocalDateTime.parse(trimmed, formatter)
                        return parsed.atZone(ZoneId.of("Asia/Manila")).toInstant().toEpochMilli()
                    } else {
                        val parsedDate = LocalDate.parse(trimmed, formatter)
                        return parsedDate.atStartOfDay(ZoneId.of("Asia/Manila")).toInstant().toEpochMilli()
                    }
                } catch (_: Exception) {}
            }
        }
        return 0L
    }

    fun getPointsForWeek(
        weekOffset: Int,
        submissions: List<Submission>,
        userId: String
    ): List<Pair<String, Int>> {
        val today = LocalDate.now(ZoneId.of("Asia/Manila"))
        val dayOfWeekVal = today.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val thisMonday = today.minusDays((dayOfWeekVal - 1).toLong())
        val targetMonday = thisMonday.minusWeeks(weekOffset.toLong())

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val weekDates = (0..6).map { targetMonday.plusDays(it.toLong()) }
        val approvedSubs = submissions.filter {
            it.submittedAt > 0L &&
                (userId.isBlank() || it.userId == userId) &&
                it.status.equals("approved", ignoreCase = true)
        }

        return weekDates.mapIndexed { idx, date ->
            val dayName = dayNames[idx]
            val subsOnDay = approvedSubs.filter { sub ->
                val subDate = parseSubmissionDate(sub.submittedAt)
                subDate != null && subDate == date
            }
            val pointsFromSubs = subsOnDay.sumOf { it.awardedPoints ?: it.pointValue }

            if (pointsFromSubs > 0) {
                dayName to pointsFromSubs
            } else {
                dayName to 0
            }
        }
    }

    fun getMonthDays(
        year: Int,
        month: Int,
        selectedDate: LocalDate,
        submissions: List<Submission> = emptyList(),
        userId: String = ""
    ): List<CalendarDay> {
        return try {
            val yearMonth = YearMonth.of(year, month)
            val firstDayOfMonth = yearMonth.atDay(1)
            val daysInMonth = yearMonth.lengthOfMonth()
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = Mon, 7 = Sun
            val today = LocalDate.now(ZoneId.of("Asia/Manila"))

            val calendarDays = mutableListOf<CalendarDay>()

            // Leading days from previous month
            val prevMonth = yearMonth.minusMonths(1)
            val daysInPrevMonth = prevMonth.lengthOfMonth()
            val leadingDaysCount = firstDayOfWeek - 1

            for (i in (daysInPrevMonth - leadingDaysCount + 1)..daysInPrevMonth) {
                val date = prevMonth.atDay(i)
                calendarDays.add(
                    CalendarDay(
                        dayNumber = i,
                        date = date,
                        isCurrentMonth = false,
                        isToday = date == today,
                        isSelected = date == selectedDate
                    )
                )
            }

            val userSubmissions = submissions.filter {
                it.submittedAt > 0L && (userId.isBlank() || it.userId == userId)
            }

            // Days in current month
            for (dayNum in 1..daysInMonth) {
                val date = yearMonth.atDay(dayNum)
                val isPastOrToday = !date.isAfter(today)

                val daySubs = userSubmissions.filter { sub ->
                    val subDate = parseSubmissionDate(sub.submittedAt)
                    subDate != null && subDate == date
                }
                val approvedDaySubs = daySubs.filter { it.status.equals("approved", ignoreCase = true) }
                val dayPoints = approvedDaySubs.sumOf { it.awardedPoints ?: it.pointValue }

                val hasQuests: Boolean
                val hasWorkouts: Boolean
                val hasStreak: Boolean

                hasQuests = daySubs.any { it.submissionType == "video" || it.questCode.isNotEmpty() }
                hasWorkouts = daySubs.any { it.submissionType == "screenshot" || it.activityType.isNotEmpty() }
                hasStreak = isPastOrToday && daySubs.isNotEmpty()

                calendarDays.add(
                    CalendarDay(
                        dayNumber = dayNum,
                        date = date,
                        isCurrentMonth = true,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        hasQuests = hasQuests,
                        hasWorkouts = hasWorkouts,
                        hasStreak = hasStreak,
                        pointsEarned = dayPoints
                    )
                )
            }

            // Trailing days to fill 7-column grid
            val totalCells = ((calendarDays.size + 6) / 7) * 7
            val trailingCount = totalCells - calendarDays.size
            val nextMonth = yearMonth.plusMonths(1)
            for (dayNum in 1..trailingCount) {
                val date = nextMonth.atDay(dayNum)
                calendarDays.add(
                    CalendarDay(
                        dayNumber = dayNum,
                        date = date,
                        isCurrentMonth = false,
                        isToday = date == today,
                        isSelected = date == selectedDate
                    )
                )
            }

            calendarDays
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun formatRelativeTime(millis: Long): String {
        if (millis <= 0L) return "Just now"
        val now = System.currentTimeMillis()
        val diff = (now - millis).coerceAtLeast(0L)
        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> {
                val mins = (diff / 60_000L).coerceAtLeast(1L)
                if (mins == 1L) "1 min ago" else "$mins mins ago"
            }
            diff < 86_400_000L -> {
                val hrs = (diff / 3_600_000L).coerceAtLeast(1L)
                if (hrs == 1L) "1 hr ago" else "$hrs hrs ago"
            }
            diff < 86_400_000L * 7L -> {
                val days = (diff / 86_400_000L).coerceAtLeast(1L)
                if (days == 1L) "1 day ago" else "$days days ago"
            }
            else -> {
                try {
                    val instant = Instant.ofEpochMilli(millis)
                    val zdt = instant.atZone(ZoneId.of("Asia/Manila"))
                    zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
                } catch (e: Exception) {
                    "Recently"
                }
            }
        }
    }

    fun formatRelativeTime(timestampInput: Any?): String {
        val millis = parseToEpochMillis(timestampInput)
        return formatRelativeTime(millis)
    }

    fun formatTimeOnly(millis: Long): String {
        if (millis <= 0L) return "09:00 AM"
        return try {
            val instant = Instant.ofEpochMilli(millis)
            val zdt = instant.atZone(ZoneId.of("Asia/Manila"))
            zdt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
        } catch (e: Exception) {
            "09:00 AM"
        }
    }

    fun formatTimeOnly(timestampInput: Any?): String {
        val millis = parseToEpochMillis(timestampInput)
        return formatTimeOnly(millis)
    }
}
