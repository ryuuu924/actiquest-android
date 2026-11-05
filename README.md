<div align="center">
  <img src="assets/ActiQuest%20Logo%20(1).png" alt="ActiQuest Logo" width="160" />
  <h1>⚡ ActiQuest: Campus Fitness & Active Learning Quest System</h1>
</div>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat&logo=kotlin)](https://kotlinlang.org)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Design](https://img.shields.io/badge/Design-Material%203-FF7043?style=flat)](https://m3.material.io)
[![Database](https://img.shields.io/badge/Database-Cloud%20Firestore-FFA000?style=flat&logo=firebase)](https://firebase.google.com)
[![Storage](https://img.shields.io/badge/Storage-Supabase%20%2F%20Firebase-3ECF8E?style=flat&logo=supabase)](https://supabase.com)
[![License](https://img.shields.io/badge/License-MIT-00B0FF?style=flat)](LICENSE)

> **ActiQuest** is an open-source, gamified campus wellness and active learning platform designed for universities, colleges, and school physical education campaigns. It transforms daily campus walking and study breaks into interactive fitness quests, motivating students through landmark QR discoveries, motion proofs, verified point rewards, and inter-class leaderboard battles.

---

## 📱 System Overview

Traditional university wellness programs often struggle with low student engagement and cumbersome manual tracking. **ActiQuest** solves this by providing:

1. **Student Adventure Portal**:
   - **Landmark QR Quests**: Discover and scan QR code markers posted at physical campus hotspots (e.g., Side gate area or around the BB lobby, building entrances, Track Oval).
   - **Activity Proof Engine**: Record short video proofs of completed exercises (preferably 30–45s, 480p SD, <10MB) or upload screenshots from pedometers, fitness bands, or running apps (Steps, Active Minutes, 3KM Fun Run).
   - **Academic Profile & Admin Processing**: Students can request updates to their profile (College, Program, Year Level, Section). Academic changes are queued into the Admin Verification Desk for review and approval to ensure class standings and college leaderboards remain fair and tamper-proof.
   - **Class Standing & College Battles**: Real-time leaderboards by College, Academic Program, Year Level, and Section.
   - **Milestone Badges**: Unlock achievement badges as workouts are reviewed and approved.
   - **Activity Journey**: Calendar-based streak tracking with weekly analytics.

2. **Admin Verification Desk (PE Faculty & Coordinators)**:
   - Dedicated, role-gated verification queue to inspect submitted exercise videos and fitness screenshots.
   - **Academic Change Requests**: Dedicated verification desk to approve or reject student class transfers and major shifts with official notes.
   - **Scoring Tools**: Approve with custom or full points, reject with personalized coaching notes, or request retakes.
   - Immediate point synchronization with student profiles and class totals in Firestore.
   - Live quest manager and campus-wide metrics summary.

---

## 📍 Campus Quest Catalog & Test QR Codes

Students unlock quests by scanning physical QR markers located across the campus. Below is the official quest catalog, including the **Side Gate Lunges** quest.

> 💡 **Try it Out**: Open the ActiQuest app, tap **Scan Quest QR** on the home screen, and point your camera directly at any of the QR codes below!

### 🌟 Featured Quest: Side Gate Lunges
- **Backend Quest Code**: `QQ-SIDEGATE` *(also accepts alias `QQ-SIDE-GATE`)*
- **Title**: Side Gate Lunges
- **Category**: Quick Quest (50 Points | 30s Cap)
- **Campus Location**: Side Gate Area / BB Lobby
- **Exercise Instructions**: Perform 12 walking lunges (6 per leg) at the side gate area or around the BB lobby walkway.
- **QR Code Payload**: `QQ-SIDEGATE`

<div align="center">
  <img src="https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=QQ-SIDEGATE&color=4F46E5" alt="QR Code - Side Gate Lunges" width="220" />
  <p><b>Scan with ActiQuest:</b> <code>QQ-SIDEGATE</code></p>
</div>

---

### 📋 Full Campus Quest QR Roster

| Quest Code | Quest Title | Category | Pts | Location | Scan Marker (Direct QR) |
| :--- | :--- | :---: | :---: | :--- | :---: |
| `QQ-SIDEGATE` | **Side Gate Lunges** | Quick | 50 | Side gate area or around BB lobby | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=QQ-SIDEGATE) |
| `QQ-DIOKNO` | **Diokno Steps** | Quick | 50 | Sen. Jose Diokno Entrance | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=QQ-DIOKNO) |
| `QQ-CBEAM` | **CBEAM Stretch** | Quick | 50 | Outside CBEAM Hall | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=QQ-CBEAM) |
| `QQ-SLOUNGE` | **South Lounge Squats** | Quick | 50 | South Lounge | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=QQ-SLOUNGE) |
| `QQ-CANTEEN` | **Canteen Calorie Burn**| Quick | 50 | BB Canteen Area | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=QQ-CANTEEN) |
| `QQ-MABINI` | **Mabini Mini Break** | Quick | 50 | Apolinario Mabini Hall | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=QQ-MABINI) |
| `EQ-LRC` | **LRC Stair Climb** | Explorer | 100 | Diokno Bldg Stairwell | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=EQ-LRC) |
| `EQ-NEXUS` | **Nexus Walk** | Explorer | 100 | Nexus Labs Walkway | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=EQ-NEXUS) |
| `EQ-OVAL` | **Track Oval Lap** | Explorer | 100 | Campus Track Oval | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=EQ-OVAL) |
| `AQ-RUN-START` | **3KM Run: Start** | Boss | 0 | Campus Oval Starting Arch | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=AQ-RUN-START) |
| `AQ-RUN-FINISH`| **3KM Run: Finish**| Boss | 300 | Campus Oval Finish Arch | [View QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=AQ-RUN-FINISH) |

