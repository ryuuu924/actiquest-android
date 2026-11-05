package com.actiquest.domain

import java.util.UUID

data class User(
    val uid: String = "",
    val name: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val college: String = "CITE",
    val program: String = "BS Computer Science",
    val yearLevel: String = "1st Year",
    val section: String = "1A",
    val classId: String = "CITE-BSCS-1A",
    val totalPoints: Int = 0,
    val targetPoints: Int = 500,
    val streak: Int = 1,
    val badges: List<String> = emptyList(),
    val approvedCount: Int = 0,
    val expoPushToken: String = "",
    val consentGiven: Boolean = true,
    val profilePicUri: String = "",
    val nickname: String = "",
    val pendingAcademicUpdate: Boolean = false,
    val pendingAcademicDetails: String = ""
) {
    fun getDisplayName(): String {
        if (nickname.isNotBlank()) return nickname.trim()
        if (firstName.isNotBlank()) return firstName.trim()
        val parts = name.trim().split(" ")
        return parts.firstOrNull()?.ifEmpty { "Warrior" } ?: "Warrior"
    }

    fun getEffectiveFirstName(): String {
        if (firstName.isNotBlank()) return firstName.trim()
        val parts = name.trim().split(" ")
        return parts.firstOrNull()?.ifEmpty { "Warrior" } ?: "Warrior"
    }

    fun getFullName(): String {
        if (firstName.isNotBlank() || lastName.isNotBlank()) {
            return "${firstName.trim()} ${lastName.trim()}".trim()
        }
        return name.ifBlank { "Student" }
    }
}

data class AcademicChangeRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val currentCollege: String = "",
    val currentProgram: String = "",
    val currentYearLevel: String = "",
    val currentSection: String = "",
    val requestedCollege: String = "",
    val requestedProgram: String = "",
    val requestedYearLevel: String = "",
    val requestedSection: String = "",
    val reason: String = "",
    val status: String = "pending", // pending, approved, rejected
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewerNote: String? = null
)

data class College(
    val code: String,
    val name: String,
    val programs: List<String>
)

data class ClassModel(
    val classId: String = "",
    val name: String = "",
    val college: String = "",
    val program: String = "",
    val yearLevel: String = "",
    val section: String = "",
    val totalPoints: Int = 0,
    val memberCount: Int = 0
)

data class Quest(
    val questCode: String = "",
    val title: String = "",
    val instructions: String = "",
    val location: String = "",
    val category: String = "quick", // quick, explorer, boss
    val pointValue: Int = 50,
    val maxSeconds: Int = 30,
    val active: Boolean = true,
    val activityDurationSeconds: Int? = null,
    val evidenceMode: String = "",
    val minimumEvidenceDurationSeconds: Int = 0,
    val maximumEvidenceDurationSeconds: Int = 0,
    val maximumVideoSizeBytes: Long = 0L
)

data class Submission(
    val submissionId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val studentName: String = "",
    val classId: String = "",
    val questCode: String = "",
    val questTitle: String = "",
    val videoPath: String = "",
    val pointValue: Int = 0,
    val status: String = "pending", // pending, approved, rejected
    val submittedAt: Long = 0L,
    val reviewedAt: Long? = null,
    val reviewerNote: String? = null,
    val submissionType: String = "video", // "video" (QR quest) or "screenshot" (pedometer / fitness proof)
    val imagePath: String = "",
    val activityType: String = "", // "Campus Video Quest", "Steps Count", "Active Minutes", "Campus Run", "Cycling", "Workout"
    val activityMetricValue: String = "", // e.g. "8,450 Steps", "45 Active Mins", "QR Video Proof"
    val studentNotes: String = "",
    val awardedPoints: Int? = null, // Points granted by admin (supports partial or custom points)
    val videoProvider: String = "local", // "local", "google_drive", "firebase", "supabase"
    val driveFileId: String = "",
    val driveUrl: String = ""
) {
    constructor(
        submissionId: String = UUID.randomUUID().toString(),
        userId: String = "",
        studentName: String = "",
        classId: String = "",
        questCode: String = "",
        questTitle: String = "",
        videoPath: String = "",
        pointValue: Int = 0,
        status: String = "pending",
        submittedAt: Any?,
        reviewedAt: Any? = null,
        reviewerNote: String? = null,
        submissionType: String = "video",
        imagePath: String = "",
        activityType: String = "",
        activityMetricValue: String = "",
        studentNotes: String = "",
        awardedPoints: Int? = null,
        videoProvider: String = "local",
        driveFileId: String = "",
        driveUrl: String = ""
    ) : this(
        submissionId = submissionId,
        userId = userId,
        studentName = studentName,
        classId = classId,
        questCode = questCode,
        questTitle = questTitle,
        videoPath = videoPath,
        pointValue = pointValue,
        status = status,
        submittedAt = DateTimeUtils.parseToEpochMillis(submittedAt),
        reviewedAt = if (reviewedAt != null) DateTimeUtils.parseToEpochMillis(reviewedAt) else null,
        reviewerNote = reviewerNote,
        submissionType = submissionType,
        imagePath = imagePath,
        activityType = activityType,
        activityMetricValue = activityMetricValue,
        studentNotes = studentNotes,
        awardedPoints = awardedPoints,
        videoProvider = videoProvider,
        driveFileId = driveFileId,
        driveUrl = driveUrl
    )
    fun getEffectiveActivityType(): String {
        if (activityType.isNotBlank() && !activityType.equals("Steps Count", ignoreCase = true)) {
            return activityType
        }
        if (submissionType == "video") return "Campus Video Quest"
        return activityType.ifBlank { "Steps Count" }
    }

    fun getEffectiveMetricValue(): String {
        if (activityMetricValue.isNotBlank()) return activityMetricValue
        if (submissionType == "video") return "QR Video Proof ($pointValue pts)"
        return "Logged Activity"
    }
}

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val requiredApprovedCount: Int = 0,
    val category: String = "Milestone"
)

