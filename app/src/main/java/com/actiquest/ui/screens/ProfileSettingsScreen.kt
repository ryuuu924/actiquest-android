package com.actiquest.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.actiquest.data.Repository
import com.actiquest.domain.AcademicChangeRequest
import com.actiquest.domain.DateTimeUtils
import com.actiquest.domain.BadgeCatalog
import com.actiquest.domain.CollegeCatalog
import com.actiquest.domain.User
import com.actiquest.ui.components.UserAvatarView
import com.actiquest.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileSettingsContent(
    navController: NavController,
    isAdmin: Boolean = false,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = ThemeManager.isDarkMode

    val currentUserId by Repository.currentUserId.collectAsState()
    val userState by remember(currentUserId) { Repository.listenUser(currentUserId) }.collectAsState(initial = null)
    val submissionsState by remember(currentUserId) { Repository.listenUserSubmissions(currentUserId) }.collectAsState(initial = emptyList())
    val activeStreak = remember(submissionsState, currentUserId) {
        DateTimeUtils.calculateStreaks(submissionsState, currentUserId).currentStreak
    }
    val pendingSubmissionsState by remember { Repository.listenPendingSubmissions() }.collectAsState(initial = emptyList())

    val user = userState ?: User(
        uid = currentUserId,
        name = if (isAdmin) "Campus Admin" else "Juan de la Cruz",
        email = if (isAdmin) "admin.wellness@dlsl.edu.ph" else "juan.delacruz@dlsl.edu.ph"
    )

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var questRemindersEnabled by remember { mutableStateOf(true) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = if (isAdmin) "Sign Out of Admin Portal" else "Sign Out of ActiQuest",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else LightTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to end your current session?",
                    fontFamily = SfSemiFontFamily,
                    color = if (isDark) SlateLight else LightTextSecondary
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
                    Text("Sign Out", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
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

    if (showEditProfileDialog) {
        var editFirstName by remember { mutableStateOf(user.firstName.ifBlank { user.name.split(" ").firstOrNull() ?: "" }) }
        var editLastName by remember { mutableStateOf(user.lastName.ifBlank { user.name.split(" ").drop(1).joinToString(" ") }) }
        var editNickname by remember { mutableStateOf(user.nickname) }
        var editCollege by remember { mutableStateOf(user.college) }
        var editProgram by remember { mutableStateOf(user.program) }
        var editYear by remember { mutableStateOf(user.yearLevel) }
        var editSection by remember { mutableStateOf(user.section) }
        var editReason by remember { mutableStateOf("") }

        val academicChanged = editCollege.trim() != user.college.trim() ||
                editProgram.trim() != user.program.trim() ||
                editYear.trim() != user.yearLevel.trim() ||
                editSection.trim() != user.section.trim()

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = if (isAdmin) "Edit Admin Profile" else "Edit Profile & Academics",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editFirstName,
                            onValueChange = { editFirstName = it },
                            label = { Text("First Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editLastName,
                            onValueChange = { editLastName = it },
                            label = { Text("Last Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editNickname,
                        onValueChange = { editNickname = it },
                        label = { Text("Preferred Nickname") },
                        placeholder = { Text("e.g. Ryu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isAdmin && academicChanged) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CobaltBlue.copy(alpha = 0.10f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Academic info affects official class standings. Changes will be submitted to the Admin Verification Desk for review.",
                                    fontFamily = SfSemiFontFamily,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.White else CobaltBlue,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    var collegeDropdownExpanded by remember { mutableStateOf(false) }
                    var programDropdownExpanded by remember { mutableStateOf(false) }

                    val collegesList = CollegeCatalog.COLLEGES.map { it.code }
                    val currentCollegeObj = CollegeCatalog.COLLEGES.find { it.code.equals(editCollege, ignoreCase = true) }
                    val availablePrograms = currentCollegeObj?.programs ?: (CollegeCatalog.COLLEGES.firstOrNull()?.programs ?: emptyList())

                    // College Dropdown Picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editCollege,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("College") },
                            trailingIcon = {
                                IconButton(onClick = { collegeDropdownExpanded = !collegeDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select College")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { collegeDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = collegeDropdownExpanded,
                            onDismissRequest = { collegeDropdownExpanded = false },
                            modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                        ) {
                            collegesList.forEach { colCode ->
                                DropdownMenuItem(
                                    text = {
                                        val fullColName = CollegeCatalog.COLLEGES.find { it.code == colCode }?.name ?: colCode
                                        Text("$colCode - $fullColName", fontSize = 12.sp)
                                    },
                                    onClick = {
                                        editCollege = colCode
                                        val newProgList = CollegeCatalog.COLLEGES.find { it.code == colCode }?.programs
                                        if (!newProgList.isNullOrEmpty()) {
                                            editProgram = newProgList.first()
                                        }
                                        collegeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Program Dropdown Picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editProgram,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Degree Program") },
                            trailingIcon = {
                                IconButton(onClick = { programDropdownExpanded = !programDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Program")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { programDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = programDropdownExpanded,
                            onDismissRequest = { programDropdownExpanded = false },
                            modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                        ) {
                            availablePrograms.forEach { prog ->
                                DropdownMenuItem(
                                    text = { Text(prog, fontSize = 12.sp) },
                                    onClick = {
                                        editProgram = prog
                                        programDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    var yearExpanded by remember { mutableStateOf(false) }
                    var sectionExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = editYear,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Year Level") },
                                trailingIcon = {
                                    IconButton(onClick = { yearExpanded = !yearExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Year")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { yearExpanded = true }
                            )
                            DropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false }
                            ) {
                                CollegeCatalog.YEAR_LEVELS.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y) },
                                        onClick = {
                                            editYear = y
                                            yearExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = editSection,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Section") },
                                trailingIcon = {
                                    IconButton(onClick = { sectionExpanded = !sectionExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Section")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { sectionExpanded = true }
                            )
                            DropdownMenu(
                                expanded = sectionExpanded,
                                onDismissRequest = { sectionExpanded = false }
                            ) {
                                CollegeCatalog.SECTIONS.forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text(sec) },
                                        onClick = {
                                            editSection = sec
                                            sectionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (!isAdmin && academicChanged) {
                        OutlinedTextField(
                            value = editReason,
                            onValueChange = { editReason = it },
                            label = { Text("Reason for academic change") },
                            placeholder = { Text("e.g. Shifting program, section adjustment") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val fullName = "${editFirstName.trim()} ${editLastName.trim()}".trim()
                            if (isAdmin) {
                                Repository.updateUserProfile(
                                    uid = user.uid,
                                    name = fullName,
                                    firstName = editFirstName.trim(),
                                    lastName = editLastName.trim(),
                                    college = editCollege,
                                    program = editProgram,
                                    yearLevel = editYear,
                                    section = editSection,
                                    nickname = editNickname.trim()
                                )
                                Toast.makeText(context, "Admin profile updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Repository.updateUserProfile(
                                    uid = user.uid,
                                    name = fullName,
                                    firstName = editFirstName.trim(),
                                    lastName = editLastName.trim(),
                                    college = user.college,
                                    program = user.program,
                                    yearLevel = user.yearLevel,
                                    section = user.section,
                                    nickname = editNickname.trim()
                                )
                                if (academicChanged) {
                                    val req = AcademicChangeRequest(
                                        userId = user.uid,
                                        studentName = fullName.ifEmpty { user.name },
                                        studentEmail = user.email,
                                        currentCollege = user.college,
                                        currentProgram = user.program,
                                        currentYearLevel = user.yearLevel,
                                        currentSection = user.section,
                                        requestedCollege = editCollege.trim(),
                                        requestedProgram = editProgram.trim(),
                                        requestedYearLevel = editYear.trim(),
                                        requestedSection = editSection.trim(),
                                        reason = editReason.trim().ifEmpty { "Academic enrollment / section adjustment" }
                                    )
                                    Repository.submitAcademicChangeRequest(req)
                                    Toast.makeText(context, "Academic update submitted for Admin Verification Desk review!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Profile name updated successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                ) {
                    Text(
                        if (!isAdmin && academicChanged) "Submit for Admin Review" else "Save Changes",
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", fontFamily = SfSemiFontFamily, color = if (isDark) SlateLight else LightTextSecondary)
                }
            },
            containerColor = if (isDark) SurfaceCharcoal else Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(com.actiquest.R.drawable.actiquest_logo),
                        contentDescription = "ActiQuest Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ActiQuest",
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (isDark) Color.White else LightTextPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CobaltBlue.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = "v1.2.0 • Beta Edition",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CobaltBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = "Gamified campus wellness & physical movement platform built to inspire active habits, campus discovery, and friendly inter-class competitions.",
                        fontFamily = SfSemiFontFamily,
                        fontSize = 13.sp,
                        color = if (isDark) SlateLight else LightTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) SurfaceElevated.copy(alpha = 0.7f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text("📍", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Campus Landmark Quests",
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                    Text(
                                        "Scan QR markers at spots like Side Gate, Diokno Steps & the Track Oval to trigger timed exercises.",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 11.5.sp,
                                        color = if (isDark) SlateLight else LightTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Text("🎥", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Activity & Motion Proof",
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                    Text(
                                        "Record exercise video proof or upload daily pedometer step counts directly for verification.",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 11.5.sp,
                                        color = if (isDark) SlateLight else LightTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Text("🛡️", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Admin Verification Desk",
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                    Text(
                                        "Faculty coordinators review proofs, award custom points, and award milestone badges.",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 11.5.sp,
                                        color = if (isDark) SlateLight else LightTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Text("🏆", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Inter-Class Leaderboards",
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isDark) Color.White else LightTextPrimary
                                    )
                                    Text(
                                        "Points fuel real-time standings between colleges, year levels, and sections.",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 11.5.sp,
                                        color = if (isDark) SlateLight else LightTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Open Source Campaign Template • Made by Yuri Yabut",
                        fontFamily = SfSemiFontFamily,
                        fontSize = 10.5.sp,
                        color = if (isDark) TextMuted else Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Close",
                        fontFamily = SfBoldFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            containerColor = if (isDark) SurfaceCharcoal else Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) SurfaceCharcoal.copy(alpha = 0.88f) else Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Uploadable Avatar
                    UserAvatarView(
                        photoUri = user.profilePicUri.ifEmpty { null },
                        name = user.name,
                        size = 84.dp,
                        showUploadBadge = true,
                        onPhotoSelected = { uri ->
                            coroutineScope.launch {
                                Repository.updateUserProfilePic(user.uid, uri.toString())
                                Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = user.name,
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (isDark) Color.White else LightTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = user.email.ifEmpty { if (isAdmin) "admin.wellness@dlsl.edu.ph" else "student@actiquest.com" },
                            fontFamily = SfSemiFontFamily,
                            fontSize = 12.sp,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Role or Academic Badge Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAdmin) CobaltBlue.copy(alpha = 0.15f) else Color(0xFF7C3AED).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.School,
                                contentDescription = null,
                                tint = if (isAdmin) CobaltBlue else Color(0xFFA78BFA),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isAdmin) "Authorized Campus Admin" else "${user.college} • ${user.program}",
                                fontFamily = SfSemiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isAdmin) (if (isDark) Color(0xFF93C5FD) else CobaltBlue) else (if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED))
                            )
                        }
                    }

                    // Stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (!isAdmin) {
                            StatMiniItem(label = "Total Points", value = "${user.totalPoints} pts", icon = Icons.Default.Stars, isDark = isDark)
                            StatMiniItem(label = "Quests Won", value = "${user.approvedCount}", icon = Icons.Default.CheckCircle, isDark = isDark)
                            StatMiniItem(label = "Streak", value = "$activeStreak days", icon = Icons.Default.LocalFireDepartment, isDark = isDark)
                        } else {
                            StatMiniItem(label = "Pending Queue", value = "${pendingSubmissionsState.size}", icon = Icons.Default.PendingActions, isDark = isDark)
                            StatMiniItem(label = "Portal Role", value = "Admin", icon = Icons.Default.VerifiedUser, isDark = isDark)
                            StatMiniItem(label = "Active Classes", value = "12", icon = Icons.Default.Groups, isDark = isDark)
                        }
                    }
                }
            }
        }

        // Quick Actions (Student Submissions & Badges)
        if (!isAdmin) {
            item {
                Text(
                    text = "My Activity & Quests",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDark) Color.White else LightTextPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) SurfaceCharcoal.copy(alpha = 0.88f) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))
                ) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Default.HistoryEdu,
                            title = "My Submissions History",
                            subtitle = "${submissionsState.size} submitted records",
                            isDark = isDark,
                            onClick = { navController.navigate("my_submissions") }
                        )
                        HorizontalDivider(color = if (isDark) SurfaceElevatedHigh.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                        SettingsNavRow(
                            icon = Icons.Default.EmojiEvents,
                            title = "Badges & Trophies",
                            subtitle = "${user.badges.size} badges unlocked",
                            isDark = isDark,
                            onClick = { navController.navigate("badges") }
                        )
                        HorizontalDivider(color = if (isDark) SurfaceElevatedHigh.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                        SettingsNavRow(
                            icon = Icons.Default.Edit,
                            title = "Edit Academic Information",
                            subtitle = if (user.pendingAcademicUpdate) "⚠️ Change Pending Admin Review" else "Class: ${user.classId}",
                            isDark = isDark,
                            onClick = { showEditProfileDialog = true }
                        )
                    }
                }
            }
        }

        if (user.pendingAcademicUpdate) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.12f) else Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color(0xFFFDE68A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Academic Change Under Review",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color.White else Color(0xFF92400E)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "IN QUEUE",
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Requested: ${user.pendingAcademicDetails.ifEmpty { "Class Adjustment" }}",
                                fontFamily = SfSemiFontFamily,
                                fontSize = 11.5.sp,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F),
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Admin Verification Desk will review and update your official class leaderboard standing.",
                                fontFamily = SfSemiFontFamily,
                                fontSize = 10.5.sp,
                                color = if (isDark) SlateLight else Color(0xFF92400E).copy(alpha = 0.8f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // App Preferences & Theme Switch
        item {
            Text(
                text = "Preferences & Appearance",
                fontFamily = SfBoldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else LightTextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) SurfaceCharcoal.copy(alpha = 0.88f) else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))
            ) {
                Column {
                    // Theme Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CobaltBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA78BFA) else CobaltBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isDark) "Dark Mode" else "Light Mode",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                                Text(
                                    text = if (isDark) "Deep Obsidian visual style" else "Crisp high-contrast light style",
                                    fontFamily = SfSemiFontFamily,
                                    fontSize = 11.sp,
                                    color = if (isDark) SlateLight else LightTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = !isDark, // Checked means Light Mode
                            onCheckedChange = { isLight ->
                                ThemeManager.setDarkModeEnabled(!isLight)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CobaltBlue,
                                uncheckedThumbColor = Color(0xFFA78BFA),
                                uncheckedTrackColor = SurfaceElevatedHigh
                            )
                        )
                    }

                    HorizontalDivider(color = if (isDark) SurfaceElevatedHigh.copy(alpha = 0.5f) else Color(0xFFF1F5F9))

                    // Push Notifications Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MintEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MintEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Quest & Review Alerts",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else LightTextPrimary
                                )
                                Text(
                                    text = "Notify when submissions are approved",
                                    fontFamily = SfSemiFontFamily,
                                    fontSize = 11.sp,
                                    color = if (isDark) SlateLight else LightTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = pushNotificationsEnabled,
                            onCheckedChange = { pushNotificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MintEmerald
                            )
                        )
                    }
                }
            }
        }

        // Account & Security
        item {
            Text(
                text = "Account & System",
                fontFamily = SfBoldFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else LightTextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) SurfaceCharcoal.copy(alpha = 0.88f) else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))
            ) {
                Column {
                    SettingsNavRow(
                        icon = Icons.Default.Info,
                        title = "About ActiQuest",
                        subtitle = "Version, purpose, campaign info & credits",
                        isDark = isDark,
                        onClick = { showAboutDialog = true }
                    )
                    HorizontalDivider(color = if (isDark) SurfaceElevatedHigh.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                    SettingsNavRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "Data collection, encryption & deletion rights",
                        isDark = isDark,
                        onClick = { navController.navigate("privacy_policy") }
                    )
                    HorizontalDivider(color = if (isDark) SurfaceElevatedHigh.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                    SettingsNavRow(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        subtitle = "Honest proof, student conduct & guidelines",
                        isDark = isDark,
                        onClick = { navController.navigate("terms_of_service") }
                    )
                    HorizontalDivider(color = if (isDark) SurfaceElevatedHigh.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                    SettingsNavRow(
                        icon = Icons.Default.Logout,
                        iconTint = StatusRejected,
                        title = "Sign Out",
                        subtitle = "End current session safely",
                        isDark = isDark,
                        onClick = { showLogoutDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMiniItem(
    label: String,
    value: String,
    icon: ImageVector,
    isDark: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CobaltBlue,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            fontFamily = SfBoldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isDark) Color.White else LightTextPrimary
        )
        Text(
            text = label,
            fontFamily = SfSemiFontFamily,
            fontSize = 10.sp,
            color = if (isDark) SlateLight else LightTextSecondary
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    iconTint: Color = CobaltBlue,
    title: String,
    subtitle: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDark) Color.White else LightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontFamily = SfSemiFontFamily,
                    fontSize = 11.sp,
                    color = if (isDark) SlateLight else LightTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDark) SlateSecondary else LightTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
