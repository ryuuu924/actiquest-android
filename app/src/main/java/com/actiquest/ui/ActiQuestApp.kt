package com.actiquest.ui

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.actiquest.data.Repository
import com.actiquest.ui.screens.*

@Composable
fun ActiQuestApp() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val firebaseUser = remember {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            } else null
        } catch (_: Exception) {
            null
        }
    }
    var isAuthenticated by remember { mutableStateOf(firebaseUser != null) }
    var startRoute by remember { mutableStateOf("home") }

    LaunchedEffect(firebaseUser) {
        if (firebaseUser != null) {
            val isAdmin = Repository.checkIsAdmin(firebaseUser.uid, firebaseUser.email ?: "")
            startRoute = if (isAdmin) "admin_review" else "home"
            Repository.switchUserSession(firebaseUser.uid, asAdmin = isAdmin)
        }
    }

    val handleLogout = {
        Repository.signOut()
        isAuthenticated = false
        startRoute = "home"
    }

    if (!isAuthenticated) {
        LoginScreen(
            onLoginSuccess = { isAdmin ->
                startRoute = if (isAdmin) "admin_review" else "home"
                isAuthenticated = true
            }
        )
    } else {
        ActiQuestNavHost(
            navController = navController,
            startDestination = startRoute,
            onLogout = handleLogout
        )
    }
}

@Composable
fun ActiQuestNavHost(
    navController: NavHostController,
    startDestination: String = "home",
    onLogout: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") { 
            HomeScreen(navController, onLogout = onLogout) 
        }
        composable(
            route = "my_submissions?filter={filter}&submissionId={submissionId}",
            arguments = listOf(
                navArgument("filter") {
                    type = NavType.StringType
                    defaultValue = "All"
                },
                navArgument("submissionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            StudentSubmissionsScreen(
                navController = navController,
                onLogout = onLogout,
                initialFilter = backStackEntry.arguments?.getString("filter") ?: "All",
                targetSubmissionId = backStackEntry.arguments?.getString("submissionId")
            )
        }
        composable("admin_review") { 
            AdminReviewScreen(navController, onLogout = onLogout) 
        }
        composable("badges") { 
            BadgesScreen(navController) 
        }
        composable("standings") { 
            WeeklyClassStandingsScreen(navController) 
        }
        composable("activity_journey") { 
            ActivityJourneyScreen(navController) 
        }
        composable("privacy_policy") {
            PrivacyAndTermsScreen(navController, initialTab = 0)
        }
        composable("terms_of_service") {
            PrivacyAndTermsScreen(navController, initialTab = 1)
        }
        composable("notifications") { 
            NotificationHistoryScreen(navController) 
        }
        composable("submit_proof") {
            SubmitFitnessProofScreen(navController)
        }
        composable("scan") { 
            ScanScreen(navController) 
        }
        composable("quest_detail/{questCode}") { backStackEntry ->
            val questCode = backStackEntry.arguments?.getString("questCode") ?: ""
            QuestDetailScreen(navController, questCode)
        }
        composable("quest_briefing/{questCode}") { backStackEntry ->
            val questCode = backStackEntry.arguments?.getString("questCode") ?: ""
            QuestBriefingScreen(navController, questCode)
        }
        composable("record/{questCode}/{maxSeconds}") { backStackEntry ->
            val questCode = backStackEntry.arguments?.getString("questCode") ?: ""
            val maxSeconds = backStackEntry.arguments?.getString("maxSeconds")?.toIntOrNull() ?: 30
            RecordScreen(navController, questCode, maxSeconds)
        }
    }
}
