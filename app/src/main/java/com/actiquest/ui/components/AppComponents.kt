package com.actiquest.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.actiquest.R
import com.actiquest.domain.DateTimeUtils
import com.actiquest.domain.Submission
import com.actiquest.ui.theme.*

@Composable
fun UserAvatarView(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showUploadBadge: Boolean = false,
    onPhotoSelected: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && onPhotoSelected != null) {
            onPhotoSelected(uri)
        }
    }

    val isDark = ThemeManager.isDarkMode
    val initials = name.trim().split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "AQ" }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onPhotoSelected != null) {
                    Modifier.clickable { photoPickerLauncher.launch("image/*") }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing border ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            CobaltBlue,
                            Color(0xFF8B5CF6),
                            GlowingCyan,
                            CobaltBlue
                        )
                    )
                )
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (isDark) SurfaceCharcoal else Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default high-aesthetic avatar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        CobaltBlue.copy(alpha = 0.85f),
                                        Color(0xFF7C3AED)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (size.value * 0.38f).sp
                        )
                    }
                }
            }
        }

        // Upload edit badge
        if (showUploadBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.36f)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(CobaltBlue)
                    .border(1.5.dp, if (isDark) DarkNavyBackground else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Upload Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.20f)
                )
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun AppBottomNavBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 6.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            val totalWidth = this.maxWidth
            val itemCount = items.size.coerceAtLeast(1)
            val tabWidth = totalWidth / itemCount
            
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "bottom_nav_pill_offset"
            )

            // Sliding background indicator pill
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    Box(
                        modifier = Modifier
                            .width(tabWidth)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .selectable(
                                selected = isSelected,
                                role = Role.Tab,
                                onClick = { onItemSelected(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (item.badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .clip(CircleShape)
                                            .background(StatusPending)
                                    )
                                }
                            }

                            if (isSelected) {
                                Text(
                                    text = item.title,
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = listOf(
        BottomNavItem("Home", Icons.Default.Home, Icons.Default.Home),
        BottomNavItem("Quests", Icons.Default.Explore, Icons.Default.Explore),
        BottomNavItem("Ranks", Icons.Default.Leaderboard, Icons.Default.Leaderboard),
        BottomNavItem("Profile", Icons.Default.Person, Icons.Default.Person)
    )
) {
    AppBottomNavBar(
        items = items,
        selectedIndex = selectedTab,
        onItemSelected = onTabSelected,
        modifier = modifier
    )
}

@Composable
fun WellbeingScoreHeroCard(
    currentPoints: Int = 0,
    targetPoints: Int = 500,
    scorePercent: Int? = null,
    trendText: String = "↑ 12% from last week",
    modifier: Modifier = Modifier,
    onEditTarget: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val isDark = ThemeManager.isDarkMode
    val safeTarget = targetPoints.coerceAtLeast(1)
    val calculatedPercent = scorePercent ?: ((currentPoints.toFloat() / safeTarget) * 100).toInt().coerceIn(0, 100)

    val animatedProgress by animateFloatAsState(
        targetValue = (calculatedPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(26.dp),
        color = if (isDark) SurfaceCharcoal.copy(alpha = 0.92f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = if (isDark) listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.5f),
                    CobaltBlue.copy(alpha = 0.35f),
                    SurfaceElevatedHigh
                ) else listOf(
                    Color(0xFFE0E7FF),
                    Color(0xFFCBD5E1)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient glowing gradient background fading seamlessly with no hard boxy boundaries
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) listOf(
                                Color(0xFF7C3AED).copy(alpha = 0.16f),
                                Color(0xFF2563EB).copy(alpha = 0.08f),
                                Color.Transparent
                            ) else listOf(
                                Color(0xFFEEF2FF).copy(alpha = 0.7f),
                                Color(0xFFF8FAFC),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left metrics column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Activity Score",
                            fontFamily = SfSemiFontFamily,
                            fontSize = 13.sp,
                            color = if (isDark) SlateLight else LightTextSecondary
                        )

                        // Target Goal Pill that can be clicked to edit target
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) SurfaceElevated.copy(alpha = 0.8f) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                if (isDark) CobaltBlue.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.clickable { onEditTarget?.invoke() ?: onClick?.invoke() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = "Target Goal",
                                    tint = if (isDark) GlowingCyan else CobaltBlue,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Goal: $safeTarget pts",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isDark) GlowingCyan else CobaltBlue
                                )
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Goal",
                                    tint = if (isDark) SlateLight else LightTextSecondary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$calculatedPercent%",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 38.sp,
                            color = if (isDark) Color.White else LightTextPrimary,
                            letterSpacing = (-0.8).sp
                        )

                        Text(
                            text = "($currentPoints / $safeTarget pts)",
                            fontFamily = SfSemiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Trend pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MintEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MintEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = trendText,
                            fontFamily = SfSemiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MintEmerald
                        )
                    }
                }

                // Right circular progress ring
                Box(
                    modifier = Modifier.size(92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Canvas circular ring
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.5.dp.toPx()

                        // Background track
                        drawCircle(
                            color = if (isDark) Color(0xFF262338) else Color(0xFFE2E8F0),
                            style = Stroke(width = strokeWidth)
                        )

                        // Progress arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    CobaltBlue,
                                    Color(0xFF8B5CF6),
                                    GlowingCyan,
                                    MintEmerald
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Centered glowing heart/target icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CobaltBlue.copy(alpha = 0.30f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.40f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatGridCard(
    label: String = "",
    value: String = "",
    icon: ImageVector = Icons.Default.Star,
    iconColor: Color = CobaltBlue,
    accentColor: Color = iconColor,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkMode
    val finalColor = if (accentColor != iconColor) accentColor else iconColor

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) SurfaceCharcoal.copy(alpha = 0.90f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDark) SurfaceElevatedHigh.copy(alpha = 0.9f) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(finalColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = finalColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = if (isDark) Color.White else LightTextPrimary,
                    maxLines = 1
                )
                Text(
                    text = label,
                    fontFamily = SfSemiFontFamily,
                    fontSize = 11.5.sp,
                    color = if (isDark) SlateLight else LightTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DailyActivityBarChart(
    submissions: List<Submission> = emptyList(),
    userId: String = "",
    dayPoints: List<Pair<String, Int>>? = null,
    activeDayIndex: Int = DateTimeUtils.getTodayDayOfWeekIndex(),
    title: String = "Daily Activity",
    days: List<String>? = null,
    values: List<Float>? = null,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkMode
    var selectedWeekFilter by remember { mutableStateOf("This Week") }
    var weekDropdownExpanded by remember { mutableStateOf(false) }
    var inspectedDayIndex by remember(selectedWeekFilter) { mutableStateOf<Int?>(null) }

    val weekOptions = listOf("This Week", "Last Week", "2 Weeks Ago", "3 Weeks Ago")

    val weekOffset = when (selectedWeekFilter) {
        "Last Week" -> 1
        "2 Weeks Ago" -> 2
        "3 Weeks Ago" -> 3
        else -> 0
    }

    val activeWeekData: List<Pair<String, Int>> = remember(selectedWeekFilter, submissions, userId, dayPoints, days, values) {
        if (days != null && values != null && days.size == values.size) {
            days.zip(values) { d, v -> d to v.toInt() }
        } else if (dayPoints != null && submissions.isEmpty() && userId.isEmpty()) {
            dayPoints
        } else {
            DateTimeUtils.getPointsForWeek(
                weekOffset = weekOffset,
                submissions = submissions,
                userId = userId
            )
        }
    }

    val totalWeekPoints = remember(activeWeekData) { activeWeekData.sumOf { it.second } }
    val maxPoint = (activeWeekData.maxOfOrNull { it.second } ?: 100).coerceAtLeast(50)
    val effectiveActiveIndex = inspectedDayIndex ?: if (selectedWeekFilter == "This Week") activeDayIndex else {
        val maxIdx = activeWeekData.indexOfFirst { it.second == activeWeekData.maxOf { p -> p.second } }
        if (maxIdx >= 0 && activeWeekData[maxIdx].second > 0) maxIdx else 6
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) SurfaceCharcoal.copy(alpha = 0.90f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDark) SurfaceElevatedHigh.copy(alpha = 0.85f) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else LightTextPrimary
                    )
                    Text(
                        text = "$totalWeekPoints pts earned ($selectedWeekFilter)",
                        fontFamily = SfSemiFontFamily,
                        fontSize = 11.5.sp,
                        color = if (isDark) SlateLight else LightTextSecondary
                    )
                }

                // Interactive Week Selector Dropdown Button
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) SurfaceElevated else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.clickable { weekDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedWeekFilter,
                                fontFamily = SfSemiFontFamily,
                                fontSize = 11.5.sp,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select week",
                                tint = if (isDark) SlateLight else LightTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = weekDropdownExpanded,
                        onDismissRequest = { weekDropdownExpanded = false },
                        modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                    ) {
                        weekOptions.forEach { week ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = week,
                                        fontFamily = if (week == selectedWeekFilter) SfBoldFontFamily else SfSemiFontFamily,
                                        fontWeight = if (week == selectedWeekFilter) FontWeight.Bold else FontWeight.Normal,
                                        color = if (week == selectedWeekFilter) (if (isDark) GlowingCyan else CobaltBlue)
                                                else (if (isDark) Color.White else LightTextPrimary)
                                    )
                                },
                                onClick = {
                                    selectedWeekFilter = week
                                    weekDropdownExpanded = false
                                },
                                leadingIcon = {
                                    if (week == selectedWeekFilter) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isDark) GlowingCyan else CobaltBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 7 Bars with anim
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                activeWeekData.forEachIndexed { idx, pair ->
                    val isCurrentDay = idx == effectiveActiveIndex
                    val fraction = if (pair.second > 0) {
                        (pair.second.toFloat() / maxPoint).coerceIn(0.18f, 1f)
                    } else {
                        0.08f
                    }

                    val animHeight by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(700 + idx * 60, easing = FastOutSlowInEasing),
                        label = "bar_height"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { inspectedDayIndex = idx }
                    ) {
                        // Fixed Tooltip Slot
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (isCurrentDay) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color.White else CobaltBlue,
                                    shadowElevation = 3.dp
                                ) {
                                    Text(
                                        text = "${pair.second} pts",
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = if (isDark) Color(0xFF0F172A) else Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Vertical capsule bar
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height((80 * animHeight).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrentDay) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                GlowingCyan,
                                                Color(0xFF8B5CF6),
                                                CobaltBlue
                                            )
                                        )
                                    } else {
                                        if (isDark) Brush.verticalGradient(
                                            listOf(
                                                SurfaceElevatedHigh,
                                                SurfaceElevated
                                            )
                                        ) else Brush.verticalGradient(
                                            listOf(
                                                Color(0xFFCBD5E1),
                                                Color(0xFFE2E8F0)
                                            )
                                        )
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day label
                        Text(
                            text = pair.first,
                            fontFamily = if (isCurrentDay) SfBoldFontFamily else SfSemiFontFamily,
                            fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = if (isCurrentDay) {
                                if (isDark) Color(0xFFA78BFA) else CobaltBlue
                            } else {
                                if (isDark) SlateSecondary else LightTextSecondary
                            }
                        )
                    }
                }
            }
        }
    }
}
