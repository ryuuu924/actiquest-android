# ActiQuest Web (`actiquest-web`)

Official open-source web portal and approved-testers distribution system for the ActiQuest Android gamified fitness app.

---

## 100% Free Stack Architecture (Zero Credit Card Required)

- **Web Hosting**: **Vercel** (Free Hobby Tier — Unlimited deployments & CDN).
- **Authentication**: **Firebase Auth** (Free Spark Plan — 50,000 monthly active users included free).
- **APK Binary Hosting**: **GitHub Releases** (Free Unlimited Asset Storage & Bandwidth).
- **Access Control**: **Fail-closed whitelist enforcement** via `APPROVED_BETA_TESTERS_EMAILS` and the `approved_testers` Firestore collection. The Android signup check and APK download endpoint use the same server-side approval check.

---

## Deploying to Vercel (Step-by-Step)

### Step 1: Upload Your Beta APK to GitHub Releases
1. Build a **signed release APK** using the steps in the project-root [README](../README.md#build-a-signed-release-apk). Do not distribute `app-release-unsigned.apk`.
2. Verify it with `apksigner verify --verbose app/build/outputs/apk/release/app-release.apk`.
3. Go to your **`actiquest-android`** repository on GitHub.
4. Click **Releases** > **Draft a new release**.
5. Create a tag (e.g. `v1.2.0`).
6. Drag and drop the verified `app-release.apk` file into the release assets.
7. Click **Publish release**.
8. Copy the direct link to the uploaded `.apk` file (e.g. `https://github.com/YOUR_USERNAME/actiquest-android/releases/download/v1.2.0/app-release.apk`).

### Step 2: Push `actiquest-web` to GitHub
```bash
cd "C:/Users/Ms Nancy Yabut/AndroidStudioProjects/ActiQuest/web-portal"
git init
git add .
git commit -m "Initial commit of actiquest-web"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/actiquest-web.git
git push -u origin main
```

### Step 3: Deploy on Vercel
1. Log into [Vercel Console](https://vercel.com/) with your GitHub account.
2. Click **Add New** > **Project** and import `actiquest-web`.
3. Under **Environment Variables**, add:

| Variable | Value |
| :--- | :--- |
| `NEXT_PUBLIC_FIREBASE_API_KEY` | Your Firebase Web API Key |
| `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN` | `your_project_id.firebaseapp.com` |
| `NEXT_PUBLIC_FIREBASE_PROJECT_ID` | `your_project_id` |
| `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID` | Your Messaging Sender ID |
| `NEXT_PUBLIC_FIREBASE_APP_ID` | Your Firebase Web App ID |
| `FIREBASE_ADMIN_CLIENT_EMAIL` | `firebase-adminsdk-xxx@your_project.iam.gserviceaccount.com` |
| `FIREBASE_ADMIN_PRIVATE_KEY` | Your Firebase Admin Service Account Private Key string |
| `BETA_APK_DOWNLOAD_URL` | Direct GitHub Release APK URL |
| `APPROVED_BETA_TESTERS_EMAILS` | Comma-separated list of approved tester emails |

4. Click **Deploy**. Vercel will launch your portal at `https://actiquest-web.vercel.app`.

### Step 4: Add Approved Testers
1. Before rules deployment, add all approved accounts, including existing users who should keep access. In Firebase Console → **Firestore Database**, create `approved_testers` documents with the document ID set to the tester's lowercase email address (for example, `student@example.com`).
2. Set `approved: true` and `active: true`. A missing `approved` or `active` field defaults to true; set either field to `false` or delete the document to revoke access.
3. Alternatively, add lowercase addresses to `APPROVED_BETA_TESTERS_EMAILS` in Vercel. The API creates a matching approved Firestore document on the first successful check so Firestore rules and Android use the same allowlist. To revoke an environment-approved tester, remove the address from the Vercel variable and set/delete the Firestore document. The administrator addresses configured in the app/rules remain exempt.
4. Ensure the Firebase Admin service account environment variables are configured. They are used only by server API routes; never put the private key in Android or `NEXT_PUBLIC_*` variables.

### Step 5: Point Android Builds at the Deployed Portal
The Android app defaults to `https://actiquest-web.vercel.app/api/check-beta-access`. If your Vercel deployment uses another domain, pass its endpoint when building:

```powershell
.\gradlew.bat -PactiquestBetaAccessCheckUrl=https://your-portal.vercel.app/api/check-beta-access assembleRelease
```

The Android app checks approval before creating a password account and before continuing a Google sign-in. Firestore rules independently enforce approval for app data, so an unavailable approval service blocks signup rather than silently allowing it. The demo sign-in and local-only account fallback are available only in debug builds.

### Step 6: Publish Firestore Rules and Verify
1. Back up the current rules in Firebase Console.
2. Publish the project-root [`firestore.rules`](../firestore.rules), which combines verified-user/admin behavior, user-owned data access, notifications, and approved-beta checks.
3. Test an approved account, an unapproved address, and a revoked address. Only the approved account should create a student profile, read app data, or download the APK.
4. In the Android app, open Notifications and confirm the list remains stable while the screen recomposes and while new notifications arrive.
