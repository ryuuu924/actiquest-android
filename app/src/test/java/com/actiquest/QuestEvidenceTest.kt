package com.actiquest

import com.actiquest.domain.Quest
import com.actiquest.domain.QuestEvidenceMode
import com.actiquest.domain.SeedQuests
import com.actiquest.domain.activityDurationLabel
import com.actiquest.domain.classificationLabel
import com.actiquest.domain.evidenceConfiguration
import com.actiquest.domain.evidenceDurationLabel
import com.actiquest.domain.validateEvidence
import com.actiquest.domain.withResolvedEvidenceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestEvidenceTest {
    @Test
    fun everySeedQuestResolvesToAValidEvidencePolicy() {
        assertEquals(11, SeedQuests.QUEST_MAP.size)
        SeedQuests.QUEST_MAP.values.forEach { quest ->
            val configuration = quest.evidenceConfiguration()
            if (configuration.mode == QuestEvidenceMode.CHECKPOINT) {
                assertEquals(0, configuration.maximumEvidenceDurationSeconds)
            } else {
                assertEquals(0, configuration.minimumEvidenceDurationSeconds)
                assertTrue(configuration.maximumEvidenceDurationSeconds <= 600)
            }
            assertTrue(configuration.maximumVideoSizeBytes > 0)
        }
    }

    @Test
    fun twoMinuteQuestKeepsItsMaximumWithoutRequiringAMinimumClip() {
        val quest = SeedQuests.QUEST_MAP.getValue("QQ-CBEAM")
        val configuration = quest.evidenceConfiguration()

        assertEquals(120, configuration.activityDurationSeconds)
        assertEquals(QuestEvidenceMode.FULL_ACTIVITY, configuration.mode)
        assertEquals(0, configuration.minimumEvidenceDurationSeconds)
        assertEquals(150, configuration.maximumEvidenceDurationSeconds)
        assertEquals("Activity: 2m", quest.activityDurationLabel())
        assertEquals("Full activity • up to 2m 30s", quest.evidenceDurationLabel())
    }

    @Test
    fun qrStartCheckpointDoesNotRequireVideoEvidence() {
        val quest = SeedQuests.QUEST_MAP.getValue("AQ-RUN-START")
        val configuration = quest.evidenceConfiguration()

        assertEquals(QuestEvidenceMode.CHECKPOINT, configuration.mode)
        assertEquals(0, configuration.maximumEvidenceDurationSeconds)
        assertEquals("QR checkpoint • no video required", quest.evidenceDurationLabel())
    }

    @Test
    fun localVideoValidationAcceptsAnyDurationUpToTheMaximumAndRejectsViolations() {
        val configuration = SeedQuests.QUEST_MAP.getValue("QQ-CBEAM").evidenceConfiguration()

        configuration.validateEvidence(durationMillis = 0, sizeBytes = 1)
        configuration.validateEvidence(durationMillis = 1, sizeBytes = 1)
        configuration.validateEvidence(durationMillis = 1_000, sizeBytes = 1)
        configuration.validateEvidence(durationMillis = 120_000, sizeBytes = 1)
        configuration.validateEvidence(durationMillis = 150_000, sizeBytes = configuration.maximumVideoSizeBytes)
        assertRejected { configuration.validateEvidence(durationMillis = 150_001, sizeBytes = 1) }
        assertRejected {
            configuration.validateEvidence(
                durationMillis = 1,
                sizeBytes = configuration.maximumVideoSizeBytes + 1
            )
        }
    }

    @Test
    fun checkpointEvidenceRejectsVideoSubmission() {
        val configuration = SeedQuests.QUEST_MAP.getValue("AQ-RUN-START").evidenceConfiguration()
        assertRejected { configuration.validateEvidence(durationMillis = 1_000, sizeBytes = 1) }
    }

    @Test
    fun legacyQuestWithoutEvidenceFieldsRetainsItsExistingMaximum() {
        val quest = Quest(questCode = "OLD-QUEST", maxSeconds = 75)
        val resolved = quest.withResolvedEvidenceConfiguration()

        assertEquals(75, resolved.maxSeconds)
        assertEquals(75, resolved.evidenceConfiguration().maximumEvidenceDurationSeconds)
        assertEquals(0, resolved.evidenceConfiguration().minimumEvidenceDurationSeconds)
        assertEquals("QUICK QUEST", resolved.classificationLabel())
    }

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            fail("Expected the evidence policy to reject this video.")
        } catch (_: IllegalArgumentException) {
            // Expected policy rejection.
        }
    }
}
