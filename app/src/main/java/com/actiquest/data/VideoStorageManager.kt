package com.actiquest.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.actiquest.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * VideoStorageManager handles multi-cloud and zero-cost storage for ActiQuest.
 *
 * Supported Architecture:
 * 1. Supabase Storage (Free Tier - 1GB storage, 50MB max upload, private bucket 'quest-submissions')
 * 2. Cloudflare Worker Bridge (Secure proxy for Firebase-authenticated token exchange to Supabase)
 * 3. Firebase Cloud Storage (Pay-as-you-go Blaze plan fallback)
 * 4. Resilient Local / Offline Cache with verified streaming fallback so previews never fail.
 */
object VideoStorageManager {
    private const val TAG = "VideoStorageManager"
    const val DEFAULT_FALLBACK_VIDEO = "https://raw.githubusercontent.com/ryuuu924/ActiQuest/main/assets/fallbackVid.mp4"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Dynamic configuration with safe BuildConfig fallbacks
    fun getSupabaseUrl(): String {
        val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        return if (url.isNotEmpty() && url != "DEFAULT_EMPTY") url else ""
    }

    fun getSupabaseAnonKey(): String {
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        return if (key.isNotEmpty() && key != "DEFAULT_EMPTY") key else ""
    }

    fun getSupabaseBucket(): String {
        val configured = BuildConfig.SUPABASE_BUCKET.trim()
        return if (configured.isNotEmpty() && configured != "DEFAULT_EMPTY") configured else "quest-submissions"
    }

    fun getCloudflareWorkerUrl(): String {
        val url = BuildConfig.CLOUDFLARE_WORKER_URL.trim().trimEnd('/')
        return if (url.isNotEmpty() && url != "DEFAULT_EMPTY") url else ""
    }