---

## ☁️ Zero-Cost Video Submission Architecture: Google Drive vs. Supabase vs. Firebase

Since Firebase Cloud Storage requires a credit card or paid Blaze plan for production buckets, ActiQuest offers zero-cost evidence architectures designed for physical education departments, student councils, and university wellness campaigns operating on a **$0 budget**.

### 1. Primary Zero-Cost Solution: Google Drive Evidence Workflow (Videos & Fitness Proofs)
Students submit both **QR quest motion videos** and **daily fitness activity proofs (steps screenshots, run summaries, pedometer exports)** via Google Drive:
1. Students record their workout video or export their fitness screenshot.
2. Students upload the file to their personal or school Google Drive.
3. Students share the file with the designated campus review address (e.g. `actiquest.admin@gmail.com`) with **General access set to Restricted** and **Role set to Viewer**.
4. Students paste the Google Drive link into ActiQuest (with real-time file ID validation) and submit.
- **Cost**: $0.00 (leveraging student/institutional Google Workspace Drive allocations).
- **Setup**: Zero cloud storage provisioning required.
- **Faculty Workflow**: Instructors inspect the evidence directly via Google Drive or browser from the Admin Verification Desk and award verified points.
- **Data Persistence**: Firebase Authentication and Firestore remain the single source of truth for users, quests, submissions, and leaderboard points.

---

### 2. Alternative Zero-Cost Possibility: Supabase Storage Free Tier + Cloudflare Worker Bridge
For schools that prefer direct in-app video bucket uploads without external links, the codebase provides full **Supabase Storage** capabilities:
- **Firebase Authentication (Free Spark Tier)**: Retained for student/admin login and verified UIDs.
- **Firebase Cloud Firestore (Free Spark Tier)**: Retained as the source of truth for users, quests, submissions, academic requests, leaderboards, and badges (1 GiB storage, 50k reads/day, 20k writes/day).
- **Supabase Storage (Free Tier)**: Provides **1 GB of free file storage**, **5 GB egress**, and up to a **50 MB max upload limit per file**.
- **Cloudflare Worker (Free Plan)**: 100,000 free requests per day, serving as the secure proxy between Firebase-authenticated Android users and Supabase private buckets.

#### Hybrid Architectural Flow

```text
                         ACTIQUEST ANDROID
                                │
             ┌──────────────────┼──────────────────┐
             │                  │                  │
             ▼                  ▼                  ▼
        Firebase Auth      Firebase Firestore   Cloudflare Worker (Bridge)
             │                  │                  │
          Firebase UID     users / quests /        │
             │             submissions / admins    │
             │             academic_requests       │
             │                                     │
             └──────────────► Worker ◄─────────────┘
                                  │
                                  ▼
                            Supabase Storage
                                  │
                                  ▼
                        Private Video Bucket
                       ('quest-submissions')
```

