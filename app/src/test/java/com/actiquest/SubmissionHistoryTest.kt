package com.actiquest

import com.actiquest.domain.DateTimeUtils
import com.actiquest.domain.Submission
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionHistoryTest {
    private val manilaZone = ZoneId.of("Asia/Manila")

    private fun timestamp(date: LocalDate): Long =
        date.atStartOfDay(manilaZone).toInstant().toEpochMilli()

    @Test
    fun calculateStreaksUsesOnlyTheRequestedUsersSubmissionDates() {
        val today = LocalDate.now(manilaZone)
        val result = DateTimeUtils.calculateStreaks(
            submissions = listOf(
                Submission(
                    submissionId = "today-user-a",
                    userId = "user-a",
                    submittedAt = timestamp(today)
                ),
                Submission(
                    submissionId = "yesterday-user-b",
                    userId = "user-b",
                    submittedAt = timestamp(today.minusDays(1))
                )
            ),
            userId = "user-a"
        )

        assertEquals(1, result.currentStreak)
        assertEquals(1, result.totalActiveDays)
    }

    @Test
    fun reviewedAtDoesNotMoveLegacySubmissionIntoCurrentStreakOrCalendarDate() {
        val reviewedDate = LocalDate.now(manilaZone)
        val submittedDate = reviewedDate.minusDays(30)
        val submittedMonth = YearMonth.from(submittedDate)
        val reviewedMonth = YearMonth.from(reviewedDate)
        val submission = Submission(
            submissionId = "legacy-submission",
            userId = "user-a",
            status = "pending",
            submittedAt = timestamp(submittedDate),
            reviewedAt = timestamp(reviewedDate),
            submissionType = "video"
        )
        val anotherUsersSubmission = Submission(
            submissionId = "other-users-review-date-submission",
            userId = "user-b",
            submittedAt = timestamp(reviewedDate),
            submissionType = "video"
        )
        val allUserSubmissions = listOf(submission, anotherUsersSubmission)

        val days = DateTimeUtils.getMonthDays(
            year = submittedMonth.year,
            month = submittedMonth.monthValue,
            selectedDate = submittedDate,
            submissions = allUserSubmissions,
            userId = "user-a"
        )
        val reviewedDays = DateTimeUtils.getMonthDays(
            year = reviewedMonth.year,
            month = reviewedMonth.monthValue,
            selectedDate = reviewedDate,
            submissions = allUserSubmissions,
            userId = "user-a"
        )
        val submittedDay = days.single { it.isCurrentMonth && it.date == submittedDate }
        val reviewedDay = reviewedDays.single { it.isCurrentMonth && it.date == reviewedDate }

        assertTrue(submittedDay.hasQuests)
        assertTrue(submittedDay.hasStreak)
        assertFalse(reviewedDay.hasQuests)
        assertFalse(reviewedDay.hasStreak)

        val streak = DateTimeUtils.calculateStreaks(listOf(submission), "user-a")
        assertEquals(0, streak.currentStreak)
    }

    @Test
    fun calendarDoesNotInventActivityForAnEmptyUserHistory() {
        val currentMonth = YearMonth.now(manilaZone)
        val days = DateTimeUtils.getMonthDays(
            year = currentMonth.year,
            month = currentMonth.monthValue,
            selectedDate = currentMonth.atDay(1),
            submissions = emptyList(),
            userId = "user-with-no-submissions"
        )

        assertTrue(days.filter { it.isCurrentMonth }.none {
            it.hasQuests || it.hasWorkouts || it.hasStreak
        })
    }
}
