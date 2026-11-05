# Security Policy & Vulnerability Disclosure

Security is a fundamental priority for ActiQuest, especially as an open-source campus verification platform handling student activity records and leaderboards.

---

## 1. Supported Versions

| Version | Supported | Security Updates |
| :--- | :--- | :--- |
| `v1.2.x` (Latest Beta/Release) | Yes | Active security patches |
| `v1.1.x` | Yes | Critical security patches only |
| `< v1.1.0` | No | Upgrade to `v1.2.0` |

---

## 2. Reporting a Vulnerability

If you discover a security vulnerability, API key exposure, or Firestore security rules loophole in ActiQuest, **please do not open a public GitHub issue**.

### How to Report
Send a private email to our security team:
- **Email**: `yuriyabut924@gmail.com`
- **Subject Line**: `[SECURITY VULNERABILITY] <Brief Description>`

### Information to Include
- Detailed description of the vulnerability.
- Steps to reproduce the issue (including sample payload or curl request if applicable).
- Affected component (Android App, Firestore Rules, or Web Portal API).
- Potential impact on student privacy or database integrity.

### Disclosure Policy
- We will acknowledge receipt of your report within **24 hours**.
- We aim to issue a fix or security patch within **5 business days**.
- We request that you refrain from publicly disclosing the vulnerability until a patch has been published.

---

## 3. Core Security Design Standards

ActiQuest adheres to strict security guardrails:
1. **Zero Hardcoded Secrets**: All private keys, service account credentials, and keystore passwords are stored in environment variables or `local.properties`.
2. **Double-Gated Identity Enforcement**: Unapproved users cannot create or read user documents in Firestore even if they possess a forwarded `.apk` installer file.
3. **Immutable Timestamps**: Submissions strictly enforce `submittedAt == request.time` on creation via `firestore.rules`.
4. **Rate Limiting**: Next.js API routes enforce IP rate-limiting to block brute-force scripts and automated scraping.