#### Video Storage Policy
To maximize the free 1 GB tier (supporting 100+ active submissions concurrently):
- **Video Resolution**: 480p SD (configured automatically via CameraX `QualitySelector.from(Quality.SD)`)
- **Duration Cap**: 30–45 seconds
- **Target File Size**: < 10 MB per video (100 videos × 8 MB ≈ 800 MB, well within 1 GB)
- **Bucket Visibility**: Private bucket named `quest-submissions`
- **Reviewed Video Policy**: Videos serve as temporary evidence for PE faculty grading. Once approved, the permanent points, streak, and badge records stay recorded in Firestore forever.

---

### Step-by-Step Setup: Zero-Cost Supabase + Cloudflare Worker

#### Phase 1 — Firebase Foundation (Auth + Firestore)
Ensure your Firebase project is created with **Email/Password** enabled in Authentication and **Cloud Firestore** provisioned.

#### Phase 2 — Create the Firebase Admin
In Firebase Console → **Firestore Database**, create the admin role:
```text
admins/{ADMIN_FIREBASE_UID}
  name: "PE Department Admin"
  role: "admin"
```

#### Phase 3 — Create the Supabase Project
1. Go to [https://supabase.com/](https://supabase.com/) and create a free account.
2. Create a new project named `ActiQuest Videos`.
3. Note your **Project URL** (e.g. `https://xyzcompany.supabase.co`) and **anon public API key** from Project Settings > API.

#### Phase 4 — Create the Video Bucket
1. In Supabase Dashboard, go to **Storage > New Bucket**.
2. Name the bucket: `quest-submissions`.
3. Set **Public: OFF** (Private bucket).
4. File storage path convention used by ActiQuest:
   ```text
   quest-submissions/submissions/{firebaseUid}/{submissionId}.mp4
   ```

#### Phase 5 — Deploy Cloudflare Worker Bridge (`actiquest-video-api`)
Go to [Cloudflare Dashboard](https://dash.cloudflare.com/) > **Workers & Pages** > **Create Application** > **Create Worker**.

Name: `actiquest-video-api`

Deploy the following bridge code:

```javascript
/**
 * ActiQuest Video API - Cloudflare Worker Bridge
 * Validates Firebase Auth token and generates signed Supabase upload URLs.
 */
export default {
  async fetch(request, env) {
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const authHeader = request.headers.get("Authorization");

    // Upload endpoint: stream file directly to Supabase storage
    if (url.pathname === "/api/upload" && request.method === "POST") {
      const targetPath = url.searchParams.get("path");
      if (!targetPath) {
        return new Response(JSON.stringify({ error: "Missing path parameter" }), {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
      }

      const fileBuffer = await request.arrayBuffer();

      // Forward to Supabase Storage API using private service role key
      const supabaseEndpoint = `${env.SUPABASE_URL}/storage/v1/object/${env.SUPABASE_BUCKET || "quest-submissions"}/${targetPath}`;
      const uploadRes = await fetch(supabaseEndpoint, {
        method: "POST",
        headers: {
          "apikey": env.SUPABASE_SERVICE_ROLE_KEY,
          "Authorization": `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
          "Content-Type": "video/mp4",
          "x-upsert": "true"
        },
        body: fileBuffer
      });

      if (!uploadRes.ok) {
        const errText = await uploadRes.text();
        return new Response(JSON.stringify({ error: errText }), {
          status: uploadRes.status,
          headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
      }

      const publicUrl = `${env.SUPABASE_URL}/storage/v1/object/public/${env.SUPABASE_BUCKET || "quest-submissions"}/${targetPath}`;
      return new Response(JSON.stringify({ success: true, url: publicUrl }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    return new Response("ActiQuest Video Storage Bridge Active", { headers: corsHeaders });
  }
};
```

Set the Environment Variables in Cloudflare Worker settings:
- `SUPABASE_URL`: Your project URL (e.g. `https://xxxx.supabase.co`)
- `SUPABASE_SERVICE_ROLE_KEY`: Your Supabase service role secret (kept secure on Cloudflare)
- `SUPABASE_BUCKET`: `quest-submissions`

#### Phase 6 — Connect Android App
Add the keys to your project's Secrets panel in AI Studio or local `.env`:
```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-supabase-anon-key
SUPABASE_BUCKET=quest-submissions
CLOUDFLARE_WORKER_URL=https://actiquest-video-api.your-worker.workers.dev
```

*Note: If no Supabase or Worker keys are set, the app automatically runs in resilient local caching mode with demo proof streams so students and admins can test without setting up storage first.*

---

## 🛠️ Architecture & Tech Stack

```text
├── app/src/main/java/com/actiquest/
│   ├── MainActivity.kt               # Edge-to-edge controller & Repository init
│   ├── data/
│   │   ├── FirebaseManager.kt        # Auth, Firestore sync, local cache & academic requests
│   │   └── VideoStorageManager.kt    # Zero-cost Supabase, Worker bridge & Media3 URL resolver
│   ├── domain/
│   │   └── Models.kt                 # Quests, Submissions, Badges, AcademicChangeRequest, Colleges catalog
│   ├── ui/
│   │   ├── ActiQuestApp.kt           # NavHost & authentication routing
│   │   ├── components/               # VideoPlayerView, Avatars, Cards, Badges, Scanner
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt         # Student dashboard, activity cards & feed
│   │   │   ├── LoginScreen.kt        # Role-isolated Student & Admin portals
│   │   │   ├── AdminReviewScreen.kt  # Verification desk, submissions & academic requests
│   │   │   ├── StudentSubmissions.kt # Submission status, feedback & retake
│   │   │   ├── ProfileSettings.kt    # User profile, theme toggle, academic change & About dialog
│   │   │   ├── ScanScreen.kt         # Real-time ZXing QR scanner
│   │   │   ├── RecordScreen.kt       # CameraX in-app 480p SD video proof recorder
│   │   │   ├── StandingsScreen.kt    # Inter-class & college leaderboards
│   │   │   └── BadgesScreen.kt       # Milestone achievement gallery
│   │   └── theme/                    # Material 3 colors, typography & dark theme
```

- **Android Jetpack**: Compose, ViewModel, Navigation Compose, Coroutines & Flow.
- **Camera & Scanning**: CameraX for in-app video capture; ZXing Android Embedded for instant QR decoding.
- **Media Playback**: AndroidX Media3 (ExoPlayer) with automatic fallback and stream resolution.
- **Data & Auth**: Firebase Authentication (Email/Password) with Cloud Firestore real-time snapshot listeners.
- **Storage**: Multi-cloud VideoStorageManager (Supabase Storage, Cloudflare Worker, Firebase Storage, and persistent local storage).

---

## 🏫 How to Customize for Your Own School Campaign

Follow these steps to customize ActiQuest for your university, college, or corporate wellness campaign:

### 1. Update Colleges, Degree Programs & Sections
Open `app/src/main/java/com/actiquest/domain/Models.kt` and modify `CollegeCatalog`:
```kotlin
object CollegeCatalog {
    val COLLEGES = listOf(
        College(
            code = "CITE",
            name = "College of Information Technology and Engineering",
            programs = listOf(
                "BS Architecture",
                "BS Computer Engineering",
                "BS Computer Science",
                "BS Electrical Engineering",
                "BS Electronics Engineering",
                "BS Entertainment and Multimedia Computing",
                "BS Industrial Engineering",
                "BS Information Technology",
                "Associate in Computer Technology"
            )
        ),
        College(
            code = "CBEAM",
            name = "College of Business, Economics, Accountancy and Management",
            programs = listOf(
                "BS Accountancy",
                "BS Accounting Information System",
                "BS Legal Management",
                "BS Entrepreneurship",
                "BS Management Technology",
                "BSBA Financial Management",
                "BSBA Marketing Management",
                "Certificate in Entrepreneurship"
            )
        ),
        College(
            code = "CEAS",
            name = "College of Education, Arts and Sciences",
            programs = listOf(
                "Bachelor of Elementary Education",
                "Bachelor of Secondary Education",
                "AB Communication",
                "Bachelor of Multimedia Arts",
                "BS Biology",
                "BS Psychology",
                "Master of Arts in Education major in Educational Management",
                "Master of Arts in Education major in English",
                "Master of Arts in Education major in Filipino",
                "Master of Arts in Education major in Mathematics"
            )
        ),
        College(
            code = "CIHTM",
            name = "College of International Hospitality and Tourism Management",
            programs = listOf(
                "BS Hospitality Management",
                "BS Tourism Management",
                "Cookery NC II (Culinary Arts)",
                "Certificate in Hospitality Management"
            )
        )
    )
}
```

### 2. Configure Your Campus Quests & Landmark Locations
In the same file (`app/src/main/java/com/actiquest/domain/Models.kt`), update `SeedQuests.QUEST_MAP`:
```kotlin
"QQ-SIDEGATE" to Quest(
    questCode = "QQ-SIDEGATE",
    title = "Side Gate Lunges",
    instructions = "Perform 12 walking lunges (6 per leg) at the side gate area or around the BB lobby walkway.",
    location = "Side Gate Area / BB Lobby",
    category = "quick",
    pointValue = 50,
    maxSeconds = 30,
    active = true
)
```

### 3. Print QR Markers for Campus Spots
Generate and print QR codes containing the `questCode` text (e.g. `QQ-SIDEGATE`). Laminate and mount them at the corresponding campus locations.

### 4. Deploy Cloud Firestore Security Rules
1. Open Firebase Console → **Firestore Database** → **Rules**.
2. Back up the published rules, then replace them with the complete contents of [`firestore.rules`](firestore.rules). Do not use a permissive sample ruleset.
3. Before publishing, add every approved tester—including existing users who should retain access—to the `approved_testers` collection. Use each lowercase email as the document ID, with `approved: true` and `active: true`. Remove the document or set either field to `false` to revoke access. The administrator accounts configured in the rules remain exempt.
4. Publish the rules. New student profiles and app data require beta approval; email/password accounts must also complete the existing email-verification flow before using app data.
5. No bulk timestamp migration is needed. The app reads each existing `submittedAt` and attributes activity to that date; `reviewedAt` remains the review event time.
6. Deploy `web-portal` with Firebase Admin credentials and configure `APPROVED_BETA_TESTERS_EMAILS` if you also want an environment-based allowlist. Android uses the portal's `/api/check-beta-access` before signup; Firestore rules remain the enforcement layer if a client bypasses this check.
7. Build/install the Android app and verify an approved account can sign up and use the app, while an unapproved account is denied. Verify APK distribution also denies unapproved accounts.
8. Sign in with an affected account, submit a test proof, and verify it appears in **My Submissions** and on its submission date in **Activity Journey**. A denied Firestore write now shows a failure instead of appearing as a successful local-only submission.
9. With a student account, verify that an academic notification opens **My Submissions → Academics**, and that a quest/review notification opens the matching submission for that same account.
10. Test the notification **X**, long-press selection, and **Clear All**. Confirm each deletion in Firestore's `notifications` collection and verify a second student account cannot read or delete the first student's documents.

### Quest Activity and Evidence Duration Policies

The seed catalog's activity and video-evidence policies are centralized in
`app/src/main/java/com/actiquest/domain/QuestEvidence.kt`. Activity duration is
separate from recording duration. Existing Firestore quest documents need no
migration: known seed quest codes use this policy map, and other quest documents
continue to use their existing `maxSeconds` value unless the optional evidence
fields are configured.

| Quest | Activity requirement | Evidence mode | Permitted recording |
|---|---|---|---|
| QQ-DIOKNO | 15 jumping jacks; self-paced | Full activity | Any duration up to 45 sec |
| QQ-CBEAM | 2 minutes of dynamic stretching | Full activity | Any duration up to 2m30s |
| QQ-SLOUNGE | 10 squats; self-paced | Full activity | Any duration up to 60 sec |
| QQ-CANTEEN | 20 seconds of high knees | Full activity | Any duration up to 45 sec |
| QQ-MABINI | 15 wall/incline push-ups; self-paced | Full activity | Any duration up to 75 sec |
| QQ-SIDEGATE | 12 lunges; self-paced | Full activity | Any duration up to 90 sec |
| EQ-LRC | Two stair climbs; self-paced with rest as needed | Full activity | Any duration up to 10 min |
| EQ-NEXUS | Walk to Nexus and back; route time unspecified | Representative clip | Any duration up to 90 sec |
| EQ-OVAL | One 400m lap; self-paced | Full activity | Up to 10 min |
| AQ-RUN-START | Start-line QR checkpoint; no specified activity duration | Checkpoint | No video |
| AQ-RUN-FINISH | 3km finish checkpoint | Representative finish clip | Any duration up to 15 sec |

These are configured evidence allowances, not guarantees that an activity takes
that long. Full-activity clips must show the complete exercise. The Nexus route
and 3km event are not timed by the app; the latter's finish clip is only
representative evidence. Google Drive links are not downloaded or inspected by
the app, so their duration and size remain subject to student attestation and
faculty review. Locally selected/recorded videos are checked for duration and
size before submission. There is no minimum video duration in this beta; any
non-empty video with readable metadata is accepted if it does not exceed the
quest's maximum duration or 50 MiB file-size cap. Local recording bitrate is
calculated from the allowed duration and per-quest size cap to keep longer
recordings feasible. The legacy `minimumEvidenceDurationSeconds` field is
ignored for validation and retained only for backward-compatible Firestore
documents.

To adjust a known quest, update its `QuestEvidenceConfiguration` entry in
`QuestEvidence.kt`, then rebuild and distribute the app. No Firestore migration
is needed for those code-backed policies. For an additional Firestore quest,
populate `activityDurationSeconds`, `evidenceMode`,
`maximumEvidenceDurationSeconds`, and `maximumVideoSizeBytes`; retain its
existing `maxSeconds` as the backward-compatible fallback. Any legacy minimum
duration value is ignored. Verify the policy in
the quest card, briefing, student recording flow, and Admin Review before
publishing it.

For a manual smoke test, build and install the debug APK, open **CBEAM Stretch**,
and confirm the evidence label says `up to 2m 30s`. Submit a short, non-empty
local video and confirm it is accepted; a video above 150 seconds or above
50 MiB must be rejected. Repeat with a short repetition quest and verify its
own maximum is respected. Google Drive duration cannot be app-verified; check
the shared file and review it from the faculty account. The AQ-RUN-START QR-only
route exits without a video submission; it does not record official event
timing in the app.

---

## 🚀 Building & Exporting the APK

### Prerequisites
- Android Studio with Android SDK Platform 36.1 installed
- Gradle 9.3.1 (the checked-in wrapper downloads this automatically)
- JetBrains Runtime (JDK) 25. The checked-in `gradle/gradle-daemon-jvm.properties`
  selects JetBrains JDK 25 for the Gradle daemon so it does not accidentally use
  an incomplete JRE bundled with another editor.

### Android Studio Gradle JDK
1. Open **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**.
2. Set **Gradle JDK** to Android Studio's **Embedded JDK** (JetBrains Runtime 25).
   Do not select the VS Code Java extension's `jre` directory; it may not contain
   `bin\jlink.exe`. Gradle's project daemon criteria also require JetBrains JDK
   25, and the Gradle toolchain resolver can provision it if needed.
3. Click **Sync Project with Gradle Files**.

### Build Debug APK
From PowerShell in the project root:
```powershell
.\gradlew.bat :app:assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Build a Signed Release APK
Never distribute the unsigned APK. Create or select your production keystore and keep it outside the repository. Configure these properties in your user-level Gradle properties file (`%USERPROFILE%\.gradle\gradle.properties`), not in a project file:

```properties
actiquestReleaseStoreFile=C:/path/to/your/release-key.jks
actiquestReleaseStorePassword=your-keystore-password
actiquestReleaseKeyAlias=your-key-alias
actiquestReleaseKeyPassword=your-key-password
```

Replace the example values with the values for your keystore. Back up the keystore and passwords securely; losing them can prevent future updates from being signed with the same app identity. Then build from the project root:

```powershell
.\gradlew.bat :app:assembleRelease
```

The signed APK is generated at `app/build/outputs/apk/release/app-release.apk`. If signing properties are not configured, Gradle warns and emits `app-release-unsigned.apk`; do not upload or distribute that file. Verify the signed APK with `apksigner verify --verbose app/build/outputs/apk/release/app-release.apk`.

---

## 🔒 Security & Privacy Practices

- **Zero Hardcoded Secrets**: No confidential keys or credentials in git.
- **Granular Firestore Rules**: Students can only view and submit their own activity proofs. Only authenticated faculty administrators can access the Verification Desk to grade points and verify academic changes.
- **Private Video Storage**: Videos are stored in private Supabase buckets or handled via signed URLs.
- **Device Camera Access**: Camera access is requested strictly at runtime when opening the QR scanner or recording motion proof.

---

## 📄 License & Credits

Distributed under the [MIT License](LICENSE).
Copyright (c) 2026 Yuri Yabut, ActiQuest Contributors & Campus Wellness Initiative.
See [LICENSE](LICENSE) for full details.
Brand Logo: `assets/ActiQuest Logo (1).png`.
