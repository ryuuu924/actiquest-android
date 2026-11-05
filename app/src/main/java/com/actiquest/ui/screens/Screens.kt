package com.actiquest.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import java.time.LocalTime
import java.time.ZoneId
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.actiquest.R
import com.actiquest.data.Repository
import com.actiquest.domain.BadgeCatalog
import com.actiquest.domain.ClassModel
import com.actiquest.domain.Quest
import com.actiquest.domain.SeedQuests
import com.actiquest.domain.DateTimeUtils
import com.actiquest.domain.QuestEvidenceMode
import com.actiquest.domain.activityDurationLabel
import com.actiquest.domain.classificationLabel
import com.actiquest.domain.evidenceConfiguration
import com.actiquest.domain.evidenceDurationLabel
import com.actiquest.domain.inclusiveAlternative
import com.actiquest.domain.withResolvedEvidenceConfiguration
import com.actiquest.ui.components.*
import com.actiquest.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(22.dp),
    glowColor: Color = CobaltBlue,
    borderWidth: Dp = 1.2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isHovered -> 1.02f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.75f
            isHovered -> 0.90f
            else -> 0.35f
        },
        animationSpec = tween(250),
        label = "card_glow"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            glowColor.copy(alpha = glowAlpha),
            VioletAccent.copy(alpha = glowAlpha * 0.6f),
            if (isDark) Color.White.copy(alpha = glowAlpha * 0.25f) else glowColor.copy(alpha = glowAlpha * 0.4f),
            glowColor.copy(alpha = glowAlpha * 0.15f)
        )
    )

    val glassBackgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                SurfaceElevated.copy(alpha = if (isHovered) 0.92f else 0.85f),
                SurfaceCharcoal.copy(alpha = if (isHovered) 0.96f else 0.92f),
                DarkNavyBackground.copy(alpha = 0.96f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFF8FAFC)
            )
        )
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isHovered) 12.dp else 4.dp,
                shape = shape,
                ambientColor = glowColor.copy(alpha = glowAlpha * 0.4f),
                spotColor = glowColor.copy(alpha = glowAlpha * 0.6f)
            )
            .clip(shape)
            .background(glassBackgroundBrush)
            .border(borderWidth, borderBrush, shape)
            .then(clickableModifier)
    ) {
        content()
    }
}

