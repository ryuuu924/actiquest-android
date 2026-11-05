package com.actiquest

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun formatRelativeTime_handlesNumericTimestampsWithoutRecursion() {
    val now = System.currentTimeMillis()
    val justNow = com.actiquest.domain.DateTimeUtils.formatRelativeTime(now)
    assertEquals("Just now", justNow)

    val tenMinsAgo = com.actiquest.domain.DateTimeUtils.formatRelativeTime(now - 10 * 60 * 1000L)
    assertEquals("10 mins ago", tenMinsAgo)

    val twoHoursAgo = com.actiquest.domain.DateTimeUtils.formatRelativeTime(now - 2 * 3600 * 1000L)
    assertEquals("2 hrs ago", twoHoursAgo)

    val oneDayAgo = com.actiquest.domain.DateTimeUtils.formatRelativeTime(now - 24 * 3600 * 1000L)
    assertEquals("1 day ago", oneDayAgo)
  }

  @Test
  fun formatTimeOnly_handlesNumericTimestamps() {
    val now = System.currentTimeMillis()
    val formatted = com.actiquest.domain.DateTimeUtils.formatTimeOnly(now)
    assertNotNull(formatted)
    assertTrue(formatted.contains("AM") || formatted.contains("PM"))
  }

  @Test
  fun realUserNotifications_generatesUniqueNotificationsFromActualSubmissions() {
    val today = LocalDate.now(ZoneId.of("Asia/Manila"))
    fun timestamp(date: LocalDate): Long =
      date.atStartOfDay(ZoneId.of("Asia/Manila")).toInstant().toEpochMilli()

    val user = com.actiquest.domain.User(
      uid = "student_test_123",
      name = "Yuri Yabut",
      firstName = "Yuri",
      lastName = "Yabut",
      email = "yuri@dlsl.edu.ph",
      classId = "CITE-BSIT-3A",
      badges = listOf("First Steps", "Explorer"),
      streak = 3
    )

    val subs = listOf(
      com.actiquest.domain.Submission(
        submissionId = "sub_001",
        userId = "student_test_123",
        questTitle = "Diokno Steps",
        status = "approved",
        awardedPoints = 50,
        submittedAt = timestamp(today),
        reviewedAt = timestamp(today)
      ),
      com.actiquest.domain.Submission(
        submissionId = "sub_002",
        userId = "student_test_123",
        questTitle = "Campus 5000 Steps",
        status = "pending",
        submittedAt = timestamp(today.minusDays(1))
      ),
      com.actiquest.domain.Submission(
        submissionId = "sub_other_user",
        userId = "other_student_999",
        questTitle = "Other Quest",
        status = "approved"
      )
    )

    val classes = listOf(
      com.actiquest.domain.ClassModel(classId = "CITE-BSIT-3A", name = "CITE - BSIT 3A", totalPoints = 1200),
      com.actiquest.domain.ClassModel(classId = "CITE-BSCS-1A", name = "CITE - BSCS 1A", totalPoints = 900)
    )

    val notifications = com.actiquest.data.Repository.generateRealUserNotifications(user, subs, classes)
    
    // Notifications should only belong to student_test_123
    assertTrue(notifications.none { it.userId == "other_student_999" })
    
    // Should have approved submission notification
    val approvedNotif = notifications.find { it.id == "notif_appr_sub_001" }
    assertNotNull(approvedNotif)
    assertEquals("Review", approvedNotif?.category)
    assertTrue(approvedNotif?.message?.contains("+50 Points") == true)

    // Should have pending submission notification
    val pendingNotif = notifications.find { it.id == "notif_sub_sub_002" }
    assertNotNull(pendingNotif)
    assertEquals("Quest", pendingNotif?.category)

    // Should have streak notification
    val streakNotif = notifications.find { it.id == "notif_streak_student_test_123_2" }
    assertNotNull(streakNotif)
    assertEquals("Streak", streakNotif?.category)
    assertEquals("2", streakNotif?.relatedId)

    // Should have leaderboard notification for CITE-BSIT-3A (Rank #1)
    val rankNotif = notifications.find { it.id == "notif_class_rank_student_test_123_CITE-BSIT-3A" }
    assertNotNull(rankNotif)
    assertEquals("Leaderboard", rankNotif?.category)
    assertTrue(rankNotif?.title?.contains("Rank #1") == true)
  }

  @Test
  fun notificationHistoryOnlyIncludesDocumentsOwnedByTheCurrentUser() {
    val notifications = com.actiquest.data.Repository.mergeAndFilterNotifications(
      firestoreNotifs = listOf(
        com.actiquest.domain.AppNotification(id = "owned", userId = "student-a", classId = "class-a"),
        com.actiquest.domain.AppNotification(id = "other-user", userId = "student-b", classId = "class-a"),
        com.actiquest.domain.AppNotification(id = "legacy-unowned", classId = "class-a")
      ),
      localNotifs = listOf(
        com.actiquest.domain.AppNotification(id = "local-owned", userId = "student-a"),
        com.actiquest.domain.AppNotification(id = "local-other", userId = "student-b")
      ),
      currentUid = "student-a"
    )

    assertEquals(setOf("owned", "local-owned"), notifications.map { it.id }.toSet())
  }
}
