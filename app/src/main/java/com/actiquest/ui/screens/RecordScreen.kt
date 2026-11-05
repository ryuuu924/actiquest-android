package com.actiquest.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.actiquest.data.GoogleDriveConfig
import com.actiquest.data.GoogleDriveUtils
import com.actiquest.data.Repository
import com.actiquest.domain.Quest
import com.actiquest.domain.activityDurationLabel
import com.actiquest.domain.evidenceConfiguration
import com.actiquest.domain.evidenceDurationLabel
import com.actiquest.domain.validateEvidence
import com.actiquest.ui.components.VideoPlayerView
import com.actiquest.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class LocalVideoMetadata(
    val durationMillis: Long,
    val sizeBytes: Long
)

private suspend fun inspectLocalVideo(
    context: android.content.Context,
    uri: Uri,
    maximumSizeBytes: Long
): LocalVideoMetadata = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val durationMillis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?: throw IllegalArgumentException("The selected video duration could not be read.")

        var sizeBytes = 0L
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("The selected video file could not be opened.")
        input.use { stream ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                sizeBytes += read
                if (sizeBytes > maximumSizeBytes) {
                    throw IllegalArgumentException(
                        "This video exceeds the ${maximumSizeBytes / (1024 * 1024)} MB quest limit."
                    )
                }
            }
        }

        LocalVideoMetadata(durationMillis, sizeBytes)
    } finally {
        retriever.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(navController: NavController, questCode: String, maxSeconds: Int) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val warningColor = if (ThemeManager.isDarkMode) ArcadeGold else GoldAccent

    var quest by remember { mutableStateOf<Quest?>(null) }
    LaunchedEffect(questCode) {
        quest = Repository.getQuest(questCode)
    }
    val evidenceConfiguration = quest?.evidenceConfiguration()
        ?: Quest(questCode = questCode, maxSeconds = maxSeconds).evidenceConfiguration()
    val recordingMaximumSeconds = evidenceConfiguration.maximumEvidenceDurationSeconds
        .takeIf { it > 0 }
        ?: maxSeconds.coerceIn(1, 600)
    val maximumVideoSizeBytes = evidenceConfiguration.maximumVideoSizeBytes

    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasPermissions = perms[Manifest.permission.CAMERA] == true && perms[Manifest.permission.RECORD_AUDIO] == true
    }

    var activeTab by remember { mutableStateOf("drive") } // "drive" or "camera"
    var driveUrlInput by remember { mutableStateOf("") }

    LaunchedEffect(activeTab) {
        if (activeTab == "camera" && !hasPermissions) {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    var recording by remember { mutableStateOf<Recording?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(maxSeconds.coerceIn(1, 600)) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    val pickVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            videoUri = uri
        }
    }
    var isSubmitting by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e("RecordScreen", "Failed to get ProcessCameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Bind camera whenever lens facing, previewView, cameraProvider, or permissions change
    LaunchedEffect(lensFacing, previewView, cameraProvider, hasPermissions) {
        val pView = previewView ?: return@LaunchedEffect
        val provider = cameraProvider ?: return@LaunchedEffect
        if (!hasPermissions) return@LaunchedEffect

        try {
            provider.unbindAll()

            val desiredSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val finalSelector = if (provider.hasCamera(desiredSelector)) {
                desiredSelector
            } else {
                val fallbackSelector = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                if (provider.hasCamera(fallbackSelector)) {
                    fallbackSelector
                } else {
                    desiredSelector
                }
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = pView.surfaceProvider
            }

            val targetBitrate = (
                maximumVideoSizeBytes * 8L * 85L /
                    recordingMaximumSeconds.coerceAtLeast(1) / 100L
                ).coerceIn(250_000L, 3_000_000L).toInt()
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                .setTargetVideoEncodingBitRate(targetBitrate)
                .build()
            val capture = VideoCapture.withOutput(recorder)
            videoCapture = capture

            provider.bindToLifecycle(
                lifecycleOwner,
                finalSelector,
                preview,
                capture
            )
        } catch (exc: Exception) {
            Log.e("RecordScreen", "Use case binding failed for lens $lensFacing", exc)
        }
    }

    // Intercept hardware back button
    BackHandler {
        if (isRecording || videoUri != null) {
            showDiscardDialog = true
        } else {
            navController.popBackStack()
        }
    }

    // Countdown Timer logic
    LaunchedEffect(recordingMaximumSeconds, isRecording) {
        if (!isRecording) {
            timeLeft = recordingMaximumSeconds
        }
        if (isRecording) {
            val startedAt = SystemClock.elapsedRealtime()
            val maximumDurationMillis = recordingMaximumSeconds * 1000L
            val maximumRecordingSizeBytes = maximumVideoSizeBytes * 95L / 100L
            while (isRecording) {
                val elapsedMillis = SystemClock.elapsedRealtime() - startedAt
                timeLeft = ((maximumDurationMillis - elapsedMillis).coerceAtLeast(0L) + 999L)
                    .div(1000L)
                    .toInt()
                val fileSizeLimitReached =
                    recordingFile?.length()?.let { it >= maximumRecordingSizeBytes } == true
                if (elapsedMillis >= maximumDurationMillis || fileSizeLimitReached) break
                delay(250)
            }
            if (isRecording) {
                if (recordingFile?.length()?.let { it >= maximumRecordingSizeBytes } == true) {
                    Toast.makeText(context, "Recording stopped at the quest file-size limit.", Toast.LENGTH_LONG).show()
                }
                recording?.stop()
                recording = null
                isRecording = false
            }
        }
    }

    // Discard Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text("Discard Video Recording?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "Are you sure you want to exit? Any unsubmitted video for this quest will be discarded.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        recording?.stop()
                        navController.popBackStack()
                    }
                ) {
                    Text("Discard", color = StatusRejected, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Recording", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    BackHandler {
        if (isRecording || videoUri != null) {
            showDiscardDialog = true
        } else {
            if (!navController.popBackStack()) {
                navController.navigate("home") { popUpTo(0) }
            }
        }
    }

    // 1. Google Drive Link Submission View
    if (activeTab == "drive" && videoUri == null) {
        val isValidDriveUrl = GoogleDriveUtils.isValidGoogleDriveUrl(driveUrlInput)
        val fileId = GoogleDriveUtils.extractDriveFileId(driveUrlInput)
        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "SUBMIT QUEST EVIDENCE",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate("home") { popUpTo(0) }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Quest Summary Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CobaltBlue.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue)
                            ) {
                                Text(
                                    text = questCode,
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MintEmerald.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald)
                            ) {
                                Text(
                                    text = "+${quest?.pointValue ?: 50} PTS",
                                    color = MintEmerald,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = quest?.title ?: "Campus Fitness Quest",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = quest?.instructions ?: "Complete the exercise reps at the landmark location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        quest?.let { currentQuest ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${currentQuest.activityDurationLabel()} • ${currentQuest.evidenceDurationLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submission Mode Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { activeTab = "drive" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == "drive") MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (activeTab == "drive") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google Drive Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { activeTab = "camera" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == "camera") MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (activeTab == "camera") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record in App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Google Drive Instructions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateSecondary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Google Drive Video Sharing Guide",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val steps = listOf(
                            "1. Upload your exercise video to your personal Google Drive (via app or drive.google.com).",
                            "2. Open file sharing settings and keep General access set to Restricted.",
                            "3. Share the file with the designated ActiQuest Reviewer as Viewer:",
                            "4. Copy the link (e.g., https://drive.google.com/file/d/.../view) and paste it below."
                        )

                        steps.take(3).forEach { step ->
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        // Highlighted Admin Email Chip with Copy Action
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CobaltBlue.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "REVIEWER GOOGLE ACCOUNT (VIEWER):",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ElectricBlue
                                    )
                                    Text(
                                        GoogleDriveConfig.adminReviewerEmail,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager?.setPrimaryClip(
                                            ClipData.newPlainText("Admin Email", GoogleDriveConfig.adminReviewerEmail)
                                        )
                                        Toast.makeText(context, "Reviewer email copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Email", tint = MintEmerald, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Text(
                            text = steps[3],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Self-Reported Notice
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = warningColor.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, warningColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = warningColor,
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Google Drive links are reviewed by faculty; this app cannot inspect the remote file's duration, size, or sharing settings. Confirm your video meets ${quest?.evidenceDurationLabel() ?: "the quest's evidence requirements"} and share it with ${GoogleDriveConfig.adminReviewerEmail} as Viewer. (example only)",
                                    fontSize = 11.sp,
                                    color = warningColor,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Field for Drive Link
                Text(
                    "Paste Google Drive Video URL",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = driveUrlInput,
                    onValueChange = { driveUrlInput = it.trim() },
                    placeholder = {
                        Text(
                            "https://drive.google.com/file/d/...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = if (isValidDriveUrl) MintEmerald else ElectricBlue
                        )
                    },
                    trailingIcon = {
                        if (driveUrlInput.isNotEmpty()) {
                            IconButton(onClick = { driveUrlInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    isError = driveUrlInput.isNotEmpty() && !isValidDriveUrl,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        errorBorderColor = StatusRejected,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Validation Status Indicator
                if (driveUrlInput.isNotEmpty()) {
                    if (isValidDriveUrl) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MintEmerald.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Valid Google Drive File ID: ${fileId?.take(15)}...",
                                    color = MintEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusRejected.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusRejected.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusRejected, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Invalid Drive URL. Use https://drive.google.com/file/d/FILE_ID/view",
                                    color = StatusRejected,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (!isValidDriveUrl || isSubmitting) return@Button
                        isSubmitting = true
                        coroutineScope.launch {
                            try {
                                val currentUser = Repository.currentUser.value
                                if (currentUser == null || currentUser.uid.isBlank()) {
                                    throw IllegalStateException("Please sign in again before submitting.")
                                }
                                val userId = currentUser.uid
                                val studentName = currentUser.name
                                val studentClassId = currentUser.classId

                                Repository.submitQuestDriveVideo(
                                    userId = userId,
                                    studentName = studentName,
                                    questCode = questCode,
                                    questTitle = quest?.title ?: questCode,
                                    pointValue = quest?.pointValue ?: 50,
                                    driveUrl = driveUrlInput,
                                    context = context,
                                    classId = studentClassId
                                )

                                Toast.makeText(
                                    context,
                                    "Google Drive Video Submitted for Verification!",
                                    Toast.LENGTH_LONG
                                ).show()

                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                Log.e("RecordScreen", "Submission failed", e)
                                Toast.makeText(context, "Submission failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = isValidDriveUrl && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = SlateSecondary.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Submitting to Review Desk...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Video Evidence", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }
        return
    }

    if (!hasPermissions) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Camera & Mic Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "To record your quest reps using the in-app camera, please grant camera and microphone access. Alternatively, you can upload your video to Google Drive and paste the link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Grant Permissions", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { pickVideoLauncher.launch("video/*") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Video File from Device", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { activeTab = "drive" },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Switch to Google Drive Link", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        return
    }

    if (videoUri != null) {
        // Video Preview & Submission Screen
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text("REVIEW VIDEO", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = { showDiscardDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VideoPlayerView(videoUriString = videoUri.toString())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            quest?.title ?: "Quest Challenge",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ready to submit for Admin review and earn +${quest?.pointValue ?: 50} pts for your class!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    videoUri = null
                                    timeLeft = recordingMaximumSeconds
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Retake",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    isSubmitting = true
                                    coroutineScope.launch {
                                        try {
                                            val currentUser = Repository.currentUser.value
                                            if (currentUser == null || currentUser.uid.isBlank()) {
                                                throw IllegalStateException("Please sign in again before submitting.")
                                            }
                                            val localVideo = videoUri
                                                ?: throw IllegalStateException("Select or record a video before submitting.")
                                            val metadata = inspectLocalVideo(context, localVideo, maximumVideoSizeBytes)
                                            evidenceConfiguration.validateEvidence(
                                                durationMillis = metadata.durationMillis,
                                                sizeBytes = metadata.sizeBytes
                                            )

                                            Repository.submitQuestVideo(
                                                userId = currentUser.uid,
                                                studentName = currentUser.name,
                                                questCode = questCode,
                                                questTitle = quest?.title ?: questCode,
                                                pointValue = quest?.pointValue ?: 50,
                                                videoUri = videoUri,
                                                context = context,
                                                classId = currentUser.classId
                                            )

                                            Toast.makeText(context, "Quest Video Submitted Successfully!", Toast.LENGTH_LONG).show()
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("RecordScreen", "Submission failed", e)
                                            Toast.makeText(context, "Submission failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(22.dp))
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Submit Quest", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Live Camera Recording View
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also {
                    previewView = it
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isRecording) showDiscardDialog = true else navController.popBackStack()
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // Countdown Timer Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isRecording) StatusRejected.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecording) StatusRejected else SlateSecondary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.White else MintEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${timeLeft}s / ${recordingMaximumSeconds}s",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }

            // Flip Camera Toggle Button
            IconButton(
                onClick = {
                    if (!isRecording) {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    } else {
                        Toast.makeText(context, "Cannot flip camera during active recording", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip Camera",
                    tint = if (isRecording) SlateSecondary else Color.White
                )
            }
        }

        // Bottom Controls (Record Shutter)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isRecording) "Recording... Tap to stop" else "Tap button to start recording reps",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                if (!isRecording) {
                    IconButton(
                        onClick = { pickVideoLauncher.launch("video/*") },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = "Pick Video from Device",
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) StatusRejected.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(4.dp, if (isRecording) StatusRejected else MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (!isRecording) {
                                val cap = videoCapture
                                if (cap != null) {
                                    val videoFile = File(context.cacheDir, "quest_${questCode}_${System.currentTimeMillis()}.mp4")
                                    recordingFile = videoFile
                                    val outputOptions = FileOutputOptions.Builder(videoFile).build()

                                    val activeRecording = cap.output
                                        .prepareRecording(context, outputOptions)
                                        .apply {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                withAudioEnabled()
                                            }
                                        }
                                        .start(ContextCompat.getMainExecutor(context)) { event ->
                                            when (event) {
                                                is VideoRecordEvent.Start -> {
                                                    isRecording = true
                                                }
                                                is VideoRecordEvent.Finalize -> {
                                                    isRecording = false
                                                    if (!event.hasError()) {
                                                        videoUri = Uri.fromFile(videoFile)
                                                    } else {
                                                        Log.e("RecordScreen", "Video recording failed: ${event.error}")
                                                    }
                                                }
                                            }
                                        }
                                    recording = activeRecording
                                }
                            } else {
                                recording?.stop()
                                recording = null
                                isRecording = false
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .semantics {
                                contentDescription = if (isRecording) {
                                    "Stop video recording"
                                } else {
                                    "Start video recording"
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isRecording) 28.dp else 52.dp)
                                .clip(if (isRecording) RoundedCornerShape(6.dp) else CircleShape)
                                .background(if (isRecording) StatusRejected else MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                if (!isRecording) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.5.dp, SlateSecondary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
