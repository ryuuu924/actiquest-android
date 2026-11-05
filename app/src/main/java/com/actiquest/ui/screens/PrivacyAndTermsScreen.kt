package com.actiquest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.actiquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAndTermsScreen(
    navController: NavController,
    initialTab: Int = 0 // 0 = Privacy Policy, 1 = Terms of Service
) {
    val isDark = ThemeManager.isDarkMode
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val scrollState = rememberScrollState()

    BackHandler {
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
                        if (selectedTab == 0) "PRIVACY POLICY" else "TERMS OF SERVICE",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = SfBoldFontFamily,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Tab Switcher (Privacy Policy vs Terms of Service)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedTab == 0) CobaltBlue else (if (isDark) SurfaceCharcoal else Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedTab == 0) CobaltBlue else (if (isDark) SlateSecondary.copy(alpha = 0.3f) else Color(0xFFE2E8F0))
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    onClick = { selectedTab = 0 }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.White else (if (isDark) SlateLight else LightTextSecondary),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Privacy Policy",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = if (selectedTab == 0) Color.White else (if (isDark) SlateLight else LightTextPrimary)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedTab == 1) CobaltBlue else (if (isDark) SurfaceCharcoal else Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedTab == 1) CobaltBlue else (if (isDark) SlateSecondary.copy(alpha = 0.3f) else Color(0xFFE2E8F0))
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    onClick = { selectedTab = 1 }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else (if (isDark) SlateLight else LightTextSecondary),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Terms of Service",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = if (selectedTab == 1) Color.White else (if (isDark) SlateLight else LightTextPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Document Content Box
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) SurfaceCharcoal else Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == 0) {
                        // PRIVACY POLICY CONTENT
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ActiQuest Official Privacy Policy",
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        }

                        Text(
                            "Last updated: November 2026 • Version 1.2.0",
                            fontSize = 11.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) SlateLight else LightTextSecondary
                        )

                        HorizontalDivider(color = if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))

                        Text(
                            "1. Information We Collect",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "ActiQuest collects minimal personal data required for campus wellness verification: full name, student email, college, degree program, section, and user-submitted video or screenshot evidence.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "2. How Information is Used",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Your data is used strictly to credit activity points to your individual profile and official class standings, generate streak statistics, and allow faculty reviewers to verify physical quest completion.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "3. Data Security & Storage",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "All submissions are stored securely in Google Cloud Firestore and encrypted Firebase Storage. Access is protected by strict role-based Firestore Security Rules.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "4. Contact & Account Removal",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "To request data deletion or account removal, contact the ActiQuest administrator at yuriyabut924@gmail.com",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )
                    } else {
                        // TERMS OF SERVICE CONTENT
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ActiQuest Terms of Service",
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        }

                        Text(
                            "Last updated: November 2026 • Version 1.2.0",
                            fontSize = 11.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) SlateLight else LightTextSecondary
                        )

                        HorizontalDivider(color = if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))

                        Text(
                            "1. Student Conduct & Honest Proof",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Users agree to submit authentic physical fitness proof. Submitting fraudulent video reps, manipulated pedometer screenshots, or unauthorized QR code scans will result in submission rejection and point deduction.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "2. Institutional Account Requirements",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "ActiQuest is restricted to authorized student developers, faculty reviewers, and approved beta testers. Unverified accounts or non-whitelisted emails are subject to account suspension.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "3. Verification Desk Authority",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "University faculty reviewers and admins reserve the authority to award partial points, request submission retakes, or reject non-compliant proof.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
