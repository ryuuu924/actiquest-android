package com.actiquest.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.actiquest.data.GoogleDriveConfig
import com.actiquest.data.GoogleDriveUtils
import com.actiquest.data.Repository
import com.actiquest.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitFitnessProofScreen(
    navController: NavController
) {
    val isDark = ThemeManager.isDarkMode
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentUserId by Repository.currentUserId.collectAsState()
    val currentUser by Repository.currentUser.collectAsState()

    var selectedActivityType by remember { mutableStateOf("Steps Count") }
    var metricInput by remember { mutableStateOf("8500") }
    var requestedPoints by remember { mutableIntStateOf(85) }
    var studentNotes by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var driveUrlInput by remember { mutableStateOf("") }
    val isValidDriveUrl = remember(driveUrlInput) {
        GoogleDriveUtils.isValidGoogleDriveUrl(driveUrlInput)
    }
    val driveFileId = remember(driveUrlInput) {
        GoogleDriveUtils.extractDriveFileId(driveUrlInput)
    }
    val clipboardManager = remember {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }
    var isSubmitting by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate("home") { popUpTo(0) }
        }
    }

    // Modern photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Fallback file picker launcher
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Recalculate suggested points based on activity metric
    fun updateCalculatedPoints(type: String, valueText: String) {
        val num = valueText.filter { it.isDigit() }.toIntOrNull() ?: 0
        requestedPoints = when (type) {
            "Steps Count" -> (num / 100).coerceIn(10, 200) // 1 pt per 100 steps
            "Active Minutes" -> (num * 2).coerceIn(10, 200) // 2 pts per active minute
            "Campus Run" -> (num * 20).coerceIn(20, 200) // 20 pts per km
            "Cycling" -> (num * 10).coerceIn(15, 200)
            else -> (num).coerceIn(10, 200)
        }
    }

    Scaffold(
        containerColor = if (isDark) DarkNavyBackground else LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "SUBMIT FITNESS PROOF",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else LightTextPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            "Pedometer & Active Minutes Verification",
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
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else LightTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) SurfaceCharcoal else Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Intro Hero Card
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = ElectricBlue,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.2f))
                                    .border(1.dp, ElectricBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DirectionsRun,
                                    contentDescription = null,
                                    tint = if (isDark) ElectricBlue else CobaltBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Manual Activity Verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                                Text(
                                    "Upload fitness screenshots to earn class points",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = SfSemiFontFamily,
                                    color = if (isDark) SlateLight else LightTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) SurfaceElevated.copy(alpha = 0.7f) else Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = MintEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Supports Google Fit, Strava, Apple Health, Samsung Health, Garmin, Mi Fitness, and pedometer apps.",
                                    fontSize = 11.sp,
                                    fontFamily = SfSemiFontFamily,
                                    color = if (isDark) TextSecondary else LightTextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // 1. Select Activity Type
                Column {
                    Text(
                        "1. CHOOSE ACTIVITY CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) SlateLight else LightTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val activityTypes = listOf(
                        Triple("Steps Count", "🚶 Daily Steps", "Pedometer total for the day"),
                        Triple("Active Minutes", "⏱️ Active Minutes", "Cardio / Exercise minutes"),
                        Triple("Campus Run", "🏃 Outdoor Run", "Distance jogging on campus"),
                        Triple("Cycling", "🚴 Cycling / Ride", "Bicycle commute or workout")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activityTypes.take(2).forEach { (type, label, _) ->
                            val isSelected = selectedActivityType == type
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) (if (isDark) CobaltBlue.copy(alpha = 0.35f) else Color(0xFFEEF2FF)) else (if (isDark) SurfaceElevated else Color.White),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isSelected) CobaltBlue else (if (isDark) SlateSecondary.copy(alpha = 0.3f) else Color(0xFFCBD5E1))
                                ),
                                shadowElevation = if (isDark) 0.dp else 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedActivityType = type
                                        metricInput = if (type == "Steps Count") "8500" else "45"
                                        updateCalculatedPoints(type, metricInput)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        label,
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = if (isSelected) (if (isDark) Color.White else CobaltBlue) else (if (isDark) TextSecondary else LightTextSecondary)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activityTypes.takeLast(2).forEach { (type, label, _) ->
                            val isSelected = selectedActivityType == type
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) (if (isDark) CobaltBlue.copy(alpha = 0.35f) else Color(0xFFEEF2FF)) else (if (isDark) SurfaceElevated else Color.White),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isSelected) CobaltBlue else (if (isDark) SlateSecondary.copy(alpha = 0.3f) else Color(0xFFCBD5E1))
                                ),
                                shadowElevation = if (isDark) 0.dp else 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedActivityType = type
                                        metricInput = if (type == "Campus Run") "4" else "12"
                                        updateCalculatedPoints(type, metricInput)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        label,
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = if (isSelected) (if (isDark) Color.White else CobaltBlue) else (if (isDark) TextSecondary else LightTextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Metric Input & Points Calculation Card
                Column {
                    Text(
                        "2. ACTIVITY METRICS & POINTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) SlateLight else LightTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceCharcoal else Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) CobaltBlue.copy(alpha = 0.5f) else Color(0xFFCBD5E1)),
                        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val metricLabel = when (selectedActivityType) {
                                "Steps Count" -> "Recorded Steps Count"
                                "Active Minutes" -> "Active / Cardio Minutes"
                                "Campus Run" -> "Distance in Kilometers (KM)"
                                "Cycling" -> "Cycling Distance in KM"
                                else -> "Activity Amount"
                            }

                            val metricHint = when (selectedActivityType) {
                                "Steps Count" -> "e.g. 10450"
                                "Active Minutes" -> "e.g. 45"
                                "Campus Run" -> "e.g. 5"
                                "Cycling" -> "e.g. 15"
                                else -> "e.g. 50"
                            }

                            OutlinedTextField(
                                value = metricInput,
                                onValueChange = {
                                    metricInput = it
                                    updateCalculatedPoints(selectedActivityType, it)
                                },
                                label = { Text(metricLabel) },
                                placeholder = { Text(metricHint) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(
                                        when (selectedActivityType) {
                                            "Steps Count" -> Icons.Default.DirectionsWalk
                                            "Active Minutes" -> Icons.Default.Timer
                                            else -> Icons.Default.Speed
                                        },
                                        contentDescription = null,
                                        tint = (if (isDark) ElectricBlue else CobaltBlue)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CobaltBlue,
                                    unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.5f) else Color(0xFFCBD5E1),
                                    focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                    unfocusedTextColor = if (isDark) Color.White else LightTextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Points preview banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MintEmerald.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "REQUESTED REWARD",
                                            fontSize = 9.sp,
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Black,
                                            color = MintEmerald,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            "Admin can approve full or partial points",
                                            fontSize = 11.sp,
                                            fontFamily = SfSemiFontFamily,
                                            color = if (isDark) TextSecondary else LightTextSecondary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MintEmerald.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald)
                                    ) {
                                        Text(
                                            "+$requestedPoints PTS",
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = MintEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Google Drive Fitness Proof Evidence (Zero-Cost & Free)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "3. FITNESS PROOF: GOOGLE DRIVE LINK",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MintEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                "ZERO-COST PROOF",
                                color = MintEmerald,
                                fontSize = 7.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Google Drive Sharing Guide Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) SurfaceCharcoal else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) CobaltBlue.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                        ),
                        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = CobaltBlue.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = if (isDark) ElectricBlue else CobaltBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Google Drive Fitness Evidence Guide",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfBoldFontFamily,
                                        color = if (isDark) Color.White else LightTextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Share proof without cloud storage fees",
                                        fontSize = 11.sp,
                                        fontFamily = SfSemiFontFamily,
                                        color = if (isDark) SlateLight else LightTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val steps = listOf(
                                "1. Upload your fitness summary, steps screenshot, or activity export to Google Drive.",
                                "2. Open sharing settings and keep General access set to Restricted.",
                                "3. Share the file with the designated ActiQuest Reviewer as Viewer:",
                                "4. Copy the link (e.g., https://drive.google.com/file/d/.../view) and paste it below."
                            )

                            steps.take(3).forEach { step ->
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = SfSemiFontFamily,
                                    color = if (isDark) TextSecondary else LightTextSecondary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            // Reviewer Email Chip with Copy Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CobaltBlue.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.5f)),
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
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDark) ElectricBlue else CobaltBlue
                                        )
                                        Text(
                                            GoogleDriveConfig.adminReviewerEmail,
                                            fontSize = 12.sp,
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else LightTextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager?.setPrimaryClip(
                                                ClipData.newPlainText("Reviewer Email", GoogleDriveConfig.adminReviewerEmail)
                                            )
                                            Toast.makeText(context, "Reviewer email copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy Email",
                                            tint = MintEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = steps[3],
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = SfSemiFontFamily,
                                color = if (isDark) TextSecondary else LightTextSecondary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Notice Banner
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberWarning.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Please verify access is granted to ${GoogleDriveConfig.adminReviewerEmail} as Viewer so faculty can inspect your proof.",
                                        fontSize = 10.sp,
                                        fontFamily = SfSemiFontFamily,
                                        color = AmberWarning,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Field for Drive Link
                    OutlinedTextField(
                        value = driveUrlInput,
                        onValueChange = { driveUrlInput = it.trim() },
                        label = { Text("Google Drive Proof Link") },
                        placeholder = {
                            Text(
                                "https://drive.google.com/file/d/...",
                                fontSize = 12.sp,
                                color = if (isDark) SlateLight else LightTextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = if (isValidDriveUrl) MintEmerald else (if (isDark) ElectricBlue else CobaltBlue)
                            )
                        },
                        trailingIcon = {
                            if (driveUrlInput.isNotEmpty()) {
                                IconButton(onClick = { driveUrlInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = SlateLight)
                                }
                            }
                        },
                        isError = driveUrlInput.isNotEmpty() && !isValidDriveUrl,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CobaltBlue,
                            unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.4f) else Color(0xFFCBD5E1),
                            errorBorderColor = StatusRejected,
                            focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                            unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                            focusedContainerColor = if (isDark) SurfaceElevated else Color.White,
                            unfocusedContainerColor = if (isDark) SurfaceElevated else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Validation Pill or Quick Action Row
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
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Valid Google Drive File ID: ${driveFileId?.take(16)}...",
                                        color = MintEmerald,
                                        fontSize = 11.sp,
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusRejected.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusRejected.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusRejected, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Link must be a Google Drive file URL (drive.google.com/file/d/...)",
                                        color = StatusRejected,
                                        fontSize = 11.sp,
                                        fontFamily = SfSemiFontFamily
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Drive Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                if (!clipText.isNullOrBlank()) {
                                    driveUrlInput = clipText
                                    Toast.makeText(context, "Pasted link from clipboard", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste Link", fontSize = 11.sp, fontFamily = SfBoldFontFamily)
                        }

                        if (isValidDriveUrl) {
                            Button(
                                onClick = {
                                    GoogleDriveUtils.openInGoogleDrive(context, driveUrlInput)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Open", fontSize = 11.sp, fontFamily = SfBoldFontFamily)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Local Screenshot Attachment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "OR ATTACH SCREENSHOT (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        if (selectedImageUri != null) {
                            TextButton(onClick = { selectedImageUri = null }) {
                                Text("Remove", color = StatusRejected, fontSize = 11.sp, fontFamily = SfBoldFontFamily)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (selectedImageUri != null) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceCharcoal else Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Fitness proof screenshot",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Screenshot selected",
                                        color = MintEmerald,
                                        fontSize = 11.sp,
                                        fontFamily = SfBoldFontFamily
                                    )
                                    TextButton(
                                        onClick = {
                                            try {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            } catch (e: Exception) {
                                                getContentLauncher.launch("image/*")
                                            }
                                        }
                                    ) {
                                        Text("Change", color = CobaltBlue, fontSize = 11.sp, fontFamily = SfBoldFontFamily)
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                try {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } catch (e: Exception) {
                                    getContentLauncher.launch("image/*")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Local Screenshot Photo", fontSize = 12.sp, fontFamily = SfBoldFontFamily)
                        }
                    }
                }

                // 4. Student Notes / Reflection (Optional)
                Column {
                    Text(
                        "4. STUDENT NOTES / CONTEXT (OPTIONAL)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) SlateLight else LightTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = studentNotes,
                        onValueChange = { studentNotes = it },
                        label = { Text("Notes for Admin Reviewer") },
                        placeholder = { Text("e.g., Completed evening brisk walk around campus track oval.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CobaltBlue,
                            unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.4f) else Color(0xFFCBD5E1),
                            focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                            unfocusedTextColor = if (isDark) Color.White else LightTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Submit Button
                val hasValidProof = isValidDriveUrl || selectedImageUri != null || driveUrlInput.isNotBlank()
                Button(
                    onClick = {
                        if (driveUrlInput.isNotBlank() && !isValidDriveUrl) {
                            Toast.makeText(
                                context,
                                "Please paste a valid Google Drive file URL (e.g., https://drive.google.com/file/d/...)",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        if (driveUrlInput.isBlank() && selectedImageUri == null) {
                            Toast.makeText(
                                context,
                                "Please enter a Google Drive link or select a screenshot.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (currentUserId.isBlank()) {
                            Toast.makeText(context, "Please sign in again before submitting.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val formattedMetric = when (selectedActivityType) {
                            "Steps Count" -> "${metricInput.trim()} Steps"
                            "Active Minutes" -> "${metricInput.trim()} Active Mins"
                            "Campus Run" -> "${metricInput.trim()} KM Run"
                            "Cycling" -> "${metricInput.trim()} KM Cycling"
                            else -> metricInput.trim()
                        }

                        isSubmitting = true
                        coroutineScope.launch {
                            try {
                                Repository.submitScreenshotProof(
                                    userId = currentUserId,
                                    studentName = currentUser?.name ?: "Juan de la Cruz",
                                    activityType = selectedActivityType,
                                    metricValue = formattedMetric,
                                    requestedPoints = requestedPoints,
                                    studentNotes = studentNotes.trim(),
                                    imageUri = selectedImageUri,
                                    context = context,
                                    driveUrl = driveUrlInput.trim(),
                                    driveFileId = driveFileId ?: "",
                                    classId = currentUser?.classId ?: ""
                                )
                                Toast.makeText(
                                    context,
                                    if (isValidDriveUrl) "Fitness proof submitted via Google Drive for Admin verification!" else "Fitness proof submitted for Admin verification!",
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.navigate("my_submissions") {
                                    popUpTo("home")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Submission failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = !isSubmitting && metricInput.isNotEmpty() && hasValidProof,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SUBMIT PROOF FOR VERIFICATION",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
