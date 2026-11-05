import { adminAuth, adminDb } from '@/lib/firebaseAdmin';

const ADMIN_EMAILS = new Set([
  'actiquest.admin@gmail.com',
  'new_admin@test.com',
  'real_admin@test.com',
]);

const rateLimit = new Map<string, { count: number; resetAt: number }>();

export async function isApprovedBetaEmail(email: string, uid?: string): Promise<boolean> {
  const normalizedEmail = email.trim().toLowerCase();
  if (!normalizedEmail) return false;

  if (ADMIN_EMAILS.has(normalizedEmail)) return true;

  const approvedEmails = (process.env.APPROVED_BETA_TESTERS_EMAILS || '')
    .split(',')
    .map((value) => value.trim().toLowerCase())
    .filter(Boolean);

  const tester = await adminDb.collection('approved_testers').doc(normalizedEmail).get();
  if (tester.exists) {
    const testerData = tester.data();
    return testerData?.approved !== false && testerData?.active !== false;
  }

  if (approvedEmails.includes(normalizedEmail)) {
    await adminDb.collection('approved_testers').doc(normalizedEmail).set({
      email: normalizedEmail,
      approved: true,
      active: true,
      source: 'environment',
      updatedAt: new Date(),
    });
    return true;
  }

  let resolvedUid = uid;
  if (!resolvedUid) {
    try {
      resolvedUid = (await adminAuth.getUserByEmail(normalizedEmail)).uid;
    } catch (error) {
      if (
        error instanceof Error &&
        'code' in error &&
        error.code === 'auth/user-not-found'
      ) {
        return false;
      }
      throw error;
    }
  }

  const [adminByEmail, adminByUid, user] = await Promise.all([
    adminDb.collection('admins').doc(normalizedEmail).get(),
    adminDb.collection('admins').doc(resolvedUid).get(),
    adminDb.collection('users').doc(resolvedUid).get(),
  ]);
  return adminByEmail.exists || adminByUid.exists || user.get('role') === 'admin';
}

export function isBetaCheckRateLimited(request: Request): boolean {
  const ip = request.headers.get('x-forwarded-for')?.split(',')[0]?.trim() || 'unknown';
  const now = Date.now();
  const current = rateLimit.get(ip);

  if (!current || now >= current.resetAt) {
    rateLimit.set(ip, { count: 1, resetAt: now + 60_000 });
    return false;
  }

  current.count += 1;
  return current.count > 10;
}
