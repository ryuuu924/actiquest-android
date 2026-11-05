package com.actiquest.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.actiquest.data.Repository
import com.actiquest.domain.Badge
import com.actiquest.domain.BadgeCatalog
import com.actiquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(navController: NavController) {
    val isDark = ThemeManager.isDarkMode
    val currentUserId by Repository.currentUserId.collectAsState()
    val userState by remember(currentUserId) { Repository.listenUser(currentUserId) }.collectAsState(initial = null)
    val user = userState

    val earnedBadgeTitles = user?.badges ?: emptyList()
    val totalBadges = BadgeCatalog.ALL_BADGES.size
    val earnedCount = BadgeCatalog.ALL_BADGES.count { earnedBadgeTitles.contains(it.title) }

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedBadgeForDetail by remember { mutableStateOf<Badge?>(null) }

    val categories = listOf("All", "Milestone", "Mastery", "Streak", "Special")
    val filteredBadges = remember(selectedCategory) {
        if (selectedCategory == "All") {
            BadgeCatalog.ALL_BADGES
        } else {
            BadgeCatalog.ALL_BADGES.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    androidx.activity.compose.BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate("home") { popUpTo(0) }
        }
    }

    Scaffold(
        containerColor = if (isDark) DarkNavyBackground else LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CAMPUS BADGES",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else LightTextPrimary,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false
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
                            contentDescription = "Back to Home",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Unlocked summary banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceElevated else Color.White),
                elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                if (isDark) {
                                    listOf(CobaltBlue.copy(alpha = 0.35f), IndigoPurple.copy(alpha = 0.25f))
                                } else {
                                    listOf(CobaltBlue.copy(alpha = 0.12f), MintEmerald.copy(alpha = 0.12f))
                                }
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "TROPHY COLLECTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = SfBoldFontFamily,
                                color = MintEmerald,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$earnedCount of $totalBadges Badges Unlocked",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { if (totalBadges > 0) earnedCount.toFloat() / totalBadges else 0f },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MintEmerald,
                                trackColor = if (isDark) SurfaceCharcoal else Color(0xFFE2E8F0)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MintEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintEmerald),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🏆", fontSize = 26.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontFamily = SfSemiFontFamily, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CobaltBlue,
                            selectedLabelColor = Color.White,
                            containerColor = if (isDark) SurfaceCharcoal else Color.White,
                            labelColor = if (isDark) TextSecondary else LightTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) CobaltBlue else (if (isDark) SlateSecondary.copy(alpha = 0.4f) else Color(0xFFE2E8F0))
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badge Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredBadges) { badge ->
                    val isEarned = earnedBadgeTitles.contains(badge.title)
                    BadgeGridItem(
                        badge = badge,
                        isEarned = isEarned,
                        approvedCount = user?.approvedCount ?: 0,
                        onClick = { selectedBadgeForDetail = badge }
                    )
                }
            }
        }
    }

    // Badge Details Dialog
    selectedBadgeForDetail?.let { badge ->
        val isEarned = earnedBadgeTitles.contains(badge.title)
        BadgeDetailDialog(
            badge = badge,
            isEarned = isEarned,
            approvedCount = user?.approvedCount ?: 0,
            onDismiss = { selectedBadgeForDetail = null }
        )
    }
}

@Composable
fun BadgeGridItem(
    badge: Badge,
    isEarned: Boolean,
    approvedCount: Int,
    onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                if (isEarned) SurfaceElevated else SurfaceCharcoal
            } else {
                if (isEarned) Color(0xFFF0FDF4) else Color.White
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEarned) MintEmerald.copy(alpha = 0.7f) else (if (isDark) SlateSecondary.copy(alpha = 0.25f) else Color(0xFFE2E8F0))
        ),
        elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEarned) MintEmerald.copy(alpha = 0.15f) else (if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0xFFF1F5F9))
                    )
                    .border(
                        width = if (isEarned) 2.dp else 1.dp,
                        color = if (isEarned) MintEmerald else (if (isDark) SlateSecondary.copy(alpha = 0.3f) else Color(0xFFCBD5E1)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isEarned) {
                    Text(badge.iconEmoji, fontSize = 32.sp)
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = if (isDark) SlateSecondary else LightTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                badge.title,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = SfBoldFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (isEarned) (if (isDark) Color.White else LightTextPrimary) else (if (isDark) TextMuted else LightTextSecondary),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isEarned) MintEmerald.copy(alpha = 0.2f) else (if (isDark) SlateSecondary.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
            ) {
                Text(
                    if (isEarned) "UNLOCKED" else "LOCKED",
                    color = if (isEarned) MintEmerald else (if (isDark) SlateSecondary else LightTextSecondary),
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun BadgeDetailDialog(
    badge: Badge,
    isEarned: Boolean,
    approvedCount: Int,
    onDismiss: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceCharcoal else Color.White),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isEarned) MintEmerald.copy(alpha = 0.8f) else CobaltBlue.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEarned) MintEmerald.copy(alpha = 0.2f) else (if (isDark) SurfaceElevated else Color(0xFFF1F5F9))
                        )
                        .border(
                            width = 2.dp,
                            color = if (isEarned) MintEmerald else (if (isDark) SlateSecondary else Color(0xFFCBD5E1)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge.iconEmoji, fontSize = 42.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    badge.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else LightTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEarned) MintEmerald.copy(alpha = 0.2f) else CobaltBlue.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isEarned) MintEmerald else CobaltBlue
                    )
                ) {
                    Text(
                        if (isEarned) "✓ BADGE EARNED" else "${badge.category.uppercase()} BADGE",
                        color = if (isEarned) MintEmerald else (if (isDark) ElectricBlue else CobaltBlue),
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = SfSemiFontFamily,
                    color = if (isDark) TextSecondary else LightTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                if (badge.requiredApprovedCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Requirement Progress",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = SfSemiFontFamily,
                                color = if (isDark) TextMuted else LightTextSecondary
                            )
                            Text(
                                "$approvedCount / ${badge.requiredApprovedCount} Quests",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isEarned) MintEmerald else (if (isDark) Color.White else LightTextPrimary)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = {
                                (approvedCount.toFloat() / badge.requiredApprovedCount).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (isEarned) MintEmerald else CobaltBlue,
                            trackColor = if (isDark) SurfaceElevated else Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEarned) MintEmerald else CobaltBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Close", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
