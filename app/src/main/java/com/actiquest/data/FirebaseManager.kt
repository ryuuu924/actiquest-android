package com.actiquest.data

import android.net.Uri
import com.actiquest.BuildConfig
import com.actiquest.domain.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

sealed class AuthResult {
    data class Success(val user: User, val isAdmin: Boolean, val isNewUser: Boolean = false) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class RequiresVerification(val message: String) : AuthResult()
}

object Repository {
    private var appContext: android.content.Context? = null
    private val betaAccessClient = OkHttpClient.Builder()
        .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private const val PREFS_NAME = "actiquest_auth_prefs"
    private const val KEY_REMEMBER_ME = "remember_me_enabled"
    private const val KEY_REMEMBERED_EMAIL = "remembered_email"

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    fun saveRememberMe(email: String, enabled: Boolean) {
        try {
            val prefs = appContext?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs?.edit()?.apply {
                putBoolean(KEY_REMEMBER_ME, enabled)
                if (enabled) {
                    putString(KEY_REMEMBERED_EMAIL, email.trim())
                } else {
                    remove(KEY_REMEMBERED_EMAIL)
                }
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRememberedEmail(): String {
        return try {
            val prefs = appContext?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            if (prefs?.getBoolean(KEY_REMEMBER_ME, false) == true) {
                prefs.getString(KEY_REMEMBERED_EMAIL, "") ?: ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun isRememberMeEnabled(): Boolean {
        return try {
            val prefs = appContext?.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs?.getBoolean(KEY_REMEMBER_ME, false) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun validatePasswordStrict(password: String): String? {
        if (password.length < 8) {
            return "Password must be at least 8 characters long."
        }
        if (!password.any { it.isUpperCase() }) {
            return "Password must contain at least one uppercase letter (A-Z)."
        }
        if (!password.any { it.isLowerCase() }) {
            return "Password must contain at least one lowercase letter (a-z)."
        }
        if (!password.any { it.isDigit() }) {
            return "Password must contain at least one number (0-9)."
        }
        val specialChars = "!@#$%^&*()_+-=[]{}|;:'\",.<>/?~`"
        if (!password.any { it in specialChars || !it.isLetterOrDigit() }) {
            return "Password must contain at least one special character (e.g. !@#$%^&*)."
        }
        return null
    }

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val storage by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val auth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    // In-memory fallback state for immediate preview/offline functionality
    private val _localUsers = MutableStateFlow<Map<String, User>>(
        mapOf(
            "student_default" to User(
                uid = "student_default",
                name = "Juan de la Cruz",
                firstName = "Juan",
                lastName = "de la Cruz",
                email = "juan.delacruz@dlsl.edu.ph",
                college = "CITE",
                program = "BS Computer Science",
                yearLevel = "1st Year",
                section = "Section A",
                classId = "CITE-BSCS-1A",
                totalPoints = 230,
                streak = 4,
                approvedCount = 3,
                badges = listOf("First Steps"),
                consentGiven = true
            ),
            "admin_default" to User(
                uid = "admin_default",
                name = "Wellness Admin",
                firstName = "Wellness",
                lastName = "Admin",
                email = "admin.wellness@dlsl.edu.ph",
                college = "Staff",
                program = "Wellness & Sports Division",
                yearLevel = "Staff",
                section = "Admin",
                classId = "ADMIN-STAFF",
                totalPoints = 0,
                streak = 1,
                approvedCount = 0,
                badges = emptyList(),
                consentGiven = true
            )
        )
    )

    private val _localAdmins = MutableStateFlow<Set<String>>(
        setOf("admin_default", "admin_demo", "yuriyabut924@gmail.com", "admin.wellness@dlsl.edu.ph", "coach.sarah@dlsl.edu.ph")
    )

    private val _localPasswords = MutableStateFlow<Map<String, String>>(
        mapOf(
            "juan.delacruz@dlsl.edu.ph" to "pass123",
            "admin.wellness@dlsl.edu.ph" to "admin123"
        )
    )

    private val _localClasses = MutableStateFlow<List<ClassModel>>(
        listOf(
            ClassModel("CITE-BSCS-1A", "CITE - BSCS 1A", "CITE", "BS Computer Science", "1st Year", "Section A", 1950, 34),
            ClassModel("CBEAM-BSA-2A", "CBEAM - BSA 2A", "CBEAM", "BS Accountancy", "2nd Year", "Section A", 1620, 29),
            ClassModel("CEAS-ABCOMM-1A", "CEAS - ABCOMM 1A", "CEAS", "AB Communication", "1st Year", "Section A", 1540, 26),
            ClassModel("CEAS-BMA-4A", "CEAS - BMMA 4A", "CEAS", "Bachelor of Multimedia Arts", "4th Year", "Section A", 1490, 28),
            ClassModel("CON-BSN-1B", "CON - BSN 1B", "CON", "BS Nursing", "1st Year", "Section B", 1320, 30),
            ClassModel("CIHTM-BSHM-2C", "CIHTM - BSHM 2C", "CIHTM", "BS Hospitality Management", "2nd Year", "Section C", 1180, 27),
            ClassModel("CCJE-BSFS-3A", "CCJE - BSFS 3A", "CCJE", "BS Forensic Science", "3rd Year", "Section A", 1040, 25),
            ClassModel("CITE-BSIT-3A", "CITE - BSIT 3A", "CITE", "BS Information Technology", "3rd Year", "Section A", 890, 26)
        )
    )

    private val _localNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val _readNotificationIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val _clearedNotificationIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private val _localSubmissions = MutableStateFlow<List<Submission>>(
        listOf(
            Submission(
                submissionId = "sub_demo_01",
                userId = "student_default",
                studentName = "Juan de la Cruz",
                questCode = "QQ-DIOKNO",
                questTitle = "Diokno Steps Challenge",
                videoPath = "sample_video_local",
                pointValue = 50,
                status = "pending",
                submittedAt = System.currentTimeMillis() - 1000L * 60L * 15L,
                submissionType = "video"
            ),
            Submission(
                submissionId = "sub_demo_02",
                userId = "student_default",
                studentName = "Juan de la Cruz",
                questCode = "",
                questTitle = "Daily 10,000 Steps Goal",
                pointValue = 100,
                status = "pending",
                submittedAt = System.currentTimeMillis() - 1000L * 60L * 35L,
                submissionType = "screenshot",
                imagePath = "sample_screenshot_steps",
                activityType = "Steps Count",
                activityMetricValue = "10,480 Steps",
                studentNotes = "Logged morning campus walk and library laps via Google Fit pedometer."
            ),
            Submission(
                submissionId = "sub_demo_03",
                userId = "student_marcus",
                studentName = "Marcus Chen",
                questCode = "QQ-CBEAM",
                questTitle = "CBEAM Stretch",
                videoPath = "sample_video_local_2",
                pointValue = 50,
                status = "pending",
                submittedAt = System.currentTimeMillis() - 1000L * 60L * 60L,
                submissionType = "video"
            )
        )
    )

    private val _localAcademicRequests = MutableStateFlow<List<AcademicChangeRequest>>(emptyList())

    // Current active session role
    private val _currentUserId = MutableStateFlow("student_default")
    val currentUserId = _currentUserId.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(_localUsers.value["student_default"])
    val currentUser = _currentUser.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode = _isAdminMode.asStateFlow()

    fun switchUserSession(uid: String, asAdmin: Boolean = false) {
        _currentUserId.value = uid
        val user = _localUsers.value[uid] ?: User(uid = uid, name = if (asAdmin) "Admin" else "Student $uid")
        _currentUser.value = user
        _isAdminMode.value = asAdmin || _localAdmins.value.contains(uid)
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _currentUserId.value = ""
        _currentUser.value = null
        _isAdminMode.value = false
    }

    private suspend fun isApprovedBetaUser(email: String, idToken: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().put("email", email.trim().lowercase())
            if (!idToken.isNullOrBlank()) payload.put("idToken", idToken)
            val request = Request.Builder()
                .url(BuildConfig.BETA_ACCESS_CHECK_URL)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            betaAccessClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Beta access service returned HTTP ${response.code}.")
                }
                JSONObject(response.body?.string().orEmpty()).optBoolean("approved", false)
            }
        }

    suspend fun checkBetaAccess(email: String): Boolean = isApprovedBetaUser(email)

    fun formatFirebaseErrorMessage(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "No account found with this email. Please check your email or create a new account."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect credentials. Please verify your email and password."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please sign in instead."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseNetworkException -> "Network error connecting to Firebase. Please check your internet connection."
            else -> {
                val msg = e.localizedMessage ?: "Authentication failed."
                when {
                    msg.contains("password", ignoreCase = true) && msg.contains("invalid", ignoreCase = true) ->
                        "Invalid password or email address."
                    msg.contains("user-not-found", ignoreCase = true) ->
                        "No account found with this email."
                    msg.contains("email-already-in-use", ignoreCase = true) ->
                        "This email is already registered. Please sign in."
                    msg.contains("network", ignoreCase = true) ->
                        "Network error. Please verify your connection."
                    else -> msg
                }
            }
        }
    }

    suspend fun sendPasswordReset(email: String): Result<String> {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("Please enter an email address."))
        val fa = auth
        if (fa != null) {
            return try {
                fa.sendPasswordResetEmail(trimmed).await()
                Result.success("Password reset email sent to $trimmed. Check your inbox.")
            } catch (e: Exception) {
                Result.failure(Exception(formatFirebaseErrorMessage(e)))
            }
        }
        return Result.success("Password reset instructions simulated for $trimmed.")
    }

    fun parseUserFromSnapshot(snap: DocumentSnapshot, uid: String, fallbackEmail: String = ""): User {
        val email = snap.getString("email") ?: fallbackEmail
        val rawName = snap.getString("name") ?: email.substringBefore("@").replace(".", " ").capitalizeWords()
        val firstName = snap.getString("firstName") ?: rawName.split(" ").firstOrNull() ?: "Student"
        val lastName = snap.getString("lastName") ?: rawName.split(" ").drop(1).joinToString(" ")
        val college = snap.getString("college") ?: "CITE"
        val program = snap.getString("program") ?: "BS Computer Science"
        val yearLevel = snap.getString("yearLevel") ?: "1st Year"
        val section = snap.getString("section") ?: "Section A"
        val computedClassId = buildClassId(college, program, yearLevel, section)
        val rawStoredClassId = snap.getString("classId") ?: ""
        val effectiveClassId = if (rawStoredClassId.isNotBlank() && normalizeClassId(rawStoredClassId) == normalizeClassId(computedClassId)) {
            rawStoredClassId
        } else {
            computedClassId
        }
        val totalPoints = snap.getLong("totalPoints")?.toInt() ?: 0
        val targetPoints = snap.getLong("targetPoints")?.toInt() ?: 500
        val streak = snap.getLong("streak")?.toInt() ?: 1
        val badges = (snap.get("badges") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val approvedCount = snap.getLong("approvedCount")?.toInt() ?: 0
        val profilePicUri = snap.getString("profilePicUri") ?: ""
        val nickname = snap.getString("nickname") ?: ""

        return User(
            uid = uid,
            name = rawName,
            firstName = firstName,
            lastName = lastName,
            email = email,
            college = college,
            program = program,
            yearLevel = yearLevel,
            section = section,
            classId = effectiveClassId,
            totalPoints = totalPoints,
            targetPoints = targetPoints,
            streak = streak,
            badges = badges,
            approvedCount = approvedCount,
            profilePicUri = profilePicUri,
            nickname = nickname
        )
    }

    suspend fun checkIsAdmin(uid: String, email: String = ""): Boolean {
        val lowerEmail = email.trim().lowercase()
        if (_localAdmins.value.contains(uid)) return true
        if (lowerEmail.isNotEmpty() && _localAdmins.value.contains(lowerEmail)) return true

        val db = firestore ?: return _localAdmins.value.contains(uid) || (lowerEmail.isNotEmpty() && _localAdmins.value.contains(lowerEmail))
        return try {
            // 1. Check admins/{uid} document
            val docByUid = db.collection("admins").document(uid).get().await()
            if (docByUid.exists()) return true

            // 2. Check admins/{email} document
            if (lowerEmail.isNotEmpty()) {
                val docByEmail = db.collection("admins").document(lowerEmail).get().await()
                if (docByEmail.exists()) return true
            }

            // 3. Check users/{uid} for role == "admin" or isAdmin == true
            val userDoc = db.collection("users").document(uid).get().await()
            if (userDoc.exists()) {
                val role = userDoc.getString("role")
                val isAdminFlag = userDoc.getBoolean("isAdmin") ?: false
                if (role.equals("admin", ignoreCase = true) || isAdminFlag) return true
            }

            false
        } catch (e: Exception) {
            _localAdmins.value.contains(uid) || (lowerEmail.isNotEmpty() && _localAdmins.value.contains(lowerEmail))
        }
    }

    suspend fun signInWithEmail(email: String, pass: String, selectedRoleIsAdmin: Boolean): AuthResult {
        val trimmedEmail = email.trim()
        val lowerEmail = trimmedEmail.lowercase()
        
        // 1. Try real Firebase Auth if initialized
        val fa = auth
        if (fa != null && trimmedEmail.isNotEmpty() && pass.isNotEmpty()) {
            val res = try {
                fa.signInWithEmailAndPassword(trimmedEmail, pass).await()
            } catch (e: Exception) {
                if (e is FirebaseNetworkException) {
                    return AuthResult.Error("Network error. Please check your internet connection.")
                }
                // Catch all auth exceptions cleanly and show generic error on login to prevent email enumeration
                return AuthResult.Error("Invalid credentials. Please verify your email and password.")
            }

            val firebaseUser = res.user
            val uid = firebaseUser?.uid ?: ""

            try {
                val idToken = firebaseUser?.getIdToken(false)?.await()?.token
                if (!isApprovedBetaUser(trimmedEmail, idToken)) {
                    fa.signOut()
                    return AuthResult.Error("This account is not approved for the ActiQuest beta. Contact the administrator to request access.")
                }
            } catch (e: Exception) {
                fa.signOut()
                android.util.Log.e("FirebaseManager", "Unable to verify beta access for $trimmedEmail", e)
                return AuthResult.Error("Could not verify beta access. Check your connection and try again.")
            }

            // Explicitly trigger user.reload() to update email verification flag
            try {
                firebaseUser?.reload()?.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val isEmailVerified = firebaseUser?.isEmailVerified ?: false
            val isAdmin = checkIsAdmin(uid, trimmedEmail)

            val db = firestore
            var userDoc: User? = null
            var requiresVerificationInDoc = false

            if (db != null && uid.isNotEmpty()) {
                try {
                    val snap = db.collection("users").document(uid).get().await()
                    if (snap.exists()) {
                        userDoc = parseUserFromSnapshot(snap, uid, trimmedEmail)
                        requiresVerificationInDoc = snap.getBoolean("requiresEmailVerification") ?: false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (selectedRoleIsAdmin && !isAdmin) {
                fa.signOut()
                return AuthResult.Error("Access Denied: Your account ($trimmedEmail) is not registered as an authorized Admin. Please switch to the Student tab.")
            }
            if (!selectedRoleIsAdmin && isAdmin) {
                fa.signOut()
                return AuthResult.Error("This is an Admin account. Please switch to the Admin tab above to sign in with your admin credentials.")
            }

            // Legacy profiles without a verification flag default to false; new sign-ups store true.
            val isExplicitlyExempt = userDoc != null && requiresVerificationInDoc == false
            if (!isEmailVerified && !isExplicitlyExempt && !isAdmin) {
                fa.signOut()
                return AuthResult.Error("Your email address ($trimmedEmail) is not verified yet. Please check your inbox and verify your email before signing in.")
            }

            if (userDoc == null) {
                val defaultName = firebaseUser?.displayName?.ifEmpty { null }
                    ?: trimmedEmail.substringBefore("@").replace(".", " ").capitalizeWords()
                val parts = defaultName.split(" ")
                val fName = parts.firstOrNull() ?: defaultName
                val lName = parts.drop(1).joinToString(" ")

                userDoc = User(
                    uid = uid,
                    name = defaultName,
                    firstName = fName,
                    lastName = lName,
                    email = trimmedEmail,
                    college = if (isAdmin) "Staff" else "CITE",
                    program = if (isAdmin) "Wellness & Sports Division" else "BS Computer Science",
                    yearLevel = if (isAdmin) "Staff" else "1st Year",
                    section = if (isAdmin) "Admin" else "Section A",
                    classId = if (isAdmin) "ADMIN-STAFF" else "CITE-BSCS-1A"
                )

                // Write user to Firestore so subsequent loads retrieve the full profile
                try {
                    db?.collection("users")?.document(uid)?.set(userDoc, SetOptions.merge())?.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _currentUserId.value = uid
            _currentUser.value = userDoc
            _isAdminMode.value = isAdmin && selectedRoleIsAdmin
            val current = _localUsers.value.toMutableListMap()
            current[uid] = userDoc
            _localUsers.value = current

            return AuthResult.Success(userDoc, _isAdminMode.value)
        }

        // 2. Demo / offline fallback
        if (!BuildConfig.DEBUG) {
            return AuthResult.Error("Firebase sign-in is unavailable. Please check your connection and try again.")
        }
        if (selectedRoleIsAdmin) {
            val isAuthorized = _localAdmins.value.contains(lowerEmail) || lowerEmail.contains("admin") || lowerEmail.contains("coach")
            if (isAuthorized) {
                val storedPass = _localPasswords.value[lowerEmail] ?: "admin123"
                if (pass.isNotEmpty() && pass != storedPass && pass != "admin123") {
                    return AuthResult.Error("Incorrect admin password. Please verify your credentials.")
                }
                switchUserSession("admin_default", asAdmin = true)
                return AuthResult.Success(_localUsers.value["admin_default"]!!, true)
            } else {
                return AuthResult.Error("Access Denied: Your account ($trimmedEmail) is not authorized as an Admin.")
            }
        } else {
            val isAttemptingAdmin = _localAdmins.value.contains(lowerEmail) || lowerEmail.contains("admin") || lowerEmail.contains("coach")
            if (isAttemptingAdmin) {
                return AuthResult.Error("This is an Admin account. Please switch to the Admin tab above to sign in with your admin credentials.")
            }

            // Check if user already registered locally in this session
            val existing = _localUsers.value.values.find { it.email.equals(trimmedEmail, ignoreCase = true) }
            val user = if (existing != null) {
                val storedPass = _localPasswords.value[lowerEmail]
                if (storedPass != null && storedPass != pass) {
                    return AuthResult.Error("Incorrect password. Please verify your credentials.")
                }
                existing
            } else {
                val uid = "student_" + UUID.randomUUID().toString().take(6)
                val rawName = trimmedEmail.substringBefore("@").replace(".", " ").capitalizeWords().ifEmpty { "Student Warrior" }
                val u = User(
                    uid = uid,
                    name = rawName,
                    firstName = rawName.split(" ").firstOrNull() ?: "Student",
                    lastName = rawName.split(" ").drop(1).joinToString(" "),
                    email = trimmedEmail,
                    college = "CITE",
                    program = "BS Computer Science",
                    yearLevel = "1st Year",
                    section = "Section A",
                    classId = "CITE-BSCS-1A"
                )
                val passMap = _localPasswords.value.toMutableMap()
                passMap[lowerEmail] = pass
                _localPasswords.value = passMap
                u
            }

            val current = _localUsers.value.toMutableListMap()
            current[user.uid] = user
            _localUsers.value = current
            switchUserSession(user.uid, asAdmin = false)
            return AuthResult.Success(user, false)
        }
    }

    suspend fun signUpStudent(
        name: String = "",
        firstName: String = "",
        lastName: String = "",
        email: String,
        pass: String,
        college: String,
        program: String,
        yearLevel: String,
        section: String
    ): AuthResult {
        val trimmedEmail = email.trim()
        val lowerEmail = trimmedEmail.lowercase()
        val classId = buildClassId(college, program, yearLevel, section)

        // 1. Strict client-side password validation
        val passwordError = validatePasswordStrict(pass)
        if (passwordError != null) {
            return AuthResult.Error(passwordError)
        }

        if (auth == null && !BuildConfig.DEBUG) {
            return AuthResult.Error("Firebase sign-up is unavailable. Please try again later.")
        }

        if (auth != null) {
            try {
                if (!isApprovedBetaUser(trimmedEmail)) {
                    return AuthResult.Error("This email is not approved for the ActiQuest beta. Contact the administrator to request access.")
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseManager", "Unable to verify beta signup access for $trimmedEmail", e)
                return AuthResult.Error("Could not verify beta access. Check your connection and try again.")
            }
        }

        val resolvedFirstName = firstName.trim().ifEmpty { name.trim().split(" ").firstOrNull() ?: "" }
        val resolvedLastName = lastName.trim().ifEmpty { name.trim().split(" ").drop(1).joinToString(" ") }
        val resolvedFullName = if (resolvedFirstName.isNotBlank() || resolvedLastName.isNotBlank()) {
            "$resolvedFirstName $resolvedLastName".trim()
        } else {
            name.trim().ifEmpty { "New Student" }
        }

        val fa = auth
        if (fa != null && trimmedEmail.isNotEmpty() && pass.isNotEmpty()) {
            var accountCreated = false
            try {
                // 1. Create account with Firebase Auth
                val res = fa.createUserWithEmailAndPassword(trimmedEmail, pass).await()
                accountCreated = true
                val firebaseUser = res.user
                val uid = firebaseUser?.uid ?: UUID.randomUUID().toString()

                // 2. Immediately send verification email
                try {
                    firebaseUser?.sendEmailVerification()?.await()
                } catch (e: Exception) {
                    fa.signOut()
                    return AuthResult.Error("Failed to send verification email. Please check if the email address is valid: ${e.localizedMessage}")
                }

                // 3. Set Firebase Auth display name
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(resolvedFullName)
                        .build()
                    firebaseUser?.updateProfile(profileUpdates)?.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val newUser = User(
                    uid = uid,
                    name = resolvedFullName,
                    firstName = resolvedFirstName.ifEmpty { "Student" },
                    lastName = resolvedLastName,
                    email = trimmedEmail,
                    college = college,
                    program = program,
                    yearLevel = yearLevel,
                    section = section,
                    classId = classId,
                    totalPoints = 0,
                    targetPoints = 500,
                    streak = 1,
                    badges = emptyList(),
                    approvedCount = 0,
                    consentGiven = true
                )

                // 4. Save student profile to Firestore 'users' collection with requiresEmailVerification flag
                val userMap = hashMapOf<String, Any>(
                    "uid" to uid,
                    "name" to resolvedFullName,
                    "firstName" to resolvedFirstName.ifEmpty { "Student" },
                    "lastName" to resolvedLastName,
                    "email" to trimmedEmail,
                    "college" to college,
                    "program" to program,
                    "yearLevel" to yearLevel,
                    "section" to section,
                    "classId" to classId,
                    "role" to "student",
                    "requiresEmailVerification" to true,
                    "totalPoints" to 0L,
                    "targetPoints" to 500L,
                    "streak" to 1L,
                    "badges" to emptyList<String>(),
                    "approvedCount" to 0L,
                    "consentGiven" to true,
                    "profilePicUri" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(uid)?.set(userMap, SetOptions.merge())?.await()

                // 5. Upsert class in Firestore 'classes' collection with exact 8 fields and live memberCount
                try {
                    val classRef = firestore?.collection("classes")?.document(classId)
                    if (classRef != null) {
                        val usersInClassSnap = firestore?.collection("users")
                            ?.whereEqualTo("classId", classId)
                            ?.get()?.await()
                        val exactMemberCount = maxOf(1, usersInClassSnap?.size() ?: 1)

                        val classSnap = classRef.get().await()
                        val existingTotalPoints = classSnap.getLong("totalPoints")?.toInt() ?: 0
                        val className = "$college - ${getProgramAbbreviation(program)} ${getYearDigit(yearLevel)}${getSectionLetter(section)}"

                        val classData = ClassModel(
                            classId = classId,
                            college = college,
                            memberCount = exactMemberCount,
                            name = className,
                            program = program,
                            section = section,
                            totalPoints = existingTotalPoints,
                            yearLevel = yearLevel
                        )
                        classRef.set(classData, SetOptions.merge()).await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try { reconcileClassAggregates() } catch (_: Exception) {}

                // 6. Sign out user immediately so they must verify email before logging in
                try {
                    fa.signOut()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return AuthResult.RequiresVerification(
                    "Account created successfully! A verification link has been sent to $trimmedEmail. Please check your inbox and verify your email before logging in."
                )
            } catch (e: Exception) {
                if (accountCreated) fa.signOut()
                if (e is FirebaseAuthUserCollisionException) {
                    return AuthResult.Error("An account with this email address already exists. Please sign in.")
                }
                return AuthResult.Error(formatFirebaseErrorMessage(e))
            }
        }

        // Local registration fallback
        if (!BuildConfig.DEBUG) {
            return AuthResult.Error("Firebase sign-up is unavailable. Please try again later.")
        }
        val uid = "student_" + UUID.randomUUID().toString().take(6)
        val newUser = User(
            uid = uid,
            name = resolvedFullName,
            firstName = resolvedFirstName.ifEmpty { "Student" },
            lastName = resolvedLastName,
            email = trimmedEmail,
            college = college,
            program = program,
            yearLevel = yearLevel,
            section = section,
            classId = classId,
            totalPoints = 0,
            targetPoints = 500,
            streak = 1,
            badges = emptyList(),
            approvedCount = 0,
            consentGiven = true
        )
        val currentUsers = _localUsers.value.toMutableListMap()
        currentUsers[uid] = newUser
        _localUsers.value = currentUsers

        val passMap = _localPasswords.value.toMutableMap()
        passMap[lowerEmail] = pass
        _localPasswords.value = passMap

        // Update or add to local classes list
        val currentClasses = _localClasses.value.toMutableList()
        val classIndex = currentClasses.indexOfFirst { it.classId == classId }
        if (classIndex >= 0) {
            val existingClass = currentClasses[classIndex]
            currentClasses[classIndex] = existingClass.copy(memberCount = existingClass.memberCount + 1)
        } else {
            val className = "$college - ${getProgramAbbreviation(program)} ${getYearDigit(yearLevel)}${getSectionLetter(section)}"
            currentClasses.add(ClassModel(classId, className, college, program, yearLevel, section, 0, 1))
        }
        _localClasses.value = currentClasses

        switchUserSession(uid, asAdmin = false)
        return AuthResult.Success(newUser, false)
    }

    fun buildClassId(college: String, program: String, year: String, sec: String): String {
        val cleanCollege = college.trim().replace("[^A-Za-z0-9]".toRegex(), "").uppercase()
        val progCode = getProgramAbbreviation(program).trim().replace(" ", "")
        val yearNum = getYearDigit(year).trim()
        val secCode = getSectionLetter(sec).trim()
        return "$cleanCollege-$progCode-$yearNum$secCode"
    }

    fun normalizeClassId(classId: String): String {
        val trimmed = classId.trim()
        return when {
            trimmed.equals("CITE-BS A-1A", ignoreCase = true) -> "CITE-BSARCH-1A"
            trimmed.contains("BS A-", ignoreCase = true) -> trimmed.replace("BS A-", "BSARCH-")
            trimmed.contains("BS A ", ignoreCase = true) -> trimmed.replace("BS A ", "BSARCH-")
            else -> trimmed.replace(" ", "")
        }
    }

    fun getProgramAbbreviation(program: String): String {
        val p = program.trim()
        return when {
            p.contains("Architecture", ignoreCase = true) -> "BSARCH"
            p.contains("Information Technology", ignoreCase = true) -> "BSIT"
            p.contains("Computer Science", ignoreCase = true) -> "BSCS"
            p.contains("Entertainment and Multimedia", ignoreCase = true) || p.contains("BSEMC", ignoreCase = true) -> "BSEMC"
            p.contains("Electronics Engineering", ignoreCase = true) || p.contains("BSECE", ignoreCase = true) -> "BSECE"
            p.contains("Electrical Engineering", ignoreCase = true) -> "BSEE"
            p.contains("Computer Engineering", ignoreCase = true) -> "BSCPE"
            p.contains("Industrial Engineering", ignoreCase = true) -> "BSIE"
            p.contains("Associate in Computer Technology", ignoreCase = true) || p.contains("ACT", ignoreCase = true) -> "ACT"
            p.contains("Accounting Information", ignoreCase = true) || p.contains("BSAIS", ignoreCase = true) -> "BSAIS"
            p.contains("Accountancy", ignoreCase = true) -> "BSA"
            p.contains("Legal Management", ignoreCase = true) -> "BSLM"
            p.contains("Financial Management", ignoreCase = true) -> "BSBA-FM"
            p.contains("Marketing Management", ignoreCase = true) -> "BSBA-MM"
            p.contains("Management Technology", ignoreCase = true) -> "BSMT"
            p.contains("Certificate in Entrepreneurship", ignoreCase = true) -> "CERT-ENTREP"
            p.contains("Entrepreneurship", ignoreCase = true) -> "BSENTREP"
            p.contains("Business Administration", ignoreCase = true) -> "BSBA"
            p.contains("Elementary Education", ignoreCase = true) -> "BEED"
            p.contains("Secondary Education", ignoreCase = true) -> "BSED"
            p.contains("Multimedia Arts", ignoreCase = true) -> "BMMA"
            p.contains("Communication", ignoreCase = true) -> "ABCOMM"
            p.contains("Biology", ignoreCase = true) -> "BSBIO"
            p.contains("Psychology", ignoreCase = true) -> "BSPSYCH"
            p.contains("Master of Arts in Education", ignoreCase = true) || p.contains("MAED", ignoreCase = true) -> "MAED"
            p.contains("Hospitality", ignoreCase = true) -> if (p.contains("Certificate", ignoreCase = true)) "CERT-HM" else "BSHM"
            p.contains("Tourism", ignoreCase = true) -> "BSTM"
            p.contains("Cookery", ignoreCase = true) -> "COOKERY"
            p.contains("Nursing", ignoreCase = true) -> "BSN"
            p.contains("Forensic", ignoreCase = true) -> "BSFS"
            p.contains("Criminology", ignoreCase = true) -> "BSCRIM"
            else -> p.replace("[^A-Za-z0-9]".toRegex(), "").take(6).uppercase().ifEmpty { "GEN" }
        }
    }

    private fun getYearDigit(year: String): String {
        return when {
            year.startsWith("1") -> "1"
            year.startsWith("2") -> "2"
            year.startsWith("3") -> "3"
            year.startsWith("4") -> "4"
            else -> "1"
        }
    }

    private fun getSectionLetter(section: String): String {
        val clean = section.trim().uppercase()
        val stripped = clean.replace("SECTION", "").replace("SEC", "").trim()
        val letterMatch = Regex("[A-J]").find(stripped)?.value
        if (letterMatch != null) return letterMatch
        val digitMatch = Regex("[1-9]").find(stripped)?.value
        if (digitMatch != null) return digitMatch
        return "A"
    }

    suspend fun signInWithGoogleCredential(credential: AuthCredential): AuthResult {
        val fa = auth ?: return AuthResult.Error("Firebase Auth is not available.")
        return try {
            val res = fa.signInWithCredential(credential).await()
            val firebaseUser = res.user ?: return AuthResult.Error("Failed to sign in with Google credential.")
            val uid = firebaseUser.uid
            val email = firebaseUser.email ?: ""
            val idToken = firebaseUser.getIdToken(false).await().token
            if (email.isBlank() || !isApprovedBetaUser(email, idToken)) {
                fa.signOut()
                return AuthResult.Error("This account is not approved for the ActiQuest beta. Contact the administrator to request access.")
            }
            val displayName = firebaseUser.displayName?.ifEmpty { null }
                ?: email.substringBefore("@").replace(".", " ").capitalizeWords()
            val photoUri = firebaseUser.photoUrl?.toString() ?: ""

            val isAdmin = checkIsAdmin(uid, email)
            val db = firestore
            var userDoc: User? = null
            var isNewGoogleUser = false

            if (db != null && uid.isNotEmpty()) {
                try {
                    val snap = db.collection("users").document(uid).get().await()
                    if (snap.exists()) {
                        userDoc = parseUserFromSnapshot(snap, uid, email)
                    } else {
                        isNewGoogleUser = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                isNewGoogleUser = true
            }

            if (userDoc == null) {
                val parts = displayName.split(" ")
                val fName = parts.firstOrNull() ?: displayName
                val lName = parts.drop(1).joinToString(" ")

                userDoc = User(
                    uid = uid,
                    name = displayName,
                    firstName = fName,
                    lastName = lName,
                    email = email,
                    college = if (isAdmin) "Staff" else "CITE",
                    program = if (isAdmin) "Wellness & Sports Division" else "BS Computer Science",
                    yearLevel = if (isAdmin) "Staff" else "1st Year",
                    section = if (isAdmin) "Admin" else "Section A",
                    classId = if (isAdmin) "ADMIN-STAFF" else "CITE-BSCS-1A",
                    profilePicUri = photoUri,
                    consentGiven = true
                )

                try {
                    val userMap = hashMapOf<String, Any>(
                        "uid" to uid,
                        "name" to displayName,
                        "firstName" to fName,
                        "lastName" to lName,
                        "email" to email,
                        "college" to userDoc.college,
                        "program" to userDoc.program,
                        "yearLevel" to userDoc.yearLevel,
                        "section" to userDoc.section,
                        "classId" to userDoc.classId,
                        "role" to if (isAdmin) "admin" else "student",
                        "requiresEmailVerification" to false,
                        "totalPoints" to 0L,
                        "targetPoints" to 500L,
                        "streak" to 1L,
                        "badges" to emptyList<String>(),
                        "approvedCount" to 0L,
                        "consentGiven" to true,
                        "profilePicUri" to photoUri,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db?.collection("users")?.document(uid)?.set(userMap, SetOptions.merge())?.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _currentUserId.value = uid
            _currentUser.value = userDoc
            _isAdminMode.value = isAdmin
            val current = _localUsers.value.toMutableListMap()
            current[uid] = userDoc
            _localUsers.value = current

            AuthResult.Success(userDoc, isAdmin, isNewUser = isNewGoogleUser)
        } catch (e: Exception) {
            fa.signOut()
            AuthResult.Error(formatFirebaseErrorMessage(e))
        }
    }

    suspend fun getQuest(questCode: String): Quest? {
        val trimmedCode = questCode.trim().uppercase()
        val normalizedCode = when (trimmedCode) {
            "QQ-SIDE-GATE", "SIDE-GATE", "SIDEGATE" -> "QQ-SIDEGATE"
            else -> trimmedCode
        }
        try {
            val db = firestore
            if (db != null) {
                val doc = db.collection("quests").document(normalizedCode).get().await()
                if (doc.exists()) {
                    val q = doc.toObject(Quest::class.java)
                    if (q != null && q.active) return q.withResolvedEvidenceConfiguration()
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        val seed = SeedQuests.QUEST_MAP[normalizedCode]
        if (seed != null && seed.active) return seed.withResolvedEvidenceConfiguration()

        return null
    }

    fun listenUser(userId: String): Flow<User?> = callbackFlow {
        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            val listener = db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(_localUsers.value[userId])
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val u = parseUserFromSnapshot(snapshot, userId)
                        val localUser = _localUsers.value[userId]
                        // Ensure total points never regress if locally approved points are higher
                        val resolvedPoints = maxOf(u.totalPoints, localUser?.totalPoints ?: 0)
                        val resolvedApprovedCount = maxOf(u.approvedCount, localUser?.approvedCount ?: 0)
                        val resolvedBadges = (u.badges + (localUser?.badges ?: emptyList())).distinct()
                        val mergedUser = u.copy(
                            totalPoints = resolvedPoints,
                            approvedCount = resolvedApprovedCount,
                            badges = resolvedBadges
                        )
                        val usersMap = _localUsers.value.toMutableListMap()
                        usersMap[userId] = mergedUser
                        _localUsers.value = usersMap
                        if (_currentUserId.value == userId) {
                            _currentUser.value = mergedUser
                        }
                        trySend(mergedUser)

                        // If academic info was manually edited in console, persist updated computed classId back to user document
                        val storedClassId = snapshot.getString("classId") ?: ""
                        if (storedClassId.isBlank() || normalizeClassId(storedClassId) != normalizeClassId(u.classId)) {
                            db.collection("users").document(userId).set(mapOf("classId" to u.classId), SetOptions.merge())
                        }

                        // Trigger class reconciliation to automatically reflect any manual database edits to academic profile
                        launch {
                            try { reconcileClassAggregates() } catch (_: Exception) {}
                        }
                    } else {
                        trySend(_localUsers.value[userId])
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val user = _localUsers.value[userId] ?: User(uid = userId, name = "Student $userId")
            trySend(user)
            awaitClose {}
        }
    }

    private var lastReconcileTime = 0L

    fun listenClasses(): Flow<List<ClassModel>> = callbackFlow {
        val db = firestore
        if (db != null) {
            val now = System.currentTimeMillis()
            if (now - lastReconcileTime > 30_000L) {
                lastReconcileTime = now
                launch {
                    try {
                        reconcileClassAggregates()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val listener = db.collection("classes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(_localClasses.value.sortedByDescending { it.totalPoints })
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val model = doc.toObject(ClassModel::class.java) ?: return@mapNotNull null
                                val effId = model.classId.ifEmpty { doc.id }
                                model.copy(classId = effId)
                            } catch (e: Exception) { null }
                        }.sortedByDescending { it.totalPoints }
                        trySend(list)
                    } else {
                        trySend(_localClasses.value.sortedByDescending { it.totalPoints })
                    }
                }
            awaitClose { listener.remove() }
        } else {
            trySend(_localClasses.value.sortedByDescending { it.totalPoints })
            awaitClose {}
        }
    }

    fun parseSubmissionFromSnapshot(doc: DocumentSnapshot): Submission {
        return try {
            val data = doc.data ?: emptyMap<String, Any>()
            val submissionId = doc.id.ifEmpty { data["submissionId"]?.toString() ?: UUID.randomUUID().toString() }
            val userId = data["userId"]?.toString() ?: ""
            val studentName = data["studentName"]?.toString() ?: ""
            val classId = data["classId"]?.toString() ?: ""
            val questCode = data["questCode"]?.toString() ?: ""
            val questTitle = data["questTitle"]?.toString() ?: ""
            val videoPath = data["videoPath"]?.toString() ?: ""
            val pointValue = (data["pointValue"] as? Number)?.toInt() ?: 0
            val status = data["status"]?.toString() ?: "pending"

            val rawSubmitted = data["submittedAt"]
            val submittedAtMillis = DateTimeUtils.parseToEpochMillis(rawSubmitted)

            val rawReviewed = data["reviewedAt"]
            val reviewedAtMillis = if (rawReviewed != null) DateTimeUtils.parseToEpochMillis(rawReviewed) else null

            val reviewerNote = data["reviewerNote"]?.toString()
            val submissionType = data["submissionType"]?.toString() ?: "video"
            val imagePath = data["imagePath"]?.toString() ?: ""
            val activityType = data["activityType"]?.toString() ?: ""
            val activityMetricValue = data["activityMetricValue"]?.toString() ?: ""
            val studentNotes = data["studentNotes"]?.toString() ?: ""
            val awardedPoints = (data["awardedPoints"] as? Number)?.toInt()
            val videoProvider = data["videoProvider"]?.toString() ?: "local"
            val driveFileId = data["driveFileId"]?.toString() ?: ""
            val driveUrl = data["driveUrl"]?.toString() ?: ""

            val sub = Submission(
                submissionId = submissionId,
                userId = userId,
                studentName = studentName,
                classId = classId,
                questCode = questCode,
                questTitle = questTitle,
                videoPath = videoPath,
                pointValue = pointValue,
                status = status,
                submittedAt = submittedAtMillis,
                reviewedAt = reviewedAtMillis,
                reviewerNote = reviewerNote,
                submissionType = submissionType,
                imagePath = imagePath,
                activityType = activityType,
                activityMetricValue = activityMetricValue,
                studentNotes = studentNotes,
                awardedPoints = awardedPoints,
                videoProvider = videoProvider,
                driveFileId = driveFileId,
                driveUrl = driveUrl
            )

            sub.copy(
                activityType = sub.getEffectiveActivityType(),
                activityMetricValue = sub.getEffectiveMetricValue()
            )
        } catch (e: Exception) {
            Submission(submissionId = doc.id)
        }
    }

    private fun mergeRemoteSubmissions(remoteList: List<Submission>) {
        if (remoteList.isEmpty()) return
        val current = _localSubmissions.value.toMutableList()
        val hasActualRemote = remoteList.any { !it.submissionId.startsWith("sub_demo_") }
        
        // When real submissions exist, purge the demo placeholder submissions to eliminate flickering
        val baseList = if (hasActualRemote) {
            current.filter { !it.submissionId.startsWith("sub_demo_") }.toMutableList()
        } else {
            current
        }

        for (remote in remoteList) {
            if (hasActualRemote && remote.submissionId.startsWith("sub_demo_")) continue

            val idx = baseList.indexOfFirst { it.submissionId == remote.submissionId }
            if (idx >= 0) {
                val local = baseList[idx]
                val isLocalFinal = local.status.equals("approved", ignoreCase = true) || local.status.equals("rejected", ignoreCase = true)
                val isRemotePending = remote.status.equals("pending", ignoreCase = true) || remote.status.equals("in review", ignoreCase = true)
                // Retain optimistic approval/rejection so it never flickers back to "in review"
                if (isLocalFinal && isRemotePending) {
                    // Do not revert local approved status
                } else {
                    baseList[idx] = remote
                }
            } else {
                baseList.add(remote)
            }
        }
        _localSubmissions.value = baseList
    }

    fun listenPendingSubmissions(): Flow<List<Submission>> = callbackFlow {
        val db = firestore
        if (db != null) {
            val listener = db.collection("submissions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val localPending = _localSubmissions.value.filter {
                            it.status.equals("pending", ignoreCase = true) || it.status.equals("in review", ignoreCase = true)
                        }.sortedBy { it.submittedAt }
                        trySend(localPending)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val remoteList = snapshot.documents.mapNotNull { parseSubmissionFromSnapshot(it) }
                        mergeRemoteSubmissions(remoteList)
                    }
                    val pendingList = _localSubmissions.value.filter {
                        it.status.equals("pending", ignoreCase = true) || it.status.equals("in review", ignoreCase = true)
                    }.sortedBy { it.submittedAt }
                    trySend(pendingList)
                }
            awaitClose { listener.remove() }
        } else {
            val localPending = _localSubmissions.value.filter {
                it.status.equals("pending", ignoreCase = true) || it.status.equals("in review", ignoreCase = true)
            }.sortedBy { it.submittedAt }
            trySend(localPending)
            awaitClose {}
        }
    }

    fun listenAllSubmissions(): Flow<List<Submission>> = callbackFlow {
        val db = firestore
        if (db != null) {
            val listener = db.collection("submissions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(_localSubmissions.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { parseSubmissionFromSnapshot(it) }
                            .sortedByDescending { it.submittedAt }
                        _localSubmissions.value = list
                        trySend(list)
                    } else {
                        trySend(_localSubmissions.value)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            trySend(_localSubmissions.value)
            awaitClose { }
        }
    }

    fun listenAllUsers(): Flow<List<User>> = callbackFlow {
        val db = firestore
        if (db != null) {
            val listener = db.collection("users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(_localUsers.value.values.toList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { parseUserFromSnapshot(it, it.id) }
                        val currentUsers = list.associateBy { it.uid }
                        _localUsers.value = currentUsers
                        trySend(list)
                    } else {
                        trySend(_localUsers.value.values.toList())
                    }
                }
            awaitClose { listener.remove() }
        } else {
            trySend(_localUsers.value.values.toList())
            awaitClose { }
        }
    }

    fun listenUserSubmissions(userId: String): Flow<List<Submission>> = callbackFlow {
        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            val listener = db.collection("submissions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val localSubs = _localSubmissions.value.filter { it.userId == userId }
                            .sortedByDescending { it.submittedAt }
                        trySend(localSubs)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val remoteList = snapshot.documents.mapNotNull { parseSubmissionFromSnapshot(it) }
                        mergeRemoteSubmissions(remoteList)
                    }
                    val userSubs = _localSubmissions.value.filter { it.userId == userId }
                        .sortedByDescending { it.submittedAt }
                    trySend(userSubs)
                }
            awaitClose { listener.remove() }
        } else {
            val userSubs = _localSubmissions.value.filter { it.userId == userId }
                .sortedByDescending { it.submittedAt }
            trySend(userSubs)
            awaitClose {}
        }
    }

    suspend fun submitQuestVideo(
        userId: String,
        studentName: String,
        questCode: String,
        questTitle: String,
        pointValue: Int,
        videoUri: Uri?,
        context: android.content.Context? = null,
        classId: String = ""
    ): Submission {
        val oneHourAgo = System.currentTimeMillis() - 3_600_000L
        val recentSubmissionsCount = _localSubmissions.value.count {
            it.userId == userId && it.submittedAt > oneHourAgo
        }
        if (recentSubmissionsCount >= 10) {
            throw IllegalStateException("Submission rate limit reached: You can submit a maximum of 10 quests or proofs per hour. Please wait before submitting again.")
        }

        val targetContext = context ?: appContext
        val submissionMillis = System.currentTimeMillis()
        val submissionId = UUID.randomUUID().toString()
        var videoStorageUrl = videoUri?.toString() ?: VideoStorageManager.DEFAULT_FALLBACK_VIDEO

        if (videoUri != null && targetContext != null) {
            try {
                videoStorageUrl = VideoStorageManager.uploadVideoProof(
                    context = targetContext,
                    videoUri = videoUri,
                    userId = userId,
                    questCode = questCode,
                    submissionId = submissionId
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val resolvedClassId = if (classId.isNotBlank()) {
            normalizeClassId(classId)
        } else {
            normalizeClassId(
                _localUsers.value[userId]?.classId
                    ?: _currentUser.value?.takeIf { it.uid == userId }?.classId
                    ?: ""
            )
        }

        val submission = Submission(
            submissionId = submissionId,
            userId = userId,
            studentName = studentName,
            classId = resolvedClassId,
            questCode = questCode,
            questTitle = questTitle,
            videoPath = videoStorageUrl,
            pointValue = pointValue,
            status = "pending",
            submittedAt = submissionMillis,
            submissionType = "video",
            activityType = "Campus Video Quest",
            activityMetricValue = "QR Video Proof ($pointValue pts)",
            videoProvider = "local",
            driveFileId = "",
            driveUrl = ""
        )

        try {
            val subMap = hashMapOf<String, Any>(
                "submissionId" to submissionId,
                "userId" to userId,
                "studentName" to studentName,
                "classId" to resolvedClassId,
                "questCode" to questCode,
                "questTitle" to questTitle,
                "videoPath" to videoStorageUrl,
                "pointValue" to pointValue,
                "status" to "pending",
                "submittedAt" to FieldValue.serverTimestamp(),
                "submissionType" to "video",
                "imagePath" to "",
                "activityType" to submission.activityType,
                "activityMetricValue" to submission.activityMetricValue,
                "studentNotes" to "",
                "videoProvider" to submission.videoProvider,
                "driveFileId" to submission.driveFileId,
                "driveUrl" to submission.driveUrl
            )
            firestore?.collection("submissions")?.document(submissionId)?.set(subMap)?.await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Firestore error saving video submission: ${e.message}", e)
            throw e
        }

        val current = _localSubmissions.value.filter { !it.submissionId.startsWith("sub_demo_") }.toMutableList()
        current.add(0, submission)
        _localSubmissions.value = current
        notifyActiveStreakIfNeeded(userId, resolvedClassId)

        addNotification(
            title = "Proof Submitted ⏳",
            message = "Your submission for '$questTitle' has been submitted and is awaiting faculty verification.",
            category = "Quest",
            userId = userId,
            classId = resolvedClassId,
            relatedId = submission.submissionId,
            id = "notif_sub_${submission.submissionId}"
        )

        return submission
    }

    suspend fun submitQuestDriveVideo(
        userId: String,
        studentName: String,
        questCode: String,
        questTitle: String,
        pointValue: Int,
        driveUrl: String,
        context: android.content.Context? = null,
        classId: String = ""
    ): Submission {
        val oneHourAgo = System.currentTimeMillis() - 3_600_000L
        val recentSubmissionsCount = _localSubmissions.value.count {
            it.userId == userId && it.submittedAt > oneHourAgo
        }
        if (recentSubmissionsCount >= 10) {
            throw IllegalStateException("Submission rate limit reached: You can submit a maximum of 10 quests or proofs per hour. Please wait before submitting again.")
        }

        val canonicalUrl = GoogleDriveUtils.buildCanonicalDriveUrl(driveUrl)
        val fileId = GoogleDriveUtils.extractDriveFileId(driveUrl) ?: ""
        val submissionMillis = System.currentTimeMillis()
        val submissionId = java.util.UUID.randomUUID().toString()

        val resolvedClassId = if (classId.isNotBlank()) {
            normalizeClassId(classId)
        } else {
            normalizeClassId(
                _localUsers.value[userId]?.classId
                    ?: _currentUser.value?.takeIf { it.uid == userId }?.classId
                    ?: ""
            )
        }

        val submission = Submission(
            submissionId = submissionId,
            userId = userId,
            studentName = studentName,
            classId = resolvedClassId,
            questCode = questCode,
            questTitle = questTitle,
            videoPath = canonicalUrl,
            pointValue = pointValue,
            status = "pending",
            submittedAt = submissionMillis,
            submissionType = "video",
            activityType = "Campus Video Quest",
            activityMetricValue = "QR Video Proof ($pointValue pts)",
            videoProvider = "google_drive",
            driveFileId = fileId,
            driveUrl = canonicalUrl
        )

        try {
            val db = firestore
            if (db != null) {
                val subMap = hashMapOf<String, Any>(
                    "submissionId" to submissionId,
                    "userId" to userId,
                    "studentName" to studentName,
                    "classId" to resolvedClassId,
                    "questCode" to questCode,
                    "questTitle" to questTitle,
                    "videoPath" to canonicalUrl,
                    "pointValue" to pointValue,
                    "status" to "pending",
                    "submittedAt" to FieldValue.serverTimestamp(),
                    "submissionType" to "video",
                    "imagePath" to "",
                    "activityType" to submission.activityType,
                    "activityMetricValue" to submission.activityMetricValue,
                    "studentNotes" to "",
                    "videoProvider" to submission.videoProvider,
                    "driveFileId" to fileId,
                    "driveUrl" to canonicalUrl
                )
                db.collection("submissions").document(submissionId).set(subMap).await()
                android.util.Log.d("FirebaseManager", "Successfully saved Drive submission to Firestore: $submissionId")
            } else {
                android.util.Log.w("FirebaseManager", "Firestore not connected, saved Drive submission to local session.")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Firestore error saving Drive submission: ${e.message}", e)
            throw e
        }

        val current = _localSubmissions.value.filter { !it.submissionId.startsWith("sub_demo_") }.toMutableList()
        current.add(0, submission)
        _localSubmissions.value = current
        notifyActiveStreakIfNeeded(userId, resolvedClassId)

        addNotification(
            title = "Proof Submitted ⏳",
            message = "Your submission for '$questTitle' has been submitted and is awaiting faculty verification.",
            category = "Quest",
            userId = userId,
            classId = resolvedClassId,
            relatedId = submission.submissionId,
            id = "notif_sub_${submission.submissionId}"
        )

        return submission
    }

    suspend fun submitScreenshotProof(
        userId: String,
        studentName: String,
        activityType: String,
        metricValue: String,
        requestedPoints: Int,
        studentNotes: String,
        imageUri: Uri? = null,
        context: android.content.Context? = null,
        driveUrl: String = "",
        driveFileId: String = "",
        classId: String = ""
    ): Submission {
        val oneHourAgo = System.currentTimeMillis() - 3_600_000L
        val recentSubmissionsCount = _localSubmissions.value.count {
            it.userId == userId && it.submittedAt > oneHourAgo
        }
        if (recentSubmissionsCount >= 10) {
            throw IllegalStateException("Submission rate limit reached: You can submit a maximum of 10 quests or proofs per hour. Please wait before submitting again.")
        }

        val targetContext = context ?: appContext
        val submissionMillis = System.currentTimeMillis()
        val submissionId = UUID.randomUUID().toString()
        val hasDriveLink = driveUrl.isNotBlank()
        val resolvedDriveFileId = if (driveFileId.isNotBlank()) driveFileId else (GoogleDriveUtils.extractDriveFileId(driveUrl) ?: "")
        var imageStorageUrl = if (hasDriveLink && imageUri == null) driveUrl.trim() else (imageUri?.toString() ?: "sample_screenshot_steps")

        if (imageUri != null && targetContext != null) {
            try {
                imageStorageUrl = VideoStorageManager.uploadScreenshotProof(
                    context = targetContext,
                    imageUri = imageUri,
                    userId = userId,
                    activityType = activityType,
                    submissionId = submissionId
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val resolvedClassId = if (classId.isNotBlank()) {
            normalizeClassId(classId)
        } else {
            normalizeClassId(
                _localUsers.value[userId]?.classId
                    ?: _currentUser.value?.takeIf { it.uid == userId }?.classId
                    ?: ""
            )
        }

        val submission = Submission(
            submissionId = submissionId,
            userId = userId,
            studentName = studentName,
            classId = resolvedClassId,
            questCode = "",
            questTitle = "$activityType: $metricValue",
            videoPath = if (hasDriveLink) driveUrl.trim() else "",
            imagePath = imageStorageUrl,
            pointValue = requestedPoints,
            status = "pending",
            submittedAt = submissionMillis,
            submissionType = "screenshot",
            activityType = activityType,
            activityMetricValue = metricValue,
            studentNotes = studentNotes,
            videoProvider = if (hasDriveLink) "google_drive" else "local",
            driveFileId = resolvedDriveFileId,
            driveUrl = if (hasDriveLink) driveUrl.trim() else ""
        )

        try {
            val subMap = hashMapOf<String, Any>(
                "submissionId" to submissionId,
                "userId" to userId,
                "studentName" to studentName,
                "classId" to resolvedClassId,
                "questCode" to "",
                "questTitle" to submission.questTitle,
                "videoPath" to submission.videoPath,
                "imagePath" to imageStorageUrl,
                "pointValue" to requestedPoints,
                "status" to "pending",
                "submittedAt" to FieldValue.serverTimestamp(),
                "submissionType" to "screenshot",
                "activityType" to activityType,
                "activityMetricValue" to metricValue,
                "studentNotes" to studentNotes,
                "videoProvider" to submission.videoProvider,
                "driveFileId" to resolvedDriveFileId,
                "driveUrl" to submission.driveUrl
            )
            firestore?.collection("submissions")?.document(submissionId)?.set(subMap)?.await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Firestore error saving fitness submission: ${e.message}", e)
            throw e
        }

        val current = _localSubmissions.value.filter { !it.submissionId.startsWith("sub_demo_") }.toMutableList()
        current.add(0, submission)
        _localSubmissions.value = current
        notifyActiveStreakIfNeeded(userId, resolvedClassId)

        addNotification(
            title = "Fitness Proof Submitted ⏳",
            message = "Your proof for '$activityType ($metricValue)' has been queued for faculty review.",
            category = "Quest",
            userId = userId,
            classId = resolvedClassId,
            relatedId = submission.submissionId,
            id = "notif_sub_${submission.submissionId}"
        )

        return submission
    }

    suspend fun approveSubmission(
        submission: Submission,
        awardedPoints: Int? = null,
        reviewerNote: String? = null
    ): Boolean {
        val db = firestore
        val submissionId = submission.submissionId
        val reviewMillis = System.currentTimeMillis()
        val finalPoints = awardedPoints ?: submission.pointValue

        // 1. Immediately update local memory state so UI responds instantly without flickering
        val updatedSubs = _localSubmissions.value.map {
            if (it.submissionId == submissionId) {
                it.copy(
                    status = "approved",
                    reviewedAt = reviewMillis,
                    awardedPoints = finalPoints,
                    reviewerNote = reviewerNote ?: it.reviewerNote
                )
            } else it
        }
        _localSubmissions.value = updatedSubs

        // 2. Fetch authoritative student profile directly from Firestore
        var studentDoc: User? = null
        if (db != null && submission.userId.isNotEmpty()) {
            try {
                val userSnap = db.collection("users").document(submission.userId).get().await()
                if (userSnap.exists()) {
                    studentDoc = parseUserFromSnapshot(userSnap, submission.userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val baseUser = studentDoc 
            ?: _localUsers.value[submission.userId]
            ?: _currentUser.value?.takeIf { it.uid == submission.userId }
            ?: User(
                uid = submission.userId, 
                name = submission.studentName,
                classId = normalizeClassId(submission.classId.ifEmpty { "CITE-BSCS-1A" })
            )

        // Ensure classId is computed directly from student's true academic fields
        val computedClassId = buildClassId(baseUser.college, baseUser.program, baseUser.yearLevel, baseUser.section)
        val studentClassId = normalizeClassId(
            if (computedClassId.isNotBlank()) computedClassId
            else if (baseUser.classId.isNotBlank()) baseUser.classId 
            else if (submission.classId.isNotBlank()) submission.classId 
            else "CITE-BSCS-1A"
        )

        val newApprovedCount = baseUser.approvedCount + 1
        val newTotalPoints = baseUser.totalPoints + finalPoints
        val newBadges = baseUser.badges.toMutableList()
        val newlyEarned = BadgeCatalog.checkNewBadges(newApprovedCount, baseUser.badges)
        newBadges.addAll(newlyEarned)

        val updatedUser = baseUser.copy(
            classId = studentClassId,
            totalPoints = newTotalPoints,
            approvedCount = newApprovedCount,
            badges = newBadges.distinct()
        )
        val currentUsers = _localUsers.value.toMutableListMap()
        currentUsers[submission.userId] = updatedUser
        _localUsers.value = currentUsers

        // Immediately update _currentUser if the affected student is currently logged in!
        if (_currentUserId.value == submission.userId || _currentUser.value?.uid == submission.userId) {
            _currentUser.value = updatedUser
        }

        // Update local class leaderboard state
        _localClasses.value = _localClasses.value.map {
            if (it.classId == studentClassId || normalizeClassId(it.classId) == studentClassId) {
                it.copy(totalPoints = it.totalPoints + finalPoints)
            } else it
        }

        // 3. Persist to Firestore with atomic operations
        if (db != null) {
            try {
                val subRef = db.collection("submissions").document(submissionId)
                val userRef = db.collection("users").document(submission.userId)

                val subUpdates = mutableMapOf<String, Any>(
                    "status" to "approved",
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "awardedPoints" to finalPoints,
                    "classId" to studentClassId
                )
                if (reviewerNote != null) {
                    subUpdates["reviewerNote"] = reviewerNote
                }
                subRef.set(subUpdates, SetOptions.merge()).await()

                val userUpdates = mapOf<String, Any>(
                    "totalPoints" to FieldValue.increment(finalPoints.toLong()),
                    "approvedCount" to FieldValue.increment(1L),
                    "badges" to updatedUser.badges,
                    "classId" to studentClassId
                )
                userRef.set(userUpdates, SetOptions.merge()).await()

                // Persist class total points to the student's true class
                if (studentClassId.isNotEmpty()) {
                    try {
                        val classRef = db.collection("classes").document(studentClassId)
                        val classSnap = classRef.get().await()
                        if (classSnap.exists()) {
                            classRef.update("totalPoints", FieldValue.increment(finalPoints.toLong())).await()
                        } else {
                            val parsed = parseClassMetadataFromId(studentClassId)
                            val newClass = parsed.copy(
                                totalPoints = finalPoints,
                                memberCount = 1
                            )
                            classRef.set(newClass, SetOptions.merge()).await()
                        }

                        // Also sync legacy document alias if studentClassId is CITE-BSARCH-1A or CITE-BS A-1A
                        if (studentClassId == "CITE-BSARCH-1A" || studentClassId == "CITE-BS A-1A") {
                            try {
                                val legRef = db.collection("classes").document("CITE-BS A-1A")
                                if (legRef.get().await().exists()) {
                                    legRef.update("totalPoints", FieldValue.increment(finalPoints.toLong())).await()
                                }
                            } catch (_: Exception) {}
                        }
                    } catch (ce: Exception) {
                        ce.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Emit live notification for student
        val questTitle = submission.questTitle.ifEmpty { "Fitness Activity" }
        addNotification(
            title = "Quest Approved! 🏆",
            message = "Your submission for '$questTitle' has been verified by Admin. +$finalPoints Points awarded to your class!",
            category = "Review",
            userId = submission.userId,
            classId = studentClassId,
            relatedId = submission.submissionId,
            id = "notif_appr_${submission.submissionId}"
        )

        // Badges notification
        for (b in newlyEarned) {
            addNotification(
                title = "Badge Unlocked: $b! 🎖️",
                message = "Congratulations! You earned the official '$b' badge for your fitness dedication.",
                category = "Badge",
                userId = submission.userId,
                classId = studentClassId,
                relatedId = b,
                id = "notif_badge_${submission.userId}_${b.replace(" ", "_")}"
            )
        }

        // Leaderboard rank notification for class
        if (studentClassId.isNotEmpty()) {
            val allLiveClasses = if (db != null) {
                try {
                    val classDocs = db.collection("classes").get().await()
                    classDocs.documents.mapNotNull { doc ->
                        val cId = doc.id
                        val name = doc.getString("name") ?: cId
                        val pts = doc.getLong("totalPoints")?.toInt() ?: 0
                        cId to (name to pts)
                    }.sortedByDescending { it.second.second }
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()

            val liveRankIndex = if (allLiveClasses.isNotEmpty()) {
                allLiveClasses.indexOfFirst { it.first == studentClassId || normalizeClassId(it.first) == studentClassId }
            } else {
                val sortedClasses = _localClasses.value.sortedByDescending { it.totalPoints }
                sortedClasses.indexOfFirst { it.classId == studentClassId || normalizeClassId(it.classId) == studentClassId }
            }

            if (liveRankIndex >= 0) {
                val trueRank = liveRankIndex + 1
                if (trueRank in 1..3) {
                    val classInfo = if (allLiveClasses.isNotEmpty()) allLiveClasses[liveRankIndex] else null
                    val className = classInfo?.second?.first ?: studentClassId
                    val totalClassPts = classInfo?.second?.second ?: (baseUser.totalPoints + finalPoints)

                    val medal = when (trueRank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }
                    val rankTitle = if (trueRank == 1) "Class Standings: Rank #1! $medal" else "Leaderboard Top 3: Rank #$trueRank! $medal"
                    val rankMessage = if (trueRank == 1) {
                        "$className is currently leading the campus standings at Rank #1 with $totalClassPts pts!"
                    } else {
                        "$className has climbed to Rank #$trueRank on the campus leaderboard with $totalClassPts pts!"
                    }
                    addNotification(
                        title = rankTitle,
                        message = rankMessage,
                        category = "Leaderboard",
                        userId = submission.userId,
                        classId = studentClassId,
                        relatedId = studentClassId,
                        id = "notif_rank_${submission.userId}_${studentClassId}_${System.currentTimeMillis() / (1000 * 60 * 30)}"
                    )
                }
            }
        }

        return true
    }

    suspend fun rejectSubmission(submissionId: String, reason: String): Boolean {
        val reviewMillis = System.currentTimeMillis()
        
        // 1. Immediately update local memory state
        val updatedSubs = _localSubmissions.value.map {
            if (it.submissionId == submissionId) {
                it.copy(status = "rejected", reviewerNote = reason, reviewedAt = reviewMillis)
            } else it
        }
        _localSubmissions.value = updatedSubs

        // 2. Persist to Firestore
        val db = firestore
        if (db != null) {
            try {
                db.collection("submissions").document(submissionId).set(
                    mapOf(
                        "status" to "rejected",
                        "reviewerNote" to reason,
                        "reviewedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Send real-time notification to the student
        val sub = _localSubmissions.value.find { it.submissionId == submissionId }
        val subTitle = sub?.questTitle ?: "Fitness Activity"
        val subUserId = sub?.userId ?: ""
        val subClassId = sub?.classId ?: ""

        if (subUserId.isNotEmpty()) {
            addNotification(
                title = "Submission Needs Revision ⚠️",
                message = "Your submission for '$subTitle' was not approved. Reviewer Note: \"${reason.ifEmpty { "Proof does not meet verification requirements." }}\"",
                category = "Review",
                userId = subUserId,
                classId = subClassId,
                relatedId = submissionId,
                id = "notif_rej_${submissionId}"
            )
        }
        return true
    }

    suspend fun updateUserProfilePic(uid: String, uriString: String): Boolean {
        val user = _localUsers.value[uid] ?: _currentUser.value ?: return false
        val updated = user.copy(profilePicUri = uriString)
        
        val db = firestore
        if (db != null) {
            try {
                db.collection("users").document(uid).update("profilePicUri", uriString).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val currentUsers = _localUsers.value.toMutableListMap()
        currentUsers[uid] = updated
        _localUsers.value = currentUsers
        if (_currentUserId.value == uid) {
            _currentUser.value = updated
        }
        return true
    }

    suspend fun updateTargetPoints(uid: String, targetPoints: Int): Boolean {
        val user = _localUsers.value[uid] ?: _currentUser.value ?: return false
        val updated = user.copy(targetPoints = targetPoints.coerceAtLeast(50))

        val db = firestore
        if (db != null) {
            try {
                db.collection("users").document(uid).update("targetPoints", targetPoints).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val currentUsers = _localUsers.value.toMutableListMap()
        currentUsers[uid] = updated
        _localUsers.value = currentUsers
        if (_currentUserId.value == uid) {
            _currentUser.value = updated
        }
        return true
    }

    suspend fun updateUserProfile(
        uid: String,
        name: String,
        college: String,
        program: String,
        yearLevel: String,
        section: String,
        firstName: String = "",
        lastName: String = "",
        nickname: String = ""
    ): Boolean {
        val user = _localUsers.value[uid] ?: _currentUser.value ?: return false
        val classId = buildClassId(college, program, yearLevel, section)

        val resolvedFirstName = firstName.trim().ifEmpty { name.trim().split(" ").firstOrNull() ?: user.firstName }
        val resolvedLastName = lastName.trim().ifEmpty { name.trim().split(" ").drop(1).joinToString(" ") }.ifEmpty { user.lastName }
        val resolvedFullName = if (resolvedFirstName.isNotBlank() || resolvedLastName.isNotBlank()) {
            "$resolvedFirstName $resolvedLastName".trim()
        } else {
            name.trim().ifEmpty { user.name }
        }

        val updated = user.copy(
            name = resolvedFullName,
            firstName = resolvedFirstName,
            lastName = resolvedLastName,
            college = college,
            program = program,
            yearLevel = yearLevel,
            section = section,
            classId = classId,
            nickname = nickname.trim()
        )
        
        val db = firestore
        if (db != null) {
            try {
                db.collection("users").document(uid).set(updated, SetOptions.merge()).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val currentUsers = _localUsers.value.toMutableListMap()
        currentUsers[uid] = updated
        _localUsers.value = currentUsers
        if (_currentUserId.value == uid) {
            _currentUser.value = updated
        }
        return true
    }

    fun parseAcademicChangeRequestFromSnapshot(doc: DocumentSnapshot): AcademicChangeRequest? {
        val data = doc.data ?: return null
        return try {
            AcademicChangeRequest(
                requestId = doc.id,
                userId = data["userId"]?.toString() ?: "",
                studentName = data["studentName"]?.toString() ?: "",
                studentEmail = data["studentEmail"]?.toString() ?: "",
                currentCollege = data["currentCollege"]?.toString() ?: "",
                currentProgram = data["currentProgram"]?.toString() ?: "",
                currentYearLevel = data["currentYearLevel"]?.toString() ?: "",
                currentSection = data["currentSection"]?.toString() ?: "",
                requestedCollege = data["requestedCollege"]?.toString() ?: "",
                requestedProgram = data["requestedProgram"]?.toString() ?: "",
                requestedYearLevel = data["requestedYearLevel"]?.toString() ?: "",
                requestedSection = data["requestedSection"]?.toString() ?: "",
                reason = data["reason"]?.toString() ?: "",
                status = data["status"]?.toString() ?: "pending",
                requestedAt = DateTimeUtils.parseToEpochMillis(data["requestedAt"]),
                reviewedAt = data["reviewedAt"]?.let { DateTimeUtils.parseToEpochMillis(it) },
                reviewerNote = data["reviewerNote"]?.toString()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun listenAcademicRequests(): Flow<List<AcademicChangeRequest>> = callbackFlow {
        val db = firestore
        if (db != null) {
            val listener = db.collection("academic_requests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(_localAcademicRequests.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val remoteList = snapshot.documents.mapNotNull { parseAcademicChangeRequestFromSnapshot(it) }
                        val merged = _localAcademicRequests.value.toMutableList()
                        for (remote in remoteList) {
                            val idx = merged.indexOfFirst { it.requestId == remote.requestId }
                            if (idx >= 0) {
                                merged[idx] = remote
                            } else {
                                merged.add(remote)
                            }
                        }
                        _localAcademicRequests.value = merged
                    }
                    trySend(_localAcademicRequests.value)
                }
            awaitClose { listener.remove() }
        } else {
            trySend(_localAcademicRequests.value)
            awaitClose {}
        }
    }

    fun listenUserAcademicRequest(userId: String): Flow<AcademicChangeRequest?> = callbackFlow {
        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            val listener = db.collection("academic_requests")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val local = _localAcademicRequests.value.filter { it.userId == userId }.maxByOrNull { it.requestedAt }
                        trySend(local)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { parseAcademicChangeRequestFromSnapshot(it) }
                        val latest = list.maxByOrNull { it.requestedAt } ?: _localAcademicRequests.value.filter { it.userId == userId }.maxByOrNull { it.requestedAt }
                        trySend(latest)
                    } else {
                        val local = _localAcademicRequests.value.filter { it.userId == userId }.maxByOrNull { it.requestedAt }
                        trySend(local)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            val local = _localAcademicRequests.value.filter { it.userId == userId }.maxByOrNull { it.requestedAt }
            trySend(local)
            awaitClose {}
        }
    }

    suspend fun submitAcademicChangeRequest(request: AcademicChangeRequest): Boolean {
        val currentList = _localAcademicRequests.value.toMutableList()
        currentList.removeAll { it.requestId == request.requestId }
        currentList.add(0, request)
        _localAcademicRequests.value = currentList

        val user = _localUsers.value[request.userId] ?: _currentUser.value
        val detailsString = "${request.requestedCollege} • ${request.requestedProgram} (${request.requestedSection})"
        if (user != null) {
            val updatedUser = user.copy(
                pendingAcademicUpdate = true,
                pendingAcademicDetails = detailsString
            )
            val currentUsers = _localUsers.value.toMutableListMap()
            currentUsers[request.userId] = updatedUser
            _localUsers.value = currentUsers
            if (_currentUserId.value == request.userId) {
                _currentUser.value = updatedUser
            }
        }

        val db = firestore
        if (db != null) {
            try {
                val reqMap = hashMapOf<String, Any>(
                    "requestId" to request.requestId,
                    "userId" to request.userId,
                    "studentName" to request.studentName,
                    "studentEmail" to request.studentEmail,
                    "currentCollege" to request.currentCollege,
                    "currentProgram" to request.currentProgram,
                    "currentYearLevel" to request.currentYearLevel,
                    "currentSection" to request.currentSection,
                    "requestedCollege" to request.requestedCollege,
                    "requestedProgram" to request.requestedProgram,
                    "requestedYearLevel" to request.requestedYearLevel,
                    "requestedSection" to request.requestedSection,
                    "reason" to request.reason,
                    "status" to request.status,
                    "requestedAt" to FieldValue.serverTimestamp()
                )
                db.collection("academic_requests").document(request.requestId)
                    .set(reqMap, SetOptions.merge()).await()
                db.collection("users").document(request.userId).set(
                    mapOf(
                        "pendingAcademicUpdate" to true,
                        "pendingAcademicDetails" to detailsString
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        addNotification(
            title = "Academic Transfer Requested 📋",
            message = "Your request to transfer to ${request.requestedCollege} - ${request.requestedProgram} (${request.requestedSection}) has been submitted for review.",
            category = "Academic",
            userId = request.userId,
            classId = request.requestedSection,
            relatedId = request.requestId,
            id = "notif_acad_req_${request.requestId}"
        )

        return true
    }

    suspend fun approveAcademicChangeRequest(requestId: String, reviewerNote: String? = null): Boolean {
        val timestamp = System.currentTimeMillis()
        val request = _localAcademicRequests.value.find { it.requestId == requestId } ?: return false

        val updatedRequests = _localAcademicRequests.value.map {
            if (it.requestId == requestId) {
                it.copy(status = "approved", reviewedAt = timestamp, reviewerNote = reviewerNote)
            } else it
        }
        _localAcademicRequests.value = updatedRequests

        val user = _localUsers.value[request.userId] ?: _currentUser.value
        val newClassId = buildClassId(
            request.requestedCollege,
            request.requestedProgram,
            request.requestedYearLevel,
            request.requestedSection
        )
        val oldClassId = normalizeClassId(user?.classId ?: "${request.currentCollege}-${getProgramAbbreviation(request.currentProgram)}-${getYearDigit(request.currentYearLevel)}${getSectionLetter(request.currentSection)}")
        
        if (user != null) {
            val updatedUser = user.copy(
                college = request.requestedCollege,
                program = request.requestedProgram,
                yearLevel = request.requestedYearLevel,
                section = request.requestedSection,
                classId = newClassId,
                pendingAcademicUpdate = false,
                pendingAcademicDetails = ""
            )
            val currentUsers = _localUsers.value.toMutableMap()
            currentUsers[request.userId] = updatedUser
            _localUsers.value = currentUsers
            if (_currentUserId.value == request.userId) {
                _currentUser.value = updatedUser
            }
        }

        // Update local classes member counts
        if (oldClassId != newClassId) {
            _localClasses.value = _localClasses.value.map {
                when {
                    it.classId == oldClassId || normalizeClassId(it.classId) == oldClassId -> 
                        it.copy(memberCount = maxOf(0, it.memberCount - 1))
                    it.classId == newClassId || normalizeClassId(it.classId) == newClassId -> 
                        it.copy(memberCount = it.memberCount + 1)
                    else -> it
                }
            }
        }

        val db = firestore
        if (db != null) {
            try {
                val reqUpdates = mutableMapOf<String, Any>(
                    "status" to "approved",
                    "reviewedAt" to FieldValue.serverTimestamp()
                )
                if (reviewerNote != null) reqUpdates["reviewerNote"] = reviewerNote
                db.collection("academic_requests").document(requestId).set(reqUpdates, SetOptions.merge()).await()

                val userUpdates = mapOf<String, Any>(
                    "college" to request.requestedCollege,
                    "program" to request.requestedProgram,
                    "yearLevel" to request.requestedYearLevel,
                    "section" to request.requestedSection,
                    "classId" to newClassId,
                    "pendingAcademicUpdate" to false,
                    "pendingAcademicDetails" to ""
                )
                db.collection("users").document(request.userId).set(userUpdates, SetOptions.merge()).await()

                if (oldClassId != newClassId) {
                    try {
                        if (oldClassId.isNotBlank()) {
                            db.collection("classes").document(oldClassId).update("memberCount", FieldValue.increment(-1L))
                        }
                        val newClassRef = db.collection("classes").document(newClassId)
                        val newClassSnap = newClassRef.get().await()
                        if (newClassSnap.exists()) {
                            newClassRef.update("memberCount", FieldValue.increment(1L)).await()
                        } else {
                            val className = "${request.requestedCollege} - ${getProgramAbbreviation(request.requestedProgram)} ${getYearDigit(request.requestedYearLevel)}${getSectionLetter(request.requestedSection)}"
                            val newClass = ClassModel(
                                classId = newClassId,
                                name = className,
                                college = request.requestedCollege,
                                program = request.requestedProgram,
                                yearLevel = request.requestedYearLevel,
                                section = request.requestedSection,
                                totalPoints = 0,
                                memberCount = 1
                            )
                            newClassRef.set(newClass).await()
                        }
                    } catch (ce: Exception) {
                        ce.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        addNotification(
            title = "Academic Profile Updated! ✅",
            message = "Your academic transfer to ${request.requestedCollege} - ${request.requestedProgram} (${request.requestedSection}) has been approved by Admin.",
            category = "Academic",
            userId = request.userId,
            classId = newClassId,
            relatedId = requestId,
            id = "notif_acad_appr_$requestId"
        )

        return true
    }

    fun getProgramFullName(progCode: String): String {
        val code = progCode.trim().uppercase()
        return when {
            code.contains("BSCS") -> "BS Computer Science"
            code.contains("BSIT") -> "BS Information Technology"
            code.contains("BSARCH") || (code.contains("BSA") && code.contains("ARCH")) || code == "BS A" -> "BS Architecture"
            code.contains("BSEE") -> "BS Electrical Engineering"
            code.contains("BSA") -> "BS Accountancy"
            code.contains("ABCOMM") || code.contains("COMM") -> "AB Communication"
            code.contains("BMMA") || code.contains("BMA") -> "Bachelor of Multimedia Arts"
            code.contains("BSN") -> "BS Nursing"
            code.contains("BSHM") -> "BS Hospitality Management"
            code.contains("BSFS") -> "BS Forensic Science"
            code.contains("BSBIO") -> "BS Biology"
            code.contains("BSPSYCH") -> "BS Psychology"
            code.contains("BSCRIM") -> "BS Criminology"
            code.contains("BSTM") -> "BS Tourism Management"
            else -> "BS $code"
        }
    }

    fun parseClassMetadataFromId(rawClassId: String): ClassModel {
        val norm = normalizeClassId(rawClassId)
        val parts = norm.split("-")
        val college = parts.getOrNull(0)?.trim()?.ifEmpty { "CITE" } ?: "CITE"
        val progCode = parts.getOrNull(1)?.trim() ?: "BSCS"
        val ys = parts.getOrNull(2)?.trim() ?: "1A"

        val program = getProgramFullName(progCode)
        val yearDigit = ys.take(1)
        val yearLevel = when (yearDigit) {
            "1" -> "1st Year"
            "2" -> "2nd Year"
            "3" -> "3rd Year"
            "4" -> "4th Year"
            else -> "1st Year"
        }
        val secChar = ys.drop(1).take(1).ifEmpty { "A" }
        val section = if (secChar.matches("[A-Z]".toRegex())) "Section $secChar" else "Section A"
        val className = "$college - ${getProgramAbbreviation(program)} ${yearDigit}${secChar}"

        return ClassModel(
            classId = rawClassId,
            name = className,
            college = college,
            program = program,
            yearLevel = yearLevel,
            section = section,
            totalPoints = 0,
            memberCount = 0
        )
    }

    suspend fun reconcileClassAggregates() {
        val db = firestore ?: return
        try {
            // 1. Fetch all documents currently in 'classes' collection
            val existingClassDocs = try {
                val classesSnap = db.collection("classes").get().await()
                classesSnap.documents
            } catch (e: Exception) {
                emptyList()
            }

            // 2. Fetch all student profiles from Firestore
            val userList = try {
                val usersSnap = db.collection("users").get().await()
                usersSnap.documents.mapNotNull { doc ->
                    val role = doc.getString("role")
                    if (role == null || role.equals("student", ignoreCase = true)) {
                        parseUserFromSnapshot(doc, doc.id)
                    } else null
                }
            } catch (e: Exception) {
                emptyList()
            }

            // 3. Fetch all approved submissions
            val approvedSubs = try {
                val subsSnap = db.collection("submissions").whereEqualTo("status", "approved").get().await()
                subsSnap.documents.mapNotNull { doc ->
                    try {
                        parseSubmissionFromSnapshot(doc)
                    } catch (e: Exception) { null }
                }
            } catch (e: Exception) {
                emptyList()
            }

            // Map student approved points
            val pointsByStudent = mutableMapOf<String, Int>()
            for (sub in approvedSubs) {
                val pts = sub.awardedPoints ?: sub.pointValue
                pointsByStudent[sub.userId] = (pointsByStudent[sub.userId] ?: 0) + pts
            }

            // Group students by normalized class ID
            val studentsByClass = mutableMapOf<String, MutableList<User>>()
            for (user in userList) {
                val normClassId = normalizeClassId(user.classId)
                if (normClassId.isNotBlank() && normClassId != "ADMIN-STAFF") {
                    studentsByClass.getOrPut(normClassId) { mutableListOf() }.add(user)
                }
            }

            // Combine all class IDs (both existing class documents in Firestore and registered user class IDs)
            val allClassIds = mutableSetOf<String>()
            existingClassDocs.forEach { allClassIds.add(it.id) }
            studentsByClass.keys.forEach { allClassIds.add(it) }

            // Process every class document and fill all 8 fields to bridge all gaps
            for (docId in allClassIds) {
                val normClassId = normalizeClassId(docId)
                val members = studentsByClass[normClassId] ?: emptyList()
                val memberCount = members.size

                val existingDoc = existingClassDocs.find { it.id == docId }
                val existingPoints = existingDoc?.getLong("totalPoints")?.toInt() ?: 0

                val totalPoints = if (members.isNotEmpty()) {
                    members.sumOf { u ->
                        val fromSubs = pointsByStudent[u.uid] ?: 0
                        maxOf(u.totalPoints, fromSubs)
                    }
                } else {
                    existingPoints
                }

                val classDoc = if (members.isNotEmpty()) {
                    val firstMember = members.first()
                    val className = "${firstMember.college} - ${getProgramAbbreviation(firstMember.program)} ${getYearDigit(firstMember.yearLevel)}${getSectionLetter(firstMember.section)}"
                    ClassModel(
                        classId = docId,
                        name = className,
                        college = firstMember.college,
                        program = firstMember.program,
                        yearLevel = firstMember.yearLevel,
                        section = firstMember.section,
                        totalPoints = totalPoints,
                        memberCount = memberCount
                    )
                } else {
                    val parsed = parseClassMetadataFromId(docId)
                    parsed.copy(
                        classId = docId,
                        totalPoints = totalPoints,
                        memberCount = memberCount
                    )
                }

                // Upsert to Firestore document (Document ID == classId == docId)
                try {
                    db.collection("classes").document(docId).set(classDoc, SetOptions.merge()).await()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.w("FirebaseManager", "Reconciliation notice: ${e.message}")
        }
    }

    suspend fun rejectAcademicChangeRequest(requestId: String, reason: String): Boolean {
        val timestamp = System.currentTimeMillis()
        val request = _localAcademicRequests.value.find { it.requestId == requestId } ?: return false

        val updatedRequests = _localAcademicRequests.value.map {
            if (it.requestId == requestId) {
                it.copy(status = "rejected", reviewedAt = timestamp, reviewerNote = reason)
            } else it
        }
        _localAcademicRequests.value = updatedRequests

        val user = _localUsers.value[request.userId] ?: _currentUser.value
        if (user != null) {
            val updatedUser = user.copy(
                pendingAcademicUpdate = false,
                pendingAcademicDetails = ""
            )
            val currentUsers = _localUsers.value.toMutableListMap()
            currentUsers[request.userId] = updatedUser
            _localUsers.value = currentUsers
            if (_currentUserId.value == request.userId) {
                _currentUser.value = updatedUser
            }
        }

        val db = firestore
        if (db != null) {
            try {
                db.collection("academic_requests").document(requestId).set(
                    mapOf(
                        "status" to "rejected",
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "reviewerNote" to reason
                    ),
                    SetOptions.merge()
                ).await()
                db.collection("users").document(request.userId).set(
                    mapOf(
                        "pendingAcademicUpdate" to false,
                        "pendingAcademicDetails" to ""
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        addNotification(
            title = "Academic Request Update ℹ️",
            message = "Your academic transfer request was not approved. Note: \"${reason.ifEmpty { "Request declined by faculty admin." }}\"",
            category = "Academic",
            userId = request.userId,
            classId = user?.classId ?: "",
            relatedId = requestId,
            id = "notif_acad_rej_$requestId"
        )

        return true
    }

    fun parseNotificationFromSnapshot(doc: DocumentSnapshot): AppNotification? {
        return try {
            val data = doc.data ?: return null
            val id = doc.id.ifEmpty { data["id"]?.toString() ?: UUID.randomUUID().toString() }
            val userId = data["userId"]?.toString() ?: ""
            val classId = data["classId"]?.toString() ?: ""
            val title = data["title"]?.toString() ?: ""
            val message = data["message"]?.toString() ?: ""
            val category = data["category"]?.toString() ?: "Quest"
            val rawTs = data["timestamp"]
            val timestamp = when (rawTs) {
                is Number -> rawTs.toLong()
                is String -> rawTs.toLongOrNull() ?: try {
                    val sdf = java.text.SimpleDateFormat("MMMM d, yyyy, h:mm a", Locale.ENGLISH)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
                    sdf.parse(rawTs)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
                is com.google.firebase.Timestamp -> rawTs.toDate().time
                else -> System.currentTimeMillis()
            }
            val isRead = when (val r = data["isRead"]) {
                is Boolean -> r
                is Number -> r.toInt() != 0
                is String -> r.toBoolean()
                else -> false
            }
            val relatedId = data["relatedId"]?.toString() ?: ""
            AppNotification(
                id = id,
                userId = userId,
                classId = classId,
                title = title,
                message = message,
                category = category,
                timestamp = timestamp,
                isRead = isRead,
                relatedId = relatedId
            )
        } catch (e: Exception) {
            null
        }
    }

    fun generateRealUserNotifications(
        user: User?,
        submissions: List<Submission>,
        classes: List<ClassModel>
    ): List<AppNotification> {
        if (user == null || user.uid.isBlank()) return emptyList()
        val uid = user.uid
        val classId = user.classId
        val result = mutableListOf<AppNotification>()

        // 1. Notifications derived dynamically from user's actual submissions
        val userSubs = submissions.filter { it.userId == uid }
        val activeStreak = DateTimeUtils.calculateStreaks(userSubs, uid).currentStreak
        for (sub in userSubs) {
            val titleText = sub.questTitle.ifBlank { "Fitness Activity" }
            val ts = DateTimeUtils.parseToEpochMillis(sub.reviewedAt ?: sub.submittedAt)
            when {
                sub.status.equals("approved", ignoreCase = true) -> {
                    val pts = sub.awardedPoints ?: sub.pointValue
                    result.add(
                        AppNotification(
                            id = "notif_appr_${sub.submissionId}",
                            userId = uid,
                            classId = sub.classId.ifEmpty { classId },
                            title = "Quest Approved! 🏆",
                            message = "Your submission for '$titleText' has been verified by Admin. +$pts Points awarded to your class!",
                            category = "Review",
                            timestamp = ts,
                            isRead = false,
                            relatedId = sub.submissionId
                        )
                    )
                }
                sub.status.equals("rejected", ignoreCase = true) -> {
                    val note = sub.reviewerNote?.ifBlank { "Proof does not meet verification requirements." } ?: "Proof does not meet verification requirements."
                    result.add(
                        AppNotification(
                            id = "notif_rej_${sub.submissionId}",
                            userId = uid,
                            classId = sub.classId.ifEmpty { classId },
                            title = "Submission Needs Revision ⚠️",
                            message = "Your submission for '$titleText' was not approved. Reviewer Note: \"$note\"",
                            category = "Review",
                            timestamp = ts,
                            isRead = false,
                            relatedId = sub.submissionId
                        )
                    )
                }
                sub.status.equals("pending", ignoreCase = true) -> {
                    val metric = if (!sub.activityMetricValue.isNullOrBlank()) " (${sub.activityMetricValue})" else ""
                    result.add(
                        AppNotification(
                            id = "notif_sub_${sub.submissionId}",
                            userId = uid,
                            classId = sub.classId.ifEmpty { classId },
                            title = "Proof Submitted ⏳",
                            message = "Your submission for '$titleText'$metric is in the faculty review queue.",
                            category = "Quest",
                            timestamp = DateTimeUtils.parseToEpochMillis(sub.submittedAt),
                            isRead = false,
                            relatedId = sub.submissionId
                        )
                    )
                }
            }
        }

        // 2. Notifications from user's earned badges
        for (badge in user.badges) {
            result.add(
                AppNotification(
                    id = "notif_badge_${uid}_${badge.replace(" ", "_")}",
                    userId = uid,
                    classId = classId,
                    title = "Badge Unlocked: $badge! 🎖️",
                    message = "Congratulations! You earned the official '$badge' badge for your fitness dedication.",
                    category = "Badge",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 8,
                    isRead = false,
                    relatedId = badge
                )
            )
        }

        // 3. User's active streak notification
        if (activeStreak >= 2) {
            result.add(
                AppNotification(
                    id = "notif_streak_${uid}_$activeStreak",
                    userId = uid,
                    classId = classId,
                    title = "Active Streak Maintained! 🔥",
                    message = "You have maintained an active fitness streak of $activeStreak days. Keep going to stay on top!",
                    category = "Streak",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
                    isRead = false,
                    relatedId = activeStreak.toString()
                )
            )
        }

        // 4. Class Leaderboard standings notification
        if (classId.isNotBlank()) {
            val userClass = classes.find { it.classId == classId || normalizeClassId(it.classId) == normalizeClassId(classId) }
            if (userClass != null) {
                val sorted = classes.sortedByDescending { it.totalPoints }
                val rankIndex = sorted.indexOfFirst { it.classId == userClass.classId || normalizeClassId(it.classId) == normalizeClassId(userClass.classId) }
                val rank = if (rankIndex >= 0) rankIndex + 1 else 1
                val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "📊" }
                val title = if (rank == 1) "Class Standings: Rank #1! $medal" else if (rank <= 3) "Leaderboard Top 3: Rank #$rank! $medal" else "Weekly Campus Standings $medal"
                val classNameDisplay = userClass.name.ifEmpty { userClass.classId }
                val msg = if (rank == 1) {
                    "$classNameDisplay is currently leading campus standings at #1 with ${userClass.totalPoints} pts!"
                } else {
                    "$classNameDisplay is currently Rank #$rank with ${userClass.totalPoints} pts on the leaderboard."
                }
                result.add(
                    AppNotification(
                        id = "notif_class_rank_${uid}_${userClass.classId}",
                        userId = uid,
                        classId = userClass.classId,
                        title = title,
                        message = msg,
                        category = "Leaderboard",
                        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
                        isRead = false,
                        relatedId = userClass.classId
                    )
                )
            }
        }

        // 5. Welcome notification
        result.add(
            AppNotification(
                id = "notif_welcome_$uid",
                userId = uid,
                classId = classId,
                title = "Welcome to ActiQuest, ${user.firstName.ifEmpty { user.name }}! ✨",
                message = "Scan campus QR markers or upload pedometer proofs to earn wellness points for your section.",
                category = "System",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48,
                isRead = false,
                relatedId = uid
            )
        )

        return result
    }

    fun mergeAndFilterNotifications(
        firestoreNotifs: List<AppNotification>,
        localNotifs: List<AppNotification>,
        currentUid: String
    ): List<AppNotification> {
        if (currentUid.isBlank()) return emptyList()

        val mergedMap = mutableMapOf<String, AppNotification>()

        for (n in localNotifs) {
            if (n.userId == currentUid) {
                mergedMap[n.id] = n
            }
        }

        for (n in firestoreNotifs) {
            if (n.userId == currentUid) {
                mergedMap[n.id] = n
            }
        }

        return mergedMap.values
            .filter { !_clearedNotificationIds.contains(it.id) }
            .map { notif ->
                if (_readNotificationIds.contains(notif.id)) {
                    notif.copy(isRead = true)
                } else notif
            }
            .sortedByDescending { it.timestamp }
    }

    fun listenNotifications(): Flow<List<AppNotification>> = callbackFlow {
        val db = firestore
        var firestoreNotifs: List<AppNotification> = emptyList()
        var listener: com.google.firebase.firestore.ListenerRegistration? = null

        fun emitLatest() {
            val merged = mergeAndFilterNotifications(
                firestoreNotifs = firestoreNotifs,
                localNotifs = _localNotifications.value,
                currentUid = _currentUserId.value
            )
            trySend(merged)
        }

        emitLatest()

        val jobs = mutableListOf<kotlinx.coroutines.Job>()
        jobs.add(launch {
            _currentUserId.collect { uid ->
                listener?.remove()
                listener = null
                firestoreNotifs = emptyList()
                if (db != null && uid.isNotBlank()) {
                    listener = db.collection("notifications")
                        .whereEqualTo("userId", uid)
                        .addSnapshotListener { snap, err ->
                            if (err != null) {
                                android.util.Log.e("FirebaseManager", "Unable to listen for notifications for $uid", err)
                                emitLatest()
                                return@addSnapshotListener
                            }
                            firestoreNotifs = snap?.documents
                                ?.mapNotNull { parseNotificationFromSnapshot(it) }
                                .orEmpty()
                            emitLatest()
                        }
                }
                emitLatest()
            }
        })

        jobs.add(launch {
            _localNotifications.collect { emitLatest() }
        })

        awaitClose {
            listener?.remove()
            jobs.forEach { it.cancel() }
        }
    }

    suspend fun markNotificationRead(id: String) {
        _readNotificationIds.add(id)
        val current = _localNotifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        _localNotifications.value = current
        try {
            firestore?.collection("notifications")?.document(id)?.update("isRead", true)?.await()
        } catch (e: Exception) {
            _readNotificationIds.remove(id)
            _localNotifications.value = _localNotifications.value.map {
                if (it.id == id) it.copy(isRead = false) else it
            }
            throw e
        }
    }

    suspend fun markAllNotificationsRead(notificationIds: Collection<String>) {
        val ids = notificationIds.filter { it.isNotBlank() }.distinct()
        val db = firestore
        if (db != null && ids.isNotEmpty()) {
            ids.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.update(db.collection("notifications").document(id), "isRead", true)
                }
                batch.commit().await()
            }
        }
        _readNotificationIds.addAll(ids)
        _localNotifications.value = _localNotifications.value.map {
            if (it.id in ids) it.copy(isRead = true) else it
        }
    }

    suspend fun clearNotifications(notificationIds: Collection<String>) {
        deleteNotifications(notificationIds)
    }

    suspend fun deleteNotification(notificationId: String) {
        deleteNotifications(listOf(notificationId))
    }

    suspend fun deleteNotifications(notificationIds: Collection<String>) {
        val ids = notificationIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return
        val db = firestore
        if (db != null) {
            ids.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(db.collection("notifications").document(id))
                }
                batch.commit().await()
            }
        }
        _clearedNotificationIds.addAll(ids)
        _localNotifications.value = _localNotifications.value.filterNot { it.id in ids }
    }

    suspend fun addNotification(
        title: String,
        message: String,
        category: String = "Quest",
        userId: String = "",
        classId: String = "",
        relatedId: String = "",
        id: String = "notif_" + UUID.randomUUID().toString().take(8),
        timestamp: Long = System.currentTimeMillis()
    ) {
        val resolvedUserId = userId.ifEmpty { _currentUserId.value.ifBlank { auth?.currentUser?.uid.orEmpty() } }
        if (resolvedUserId.isBlank()) {
            android.util.Log.e("FirebaseManager", "Cannot create notification without an owner UID: $title")
            return
        }
        val resolvedClassId = classId.ifEmpty { _currentUser.value?.classId ?: "" }
        val targetUser = _localUsers.value[resolvedUserId]
            ?: _currentUser.value?.takeIf { it.uid == resolvedUserId }
        val fullName = targetUser?.getFullName() ?: targetUser?.name.orEmpty()

        val notif = AppNotification(
            id = id,
            userId = resolvedUserId,
            classId = resolvedClassId,
            title = title,
            message = message,
            category = category,
            timestamp = timestamp,
            isRead = false,
            relatedId = relatedId
        )
        val db = firestore
        if (db != null) {
            try {
                val map = hashMapOf<String, Any>(
                    "id" to notif.id,
                    "userId" to notif.userId,
                    "fullName" to fullName,
                    "classId" to notif.classId,
                    "title" to notif.title,
                    "message" to notif.message,
                    "category" to notif.category,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "isRead" to notif.isRead,
                    "relatedId" to notif.relatedId
                )
                db.collection("notifications").document(notif.id).set(map, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("FirebaseManager", "Unable to save notification ${notif.id}", e)
                return
            }
        }
        _readNotificationIds.remove(notif.id)
        _clearedNotificationIds.remove(notif.id)
        val current = listOf(notif) + _localNotifications.value.filter { it.id != notif.id }
        _localNotifications.value = current
    }

    private suspend fun notifyActiveStreakIfNeeded(userId: String, classId: String) {
        val streak = DateTimeUtils.calculateStreaks(
            submissions = _localSubmissions.value.filter { it.userId == userId },
            userId = userId
        ).currentStreak
        if (streak >= 2) {
            addNotification(
                title = "Active Streak Maintained! 🔥",
                message = "You have maintained an active fitness streak of $streak days. Keep going to stay on top!",
                category = "Streak",
                userId = userId,
                classId = classId,
                relatedId = streak.toString(),
                id = "notif_streak_${userId}_$streak"
            )
        }
    }

    private fun <K, V> Map<K, V>.toMutableListMap(): MutableMap<K, V> = this.toMutableMap()
    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
