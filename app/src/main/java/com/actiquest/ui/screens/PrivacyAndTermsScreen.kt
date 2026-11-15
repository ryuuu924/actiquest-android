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
                        if (selectedTab == 0) "PRIVACY POLICY" else "TERMS OF USE",
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

            // Tab Switcher (Privacy Policy vs Terms of Use)
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
                            "Terms of Use",
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
                                "ActiQuest Privacy Policy",
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        }

                        Text(
                            "ActiQuest beta web portal and Android application · Version 1.2.0-beta",
                            fontSize = 11.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) SlateLight else LightTextSecondary
                        )

                        HorizontalDivider(color = if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))

                        Text(
                            "1. Information Handled During the Beta",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Depending on the features you use, ActiQuest may process your email address and Firebase account identifier, display name, college or department affiliation, quest activity and points, accessibility preference, consent choices, and beta security/audit logs. Audit logs include timestamps, account identifiers when available, event type, endpoint, and whether a request was anomalous. The app does not ask for or collect your date of birth.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "2. Motion Proofs & Local Processing",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Motion proofs are processed locally where supported and minimized to what is needed to verify a quest. If you choose to submit evidence in another ActiQuest feature, only provide the minimum material needed; uploaded evidence is handled securely by project storage services.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "3. Purposes & Lawful Handling (RA 10173 & NPC Circular 2023-04)",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Information supports authentication, approved beta access, policy-consent records, account preferences, quest administration, college/department standings, user-requested exports/deletion, and service security. Information is handled only for these stated purposes in proportion to the feature being used (Proportionality Principle). Public leaderboard standings display Academic College names and aggregate totals only.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "4. Storage & Service Providers",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "The app uses Firebase Authentication and Cloud Firestore through the project's configured Firebase infrastructure. Vercel hosts companion portal API routes. These providers process data under their own privacy commitments.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "5. Retention & Data Subject Rights",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Beta test data will be wiped or anonymized when the beta is completed. Under the Data Privacy Act of 2012 (RA 10173), you may request access to, correction of, or objection to processing of your personal data; request erasure or blocking where applicable; and request data portability via Account Settings or by emailing yuriyabut924@gmail.com.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "6. Age & Guardian Consent",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Beta self-activation is limited to people aged 18 or older. Users under 18 cannot activate an account through self-service; a parent or legal guardian must contact the project team through the user's school, and access remains inactive until independent guardian verification is complete.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )
                    } else {
                        // TERMS OF USE CONTENT
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ActiQuest Terms of Use",
                                fontFamily = SfBoldFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else LightTextPrimary
                            )
                        }

                        Text(
                            "ActiQuest beta web portal and Android application · Version 1.2.0-beta",
                            fontSize = 11.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) SlateLight else LightTextSecondary
                        )

                        HorizontalDivider(color = if (isDark) SurfaceElevatedHigh else Color(0xFFE2E8F0))

                        Text(
                            "1. Beta Access & Electronic Acceptance",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "ActiQuest is a limited beta for approved testers. Access is subject to account verification, age eligibility or independently verified parent/guardian consent, and electronic acceptance of current Terms of Use and Privacy Policy. Do not share beta links or credentials with unauthorized people.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "2. Physical Safety & Health Disclaimer",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Participation in wellness and physical activities is voluntary. Follow all campus rules, posted safety instructions, and directions from authorized campus staff. Participate only when medically fit and within your physical limits. ActiQuest is not a medical service and does not provide medical advice or emergency assistance. Accessible or stationary quest options are offered without disadvantaging participants in points or standings.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "3. Intellectual Property (RA 8293)",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "ActiQuest's original software, branding, quest materials, and creator-owned works remain the property of their respective creator or rights holder under the Philippine Intellectual Property Code (Republic Act No. 8293). Do not remove attribution or use protected materials beyond applicable license terms.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "4. Fair Play & Prohibited Conduct (RA 10175)",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Under the Cybercrime Prevention Act of 2012 (RA 10175), users shall not spoof GPS or sensor readings, tamper with QR codes/markers, falsify motion evidence, use modified APKs, impersonate participants, manipulate standings, bypass access controls, or interfere with backend operation.",
                            fontSize = 12.sp,
                            fontFamily = SfSemiFontFamily,
                            color = if (isDark) Color.White else LightTextPrimary,
                            lineHeight = 18.sp
                        )

                        Text(
                            "5. Accessibility & Inclusive Participation",
                            fontFamily = SfBoldFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CobaltBlue
                        )
                        Text(
                            "Participants may enable Adaptive / Accessible Mode in Account Settings. The preference identifies stationary or accessible quest options. Approved accessible quests contribute points on the same basis as comparable quests without penalty.",
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
