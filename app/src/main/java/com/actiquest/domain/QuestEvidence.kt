package com.actiquest.domain

import java.util.Locale

enum class QuestEvidenceMode {
    FULL_ACTIVITY,
    REPRESENTATIVE_CLIP,
    CHECKPOINT
}

data class QuestEvidenceConfiguration(
    val activityDurationSeconds: Int?,
    val mode: QuestEvidenceMode,
    val minimumEvidenceDurationSeconds: Int,
    val maximumEvidenceDurationSeconds: Int,
    val maximumVideoSizeBytes: Long
)

fun QuestEvidenceConfiguration.validateEvidence(durationMillis: Long, sizeBytes: Long) {
    require(mode != QuestEvidenceMode.CHECKPOINT) {
        "This quest is verified by its QR checkpoint and does not accept video evidence."
    }
    require(durationMillis <= maximumEvidenceDurationSeconds * 1000L) {
        "This video exceeds the ${maximumEvidenceDurationSeconds}-second maximum for this quest."
    }
    require(sizeBytes in 1L..maximumVideoSizeBytes) {
        "This video is empty or exceeds this quest's file-size limit."
    }
}

private const val DEFAULT_MAX_VIDEO_SIZE_BYTES = 50L * 1024L * 1024L
private const val MAX_EVIDENCE_DURATION_SECONDS = 600

