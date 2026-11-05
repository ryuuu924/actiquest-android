package com.actiquest.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.actiquest.data.Repository
import com.actiquest.domain.AppNotification
import com.actiquest.domain.DateTimeUtils
import com.actiquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkMode
    val notificationsFlow = remember { Repository.listenNotifications() }
    val notifications by notificationsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf("All") }
    var showClearDialog by remember { mutableStateOf(false) }
    var notificationToDelete by remember { mutableStateOf<AppNotification?>(null) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedNotificationIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isDeleting by remember { mutableStateOf(false) }

    fun deleteNotifications(ids: Collection<String>, successMessage: String, onSuccess: () -> Unit) {
        if (isDeleting) return
        scope.launch {
            isDeleting = true
            try {
                Repository.deleteNotifications(ids)
                selectedNotificationIds = emptySet()
                selectionMode = false
                onSuccess()
                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Could not delete notifications: ${e.localizedMessage ?: "Please try again."}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isDeleting = false
            }
        }
    }

    fun toggleNotificationSelection(id: String) {
        selectedNotificationIds = if (id in selectedNotificationIds) {
            selectedNotificationIds - id
        } else {
            selectedNotificationIds + id
        }
        if (selectedNotificationIds.isEmpty()) selectionMode = false
    }

    // Safe Back handling
    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate("home") { popUpTo(0) }
        }
    }

    val unreadCount = notifications.count { !it.isRead }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Unread" -> notifications.filter { !it.isRead }
            "Approvals" -> notifications.filter { it.category == "Review" || it.category == "Academic" || it.title.contains("Approved", ignoreCase = true) || it.title.contains("Verified", ignoreCase = true) }
            "Quests" -> notifications.filter { it.category == "Quest" }
            "Streaks" -> notifications.filter { it.category == "Streak" || it.category == "Leaderboard" || it.category == "Badge" }
            else -> notifications
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    "Clear Notification History",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else LightTextPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to clear all notification history?",
                    fontFamily = SfSemiFontFamily,
                    color = if (isDark) SlateLight else LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteNotifications(notifications.map { it.id }, "Notifications cleared") {
                            showClearDialog = false
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text(if (isDeleting) "Deleting..." else "Clear All", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", fontFamily = SfSemiFontFamily, color = if (isDark) SlateLight else LightTextSecondary)
                }
            },
            containerColor = if (isDark) SurfaceCharcoal else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    notificationToDelete?.let { notification ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) notificationToDelete = null },
            title = {
                Text(
                    "Delete notification?",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else LightTextPrimary
                )
            },
            text = {
                Text(
                    "\"${notification.title}\" will be permanently removed from your notification history.",
                    fontFamily = SfSemiFontFamily,
                    color = if (isDark) SlateLight else LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    enabled = !isDeleting,
                    onClick = {
                        deleteNotifications(listOf(notification.id), "Notification deleted") {
                            notificationToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text(if (isDeleting) "Deleting..." else "Delete", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !isDeleting, onClick = { notificationToDelete = null }) {
                    Text("Cancel", fontFamily = SfSemiFontFamily, color = if (isDark) SlateLight else LightTextSecondary)
                }
            },
            containerColor = if (isDark) SurfaceCharcoal else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteSelectedDialog = false },
            title = {
                Text(
                    "Delete selected notifications?",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else LightTextPrimary
                )
            },
            text = {
                Text(
                    "${selectedNotificationIds.size} selected notification(s) will be permanently removed from your history.",
                    fontFamily = SfSemiFontFamily,
                    color = if (isDark) SlateLight else LightTextSecondary
                )
            },
            confirmButton = {
                Button(
                    enabled = !isDeleting,
                    onClick = {
                        deleteNotifications(selectedNotificationIds, "Selected notifications deleted") {
                            showDeleteSelectedDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text(if (isDeleting) "Deleting..." else "Delete selected", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !isDeleting, onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel", fontFamily = SfSemiFontFamily, color = if (isDark) SlateLight else LightTextSecondary)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        )
                        if (unreadCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = StatusRejected
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
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
                    if (notifications.isNotEmpty()) {
                        if (selectionMode) {
                            TextButton(onClick = {
                                selectionMode = false
                                selectedNotificationIds = emptySet()
                            }) {
                                Text("Cancel", fontFamily = SfBoldFontFamily, color = CobaltBlue)
                            }
                            IconButton(
                                enabled = selectedNotificationIds.isNotEmpty() && !isDeleting,
                                onClick = { showDeleteSelectedDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete selected notifications",
                                    tint = if (selectedNotificationIds.isNotEmpty()) StatusRejected else SlateSecondary
                                )
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            Repository.markAllNotificationsRead(notifications.map { it.id })
                                            Toast.makeText(context, "All marked as read", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not update notifications: ${e.localizedMessage ?: "Please try again."}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    "Mark Read",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = CobaltBlue
                                )
                            }
                            TextButton(onClick = { selectionMode = true }) {
                                Text("Select", fontFamily = SfBoldFontFamily, color = CobaltBlue)
                            }
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Clear All",
                                    tint = if (isDark) SlateLight else LightTextSecondary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color.Transparent else LightBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips
            val filters = listOf("All", "Unread", "Approvals", "Quests", "Streaks")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSel = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) CobaltBlue else (if (isDark) SurfaceElevated else Color.White),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSel) CobaltBlue else (if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))
                        ),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            fontFamily = if (isSel) SfBoldFontFamily else SfSemiFontFamily,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = if (isSel) Color.White else (if (isDark) SlateLight else LightTextSecondary),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            if (notifications.isNotEmpty()) {
                Text(
                    text = if (selectionMode) {
                        "${selectedNotificationIds.size} selected. Tap notifications to add or remove them."
                    } else {
                        "Tap Select or hold a notification to choose several."
                    },
                    fontFamily = SfSemiFontFamily,
                    fontSize = 11.sp,
                    color = if (isDark) SlateSecondary else LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) SurfaceElevated else Color(0xFFEEF2FF),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "No Notifications",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) Color.White else LightTextPrimary
                        )

                        Text(
                            text = if (selectedFilter == "Unread") "You are all caught up! No unread alerts." else "Updates on quest submissions, leaderboard ranks, and streaks will appear here.",
                            fontFamily = SfSemiFontFamily,
                            fontSize = 13.sp,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredNotifications, key = { index, notif -> if (notif.id.isNotBlank()) "${notif.id}_$index" else "notif_$index" }) { _, notif ->
                        NotificationItemCard(
                            notification = notif,
                            isDark = isDark,
                            isSelected = notif.id in selectedNotificationIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    toggleNotificationSelection(notif.id)
                                } else {
                                    scope.launch {
                                        try {
                                            Repository.markNotificationRead(notif.id)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not mark notification as read: ${e.localizedMessage ?: "Please try again."}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    val destination = when (notif.category) {
                                        "Review", "Quest" -> {
                                            val submissionId = notif.relatedId.takeIf { it.isNotBlank() }
                                            if (submissionId == null) {
                                                "my_submissions?filter=All"
                                            } else {
                                                "my_submissions?filter=All&submissionId=${Uri.encode(submissionId)}"
                                            }
                                        }
                                        "Academic" -> "my_submissions?filter=Academics"
                                        "Leaderboard" -> "standings"
                                        "Streak" -> "activity_journey"
                                        "Badge" -> "badges"
                                        else -> "home"
                                    }
                                    navController.navigate(destination) { launchSingleTop = true }
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    selectedNotificationIds = setOf(notif.id)
                                } else {
                                    toggleNotificationSelection(notif.id)
                                }
                            },
                            onSelect = { toggleNotificationSelection(notif.id) },
                            onDelete = {
                                notificationToDelete = notif
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationItemCard(
    notification: AppNotification,
    isDark: Boolean,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onSelect: () -> Unit = {},
    onDelete: () -> Unit
) {
    val relativeTime = DateTimeUtils.formatRelativeTime(notification.timestamp)

    val isAlert = notification.category == "Review" && (notification.title.contains("Revision", ignoreCase = true) || notification.title.contains("Rejected", ignoreCase = true))

    val iconVector = when (notification.category) {
        "Review" -> if (isAlert) Icons.Default.WarningAmber else Icons.Default.CheckCircle
        "Quest" -> Icons.Default.Explore
        "Streak" -> Icons.Default.LocalFireDepartment
        "Badge" -> Icons.Default.Stars
        "Leaderboard" -> Icons.Default.EmojiEvents
        "Academic" -> Icons.Default.School
        else -> Icons.Default.Notifications
    }

    val iconColor = when (notification.category) {
        "Review" -> if (isAlert) StatusRejected else MintEmerald
        "Quest" -> ElectricBlue
        "Streak" -> Color(0xFFF59E0B)
        "Badge" -> AccentGold
        "Leaderboard" -> AccentGold
        "Academic" -> CobaltBlue
        else -> CobaltBlue
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = when {
            isSelected && isDark -> CobaltBlue.copy(alpha = 0.18f)
            isSelected -> Color(0xFFEFF6FF)
            !notification.isRead && isDark -> SurfaceElevated.copy(alpha = 0.95f)
            !notification.isRead && !isDark -> Color(0xFFF8FAFC)
            isDark -> SurfaceCharcoal.copy(alpha = 0.85f)
            else -> Color.White
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isSelected -> CobaltBlue
                !notification.isRead -> iconColor.copy(alpha = 0.5f)
                isDark -> SurfaceElevatedHigh.copy(alpha = 0.7f)
                else -> Color(0xFFE2E8F0)
            }
        ),
        shadowElevation = if (!notification.isRead && !isDark) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(checkedColor = CobaltBlue)
                )
            }

            // Icon
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Text info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontFamily = SfBoldFontFamily,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = if (isDark) Color.White else LightTextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead && !selectionMode) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(iconColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (!selectionMode) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = if (isDark) SlateSecondary else Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Text(
                    text = notification.message,
                    fontFamily = SfSemiFontFamily,
                    fontSize = 12.5.sp,
                    color = if (isDark) SlateLight else LightTextSecondary,
                    lineHeight = 17.sp
                )

                Text(
                    text = relativeTime,
                    fontFamily = SfSemiFontFamily,
                    fontSize = 11.sp,
                    color = if (isDark) SlateSecondary else Color(0xFF94A3B8)
                )
            }
        }
    }
}
