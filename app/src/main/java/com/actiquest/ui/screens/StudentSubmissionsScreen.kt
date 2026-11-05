package com.actiquest.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.actiquest.data.GoogleDriveConfig
import com.actiquest.domain.AcademicChangeRequest
import com.actiquest.data.GoogleDriveUtils
import com.actiquest.data.Repository
import com.actiquest.domain.DateTimeUtils
import com.actiquest.domain.Submission
import com.actiquest.ui.components.*
import com.actiquest.ui.components.VideoPlayerView
import com.actiquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSubmissionsScreen(
    navController: NavController,
    onLogout: () -> Unit = {},
    initialFilter: String = "All",
    targetSubmissionId: String? = null
) {
    val isDark = ThemeManager.isDarkMode
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUserId by Repository.currentUserId.collectAsState()
    val submissions by remember(currentUserId) { Repository.listenUserSubmissions(currentUserId) }.collectAsState(initial = emptyList())
    val academicRequests by remember(currentUserId) { Repository.listenAcademicRequests() }.collectAsState(initial = emptyList())
    val userAcademicRequests = remember(academicRequests, currentUserId) {
        academicRequests.filter { it.userId == currentUserId }
    }
    val userState by remember(currentUserId) { Repository.listenUser(currentUserId) }.collectAsState(initial = null)

    var selectedFilter by remember(currentUserId, initialFilter) {
        mutableStateOf(initialFilter.takeIf { it in setOf("All", "Pending", "Approved", "Rejected", "Academics") } ?: "All")
    }
    var focusedSubmissionId by remember(currentUserId, targetSubmissionId) {
        mutableStateOf(targetSubmissionId)
    }
    var selectedMediaForPreview by remember { mutableStateOf<Submission?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate("home") { popUpTo(0) }
        }
    }

    val pendingCount = submissions.count { it.status.equals("pending", ignoreCase = true) || it.status.equals("in review", ignoreCase = true) }
    val approvedCount = submissions.count { it.status.equals("approved", ignoreCase = true) }
    val rejectedCount = submissions.count { it.status.equals("rejected", ignoreCase = true) }

    val targetSubmission = remember(submissions, currentUserId, focusedSubmissionId) {
        focusedSubmissionId?.let { id ->
            submissions.firstOrNull { it.submissionId == id && it.userId == currentUserId }
        }
    }
    val filteredList = remember(submissions, selectedFilter, targetSubmission) {
        if (selectedFilter == "All" && targetSubmission != null) {
            listOf(targetSubmission)
        } else {
            when (selectedFilter) {
                "Pending" -> submissions.filter { it.status.equals("pending", ignoreCase = true) || it.status.equals("in review", ignoreCase = true) }
                "Approved" -> submissions.filter { it.status.equals("approved", ignoreCase = true) }
                "Rejected" -> submissions.filter { it.status.equals("rejected", ignoreCase = true) }
                else -> submissions
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Sign Out of ActiQuest",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else LightTextPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to log out of your student account?",
                    fontFamily = SfSemiFontFamily,
                    color = if (isDark) TextSecondary else LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        Repository.signOut()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("Log Out", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", fontFamily = SfSemiFontFamily, color = if (isDark) TextSecondary else LightTextSecondary)
                }
            },
            containerColor = if (isDark) SurfaceCharcoal else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = if (isDark) DarkNavyBackground else LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "MY QUEST SUBMISSIONS",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else LightTextPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            "Verification Status & Activity Log",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) TextMuted else LightTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate("home") { popUpTo(0) }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = if (isDark) Color.White else LightTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) SurfaceElevated else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Log Out",
                            tint = StatusRejected,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) SurfaceCharcoal else Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("submit_proof") },
                containerColor = CobaltBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                text = { Text("Submit Fitness Proof", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ticket-style Top Overview Card
                item {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        glowColor = CobaltBlue,
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(CobaltBlue.copy(alpha = 0.2f))
                                            .border(1.dp, CobaltBlue, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.FactCheck, contentDescription = null, tint = (if (isDark) ElectricBlue else CobaltBlue), modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            userState?.name ?: "Student Warrior",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else LightTextPrimary
                                        )
                                        Text(
                                            userState?.classId ?: "BSCS - 1A",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = SfSemiFontFamily,
                                            color = if (isDark) SlateLight else LightTextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CobaltBlue.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        "${submissions.size} TOTAL",
                                        color = if (isDark) ElectricBlue else CobaltBlue,
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3-Pill Status Counter Strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusCountBox(
                                    modifier = Modifier.weight(1f),
                                    label = "In Review",
                                    count = "$pendingCount",
                                    icon = Icons.Default.HourglassTop,
                                    color = StatusPending
                                )
                                StatusCountBox(
                                    modifier = Modifier.weight(1f),
                                    label = "Approved",
                                    count = "$approvedCount",
                                    icon = Icons.Default.CheckCircle,
                                    color = MintEmerald
                                )
                                StatusCountBox(
                                    modifier = Modifier.weight(1f),
                                    label = "Retake",
                                    count = "$rejectedCount",
                                    icon = Icons.Default.Refresh,
                                    color = StatusRejected
                                )
                            }
                        }
                    }
                }

                // Quick Action Banner for Fitness Proof
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceElevated else Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("submit_proof") }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MintEmerald.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Have daily steps or cardio proof?",
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                    Text(
                                        "Attach pedometer app screenshot for points",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 11.sp,
                                        color = if (isDark) SlateLight else LightTextSecondary
                                    )
                                }
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MintEmerald)
                        }
                    }
                }

                if (targetSubmission != null && selectedFilter == "All") {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceElevated else Color(0xFFEFF6FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Showing the submission from this notification",
                                    fontFamily = SfSemiFontFamily,
                                    fontSize = 12.sp,
                                    color = if (isDark) SlateLight else LightTextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { focusedSubmissionId = null }) {
                                    Text("Show all", fontFamily = SfBoldFontFamily, color = CobaltBlue)
                                }
                            }
                        }
                    }
                }

                // Filter Pill Row
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterPill(
                                label = "All (${submissions.size})",
                                isSelected = selectedFilter == "All",
                                onClick = {
                                    selectedFilter = "All"
                                    focusedSubmissionId = null
                                }
                            )
                        }
                        item {
                            FilterPill(
                                label = "Awaiting Review ($pendingCount)",
                                isSelected = selectedFilter == "Pending",
                                activeColor = StatusPending,
                                onClick = { selectedFilter = "Pending" }
                            )
                        }
                        item {
                            FilterPill(
                                label = "Approved ($approvedCount)",
                                isSelected = selectedFilter == "Approved",
                                activeColor = MintEmerald,
                                onClick = { selectedFilter == "Approved" }
                            )
                        }
                        item {
                            FilterPill(
                                label = "Needs Retake ($rejectedCount)",
                                isSelected = selectedFilter == "Rejected",
                                activeColor = StatusRejected,
                                onClick = { selectedFilter = "Rejected" }
                            )
                        }
                        item {
                            FilterPill(
                                label = "Academics (${userAcademicRequests.size})",
                                isSelected = selectedFilter == "Academics",
                                activeColor = IndigoPurple,
                                onClick = { selectedFilter = "Academics" }
                            )
                        }
                    }
                }

                val activeListIsEmpty = if (selectedFilter == "Academics") userAcademicRequests.isEmpty() else filteredList.isEmpty()

                if (activeListIsEmpty) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceCharcoal else Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) SlateSecondary.copy(alpha = 0.3f) else Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) SurfaceElevated else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = if (isDark) SlateLight else LightTextSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    if (selectedFilter == "Academics") "No Academic Transfer Requests" else "No Submissions in this category",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    if (selectedFilter == "Academics") "Your requests for college, program, or section updates will appear here." else "Scan campus QR checkpoints or upload pedometer screenshots to earn points for your class!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = SfSemiFontFamily,
                                    color = if (isDark) TextMuted else LightTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (selectedFilter == "Academics") {
                    items(userAcademicRequests) { request ->
                        AcademicRequestCardItem(request = request, isDark = isDark)
                    }
                } else {
                    items(filteredList) { submission ->
                        SubmissionTicketCard(
                            submission = submission,
                            onPreviewMedia = { selectedMediaForPreview = submission },
                            onRetake = {
                                if (submission.submissionType == "screenshot") {
                                    navController.navigate("submit_proof")
                                } else {
                                    navController.navigate("scan")
                                }
                            }
                        )
                    }
                }
            }

            // Media Preview Modal Dialog (Handles Video and Screenshot)
            selectedMediaForPreview?.let { sub ->
                val isScreenshot = sub.submissionType == "screenshot"

                Dialog(
                    onDismissRequest = { selectedMediaForPreview = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isDark) DarkNavyBackground.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceCharcoal else Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.6f)),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            sub.questTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else LightTextPrimary
                                        )
                                        Text(
                                            if (isScreenshot) "Fitness Screenshot Proof" else "QR Quest Video Proof",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = SfSemiFontFamily,
                                            color = if (isDark) SlateLight else LightTextSecondary
                                        )
                                    }
                                    IconButton(onClick = { selectedMediaForPreview = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color.White else LightTextPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val hasDriveLink = sub.driveUrl.isNotBlank() || 
                                    sub.driveFileId.isNotBlank() || 
                                    sub.imagePath.contains("drive.google.com") || 
                                    sub.videoPath.contains("drive.google.com")
                                val driveUrl = sub.driveUrl.ifBlank {
                                    if (sub.imagePath.contains("drive.google.com")) sub.imagePath 
                                    else if (sub.videoPath.contains("drive.google.com")) sub.videoPath 
                                    else ""
                                }

                                val hasPlayableLocalVideo = !isScreenshot && sub.videoPath.isNotBlank() && !sub.videoPath.contains("drive.google.com")
                                val hasLocalImage = isScreenshot && sub.imagePath.isNotBlank() && !sub.imagePath.contains("drive.google.com")

                                // 1. Video Player (in-app recorded reps / local video proof)
                                if (hasPlayableLocalVideo) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.Black)
                                                .border(1.dp, CobaltBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        ) {
                                            VideoPlayerView(
                                                videoUriString = sub.videoPath,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }

                                // 2. Local Screenshot Proof
                                if (hasLocalImage) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.Black)
                                                .border(1.dp, MintEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = sub.imagePath,
                                                contentDescription = "Fitness Proof",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }

                                // 3. Fallback Banner for activity metric
                                if (isScreenshot && !hasLocalImage && !hasDriveLink) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isDark) SurfaceElevated else Color(0xFFF1F5F9))
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(44.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(sub.activityType, fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold, color = (if (isDark) ElectricBlue else CobaltBlue))
                                            Text(sub.activityMetricValue, fontSize = 20.sp, fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Black, color = if (isDark) Color.White else LightTextPrimary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // 4. Google Drive Evidence Card
                                if (hasDriveLink) {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceElevated else Color(0xFFF1F5F9)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = CobaltBlue.copy(alpha = 0.2f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.CloudUpload,
                                                        contentDescription = null,
                                                        tint = if (isDark) ElectricBlue else CobaltBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        if (isScreenshot) "GOOGLE DRIVE FITNESS PROOF" else "GOOGLE DRIVE VIDEO EVIDENCE",
                                                        color = if (isDark) ElectricBlue else CobaltBlue,
                                                        fontSize = 11.sp,
                                                        fontFamily = SfBoldFontFamily,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            val fileId = sub.driveFileId.ifBlank {
                                                GoogleDriveUtils.extractDriveFileId(driveUrl) ?: "Attached File"
                                            }
                                            Text(
                                                "File Reference: ${fileId.take(18)}...",
                                                fontFamily = SfSemiFontFamily,
                                                fontSize = 12.sp,
                                                color = if (isDark) SlateLight else LightTextSecondary
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Button(
                                                onClick = {
                                                    GoogleDriveUtils.openInGoogleDrive(context, driveUrl)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                                modifier = Modifier.fillMaxWidth().height(46.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Open in Google Drive", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (sub.studentNotes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "My note: ${sub.studentNotes}",
                                        fontSize = 12.sp,
                                        fontFamily = SfSemiFontFamily,
                                        color = if (isDark) TextSecondary else LightTextSecondary,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { selectedMediaForPreview = null },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Done Previewing", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubmissionTicketCard(
    submission: Submission,
    onPreviewMedia: () -> Unit,
    onRetake: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    val isScreenshot = submission.submissionType == "screenshot"
    val isApproved = submission.status.equals("approved", ignoreCase = true)
    val isRejected = submission.status.equals("rejected", ignoreCase = true)
    val statusColor = when {
        isApproved -> MintEmerald
        isRejected -> StatusRejected
        else -> StatusPending
    }

    val displayPoints = submission.awardedPoints ?: submission.pointValue
    val isPartialPoints = isApproved && submission.awardedPoints != null && submission.awardedPoints < submission.pointValue

    val relativeTime = DateTimeUtils.formatPhilippineTimestamp(submission.submittedAt)

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = statusColor,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Ticket Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = (if (isScreenshot) MintEmerald else CobaltBlue).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            (if (isScreenshot) MintEmerald else CobaltBlue).copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isScreenshot) Icons.Default.DirectionsWalk else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isScreenshot) MintEmerald else ElectricBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isScreenshot) "FITNESS PROOF" else "QR QUEST",
                                color = if (isScreenshot) MintEmerald else ElectricBlue,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isApproved -> if (isPartialPoints) "PARTIAL AWARD" else "APPROVED"
                                isRejected -> "RETAKE NEEDED"
                                else -> "IN ADMIN QUEUE"
                            },
                            color = statusColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                submission.questTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else LightTextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Route / Pipeline Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) SurfaceElevated.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) SurfaceElevatedHigh.copy(alpha = 0.4f) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isGoogleDrive = submission.videoProvider == "google_drive" || 
                    submission.driveUrl.isNotBlank() || 
                    submission.imagePath.contains("drive.google.com") ||
                    submission.videoPath.contains("drive.google.com")
                Column {
                    Text("TYPE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isDark) SlateLight else LightTextSecondary)
                    Text(
                        if (isGoogleDrive) {
                            if (isScreenshot) "Drive Proof" else "Drive Video"
                        } else {
                            if (isScreenshot) "Screenshot" else "QR Video"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ElectricBlue))
                    Text(
                        if (isScreenshot) " ── 📱 ── " else " ── 📹 ── ",
                        fontSize = 10.sp,
                        color = if (isDark) SlateLight else LightTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("STATUS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isDark) SlateLight else LightTextSecondary)
                    Text(
                        if (submission.status == "approved") "VERIFIED" else "ADMIN DESK",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            if (submission.activityMetricValue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = MintEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Activity: ${submission.activityMetricValue}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) SlateLight else LightTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Grid Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) SurfaceElevatedHigh.copy(alpha = 0.4f) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = if (isDark) SlateLight else LightTextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(relativeTime, fontSize = 11.sp, color = if (isDark) TextSecondary else LightTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintEmerald.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "+$displayPoints PTS" + (if (isPartialPoints) " (Partial)" else ""),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintEmerald
                        )
                    }
                }
            }

            // Reviewer / Feedback Note
            if (!submission.reviewerNote.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (submission.status == "rejected") StatusRejected.copy(alpha = 0.12f) else CobaltBlue.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (submission.status == "rejected") StatusRejected.copy(alpha = 0.4f) else CobaltBlue.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Feedback,
                            contentDescription = null,
                            tint = if (submission.status == "rejected") StatusRejected else (if (isDark) ElectricBlue else CobaltBlue),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Admin Reviewer Note:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (submission.status == "rejected") StatusRejected else (if (isDark) ElectricBlue else CobaltBlue)
                            )
                            Text(
                                submission.reviewerNote,
                                fontSize = 12.sp,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewMedia,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) ElectricBlue else CobaltBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, (if (isDark) ElectricBlue else CobaltBlue).copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isScreenshot) Icons.Default.PhotoLibrary else Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isScreenshot) "View Proof" else "Watch Video",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (submission.status == "rejected") {
                    Button(
                        onClick = onRetake,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRejected, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Resubmit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicRequestCardItem(request: AcademicChangeRequest, isDark: Boolean) {
    val statusColor = when (request.status.lowercase()) {
        "approved" -> MintEmerald
        "rejected" -> StatusRejected
        else -> StatusPending
    }
    val relativeTime = DateTimeUtils.formatPhilippineTimestamp(request.requestedAt)

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = statusColor,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = IndigoPurple.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPurple)
                ) {
                    Text(
                        "ACADEMIC TRANSFER",
                        color = if (isDark) ElectricBlue else CobaltBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = request.status.uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Text(
                "Transfer Request to ${request.requestedCollege}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else LightTextPrimary
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) SurfaceElevated.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) SurfaceElevatedHigh.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("From: ${request.currentCollege} • ${request.currentProgram} (${request.currentSection})", fontSize = 11.sp, color = if (isDark) SlateLight else LightTextSecondary)
                    Text("To: ${request.requestedCollege} • ${request.requestedProgram} (${request.requestedSection})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else LightTextPrimary)
                    if (request.reason.isNotBlank()) {
                        Text("Reason: \"${request.reason}\"", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = if (isDark) TextSecondary else LightTextSecondary)
                    }
                }
            }

            if (!request.reviewerNote.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (request.status == "rejected") StatusRejected.copy(alpha = 0.15f) else CobaltBlue.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Reviewer Note:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (request.status == "rejected") StatusRejected else ElectricBlue)
                        Text(request.reviewerNote!!, fontSize = 12.sp, color = if (isDark) Color.White else LightTextPrimary)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Requested $relativeTime", fontSize = 11.sp, color = if (isDark) SlateLight else LightTextSecondary)
            }
        }
    }
}