object CollegeCatalog {
    val COLLEGES = listOf(
        College(
            code = "CITE",
            name = "College of Information Technology and Engineering",
            programs = listOf(
                "BS Architecture",
                "BS Computer Engineering",
                "BS Computer Science",
                "BS Electrical Engineering",
                "BS Electronics Engineering",
                "BS Entertainment and Multimedia Computing",
                "BS Industrial Engineering",
                "BS Information Technology",
                "Associate in Computer Technology"
            )
        ),
        College(
            code = "CBEAM",
            name = "College of Business, Economics, Accountancy and Management",
            programs = listOf(
                "BS Accountancy",
                "BS Accounting Information System",
                "BS Legal Management",
                "BS Entrepreneurship",
                "BS Management Technology",
                "BSBA Financial Management",
                "BSBA Marketing Management",
                "Certificate in Entrepreneurship"
            )
        ),
        College(
            code = "CEAS",
            name = "College of Education, Arts and Sciences",
            programs = listOf(
                "Bachelor of Elementary Education",
                "Bachelor of Secondary Education",
                "AB Communication",
                "Bachelor of Multimedia Arts",
                "BS Biology",
                "BS Psychology",
                "Master of Arts in Education major in Educational Management",
                "Master of Arts in Education major in English",
                "Master of Arts in Education major in Filipino",
                "Master of Arts in Education major in Mathematics"
            )
        ),
        College(
            code = "CIHTM",
            name = "College of International Hospitality and Tourism Management",
            programs = listOf(
                "BS Hospitality Management",
                "BS Tourism Management",
                "Cookery NC II (Culinary Arts)",
                "Certificate in Hospitality Management"
            )
        ),
        College(
            code = "CON",
            name = "College of Nursing",
            programs = listOf(
                "BS Nursing"
            )
        ),
        College(
            code = "CCJE",
            name = "College of Criminal Justice Education",
            programs = listOf(
                "BS Forensic Science"
            )
        )
    )

    val YEAR_LEVELS = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
    val SECTIONS = listOf("Section A", "Section B", "Section C", "Section D", "Section E", "Section F", "Section G", "Section H", "Section I", "Section J", "Section 1", "Section 2")
}

object BadgeCatalog {
    val FIRST_STEPS = Badge("badge_first_steps", "First Steps", "Complete and get 1 approved fitness quest", "👟", 1, "Milestone")
    val EXPLORER = Badge("badge_explorer", "Explorer", "Reach 10 approved fitness quests on campus", "🧭", 10, "Milestone")
    val ADVENTURER = Badge("badge_adventurer", "Adventurer", "Conquer 25 approved fitness challenges", "⚔️", 25, "Mastery")
    val LEGEND = Badge("badge_legend", "Legend", "Achieve 50 verified fitness quests", "👑", 50, "Mastery")
    val STAMINA = Badge("badge_stamina", "Stamina Master", "Maintain a 5-day continuous wellness streak", "⚡", 0, "Streak")
    val FINAL_BOSS = Badge("badge_final_boss", "Final Boss Conqueror", "Finish the official 3KM Campus Fun Run", "🏆", 0, "Special")

    val ALL_BADGES = listOf(FIRST_STEPS, EXPLORER, ADVENTURER, LEGEND, STAMINA, FINAL_BOSS)

    fun checkNewBadges(currentApprovedCount: Int, existingBadges: List<String>): List<String> {
        val newBadges = mutableListOf<String>()
        if (currentApprovedCount >= 1 && !existingBadges.contains("First Steps")) newBadges.add("First Steps")
        if (currentApprovedCount >= 10 && !existingBadges.contains("Explorer")) newBadges.add("Explorer")
        if (currentApprovedCount >= 25 && !existingBadges.contains("Adventurer")) newBadges.add("Adventurer")
        if (currentApprovedCount >= 50 && !existingBadges.contains("Legend")) newBadges.add("Legend")
        return newBadges
    }
}