private val seedQuestEvidenceConfigurations = mapOf(
    "QQ-DIOKNO" to QuestEvidenceConfiguration(null, QuestEvidenceMode.FULL_ACTIVITY, 0, 45, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "QQ-CBEAM" to QuestEvidenceConfiguration(120, QuestEvidenceMode.FULL_ACTIVITY, 0, 150, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "QQ-SLOUNGE" to QuestEvidenceConfiguration(null, QuestEvidenceMode.FULL_ACTIVITY, 0, 60, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "QQ-CANTEEN" to QuestEvidenceConfiguration(20, QuestEvidenceMode.FULL_ACTIVITY, 0, 45, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "QQ-MABINI" to QuestEvidenceConfiguration(null, QuestEvidenceMode.FULL_ACTIVITY, 0, 75, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "QQ-SIDEGATE" to QuestEvidenceConfiguration(null, QuestEvidenceMode.FULL_ACTIVITY, 0, 90, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "EQ-LRC" to QuestEvidenceConfiguration(null, QuestEvidenceMode.FULL_ACTIVITY, 0, 600, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "EQ-NEXUS" to QuestEvidenceConfiguration(null, QuestEvidenceMode.REPRESENTATIVE_CLIP, 0, 90, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "EQ-OVAL" to QuestEvidenceConfiguration(null, QuestEvidenceMode.FULL_ACTIVITY, 0, 600, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "AQ-RUN-START" to QuestEvidenceConfiguration(null, QuestEvidenceMode.CHECKPOINT, 0, 0, DEFAULT_MAX_VIDEO_SIZE_BYTES),
    "AQ-RUN-FINISH" to QuestEvidenceConfiguration(null, QuestEvidenceMode.REPRESENTATIVE_CLIP, 0, 15, DEFAULT_MAX_VIDEO_SIZE_BYTES)
)

fun Quest.evidenceConfiguration(): QuestEvidenceConfiguration {
    val code = questCode.trim().uppercase(Locale.ROOT)
    seedQuestEvidenceConfigurations[code]?.let { return it }

    val configuredMode = QuestEvidenceMode.values().firstOrNull {
        it.name.equals(evidenceMode.trim(), ignoreCase = true)
    }
    if (configuredMode != null && maximumEvidenceDurationSeconds > 0) {
        val maximumDuration = maximumEvidenceDurationSeconds.coerceIn(1, MAX_EVIDENCE_DURATION_SECONDS)
        return QuestEvidenceConfiguration(
            activityDurationSeconds = activityDurationSeconds?.takeIf { it > 0 },
            mode = configuredMode,
            minimumEvidenceDurationSeconds = 0,
            maximumEvidenceDurationSeconds = maximumDuration,
            maximumVideoSizeBytes = maximumVideoSizeBytes.takeIf { it > 0 }
                ?.coerceAtMost(DEFAULT_MAX_VIDEO_SIZE_BYTES)
                ?: DEFAULT_MAX_VIDEO_SIZE_BYTES
        )
    }

    val fallbackMaximumDuration = maxSeconds.coerceIn(1, MAX_EVIDENCE_DURATION_SECONDS)
    return QuestEvidenceConfiguration(
        activityDurationSeconds = activityDurationSeconds?.takeIf { it > 0 },
        mode = configuredMode ?: QuestEvidenceMode.REPRESENTATIVE_CLIP,
        minimumEvidenceDurationSeconds = 0,
        maximumEvidenceDurationSeconds = maximumEvidenceDurationSeconds.takeIf { it > 0 }
            ?.coerceIn(1, MAX_EVIDENCE_DURATION_SECONDS)
            ?: fallbackMaximumDuration,
        maximumVideoSizeBytes = maximumVideoSizeBytes.takeIf { it > 0 }
            ?.coerceAtMost(DEFAULT_MAX_VIDEO_SIZE_BYTES)
            ?: DEFAULT_MAX_VIDEO_SIZE_BYTES
    )
}

fun Quest.withResolvedEvidenceConfiguration(): Quest {
    val config = evidenceConfiguration()
    return copy(
        activityDurationSeconds = config.activityDurationSeconds,
        evidenceMode = config.mode.name,
        minimumEvidenceDurationSeconds = config.minimumEvidenceDurationSeconds,
        maximumEvidenceDurationSeconds = config.maximumEvidenceDurationSeconds,
        maximumVideoSizeBytes = config.maximumVideoSizeBytes,
        maxSeconds = config.maximumEvidenceDurationSeconds
    )
}

fun Quest.classificationLabel(): String = when (category.trim().lowercase(Locale.ROOT)) {
    "explorer" -> "EXPLORER QUEST"
    "boss" -> "BOSS QUEST"
    else -> "QUICK QUEST"
}

fun formatDurationLabel(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return when {
        minutes == 0 -> "${seconds}s"
        remainingSeconds == 0 -> "${minutes}m"
        else -> "${minutes}m ${remainingSeconds}s"
    }
}

fun Quest.activityDurationLabel(): String =
    evidenceConfiguration().activityDurationSeconds?.let { "Activity: ${formatDurationLabel(it)}" }
        ?: "Activity: self-paced"

fun Quest.evidenceDurationLabel(): String {
    val config = evidenceConfiguration()
    return when (config.mode) {
        QuestEvidenceMode.CHECKPOINT -> "QR checkpoint • no video required"
        QuestEvidenceMode.FULL_ACTIVITY -> {
            "Full activity • up to ${formatDurationLabel(config.maximumEvidenceDurationSeconds)}"
        }
        QuestEvidenceMode.REPRESENTATIVE_CLIP -> {
            "Representative clip • up to ${formatDurationLabel(config.maximumEvidenceDurationSeconds)}"
        }
    }
}

fun Quest.inclusiveAlternative(): String = when (questCode.trim().uppercase(Locale.ROOT)) {
    "QQ-DIOKNO" -> "Try step jacks instead of jumping, and keep a comfortable pace."
    "QQ-CBEAM" -> "Use a gentle range of motion, pause when needed, and skip any movement that feels uncomfortable."
    "QQ-SLOUNGE" -> "Use a chair or wall for support, reduce the squat depth, or take a rest between repetitions."
    "QQ-CANTEEN" -> "March in place at a comfortable pace instead of jogging the knees high."
    "QQ-MABINI" -> "Use a wall or higher stable surface for an easier push-up variation."
    "QQ-SIDEGATE" -> "Use supported stationary lunges, reduce the depth, or walk at a comfortable pace."
    "EQ-LRC" -> "Use the handrail and take breaks; choose an accessible route instead if stairs are not suitable."
    "EQ-NEXUS" -> "Use an accessible route or mobility aid, self-pace, and rest as needed."
    "EQ-OVAL" -> "Walk, roll, or use another accessible loop at a self-selected pace."
    "AQ-RUN-START", "AQ-RUN-FINISH" -> "Choose a comfortable pace, including walk-run intervals, and stop if you feel discomfort."
    else -> "Choose a comfortable, lower-intensity variation or pause when needed."
}
