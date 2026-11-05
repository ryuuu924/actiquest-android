package com.actiquest.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.actiquest.data.Repository
import com.actiquest.domain.ActivityJourneyItem
import com.actiquest.domain.CalendarDay
import com.actiquest.domain.DateTimeUtils
import com.actiquest.domain.User
import com.actiquest.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityJourneyScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = ThemeManager.isDarkMode

    val currentUserId by Repository.currentUserId.collectAsState()
    val userState by remember(currentUserId) { Repository.listenUser(currentUserId) }.collectAsState(initial = null)
    val userSubmissions by remember(currentUserId) { Repository.listenUserSubmissions(currentUserId) }.collectAsState(initial = emptyList())

    val user = userState ?: User(uid = currentUserId, name = "Juan de la Cruz", totalPoints = 230, targetPoints = 500)

    val today = remember { LocalDate.now(ZoneId.of("Asia/Manila")) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now(ZoneId.of("Asia/Manila"))) }
    var selectedDate by remember { mutableStateOf(today) }
    var viewMode by remember { mutableStateOf("Monthly") } // "Weekly", "Monthly", "All-Time"
    var showGoalDialog by remember { mutableStateOf(false) }
    var customGoalInput by remember { mutableStateOf(user.targetPoints.toString()) }
    var showMenuDropdown by remember { mutableStateOf(false) }

    // Safe Back handling
    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate("home") { popUpTo(0) }
        }
    }

    val allJourneyItems: List<ActivityJourneyItem> = remember(userSubmissions, currentUserId) {
        userSubmissions
            .filter { it.userId == currentUserId && it.submittedAt > 0L }
            .sortedByDescending { it.submittedAt }
            .map { sub ->
                val cat = when {
                    sub.submissionType == "video" -> "Campus Quest"
                    sub.questCode.isNotBlank() -> "Campus Quest"
                    sub.activityType.isNotBlank() -> sub.activityType
                    else -> "Fitness Proof"
                }
                val metric = when {
                    sub.submissionType == "screenshot" && sub.activityMetricValue.isNotBlank() -> sub.activityMetricValue
                    sub.submissionType == "video" -> "Campus Video Proof"
                    sub.activityType.isNotBlank() -> "${sub.activityType} • Verified"
                    else -> "Verified Quest"
                }
                val stat = when (sub.status.lowercase()) {
                    "approved" -> "Approved"
                    "rejected" -> "Rejected"
                    else -> "In Review"
                }
                ActivityJourneyItem(
                    id = sub.submissionId,
                    title = sub.questTitle.ifEmpty { "Fitness Challenge" },
                    category = cat,
                    pointValue = sub.awardedPoints ?: sub.pointValue,
                    timestamp = sub.submittedAt,
                    durationOrMetric = metric,
                    status = stat
                )
            }
    }
    val activeStreak = remember(userSubmissions, currentUserId) {
        DateTimeUtils.calculateStreaks(userSubmissions, currentUserId).currentStreak
    }

    val calendarDays = remember(currentYearMonth, selectedDate, userSubmissions, user.uid) {
        DateTimeUtils.getMonthDays(
            year = currentYearMonth.year,
            month = currentYearMonth.monthValue,
            selectedDate = selectedDate,
            submissions = userSubmissions,
            userId = user.uid
        )
    }

    val dayOfWeekVal = today.dayOfWeek.value // 1 (Mon) to 7 (Sun)
    val thisMonday = remember(today) { today.minusDays((dayOfWeekVal - 1).toLong()) }
    val thisSunday = remember(today) { thisMonday.plusDays(6) }

    // Period Points & Goal Target calculation
    val (periodEarnedPoints, periodTargetPoints, periodLabel) = remember(
        viewMode, allJourneyItems, user.totalPoints, user.targetPoints, currentYearMonth, thisMonday, thisSunday
    ) {
        when (viewMode) {
            "Weekly" -> {
                val weekItems = allJourneyItems.filter { item ->
                    val itemDate = DateTimeUtils.getEpochLocalDate(item.timestamp)
                    !itemDate.isBefore(thisMonday) && !itemDate.isAfter(thisSunday)
                }
                val pts = weekItems.filter { it.status == "Approved" }.sumOf { it.pointValue }
                val displayPts = if (pts > 0) pts else (user.totalPoints / 4).coerceAtLeast(60).coerceAtMost(user.totalPoints)
                val target = (user.targetPoints / 4).coerceAtLeast(100)
                Triple(displayPts, target, "Weekly Goal Progress")
            }
            "All-Time" -> {
                val pts = user.totalPoints
                val target = maxOf(user.targetPoints, 1000)
                Triple(pts, target, "All-Time Milestone Goal")
            }
            else -> { // "Monthly"
                val monthItems = allJourneyItems.filter { item ->
                    val itemDate = DateTimeUtils.getEpochLocalDate(item.timestamp)
                    itemDate.year == currentYearMonth.year && itemDate.monthValue == currentYearMonth.monthValue
                }
                val pts = monthItems.filter { it.status == "Approved" }.sumOf { it.pointValue }
                val displayPts = if (pts > 0) pts else user.totalPoints
                val target = user.targetPoints.coerceAtLeast(1)
                Triple(displayPts, target, "Monthly Goal Progress")
            }
        }
    }

    val progressPercent = ((periodEarnedPoints.toFloat() / periodTargetPoints.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)

    var filterToSelectedDateOnly by remember(selectedDate, viewMode) { mutableStateOf(true) }

    val displayedActivities = remember(
        viewMode, selectedDate, allJourneyItems, currentYearMonth, thisMonday, thisSunday, filterToSelectedDateOnly
    ) {
        when (viewMode) {
            "Weekly" -> {
                val weekItems = allJourneyItems.filter { item ->
                    val itemDate = DateTimeUtils.getEpochLocalDate(item.timestamp)
                    !itemDate.isBefore(thisMonday) && !itemDate.isAfter(thisSunday)
                }
                if (filterToSelectedDateOnly) {
                    weekItems.filter { DateTimeUtils.getEpochLocalDate(it.timestamp) == selectedDate }
                } else {
                    weekItems
                }
            }
            "All-Time" -> {
                allJourneyItems
            }
            else -> { // "Monthly"
                val monthItems = allJourneyItems.filter { item ->
                    val itemDate = DateTimeUtils.getEpochLocalDate(item.timestamp)
                    itemDate.year == currentYearMonth.year && itemDate.monthValue == currentYearMonth.monthValue
                }
                if (filterToSelectedDateOnly) {
                    monthItems.filter { DateTimeUtils.getEpochLocalDate(it.timestamp) == selectedDate }
                } else {
                    monthItems
                }
            }
        }
    }

    if (showGoalDialog) {
        val targetGoals = listOf(250, 500, 750, 1000, 1500)
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    "Set Activity Target Goal",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else LightTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Customize your monthly point target to measure your personal and class contribution.",
                        fontFamily = SfSemiFontFamily,
                        fontSize = 13.sp,
                        color = if (isDark) SlateLight else LightTextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    "$goal pts",
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
                        onValueChange = { customGoalInput = it.filter { c -> c.isDigit() } },
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
                                showGoalDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Please enter a valid points goal", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                ) {
                    Text("Save Goal", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", fontFamily = SfSemiFontFamily, color = if (isDark) SlateLight else LightTextSecondary)
                }
            },
            containerColor = if (isDark) SurfaceCharcoal else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    val pageBgGradient = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color(0xFF1E1438),
            0.35f to DarkNavyBackground,
            1.0f to DarkNavyBackground
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0xFFF1F5F9),
            0.4f to LightBackground,
            1.0f to LightBackground
        )
    }

    Scaffold(
        containerColor = if (isDark) DarkNavyBackground else LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Journey",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate("home") { popUpTo(0) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else LightTextPrimary
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showMenuDropdown = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isDark) SurfaceElevated else Color.White)
                                .border(1.dp, if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = if (isDark) Color.White else LightTextPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false },
                            modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Set Target Goal", color = if (isDark) Color.White else LightTextPrimary) },
                                onClick = {
                                    showMenuDropdown = false
                                    customGoalInput = user.targetPoints.toString()
                                    showGoalDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Flag, contentDescription = null, tint = CobaltBlue)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Submissions", color = if (isDark) Color.White else LightTextPrimary) },
                                onClick = {
                                    showMenuDropdown = false
                                    navController.navigate("my_submissions")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.History, contentDescription = null, tint = GlowingCyan)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Leaderboards", color = if (isDark) Color.White else LightTextPrimary) },
                                onClick = {
                                    showMenuDropdown = false
                                    navController.navigate("standings")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Leaderboard, contentDescription = null, tint = MintEmerald)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color.Transparent else LightBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBgGradient)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // View Mode Tabs (Weekly, Monthly, All-Time)
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) SurfaceElevated.copy(alpha = 0.9f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Weekly", "Monthly", "All-Time").forEach { mode ->
                                val isSel = viewMode == mode
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) CobaltBlue else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewMode = mode }
                                ) {
                                    Text(
                                        text = mode,
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSel) Color.White else (if (isDark) SlateLight else LightTextSecondary),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Dynamic Top Panel based on viewMode: Weekly / Monthly / All-Time
                item {
                    when (viewMode) {
                        "Weekly" -> {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = if (isDark) Color(0xFF161326).copy(alpha = 0.95f) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF2E2648) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isDark) 0.dp else 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Weekly Schedule",
                                                fontFamily = SfBoldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = if (isDark) Color.White else LightTextPrimary
                                            )
                                            Text(
                                                text = "${thisMonday.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))} – ${thisSunday.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))}",
                                                fontFamily = SfSemiFontFamily,
                                                fontSize = 12.sp,
                                                color = if (isDark) SlateLight else LightTextSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = CobaltBlue.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "This Week",
                                                fontFamily = SfBoldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = CobaltBlue,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // 7 Days of the Week Capsules (Mon to Sun)
                                    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        (0..6).forEach { offset ->
                                            val dayDate = thisMonday.plusDays(offset.toLong())
                                            val isSelected = dayDate == selectedDate
                                            val isToday = dayDate == today
                                            val dayHasActivities = allJourneyItems.any {
                                                DateTimeUtils.getEpochLocalDate(it.timestamp) == dayDate
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = when {
                                                    isSelected -> CobaltBlue
                                                    isToday -> if (isDark) SurfaceElevated else Color(0xFFEEF2FF)
                                                    else -> if (isDark) SurfaceElevated.copy(alpha = 0.5f) else Color(0xFFF8FAFC)
                                                },
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    when {
                                                        isSelected -> CobaltBlue
                                                        isToday -> Color(0xFFF59E0B)
                                                        else -> if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        selectedDate = dayDate
                                                        filterToSelectedDateOnly = true
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = dayNames[offset],
                                                        fontFamily = SfSemiFontFamily,
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else (if (isDark) SlateLight else LightTextSecondary)
                                                    )
                                                    Text(
                                                        text = "${dayDate.dayOfMonth}",
                                                        fontFamily = SfBoldFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = if (isSelected) Color.White else (if (isDark) Color.White else LightTextPrimary)
                                                    )
                                                    if (dayHasActivities) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color.White else MintEmerald)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.size(5.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Category Legend
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LegendItem(color = Color(0xFFA78BFA), label = "Quests", isDark = isDark)
                                        LegendItem(color = GlowingCyan, label = "Workouts", isDark = isDark)
                                        LegendItem(color = Color(0xFFF59E0B), label = "Streaks", isDark = isDark)
                                    }
                                }
                            }
                        }
                        "All-Time" -> {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = if (isDark) Color(0xFF161326).copy(alpha = 0.95f) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF2E2648) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isDark) 0.dp else 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Lifetime Journey Overview",
                                                fontFamily = SfBoldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = if (isDark) Color.White else LightTextPrimary
                                            )
                                            Text(
                                                text = "${user.college} • ${user.program}",
                                                fontFamily = SfSemiFontFamily,
                                                fontSize = 12.sp,
                                                color = if (isDark) SlateLight else LightTextSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = CobaltBlue.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "All-Time",
                                                fontFamily = SfBoldFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = CobaltBlue,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        JourneyStatMetricBox(
                                            title = "Total Earned",
                                            value = "${user.totalPoints} pts",
                                            icon = Icons.Default.Stars,
                                            tint = MintEmerald,
                                            isDark = isDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                        JourneyStatMetricBox(
                                            title = "Completed",
                                            value = "${allJourneyItems.count { it.status == "Approved" }} Quests",
                                            icon = Icons.Default.CheckCircle,
                                            tint = CobaltBlue,
                                            isDark = isDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                        JourneyStatMetricBox(
                                            title = "Active Streak",
                                            value = "$activeStreak Days",
                                            icon = Icons.Default.LocalFireDepartment,
                                            tint = Color(0xFFF59E0B),
                                            isDark = isDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        else -> { // "Monthly"
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = if (isDark) Color(0xFF161326).copy(alpha = 0.95f) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF2E2648) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isDark) 0.dp else 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Calendar Header (Title + Month Selector)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Calendar",
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isDark) Color.White else LightTextPrimary
                                        )

                                        // Month Switcher with prev/next controls
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ChevronLeft,
                                                    contentDescription = "Previous Month",
                                                    tint = if (isDark) SlateLight else LightTextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isDark) SurfaceElevated else Color(0xFFF1F5F9),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    0.8.dp,
                                                    if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                                                )
                                            ) {
                                                val monthLabel = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
                                                Text(
                                                    text = monthLabel,
                                                    fontFamily = SfBoldFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.5.sp,
                                                    color = if (isDark) Color.White else LightTextPrimary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = "Next Month",
                                                    tint = if (isDark) SlateLight else LightTextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Day of Week Header Row (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                                            Text(
                                                text = day,
                                                fontFamily = SfSemiFontFamily,
                                                fontSize = 12.sp,
                                                color = if (isDark) SlateSecondary else LightTextSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Calendar Grid
                                    val rows = calendarDays.chunked(7)
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rows.forEach { week ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                week.forEach { day ->
                                                    CalendarDayCell(
                                                        day = day,
                                                        isDark = isDark,
                                                        onSelect = {
                                                            selectedDate = it.date
                                                            filterToSelectedDateOnly = true
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Category Legend
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LegendItem(color = Color(0xFFA78BFA), label = "Quests", isDark = isDark)
                                        LegendItem(color = GlowingCyan, label = "Workouts", isDark = isDark)
                                        LegendItem(color = Color(0xFFF59E0B), label = "Streaks", isDark = isDark)
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Goal Progress Summary Card
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (isDark) SurfaceElevated.copy(alpha = 0.95f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) CobaltBlue.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                        ),
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = periodLabel,
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                    Text(
                                        text = "$periodEarnedPoints of $periodTargetPoints pts goal",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 12.sp,
                                        color = if (isDark) SlateLight else LightTextSecondary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MintEmerald.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        customGoalInput = user.targetPoints.toString()
                                        showGoalDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$progressPercent%",
                                            fontFamily = SfBoldFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MintEmerald
                                        )
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Goal",
                                            tint = MintEmerald,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CobaltBlue,
                                trackColor = if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                // Section Header: "Daily Breakdown / Recent Activity"
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
                        val headerTitle = when {
                            viewMode == "All-Time" -> "All Activities (${displayedActivities.size})"
                            !filterToSelectedDateOnly -> "All ${viewMode} Activities (${displayedActivities.size})"
                            selectedDate == today -> "Today's Activity ($formattedDate)"
                            else -> "Activity on $formattedDate"
                        }
                        Text(
                            text = headerTitle,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.5.sp,
                            color = if (isDark) Color.White else LightTextPrimary
                        )

                        if (viewMode != "All-Time") {
                            Text(
                                text = if (filterToSelectedDateOnly) "Show All ($viewMode)" else "Filter by Date",
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = CobaltBlue,
                                modifier = Modifier.clickable {
                                    filterToSelectedDateOnly = !filterToSelectedDateOnly
                                }
                            )
                        } else {
                            Text(
                                text = "View Submissions",
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = CobaltBlue,
                                modifier = Modifier.clickable { navController.navigate("my_submissions") }
                            )
                        }
                    }
                }

                // List of Activities or Empty State
                if (displayedActivities.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDark) SurfaceElevated.copy(alpha = 0.8f) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = if (isDark) SlateLight else LightTextSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No Activities on ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))}",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDark) Color.White else LightTextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Select another date on the calendar or complete active campus quests to earn points!",
                                    fontFamily = SfSemiFontFamily,
                                    fontSize = 12.5.sp,
                                    color = if (isDark) SlateLight else LightTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                if (filterToSelectedDateOnly && viewMode != "All-Time") {
                                    FilledTonalButton(
                                        onClick = { filterToSelectedDateOnly = false },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = CobaltBlue.copy(alpha = 0.15f),
                                            contentColor = CobaltBlue
                                        )
                                    ) {
                                        Text("Show All $viewMode Activities", fontFamily = SfBoldFontFamily, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(displayedActivities) { activity ->
                        ActivityJourneyCard(
                            activity = activity,
                            isDark = isDark,
                            onClick = { navController.navigate("my_submissions") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JourneyStatMetricBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) SurfaceElevated else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                fontFamily = SfBoldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else LightTextPrimary
            )
            Text(
                text = title,
                fontFamily = SfSemiFontFamily,
                fontSize = 11.sp,
                color = if (isDark) SlateLight else LightTextSecondary
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    isDark: Boolean,
    onSelect: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = day.isSelected
    val isToday = day.isToday
    val isCurrentMonth = day.isCurrentMonth

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(enabled = isCurrentMonth) { onSelect(day) }
                .then(
                    when {
                        isSelected -> Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF8B5CF6),
                                    CobaltBlue
                                )
                            )
                        )
                        isToday -> Modifier
                            .background(if (isDark) SurfaceElevated else Color(0xFFEEF2FF))
                            .border(1.5.dp, Color(0xFFF59E0B), CircleShape)
                        else -> Modifier.background(Color.Transparent)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${day.dayNumber}",
                    fontFamily = if (isSelected || isToday) SfBoldFontFamily else SfSemiFontFamily,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.5.sp,
                    color = when {
                        isSelected -> Color.White
                        isToday -> if (isDark) Color(0xFFF59E0B) else CobaltBlue
                        isCurrentMonth -> if (isDark) Color.White else LightTextPrimary
                        else -> if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFCBD5E1)
                    }
                )

                // Activity indicator dot
                if (isCurrentMonth && (day.hasQuests || day.hasWorkouts || day.hasStreak)) {
                    val dotColor = when {
                        day.hasQuests -> Color(0xFFA78BFA)
                        day.hasWorkouts -> GlowingCyan
                        else -> Color(0xFFF59E0B)
                    }
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else dotColor)
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    isDark: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontFamily = SfSemiFontFamily,
            fontSize = 11.5.sp,
            color = if (isDark) SlateLight else LightTextSecondary
        )
    }
}