object SeedQuests {
    val QUEST_MAP = mapOf(
        // Quick Quests (50 pts, ~30s cap)
        "QQ-DIOKNO" to Quest(
            questCode = "QQ-DIOKNO",
            title = "Diokno Steps",
            instructions = "Perform 15 jumping jacks at the Sen. Jose Diokno Building entrance.",
            location = "Sen. Jose Diokno Building Entrance",
            category = "quick",
            pointValue = 50,
            maxSeconds = 30,
            active = true
        ),
        "QQ-CBEAM" to Quest(
            questCode = "QQ-CBEAM",
            title = "CBEAM Stretch",
            instructions = "Perform a 2-minute dynamic stretch outside CBEAM Hall.",
            location = "Outside CBEAM Hall",
            category = "quick",
            pointValue = 50,
            maxSeconds = 30,
            active = true
        ),
        "QQ-SLOUNGE" to Quest(
            questCode = "QQ-SLOUNGE",
            title = "South Lounge Squats",
            instructions = "Perform 10 bodyweight squats at the South Lounge.",
            location = "South Lounge",
            category = "quick",
            pointValue = 50,
            maxSeconds = 30,
            active = true
        ),
        "QQ-CANTEEN" to Quest(
            questCode = "QQ-CANTEEN",
            title = "Canteen Calorie Burn",
            instructions = "Perform 20 seconds of high knees near the BB Canteen.",
            location = "BB Canteen",
            category = "quick",
            pointValue = 50,
            maxSeconds = 30,
            active = true
        ),
        "QQ-MABINI" to Quest(
            questCode = "QQ-MABINI",
            title = "Mabini Mini Break",
            instructions = "Perform 15 wall or incline push-ups near Apolinario Mabini Hall.",
            location = "Apolinario Mabini Hall",
            category = "quick",
            pointValue = 50,
            maxSeconds = 30,
            active = true
        ),
        "QQ-SIDEGATE" to Quest(
            questCode = "QQ-SIDEGATE",
            title = "Side Gate Lunges",
            instructions = "Perform 12 walking lunges (6 per leg) at the side gate area or around the BB lobby walkway.",
            location = "Side Gate Area / BB Lobby",
            category = "quick",
            pointValue = 50,
            maxSeconds = 30,
            active = true
        ),

        // Explorer Quests (100 pts, ~60–90s cap)
        "EQ-LRC" to Quest(
            questCode = "EQ-LRC",
            title = "LRC Stair Climb",
            instructions = "Climb the Diokno Building's stairs twice at a steady pace.",
            location = "Sen. Jose Diokno Building (LRC)",
            category = "explorer",
            pointValue = 100,
            maxSeconds = 60,
            active = true
        ),
        "EQ-NEXUS" to Quest(
            questCode = "EQ-NEXUS",
            title = "Nexus Walk",
            instructions = "Walk to Nexus Innovation Labs and back from the Student Center.",
            location = "Nexus Innovation Labs / Student Center",
            category = "explorer",
            pointValue = 100,
            maxSeconds = 90,
            active = true
        ),
        "EQ-OVAL" to Quest(
            questCode = "EQ-OVAL",
            title = "Track Oval Lap",
            instructions = "Complete one lap (400m) around the DLSL Track Oval.",
            location = "DLSL Track Oval",
            category = "explorer",
            pointValue = 100,
            maxSeconds = 90,
            active = true
        ),

        // Campus Boss Quests
        "AQ-RUN-START" to Quest(
            questCode = "AQ-RUN-START",
            title = "Boss Battle 3KM Run: Start Line",
            instructions = "Scan at the Campus Oval starting arch to initiate your official 3KM Fun Run timing.",
            location = "Campus Oval Starting Arch",
            category = "boss",
            pointValue = 0,
            maxSeconds = 15,
            active = true
        ),
        "AQ-RUN-FINISH" to Quest(
            questCode = "AQ-RUN-FINISH",
            title = "Boss Battle 3KM Run: Finish Line",
            instructions = "Scan at the finish banner to claim victory, award the Final Boss badge, and earn 300 points for your class!",
            location = "Campus Oval Finish Arch",
            category = "boss",
            pointValue = 300,
            maxSeconds = 15,
            active = true
        )
    )
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val classId: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "Quest", // Quest, Review, Streak, System, Leaderboard, Academic, Badge
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedId: String = ""
)

data class ActivityJourneyItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "Quest", // Quest, Workout, Streak, Steps
    val pointValue: Int = 50,
    val timestamp: Long = System.currentTimeMillis(),
    val durationOrMetric: String = "Completed",
    val status: String = "Approved" // Approved, Pending
)