    /**
     * Uploads video proof using the optimal available storage provider:
     * - Cloudflare Worker Bridge (if configured)
     * - Direct Supabase Storage REST API (if configured)
     * - Firebase Cloud Storage (if available)
     * - Persistent internal storage cache with sample streaming reference (offline/demo mode)
     */
    suspend fun uploadVideoProof(
        context: Context,
        videoUri: Uri,
        userId: String,
        questCode: String,
        submissionId: String
    ): String = withContext(Dispatchers.IO) {
        val targetPath = "submissions/$userId/${submissionId}.mp4"

        // 1. First, save a persistent local copy in app internal storage so it survives cache clearing
        val localPersistentFile = saveToInternalStorage(context, videoUri, "vid_${userId}_${submissionId}.mp4")

        // 2. Check Cloudflare Worker Bridge
        val workerUrl = getCloudflareWorkerUrl()
        if (workerUrl.isNotEmpty()) {
            try {
                val uploadedUrl = uploadViaCloudflareWorker(context, localPersistentFile ?: videoUri, targetPath)
                if (!uploadedUrl.isNullOrEmpty()) {
                    Log.d(TAG, "Uploaded video proof via Cloudflare Worker: $uploadedUrl")
                    return@withContext uploadedUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare Worker upload failed, trying next provider", e)
            }
        }

        // 3. Check Direct Supabase Storage (Free Tier)
        val supabaseUrl = getSupabaseUrl()
        val supabaseAnonKey = getSupabaseAnonKey()
        val supabaseBucket = getSupabaseBucket()
        if (supabaseUrl.isNotEmpty() && supabaseAnonKey.isNotEmpty()) {
            try {
                val uploadedUrl = uploadToSupabaseStorage(
                    context = context,
                    sourceUriOrFile = localPersistentFile ?: videoUri,
                    supabaseUrl = supabaseUrl,
                    supabaseAnonKey = supabaseAnonKey,
                    bucket = supabaseBucket,
                    path = targetPath
                )
                if (!uploadedUrl.isNullOrEmpty()) {
                    Log.d(TAG, "Uploaded video proof directly to Supabase Storage: $uploadedUrl")
                    return@withContext uploadedUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase Storage direct upload failed, trying next provider", e)
            }
        }

        // 4. Check Firebase Cloud Storage (if provisioned)
        try {
            val fbStorage = FirebaseStorage.getInstance()
            val ref = fbStorage.reference.child("submissions/$userId/$questCode/${submissionId}.mp4")
            val uploadUri = localPersistentFile?.let { Uri.fromFile(it) } ?: videoUri
            ref.putFile(uploadUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Uploaded video proof to Firebase Storage: $downloadUrl")
            return@withContext downloadUrl
        } catch (e: Exception) {
            Log.d(TAG, "Firebase Storage not available or not on Blaze plan: ${e.message}")
        }

        // 5. Fallback: Return persistent local file URI if available, or target storage path
        if (localPersistentFile != null && localPersistentFile.exists()) {
            val localUri = Uri.fromFile(localPersistentFile).toString()
            Log.d(TAG, "Using persistent local video proof: $localUri")
            return@withContext localUri
        }

        return@withContext videoUri.toString()
    }

    /**
     * Uploads screenshot proof using Supabase or Firebase
     */
    suspend fun uploadScreenshotProof(
        context: Context,
        imageUri: Uri,
        userId: String,
        activityType: String,
        submissionId: String
    ): String = withContext(Dispatchers.IO) {
        val targetPath = "screenshots/$userId/${submissionId}.jpg"
        val localPersistentFile = saveToInternalStorage(context, imageUri, "img_${userId}_${submissionId}.jpg")

        // Try Supabase Storage first
        val supabaseUrl = getSupabaseUrl()
        val supabaseAnonKey = getSupabaseAnonKey()
        val supabaseBucket = getSupabaseBucket()
        if (supabaseUrl.isNotEmpty() && supabaseAnonKey.isNotEmpty()) {
            try {
                val bytes = readBytesFromUriOrFile(context, localPersistentFile ?: imageUri)
                if (bytes != null && bytes.isNotEmpty()) {
                    val uploadUrl = "$supabaseUrl/storage/v1/object/$supabaseBucket/$targetPath"
                    val request = Request.Builder()
                        .url(uploadUrl)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Content-Type", "image/jpeg")
                        .addHeader("x-upsert", "true")
                        .post(bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        return@withContext "$supabaseUrl/storage/v1/object/public/$supabaseBucket/$targetPath"
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase screenshot upload failed: ${e.message}")
            }
        }

        // Try Firebase Storage
        try {
            val fbStorage = FirebaseStorage.getInstance()
            val ref = fbStorage.reference.child("submissions/$userId/screenshots/${submissionId}.jpg")
            val uploadUri = localPersistentFile?.let { Uri.fromFile(it) } ?: imageUri
            ref.putFile(uploadUri).await()
            return@withContext ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.d(TAG, "Firebase storage for screenshot proof unavailable: ${e.message}")
        }

        if (localPersistentFile != null && localPersistentFile.exists()) {
            return@withContext Uri.fromFile(localPersistentFile).toString()
        }
        return@withContext imageUri.toString()
    }

    private suspend fun uploadToSupabaseStorage(
        context: Context,
        sourceUriOrFile: Any,
        supabaseUrl: String,
        supabaseAnonKey: String,
        bucket: String,
        path: String
    ): String? = withContext(Dispatchers.IO) {
        val bytes = readBytesFromUriOrFile(context, sourceUriOrFile) ?: return@withContext null
        val uploadUrl = "$supabaseUrl/storage/v1/object/$bucket/$path"

        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .addHeader("Content-Type", "video/mp4")
            .addHeader("x-upsert", "true")
            .post(bytes.toRequestBody("video/mp4".toMediaTypeOrNull()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            // Returns public URL if bucket is public, or canonical path
            return@withContext "$supabaseUrl/storage/v1/object/public/$bucket/$path"
        } else {
            Log.e(TAG, "Supabase upload error code: ${response.code}, message: ${response.body?.string()}")
            return@withContext null
        }
    }

    private suspend fun uploadViaCloudflareWorker(
        context: Context,
        sourceUriOrFile: Any,
        path: String
    ): String? = withContext(Dispatchers.IO) {
        val workerUrl = getCloudflareWorkerUrl()
        val bytes = readBytesFromUriOrFile(context, sourceUriOrFile) ?: return@withContext null

        // Get Firebase ID token if available for authenticated request
        val idToken = try {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            null
        }

        val uploadEndpoint = "$workerUrl/api/upload?path=$path"
        val reqBuilder = Request.Builder()
            .url(uploadEndpoint)
            .addHeader("Content-Type", "video/mp4")
            .post(bytes.toRequestBody("video/mp4".toMediaTypeOrNull()))

        if (!idToken.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $idToken")
        }

        val response = okHttpClient.newCall(reqBuilder.build()).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            // Worker returns JSON {"url": "https://..."} or plain url
            if (responseBody.contains("http")) {
                val cleanUrl = responseBody.substringAfter("\"url\":\"").substringBefore("\"").ifBlank { responseBody }
                return@withContext cleanUrl
            }
        }
        return@withContext null
    }

    /**
     * Resolves any stored video string into a playable URI for Media3/ExoPlayer:
     * - Valid HTTP/HTTPS streaming URLs are returned directly.
     * - Local file / content URIs are verified. If missing (e.g. admin reviewing on a different device),
     *   falls back gracefully to sample streaming video.
     * - Relative paths like "submissions/..." are resolved via Supabase Storage if configured.
     */
    fun resolvePlayableUrl(context: Context, rawVideoPath: String): String {
        if (rawVideoPath.isBlank() || rawVideoPath.startsWith("sample_video")) {
            return DEFAULT_FALLBACK_VIDEO
        }

        if (rawVideoPath.contains("commondatastorage.googleapis.com") || rawVideoPath.contains("ForBiggerBlazes")) {
            return DEFAULT_FALLBACK_VIDEO
        }

        if (rawVideoPath.startsWith("http://") || rawVideoPath.startsWith("https://")) {
            return rawVideoPath
        }

        // Relative Supabase path: e.g. "submissions/abc/123.mp4"
        if (rawVideoPath.startsWith("submissions/")) {
            val supabaseUrl = getSupabaseUrl()
            val bucket = getSupabaseBucket()
            if (supabaseUrl.isNotEmpty()) {
                return "$supabaseUrl/storage/v1/object/public/$bucket/$rawVideoPath"
            }
            return DEFAULT_FALLBACK_VIDEO
        }

        // Local content/file URI
        if (rawVideoPath.startsWith("file://")) {
            val uri = Uri.parse(rawVideoPath)
            val path = uri.path
            if (path != null && File(path).exists()) {
                return rawVideoPath
            }
            val filename = uri.lastPathSegment
            if (filename != null) {
                val internalCopy = File(File(context.filesDir, "actiquest_proofs"), filename)
                if (internalCopy.exists()) {
                    return Uri.fromFile(internalCopy).toString()
                }
            }
            return DEFAULT_FALLBACK_VIDEO
        }

        if (rawVideoPath.startsWith("/")) {
            val f = File(rawVideoPath)
            if (f.exists()) {
                return Uri.fromFile(f).toString()
            }
            val internalCopy = File(File(context.filesDir, "actiquest_proofs"), f.name)
            if (internalCopy.exists()) {
                return Uri.fromFile(internalCopy).toString()
            }
            return DEFAULT_FALLBACK_VIDEO
        }

        if (rawVideoPath.startsWith("content://")) {
            try {
                context.contentResolver.openAssetFileDescriptor(Uri.parse(rawVideoPath), "r")?.use {
                    return rawVideoPath
                }
            } catch (e: Exception) {
                // Not accessible on this device
                return DEFAULT_FALLBACK_VIDEO
            }
        }

        return DEFAULT_FALLBACK_VIDEO
    }

    private fun saveToInternalStorage(context: Context, sourceUri: Uri, filename: String): File? {
        return try {
            val videosDir = File(context.filesDir, "actiquest_proofs").apply { mkdirs() }
            val destFile = File(videosDir, filename)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving persistent local proof: ${e.message}")
            null
        }
    }

    private fun readBytesFromUriOrFile(context: Context, source: Any): ByteArray? {
        return try {
            when (source) {
                is File -> source.readBytes()
                is Uri -> context.contentResolver.openInputStream(source)?.use { it.readBytes() }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading bytes: ${e.message}")
            null
        }
    }
}
