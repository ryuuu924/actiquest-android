# ActiQuest Security Policy & Vulnerability Disclosure

Security and data integrity are fundamental priorities for ActiQuest, especially as a campus wellness platform handling student activity verification and class standings.

---

## 1. Supported Versions

| Version | Supported | Security Updates |
| :--- | :--- | :--- |
| `v1.2.0-beta` (Current) | Yes | Active security patches |
| `v1.1.x` | No | Upgrade to `v1.2.0-beta` |

---

## 2. Reporting a Vulnerability

If you discover a security vulnerability, API key exposure, or Firestore security rules loophole, **please do not open a public GitHub issue**.

### Reporting Process
Send a private email to the security team:
- **Email**: `actiquest.admin@gmail.com`
- **Subject Line**: `[SECURITY VULNERABILITY] <Brief Description>`

Include steps to reproduce, affected endpoints, and potential impact. Reports will be acknowledged within 24 hours.

---

## 3. Core Security Safeguards & Laws

1. **RA 10175 Compliance & Fair Play**: Prohibits modified APKs, GPS/sensor/motion spoofing, QR tampering, account impersonation, access-control bypass, and unauthorized backend access/interference.
2. **Double-Gated Identity Enforcement**: Unapproved users cannot create or read user documents in Firestore even if they possess a forwarded `.apk` installer file.
3. **Immutable Timestamps**: Submissions strictly enforce `submittedAt == request.time` on creation via `firestore.rules`.
4. **Rate Limiting**: Next.js API routes enforce IP rate-limiting (max 5 requests/min) and Android submission rate-limiting (max 10 submissions/hour).
5. **Account Deletion Protocol**: `DELETE /api/account-data` requires exact body `{"confirm":"DELETE"}`, derives target UID only from a revocation-checked verified Firebase ID token (`verifyIdToken(idToken, true)`), and requires `auth_time` within the last 10 minutes.
