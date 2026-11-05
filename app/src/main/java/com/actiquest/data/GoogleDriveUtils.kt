package com.actiquest.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.util.regex.Pattern

/**
 * Utility for parsing, validating, and launching Google Drive video links.
 */
object GoogleDriveUtils {
    private const val TAG = "GoogleDriveUtils"

    // Supported Google Drive video link patterns:
    // 1. https://drive.google.com/file/d/FILE_ID/view...
    // 2. https://drive.google.com/open?id=FILE_ID
    // 3. https://drive.google.com/uc?id=FILE_ID
    // 4. https://docs.google.com/file/d/FILE_ID
    private val FILE_D_PATTERN: Pattern = Pattern.compile(
        "^https://(?:drive|docs)\\.google\\.com/file/d/([a-zA-Z0-9_-]{15,})(?:/.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    private val OPEN_ID_PATTERN: Pattern = Pattern.compile(
        "^https://drive\\.google\\.com/(?:open|uc)\\?(?:.*&)?id=([a-zA-Z0-9_-]{15,})(?:&.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Extracts Google Drive File ID from supported URLs.
     * Returns null if URL is invalid, non-HTTPS, or from unsupported domains.
     */
    fun extractDriveFileId(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim()

        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return null
        }

        val matcher1 = FILE_D_PATTERN.matcher(trimmed)
        if (matcher1.matches()) {
            return matcher1.group(1)
        }

        val matcher2 = OPEN_ID_PATTERN.matcher(trimmed)
        if (matcher2.matches()) {
            return matcher2.group(1)
        }

        return null
    }

    /**
     * Validates whether a given URL is a legitimate Google Drive file URL.
     */
    fun isValidGoogleDriveUrl(rawUrl: String?): Boolean {
        return extractDriveFileId(rawUrl) != null
    }

    /**
     * Constructs the canonical Google Drive viewing URL from a file ID or raw URL.
     */
    fun buildCanonicalDriveUrl(fileIdOrUrl: String): String {
        val fileId = extractDriveFileId(fileIdOrUrl) ?: fileIdOrUrl.trim()
        return "https://drive.google.com/file/d/$fileId/view"
    }

    /**
     * Opens the Google Drive URL via an Android ACTION_VIEW intent.
     * Tries the official Google Drive application first, with fallback to web browser.
     */
    fun openInGoogleDrive(context: Context, driveUrl: String): Boolean {
        val canonicalUrl = if (isValidGoogleDriveUrl(driveUrl)) {
            buildCanonicalDriveUrl(driveUrl)
        } else {
            driveUrl.trim()
        }

        if (canonicalUrl.isBlank()) {
            Toast.makeText(context, "Error: Google Drive URL is empty.", Toast.LENGTH_SHORT).show()
            return false
        }

        val uri = Uri.parse(canonicalUrl)

        // Attempt to target Google Drive app directly
        val driveIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.docs")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (driveIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(driveIntent)
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google Drive app launch failed, falling back to browser chooser", e)
        }

        // Generic browser fallback
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Google Drive link in browser: ${e.message}", e)
            Toast.makeText(
                context,
                "Unable to open link. Please verify a web browser or Google Drive is installed.",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
