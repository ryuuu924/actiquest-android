package com.actiquest.ui.screens

import android.widget.Toast
import com.actiquest.BuildConfig
import com.actiquest.domain.User
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiquest.R
import com.actiquest.data.AuthResult
import com.actiquest.data.Repository
import com.actiquest.domain.CollegeCatalog
import com.actiquest.ui.theme.*
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.base.R as GmsR
import kotlinx.coroutines.launch

enum class UserRoleTab {
    STUDENT, ADMIN
}

enum class AuthMode {
    SIGN_IN, SIGN_UP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (isAdmin: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(UserRoleTab.STUDENT) }
    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }

    var rememberMe by remember { mutableStateOf(Repository.isRememberMeEnabled()) }
    var email by remember { mutableStateOf(if (rememberMe) Repository.getRememberedEmail() else "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    // Student Onboarding Academic Profile State
    var selectedCollegeCode by remember { mutableStateOf("CITE") }
    var selectedProgram by remember { mutableStateOf("BS Computer Science") }
    var selectedYearLevel by remember { mutableStateOf("1st Year") }
    var selectedSection by remember { mutableStateOf("Section A") }

    var collegeExpanded by remember { mutableStateOf(false) }
    var programExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var pendingGoogleUser by remember { mutableStateOf<User?>(null) }
    var pendingGoogleIsAdmin by remember { mutableStateOf(false) }
    var showGoogleSetupDialog by remember { mutableStateOf(false) }

    val currentCollege = remember(selectedCollegeCode) {
        CollegeCatalog.COLLEGES.find { it.code == selectedCollegeCode } ?: CollegeCatalog.COLLEGES.first()
    }

    LaunchedEffect(selectedCollegeCode) {
        selectedProgram = currentCollege.programs.firstOrNull() ?: "General"
    }

    val yearLevels = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
    val sections = listOf("Section A", "Section B", "Section C", "Section D", "Section E", "Section F", "Section G", "Section H", "Section I", "Section J")

    val isDark = ThemeManager.isDarkMode

    // Atmospheric deep violet/obsidian theme for dark mode, crisp high-contrast off-white/indigo for light mode
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color(0xFF261245), // Deep radiant violet top
            0.28f to Color(0xFF160E2E), // Mid dark indigo
            0.65f to DarkNavyBackground, // Deep obsidian base
            1.0f to DarkNavyBackground
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0xFFEEF2FF),
            0.35f to LightBackground,
            1.0f to LightBackground
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Logo Banner (Crisp, prominent logo with breathable negative space)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Logo Banner with Soft Radiant Ambient Glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Soft diffused atmospheric radial glow
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = if (isDark) {
                                            listOf(
                                                Color(0xFF8B5CF6).copy(alpha = 0.35f),
                                                CobaltBlue.copy(alpha = 0.18f),
                                                Color.Transparent
                                            )
                                        } else {
                                            listOf(
                                                Color(0xFFD5FFE9).copy(alpha = 0.85f),
                                                Color(0xFFE8FDF3).copy(alpha = 0.45f),
                                                Color.Transparent
                                            )
                                        }
                                    )
                                )
                        )
                        Image(
                            painter = painterResource(id = if (isDark) R.drawable.img_logo_banner else R.drawable.img_logo_banner_in_light_mode),
                            contentDescription = "ActiQuest Logo Banner",
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .heightIn(min = 65.dp, max = 110.dp)
                                .wrapContentHeight(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title Header
                    Text(
                        text = if (selectedTab == UserRoleTab.ADMIN) "Admin Portal Sign In"
                               else if (authMode == AuthMode.SIGN_UP) "Create Your Account"
                               else "Sign In To Your Account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else LightTextPrimary,
                            letterSpacing = (-0.3).sp,
                            fontSize = 21.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // Subtitle
                    Text(
                        text = if (selectedTab == UserRoleTab.ADMIN) "Verify student submissions & monitor class standings."
                               else if (authMode == AuthMode.SIGN_UP) "Join class leaderboards and campus wellness quests."
                               else "Manage quests, track fitness, and explore standings.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            fontSize = 12.5.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Role Switcher Tabs (Student vs Admin)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) SurfaceCharcoal.copy(alpha = 0.85f) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) SlateSecondary.copy(alpha = 0.25f) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        val tabWidth = this.maxWidth / 2
                        val indicatorOffset by animateDpAsState(
                            targetValue = if (selectedTab == UserRoleTab.STUDENT) 0.dp else tabWidth,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "role_tab_offset"
                        )
                        val indicatorColor by animateColorAsState(
                            targetValue = if (selectedTab == UserRoleTab.STUDENT) CobaltBlue else Color(0xFF7C3AED),
                            animationSpec = tween(durationMillis = 250),
                            label = "role_tab_color"
                        )

                        // Animated Sliding Background Pill
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(tabWidth)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(indicatorColor)
                        )

                        // Interactive Tab Buttons
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Student Tab
                            Box(
                                modifier = Modifier
                                    .width(tabWidth)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedTab = UserRoleTab.STUDENT
                                        errorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val iconTint by animateColorAsState(
                                        targetValue = if (selectedTab == UserRoleTab.STUDENT) Color.White else (if (isDark) SlateLight else LightTextSecondary),
                                        label = "student_icon_tint"
                                    )
                                    val textTint by animateColorAsState(
                                        targetValue = if (selectedTab == UserRoleTab.STUDENT) Color.White else (if (isDark) SlateLight else LightTextSecondary),
                                        label = "student_text_tint"
                                    )
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Student",
                                        fontFamily = if (selectedTab == UserRoleTab.STUDENT) SfBoldFontFamily else SfSemiFontFamily,
                                        fontWeight = if (selectedTab == UserRoleTab.STUDENT) FontWeight.Bold else FontWeight.Normal,
                                        color = textTint,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Admin Tab
                            Box(
                                modifier = Modifier
                                    .width(tabWidth)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedTab = UserRoleTab.ADMIN
                                        authMode = AuthMode.SIGN_IN
                                        errorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val iconTint by animateColorAsState(
                                        targetValue = if (selectedTab == UserRoleTab.ADMIN) Color.White else (if (isDark) SlateLight else LightTextSecondary),
                                        label = "admin_icon_tint"
                                    )
                                    val textTint by animateColorAsState(
                                        targetValue = if (selectedTab == UserRoleTab.ADMIN) Color.White else (if (isDark) SlateLight else LightTextSecondary),
                                        label = "admin_text_tint"
                                    )
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Admin",
                                        fontFamily = if (selectedTab == UserRoleTab.ADMIN) SfBoldFontFamily else SfSemiFontFamily,
                                        fontWeight = if (selectedTab == UserRoleTab.ADMIN) FontWeight.Bold else FontWeight.Normal,
                                        color = textTint,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Error Banner
            errorMessage?.let { error ->
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusRejected.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusRejected.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusRejected, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error,
                                color = if (isDark) Color.White else StatusRejected,
                                fontFamily = SfSemiFontFamily,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Input Form Fields Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) SurfaceCharcoal.copy(alpha = 0.92f) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF3730A3).copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                    ),
                    elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // First Name and Last Name (Two Separate Inputs for organized data)
                        if (selectedTab == UserRoleTab.STUDENT && authMode == AuthMode.SIGN_UP) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "First Name",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 12.sp,
                                        color = if (isDark) TextSecondary else LightTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = firstName,
                                        onValueChange = { firstName = it },
                                        placeholder = { Text("Juan", color = if (isDark) TextMuted else Color(0xFF94A3B8), fontFamily = SfSemiFontFamily) },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = if (isDark) SlateLight else LightTextSecondary, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                            unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                            focusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                            unfocusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                                            focusedBorderColor = CobaltBlue,
                                            unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Last Name",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 12.sp,
                                        color = if (isDark) TextSecondary else LightTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = lastName,
                                        onValueChange = { lastName = it },
                                        placeholder = { Text("Dela Cruz", color = if (isDark) TextMuted else Color(0xFF94A3B8), fontFamily = SfSemiFontFamily) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                            unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                            focusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                            unfocusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                                            focusedBorderColor = CobaltBlue,
                                            unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (selectedTab == UserRoleTab.ADMIN) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF7C3AED).copy(alpha = 0.12f) else Color(0xFFEDE9FE),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF7C3AED).copy(alpha = 0.35f) else Color(0xFFDDD6FE)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Admin Verification Desk. Only authorized administrators and fitness coaches can sign in with admin credentials.",
                                        fontFamily = SfSemiFontFamily,
                                        fontSize = 11.5.sp,
                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF5B21B6),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Email Field
                        Text(
                            if (selectedTab == UserRoleTab.ADMIN) "Admin Institutional Email" else "Student Email",
                            fontFamily = SfSemiFontFamily,
                            fontSize = 12.sp,
                            color = if (isDark) TextSecondary else LightTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = {
                                Text(
                                    if (selectedTab == UserRoleTab.ADMIN) "admin@campus.edu" else "student@campus.edu",
                                    color = if (isDark) TextMuted else Color(0xFF94A3B8),
                                    fontFamily = SfSemiFontFamily
                                )
                            },
                            leadingIcon = { 
                                Icon(
                                    if (selectedTab == UserRoleTab.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Email, 
                                    contentDescription = null, 
                                    tint = if (selectedTab == UserRoleTab.ADMIN) (if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)) else (if (isDark) SlateLight else LightTextSecondary)
                                ) 
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                focusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                                focusedBorderColor = if (selectedTab == UserRoleTab.ADMIN) Color(0xFF7C3AED) else CobaltBlue,
                                unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        Text(
                            if (selectedTab == UserRoleTab.ADMIN) "Admin Password" else "Student Password",
                            fontFamily = SfSemiFontFamily,
                            fontSize = 12.sp,
                            color = if (isDark) TextSecondary else LightTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("••••••••", color = if (isDark) TextMuted else Color(0xFF94A3B8), fontFamily = SfSemiFontFamily) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (selectedTab == UserRoleTab.ADMIN) (if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)) else (if (isDark) SlateLight else LightTextSecondary)) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = if (isDark) SlateLight else LightTextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                focusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                                focusedBorderColor = if (selectedTab == UserRoleTab.ADMIN) Color(0xFF7C3AED) else CobaltBlue,
                                unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Student Sign Up Academic Fields
                        if (selectedTab == UserRoleTab.STUDENT && authMode == AuthMode.SIGN_UP) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = if (isDark) SlateSecondary.copy(alpha = 0.25f) else Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "CLASS & DEPARTMENT AFFILIATION",
                                fontFamily = SfBoldFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MintEmerald,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // College Selection
                            ExposedDropdownMenuBox(
                                expanded = collegeExpanded,
                                onExpandedChange = { collegeExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = "${currentCollege.code} - ${currentCollege.name}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("College", fontFamily = SfSemiFontFamily) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = collegeExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                        unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                        focusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                        unfocusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                                        focusedBorderColor = CobaltBlue,
                                        unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = collegeExpanded,
                                    onDismissRequest = { collegeExpanded = false },
                                    modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                                ) {
                                    CollegeCatalog.COLLEGES.forEach { col ->
                                        DropdownMenuItem(
                                            text = { Text("${col.code} - ${col.name}", color = if (isDark) Color.White else LightTextPrimary, fontSize = 13.sp, fontFamily = SfSemiFontFamily) },
                                            onClick = {
                                                selectedCollegeCode = col.code
                                                collegeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Program Selection
                            ExposedDropdownMenuBox(
                                expanded = programExpanded,
                                onExpandedChange = { programExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedProgram,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Degree Program", fontFamily = SfSemiFontFamily) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = programExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                        unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                        focusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                        unfocusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                                        focusedBorderColor = CobaltBlue,
                                        unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = programExpanded,
                                    onDismissRequest = { programExpanded = false },
                                    modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                                ) {
                                    currentCollege.programs.forEach { prog ->
                                        DropdownMenuItem(
                                            text = { Text(prog, color = if (isDark) Color.White else LightTextPrimary, fontSize = 13.sp, fontFamily = SfSemiFontFamily) },
                                            onClick = {
                                                selectedProgram = prog
                                                programExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Year and Section Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = yearExpanded,
                                    onExpandedChange = { yearExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedYearLevel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Year", fontFamily = SfSemiFontFamily) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                            unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                            focusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                            unfocusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                                            focusedBorderColor = CobaltBlue,
                                            unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = yearExpanded,
                                        onDismissRequest = { yearExpanded = false },
                                        modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                                    ) {
                                        yearLevels.forEach { yl ->
                                            DropdownMenuItem(
                                                text = { Text(yl, color = if (isDark) Color.White else LightTextPrimary, fontSize = 13.sp, fontFamily = SfSemiFontFamily) },
                                                onClick = {
                                                    selectedYearLevel = yl
                                                    yearExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = sectionExpanded,
                                    onExpandedChange = { sectionExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedSection,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Section", fontFamily = SfSemiFontFamily) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                            unfocusedTextColor = if (isDark) Color.White else LightTextPrimary,
                                            focusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                                            unfocusedContainerColor = if (isDark) SurfaceElevated.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                                            focusedBorderColor = CobaltBlue,
                                            unfocusedBorderColor = if (isDark) SlateSecondary.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = sectionExpanded,
                                        onDismissRequest = { sectionExpanded = false },
                                        modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                                    ) {
                                        sections.forEach { sec ->
                                            DropdownMenuItem(
                                                text = { Text(sec, color = if (isDark) Color.White else LightTextPrimary, fontSize = 13.sp, fontFamily = SfSemiFontFamily) },
                                                onClick = {
                                                    selectedSection = sec
                                                    sectionExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Remember Me & Forgot Password Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { rememberMe = !rememberMe }
                                    .padding(vertical = 2.dp, horizontal = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (rememberMe) Color(0xFF7C3AED) else Color.Transparent)
                                        .border(
                                            1.2.dp,
                                            if (rememberMe) Color(0xFF7C3AED) else (if (isDark) SlateSecondary else Color(0xFF94A3B8)),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rememberMe) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Remember me",
                                    fontFamily = SfSemiFontFamily,
                                    fontSize = 11.5.sp,
                                    color = if (isDark) TextSecondary else LightTextSecondary
                                )
                            }

                            Text(
                                "Forgot password?",
                                fontFamily = SfSemiFontFamily,
                                fontSize = 11.5.sp,
                                color = if (isDark) Color(0xFFA78BFA) else CobaltBlue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (email.isBlank()) {
                                            Toast.makeText(context, "Enter your email address above to receive a password reset link.", Toast.LENGTH_LONG).show()
                                        } else {
                                            coroutineScope.launch {
                                                val res = Repository.sendPasswordReset(email)
                                                res.onSuccess { msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, err.message ?: "Failed to send reset link.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 2.dp, horizontal = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Get Started / Sign In Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null

                                    if (selectedTab == UserRoleTab.STUDENT && authMode == AuthMode.SIGN_UP) {
                                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
                                            errorMessage = "Please enter your first name, last name, email, and password"
                                            isLoading = false
                                            return@launch
                                        }

                                        val result = Repository.signUpStudent(
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            name = "${firstName.trim()} ${lastName.trim()}".trim(),
                                            email = email.trim(),
                                            pass = password,
                                            college = selectedCollegeCode,
                                            program = selectedProgram,
                                            yearLevel = selectedYearLevel,
                                            section = selectedSection
                                        )

                                        isLoading = false
                                        when (result) {
                                            is AuthResult.RequiresVerification -> {
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                                authMode = AuthMode.SIGN_IN
                                                password = ""
                                            }
                                            is AuthResult.Success -> {
                                                Repository.saveRememberMe(email, rememberMe)
                                                Toast.makeText(context, "Welcome to ActiQuest!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess(false)
                                            }
                                            is AuthResult.Error -> {
                                                errorMessage = result.message
                                            }
                                        }
                                    } else {
                                        if (email.isBlank() || password.isBlank()) {
                                            errorMessage = "Please enter both email and password"
                                            isLoading = false
                                            return@launch
                                        }

                                        val isSelectedRoleAdmin = (selectedTab == UserRoleTab.ADMIN)
                                        val result = Repository.signInWithEmail(
                                            email = email.trim(),
                                            pass = password,
                                            selectedRoleIsAdmin = isSelectedRoleAdmin
                                        )
                                        isLoading = false

                                        when (result) {
                                            is AuthResult.Success -> {
                                                Repository.saveRememberMe(email, rememberMe)
                                                onLoginSuccess(result.isAdmin)
                                            }
                                            is AuthResult.RequiresVerification -> {
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                                authMode = AuthMode.SIGN_IN
                                                password = ""
                                            }
                                            is AuthResult.Error -> {
                                                errorMessage = result.message
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == UserRoleTab.ADMIN) Color(0xFF7C3AED) else CobaltBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    if (selectedTab == UserRoleTab.ADMIN) "Sign In as Admin"
                                    else if (authMode == AuthMode.SIGN_UP) "Create Student Account"
                                    else "Sign In as Student",
                                    fontFamily = SfBoldFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }

                        if (selectedTab == UserRoleTab.STUDENT) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Google Sign-In Button (Modern Android Credential Manager API)
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            val credentialManager = CredentialManager.create(context)
                                            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                                            val webClientId = if (resId != 0) context.getString(resId) else "1019623769913-placeholder.apps.googleusercontent.com"

                                            val googleIdOption = GetGoogleIdOption.Builder()
                                                .setFilterByAuthorizedAccounts(false)
                                                .setServerClientId(webClientId)
                                                .setAutoSelectEnabled(false)
                                                .build()

                                            val request = GetCredentialRequest.Builder()
                                                .addCredentialOption(googleIdOption)
                                                .build()

                                            val result = credentialManager.getCredential(request = request, context = context)
                                            val cred = result.credential
                                            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(cred.data)
                                                val idToken = googleIdTokenCredential.idToken
                                                if (!Repository.checkBetaAccess(googleIdTokenCredential.id)) {
                                                    isLoading = false
                                                    errorMessage = "This account is not approved for the ActiQuest beta. Contact the administrator to request access."
                                                    return@launch
                                                }

                                                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                                                val authResult = Repository.signInWithGoogleCredential(firebaseCredential)
                                                isLoading = false
                                                when (authResult) {
                                                    is AuthResult.Success -> {
                                                        Repository.saveRememberMe(authResult.user.email, rememberMe)
                                                        val u = authResult.user
                                                        if (!authResult.isAdmin && authResult.isNewUser) {
                                                            pendingGoogleUser = u
                                                            pendingGoogleIsAdmin = false
                                                            showGoogleSetupDialog = true
                                                        } else {
                                                            onLoginSuccess(authResult.isAdmin)
                                                        }
                                                    }
                                                    is AuthResult.Error -> {
                                                        errorMessage = authResult.message
                                                    }
                                                    else -> {}
                                                }
                                            } else {
                                                isLoading = false
                                                errorMessage = "Unrecognized Google credential received."
                                            }
                                        } catch (e: Exception) {
                                            isLoading = false
                                            if (e is GetCredentialCancellationException) {
                                                // User canceled sign-in dialog silently
                                            } else {
                                                errorMessage = e.localizedMessage ?: "Google Sign-In failed. Please try again."
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) SlateSecondary.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isDark) SurfaceCharcoal.copy(alpha = 0.8f) else Color.White,
                                    contentColor = if (isDark) Color.White else LightTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_google),
                                        contentDescription = "Google Logo",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Sign in with Google",
                                        fontFamily = SfBoldFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Fast Demo Access for Student Only (Admin must use the two credential boxes)
            if (BuildConfig.DEBUG && selectedTab == UserRoleTab.STUDENT) {
                // "Or" Divider
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = if (isDark) SlateSecondary.copy(alpha = 0.25f) else Color(0xFFE2E8F0)
                        )
                        Text(
                            "Or",
                            fontFamily = SfSemiFontFamily,
                            fontSize = 12.sp,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = if (isDark) SlateSecondary.copy(alpha = 0.25f) else Color(0xFFE2E8F0)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Student Demo Button
                item {
                    Surface(
                        onClick = {
                            Repository.switchUserSession("student_default", asAdmin = false)
                            onLoginSuccess(false)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) SurfaceCharcoal.copy(alpha = 0.8f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) SlateSecondary.copy(alpha = 0.3f) else Color(0xFFE2E8F0)
                        ),
                        shadowElevation = if (isDark) 0.dp else 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = MintEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Continue with Student Demo",
                                fontFamily = SfSemiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Navigation Footer (Sign Up / Sign In Switch)
            if (selectedTab == UserRoleTab.STUDENT) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (authMode == AuthMode.SIGN_IN) "Don't have an account? " else "Already have an account? ",
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) SlateLight else LightTextSecondary,
                            fontSize = 12.5.sp
                        )
                        Text(
                            if (authMode == AuthMode.SIGN_IN) "Sign up" else "Sign in",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA78BFA) else CobaltBlue,
                            fontSize = 12.5.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    authMode = if (authMode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
                                    errorMessage = null
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    if (showGoogleSetupDialog && pendingGoogleUser != null) {
        val coroutineScope = rememberCoroutineScope()
        GoogleAcademicSetupDialog(
            user = pendingGoogleUser!!,
            onSaveAndContinue = { fName, lName, nick, col, prog, yr, sec ->
                coroutineScope.launch {
                    val fullName = "$fName $lName".trim()
                    Repository.updateUserProfile(
                        uid = pendingGoogleUser!!.uid,
                        name = fullName,
                        college = col,
                        program = prog,
                        yearLevel = yr,
                        section = sec,
                        firstName = fName,
                        lastName = lName,
                        nickname = nick
                    )
                    showGoogleSetupDialog = false
                    onLoginSuccess(pendingGoogleIsAdmin)
                }
            },
            onDismiss = {
                showGoogleSetupDialog = false
                onLoginSuccess(pendingGoogleIsAdmin)
            }
        )
    }
}

@Composable
fun GoogleAcademicSetupDialog(
    user: User,
    onSaveAndContinue: (firstName: String, lastName: String, nickname: String, college: String, program: String, year: String, section: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    var firstName by remember { mutableStateOf(user.firstName.ifBlank { user.name.split(" ").firstOrNull() ?: "" }) }
    var lastName by remember { mutableStateOf(user.lastName.ifBlank { user.name.split(" ").drop(1).joinToString(" ") }) }
    var nickname by remember { mutableStateOf(user.nickname) }

    var college by remember { mutableStateOf(user.college.ifBlank { "CITE" }) }
    var program by remember { mutableStateOf(user.program.ifBlank { "BS Computer Science" }) }
    var yearLevel by remember { mutableStateOf(user.yearLevel.ifBlank { "1st Year" }) }
    var section by remember { mutableStateOf(user.section.ifBlank { "Section A" }) }

    var collegeDropdownExpanded by remember { mutableStateOf(false) }
    var programDropdownExpanded by remember { mutableStateOf(false) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    val collegesList = CollegeCatalog.COLLEGES.map { it.code }
    val currentCollegeObj = CollegeCatalog.COLLEGES.find { it.code.equals(college, ignoreCase = true) }
    val availablePrograms = currentCollegeObj?.programs ?: (CollegeCatalog.COLLEGES.firstOrNull()?.programs ?: emptyList())

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceCharcoal else Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Welcome to ActiQuest! 🎉",
                    fontFamily = SfBoldFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (isDark) Color.White else LightTextPrimary
                )
                Text(
                    "Please complete your official academic profile so your points are credited to your actual class leaderboard.",
                    fontFamily = SfSemiFontFamily,
                    fontSize = 12.sp,
                    color = if (isDark) SlateLight else LightTextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Preferred Nickname") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // College Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = college,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("College") },
                        trailingIcon = {
                            IconButton(onClick = { collegeDropdownExpanded = !collegeDropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { collegeDropdownExpanded = true }
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
                                    college = colCode
                                    val newProgs = CollegeCatalog.COLLEGES.find { it.code == colCode }?.programs
                                    if (!newProgs.isNullOrEmpty()) {
                                        program = newProgs.first()
                                    }
                                    collegeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Program Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = program,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Degree Program") },
                        trailingIcon = {
                            IconButton(onClick = { programDropdownExpanded = !programDropdownExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { programDropdownExpanded = true }
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
                                    program = prog
                                    programDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = yearLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Year Level") },
                            trailingIcon = {
                                IconButton(onClick = { yearDropdownExpanded = !yearDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { yearDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = yearDropdownExpanded,
                            onDismissRequest = { yearDropdownExpanded = false },
                            modifier = Modifier.background(if (isDark) SurfaceCharcoal else Color.White)
                        ) {
                            listOf("1st Year", "2nd Year", "3rd Year", "4th Year").forEach { yr ->
                                DropdownMenuItem(
                                    text = { Text(yr, fontSize = 12.sp) },
                                    onClick = {
                                        yearLevel = yr
                                        yearDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        placeholder = { Text("e.g. Section A") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onSaveAndContinue(firstName, lastName, nickname, college, program, yearLevel, section)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Save & Join Leaderboard", fontFamily = SfBoldFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