@Composable
fun ActivityJourneyCard(
    activity: ActivityJourneyItem,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val instant = java.time.Instant.ofEpochMilli(activity.timestamp)
    val localDateTime = instant.atZone(ZoneId.of("Asia/Manila")).toLocalDateTime()
    val dayNum = localDateTime.dayOfMonth.toString()
    val monthShort = localDateTime.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
    val timeFormatted = localDateTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) SurfaceElevated.copy(alpha = 0.92f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) SurfaceElevatedHigh.copy(alpha = 0.85f) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isDark) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Left Date Badge (like 20 MAY in image)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) SurfaceCharcoal else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF2E2648) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.size(width = 48.dp, height = 48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = dayNum,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = monthShort,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = if (isDark) SlateLight else LightTextSecondary
                        )
                    }
                }

                // Middle Title & Details
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activity.title,
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = if (isDark) Color.White else LightTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = activity.durationOrMetric,
                            fontFamily = SfSemiFontFamily,
                            fontSize = 12.sp,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MintEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "+${activity.pointValue} pts",
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MintEmerald,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Right Time Pill & Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) SurfaceCharcoal else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                    )
                ) {
                    Text(
                        text = timeFormatted,
                        fontFamily = SfSemiFontFamily,
                        fontSize = 11.sp,
                        color = if (isDark) SlateLight else LightTextSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Approved",
                    tint = MintEmerald,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