@Composable
fun HomeScreen(
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = ThemeManager.isDarkMode
    val currentUserId by Repository.currentUserId.collectAsState()
    val userState by remember(currentUserId) { Repository.listenUser(currentUserId) }.collectAsState(initial = null)
    val userSubmissions by remember(currentUserId) { Repository.listenUserSubmissions(currentUserId) }.collectAsState(initial = emptyList())
    val activeStreak = remember(userSubmissions, currentUserId) {
        DateTimeUtils.calculateStreaks(userSubmissions, currentUserId).currentStreak
    }

    val user = userState
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showTargetGoalDialog by remember { mutableStateOf(false) }
    var customGoalInput by remember { mutableStateOf("") }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Double tap back button to exit on home screen, or single back to return to Home Tab
    BackHandler {
        if (selectedBottomTab != 0) {
            selectedBottomTab = 0
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Press back again to exit ActiQuest", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showTargetGoalDialog) {
        val targetGoals = listOf(250, 500, 750, 1000, 1500)
        AlertDialog(
            onDismissRequest = { showTargetGoalDialog = false },
            title = {
                Text(
                    text = "Set Target Activity Points",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else LightTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Customize your weekly point target to track your progress percentage toward healthy active living.",
                        fontFamily = SfSemiFontFamily,
                        fontSize = 13.sp,
                        color = if (isDark) SlateLight else LightTextSecondary
                    )

                    Text(
                        text = "Quick Presets",
                        fontFamily = SfBoldFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else LightTextPrimary
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(targetGoals) { goal ->
                            val isSel = customGoalInput == goal.toString()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) CobaltBlue else (if (isDark) SurfaceElevated else Color(0xFFF1F5F9)),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) CobaltBlue else (if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))
                                ),
                                modifier = Modifier.clickable { customGoalInput = goal.toString() }
                            ) {
                                Text(
                                    text = "$goal pts",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color.White else (if (isDark) SlateLight else LightTextPrimary),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customGoalInput,
                        onValueChange = { customGoalInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Custom Goal (Points)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) SurfaceCharcoal else Color.White,
                            unfocusedContainerColor = if (isDark) SurfaceCharcoal else Color.White,
                            focusedBorderColor = CobaltBlue,
                            unfocusedBorderColor = if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customGoalInput.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            coroutineScope.launch {
                                Repository.updateTargetPoints(currentUserId, parsed)
                                Toast.makeText(context, "Target points updated to $parsed pts!", Toast.LENGTH_SHORT).show()
                                showTargetGoalDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Please enter a valid points goal", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save Goal", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetGoalDialog = false }) {
                    Text(
                        "Cancel",
                        fontFamily = SfSemiFontFamily,
                        color = if (isDark) SlateLight else LightTextSecondary
                    )
                }
            },
            containerColor = if (isDark) SurfaceCharcoal else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    val allQuests = remember { SeedQuests.QUEST_MAP.values.map { it.withResolvedEvidenceConfiguration() } }
    val filteredQuests = remember(selectedCategory, searchQuery) {
        allQuests.filter { quest ->
            val matchCat = if (selectedCategory == "All") true else quest.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = if (searchQuery.isEmpty()) true else quest.title.contains(searchQuery, ignoreCase = true) || quest.location.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    val pendingSubmissionsCount = userSubmissions.count { it.status == "pending" }
    val latestSubmission = userSubmissions.firstOrNull()

    LaunchedEffect(latestSubmission?.status) {
        if (latestSubmission?.status == "approved") {
            showConfetti = true
            delay(2500)
            showConfetti = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate("scan")
        } else {
            Toast.makeText(context, "Camera permission required to scan quest markers", Toast.LENGTH_SHORT).show()
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, Icons.Default.Home),
        BottomNavItem("Quests", Icons.Default.Explore, Icons.Default.Explore),
        BottomNavItem("Ranks", Icons.Default.Leaderboard, Icons.Default.Leaderboard),
        BottomNavItem("Profile", Icons.Default.Person, Icons.Default.Person)
    )

    Scaffold(
        containerColor = if (isDark) DarkNavyBackground else LightBackground,
        bottomBar = {
            AppBottomNavBar(
                items = bottomNavItems,
                selectedIndex = selectedBottomTab,
                onItemSelected = { selectedBottomTab = it }
            )
        },
        floatingActionButton = {
            if (selectedBottomTab == 0 || selectedBottomTab == 1) {
                FloatingActionButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            navController.navigate("scan")
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Quest", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = selectedBottomTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 4 } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInHorizontally { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> width / 4 } + fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "tab_switch_transition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                    // TAB 0: HOME DASHBOARD (Matching the inspiration aesthetic)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top Greeting Header with Avatar + Notification Bell
                        item {
                            val displayName = user?.getDisplayName() ?: "Warrior"
                            val greetingExpression = DateTimeUtils.getGreeting("")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    UserAvatarView(
                                        photoUri = user?.profilePicUri?.ifEmpty { null },
                                        name = user?.getFullName() ?: user?.name ?: "Student",
                                        size = 48.dp,
                                        modifier = Modifier.clickable { selectedBottomTab = 3 }
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = greetingExpression,
                                            fontFamily = SfSemiFontFamily,
                                            fontSize = 12.sp,
                                            color = if (isDark) SlateLight else LightTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = displayName,
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isDark) Color.White else LightTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Notification Bell (Navigates to dedicated notifications history)
                                IconButton(
                                    onClick = { navController.navigate("notifications") },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) SurfaceElevated else Color.White)
                                        .border(1.dp, if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0), CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = "Notifications",
                                            tint = if (isDark) Color.White else LightTextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        if (pendingSubmissionsCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(StatusPending)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Hero Card: Wellbeing / Activity Score with Circular Gradient Ring & Target (Navigates to Activity Journey)
                        item {
                            val targetPoints = user?.targetPoints ?: 500
                            val thisWeekPoints = remember(userSubmissions, user) {
                                DateTimeUtils.getPointsForWeek(
                                    weekOffset = 0,
                                    submissions = userSubmissions,
                                    userId = user?.uid ?: ""
                                ).sumOf { it.second }
                            }
                            val lastWeekPoints = remember(userSubmissions, user) {
                                DateTimeUtils.getPointsForWeek(
                                    weekOffset = 1,
                                    submissions = userSubmissions,
                                    userId = user?.uid ?: ""
                                ).sumOf { it.second }
                            }

                            val thisWeekPercent = ((thisWeekPoints.toFloat() / targetPoints.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
                            val lastWeekPercent = ((lastWeekPoints.toFloat() / targetPoints.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
                            val diffPercent = thisWeekPercent - lastWeekPercent
                            val liveTrendText = if (diffPercent >= 0) {
                                "↑ $diffPercent% from last week"
                            } else {
                                "↓ ${kotlin.math.abs(diffPercent)}% from last week"
                            }

                            WellbeingScoreHeroCard(
                                currentPoints = thisWeekPoints,
                                targetPoints = targetPoints,
                                trendText = liveTrendText,
                                onEditTarget = {
                                    customGoalInput = (user?.targetPoints ?: 500).toString()
                                    showTargetGoalDialog = true
                                },
                                onClick = { navController.navigate("activity_journey") }
                            )
                        }

                        // 3-Column Metric Statistics Grid (Interactive routes)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricStatGridCard(
                                    icon = Icons.Default.EmojiEvents,
                                    iconColor = Color(0xFFA78BFA),
                                    value = "${user?.approvedCount ?: 0}",
                                    label = "Quests Won",
                                    onClick = { navController.navigate("my_submissions") },
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatGridCard(
                                    icon = Icons.Default.Stars,
                                    iconColor = CobaltBlue,
                                    value = "${user?.totalPoints ?: 0}",
                                    label = "Total Points",
                                    onClick = { navController.navigate("activity_journey") },
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatGridCard(
                                    icon = Icons.Default.LocalFireDepartment,
                                    iconColor = Color(0xFFF97316),
                                    value = "$activeStreak Days",
                                    label = "Active Streak",
                                    onClick = { navController.navigate("activity_journey") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Daily Activity Bar Chart with Real System Days & User Submissions
                        item {
                            val todayIndex = DateTimeUtils.getTodayDayOfWeekIndex()
                            DailyActivityBarChart(
                                submissions = userSubmissions,
                                userId = currentUserId,
                                activeDayIndex = todayIndex
                            )
                        }

                        // Fast Actions Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                navController.navigate("scan")
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isDark) SurfaceCharcoal.copy(alpha = 0.88f) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(CobaltBlue.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                tint = CobaltBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Scan Quest",
                                                fontFamily = SfBoldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDark) Color.White else LightTextPrimary
                                            )
                                            Text(
                                                "Camera Scanner",
                                                fontFamily = SfSemiFontFamily,
                                                fontSize = 10.sp,
                                                color = if (isDark) SlateLight else LightTextSecondary
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { navController.navigate("submit_proof") },
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isDark) SurfaceCharcoal.copy(alpha = 0.88f) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MintEmerald.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.DirectionsRun,
                                                contentDescription = null,
                                                tint = MintEmerald,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Fitness Proof",
                                                fontFamily = SfBoldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDark) Color.White else LightTextPrimary
                                            )
                                            Text(
                                                "Steps & Workout",
                                                fontFamily = SfSemiFontFamily,
                                                fontSize = 10.sp,
                                                color = if (isDark) SlateLight else LightTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Featured Campus Quests Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Featured Quests",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                                Text(
                                    "View All →",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CobaltBlue,
                                    modifier = Modifier.clickable { selectedBottomTab = 1 }
                                )
                            }
                        }

                        // 2 Featured Quest Cards
                        items(allQuests.take(3)) { quest ->
                            QuestCardItem(
                                quest = quest,
                                onClick = { navController.navigate("quest_detail/${quest.questCode}") }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: QUESTS CATALOG (DISCOVER)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Campus Quests",
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                            Text(
                                text = "Complete physical activities at designated DLSL landmarks",
                                fontFamily = SfSemiFontFamily,
                                fontSize = 12.sp,
                                color = if (isDark) SlateLight else LightTextSecondary
                            )
                        }

                        // Search Bar
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search quest title or location...",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = if (isDark) SlateSecondary else LightTextSecondary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = if (isDark) SurfaceCharcoal else Color.White,
                                    unfocusedContainerColor = if (isDark) SurfaceCharcoal else Color.White,
                                    focusedBorderColor = CobaltBlue,
                                    unfocusedBorderColor = if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Category Chips
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val cats = listOf("All", "Quick", "Explorer", "Boss")
                                items(cats) { cat ->
                                    val isSel = selectedCategory.equals(cat, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primary else (if (isDark) SurfaceElevated else Color.White),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSel) MaterialTheme.colorScheme.primary else (if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))
                                        ),
                                        modifier = Modifier.clickable { selectedCategory = cat }
                                    ) {
                                        Text(
                                            text = when (cat) {
                                                "Quick" -> "Quick Quests"
                                                "Explorer" -> "Explorer Quests"
                                                "Boss" -> "Boss Quests"
                                                else -> "All"
                                            },
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier
                                                .heightIn(min = 36.dp)
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredQuests.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No quests match your filter.",
                                        fontFamily = SfSemiFontFamily,
                                        color = if (isDark) SlateLight else LightTextSecondary
                                    )
                                }
                            }
                        } else {
                            items(filteredQuests) { quest ->
                                QuestCardItem(
                                    quest = quest,
                                    onClick = { navController.navigate("quest_detail/${quest.questCode}") }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: RANKINGS & CLASS STANDINGS
                    WeeklyClassStandingsScreen(navController)
                }

                3 -> {
                    // TAB 3: PROFILE & SETTINGS (Upload avatar, Theme switch, Notifications, Logout)
                    ProfileSettingsContent(
                        navController = navController,
                        isAdmin = false,
                        onLogout = onLogout
                    )
                }
            }
            }

            if (showConfetti) {
                ConfettiBurst(trigger = showConfetti)
            }
        }
    }
}

@Composable
fun QuestCardItem(
    quest: Quest,
    onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    val categoryColor = when (quest.category.lowercase()) {
        "boss" -> if (isDark) ArcadeGold else GoldAccent
        "explorer" -> ElectricBlue
        else -> MintEmerald
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) SurfaceCharcoal.copy(alpha = 0.88f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ticket Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                quest.classificationLabel(),
                                color = categoryColor,
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MintEmerald.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "+${quest.pointValue} PTS",
                            color = MintEmerald,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = quest.title,
                fontFamily = SfBoldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDark) Color.White else LightTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) SurfaceElevatedHigh.copy(alpha = 0.5f) else Color(0xFFEDF2F7),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = quest.location.ifEmpty { "Campus Checkpoint" },
                        fontFamily = SfSemiFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White else LightTextPrimary,
                        maxLines = 2,
                        softWrap = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = quest.activityDurationLabel(),
                    fontFamily = SfSemiFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = quest.evidenceDurationLabel(),
                    fontFamily = SfSemiFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start Quest Pill Button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    "VIEW CHALLENGE DETAILS",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailScreen(
    navController: NavController,
    questCode: String
) {
    val isDark = ThemeManager.isDarkMode
    var quest by remember { mutableStateOf<Quest?>(null) }

    LaunchedEffect(questCode) {
        quest = Repository.getQuest(questCode)
    }

    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate("home") { popUpTo(0) }
        }
    }

    val currentQuest = quest

    Scaffold(
        containerColor = if (isDark) DarkNavyBackground else LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentQuest?.title ?: "Campus Quest",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else LightTextPrimary,
                        letterSpacing = 0.5.sp,
                        maxLines = 2,
                        softWrap = true
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) SurfaceCharcoal else Color.White
                )
            )
        }
    ) { padding ->
        if (currentQuest == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CobaltBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CobaltBlue.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue)
                    ) {
                        Text(
                            "${currentQuest.classificationLabel()} • ${currentQuest.pointValue} POINTS",
                            color = CobaltBlue,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        currentQuest.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else LightTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MintEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            currentQuest.location.ifEmpty { "Campus" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MintEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    LiquidGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        glowColor = CobaltBlue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = CobaltBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Challenge Instructions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                currentQuest.instructions,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isDark) SlateLight else LightTextSecondary,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    currentQuest.activityDurationLabel(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    currentQuest.evidenceDurationLabel(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Accessible option: ${currentQuest.inclusiveAlternative()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "+${currentQuest.pointValue} Class Pts",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MintEmerald
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Lock Security Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) SurfaceCharcoal.copy(alpha = 0.8f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) CobaltBlue.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CobaltBlue.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = CobaltBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Physical QR checkpoint",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Visit ${currentQuest.location} and scan the campus QR marker to continue. ${currentQuest.evidenceDurationLabel()}.",
                                    color = if (isDark) SlateLight else LightTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        navController.navigate("scan")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "SCAN QR CODE AT LOCATION",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestBriefingScreen(
    navController: NavController,
    questCode: String
) {
    val isDark = ThemeManager.isDarkMode
    var quest by remember { mutableStateOf<Quest?>(null) }

    LaunchedEffect(questCode) {
        quest = Repository.getQuest(questCode)
    }

    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate("home") { popUpTo(0) }
        }
    }

    val currentQuest = quest

    Scaffold(
        containerColor = if (isDark) DarkNavyBackground else LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CHALLENGE UNLOCKED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else LightTextPrimary,
                        letterSpacing = 1.sp
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) SurfaceCharcoal else Color.White
                )
            )
        }
    ) { padding ->
        if (currentQuest == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CobaltBlue)
            }
        } else {
            val categoryColor = when (currentQuest.category.lowercase()) {
                "boss" -> if (isDark) ArcadeGold else GoldAccent
                "explorer" -> ElectricBlue
                else -> MintEmerald
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    // Checkpoint Verified Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MintEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MintEmerald.copy(alpha = 0.25f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MintEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "QR CHECKPOINT VERIFIED",
                                    fontWeight = FontWeight.Black,
                                    color = MintEmerald,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    "On-site at ${currentQuest.location}",
                                    color = if (isDark) Color.White.copy(alpha = 0.9f) else LightTextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = categoryColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor)
                        ) {
                            Text(
                                currentQuest.classificationLabel(),
                                color = categoryColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MintEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                "+${currentQuest.pointValue} PTS",
                                color = MintEmerald,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        currentQuest.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else LightTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            currentQuest.location,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quest Instructions Card
                    LiquidGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        glowColor = categoryColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = categoryColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Quest Instructions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                currentQuest.instructions,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isDark) SlateLight else LightTextSecondary,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Rubric Tips
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) SurfaceElevated.copy(alpha = 0.8f) else Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "RECORDING REQUIREMENTS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.8.sp,
                                        color = if (isDark) SlateLight else LightTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "${currentQuest.activityDurationLabel()}\n${currentQuest.evidenceDurationLabel()}\nAccessible option: ${currentQuest.inclusiveAlternative()}\nKeep the activity visible for reviewer assessment.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) SlateLight else LightTextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (currentQuest.evidenceConfiguration().mode == QuestEvidenceMode.CHECKPOINT) {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        } else {
                            navController.navigate("record/${currentQuest.questCode}/${currentQuest.maxSeconds}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Icon(
                        if (currentQuest.evidenceConfiguration().mode == QuestEvidenceMode.CHECKPOINT) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Videocam
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (currentQuest.evidenceConfiguration().mode == QuestEvidenceMode.CHECKPOINT) {
                            "CONTINUE WITHOUT VIDEO"
                        } else {
                            "PROCEED TO VIDEO RECORDING"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
