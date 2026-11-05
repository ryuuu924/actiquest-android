package com.actiquest.data

/**
 * Centralized configuration for Google Drive Video Evidence integration.
 * Defines the designated administrator email and required permissions.
 */
object GoogleDriveConfig {
    /**
     * The designated ActiQuest administrator email address that students share video evidence with.
     * Can be updated dynamically or customized for specific campus physical education departments.
     */
    var adminReviewerEmail: String = "actiquest.admin@gmail.com"

    const val REQUIRED_ROLE: String = "Viewer"
    const val REQUIRED_GENERAL_ACCESS: String = "Restricted"
}
