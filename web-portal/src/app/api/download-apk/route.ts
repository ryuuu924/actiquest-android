import { NextResponse } from 'next/server';
import { adminAuth } from '@/lib/firebaseAdmin';
import { isApprovedBetaEmail } from '@/lib/betaAccess';

export const dynamic = 'force-dynamic';

// Simple in-memory rate limiter per IP (5 requests per minute)
const rateLimitMap = new Map<string, { count: number; lastReset: number }>();

function isRateLimited(ip: string): boolean {
  const now = Date.now();
  const windowMs = 60 * 1000; // 1 minute
  const limit = 5;

  const record = rateLimitMap.get(ip) || { count: 0, lastReset: now };
  if (now - record.lastReset > windowMs) {
    record.count = 1;
    record.lastReset = now;
  } else {
    record.count += 1;
  }
  rateLimitMap.set(ip, record);

  return record.count > limit;
}

export async function GET(request: Request) {
  try {
    // 1. Bot & Rate Limit Check
    const clientIp = request.headers.get('x-forwarded-for') || 'unknown-ip';
    if (isRateLimited(clientIp)) {
      return NextResponse.json(
        { error: 'Rate limit exceeded: Too many download requests. Please wait a minute.' },
        { status: 429 }
      );
    }

    // 2. Authentication Token Check
    const authHeader = request.headers.get('authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json(
        { error: 'Unauthorized: Missing or invalid authentication token.' },
        { status: 401 }
      );
    }

    const idToken = authHeader.split('Bearer ')[1];
    const decodedToken = await adminAuth.verifyIdToken(idToken);
    const userEmail = decodedToken.email?.toLowerCase();

    if (!userEmail || !decodedToken.email_verified) {
      return NextResponse.json(
        { error: 'Forbidden: Your Google account email must be verified.' },
        { status: 403 }
      );
    }

    // 3. Enforce the same tester list used by Android account registration.
    if (!(await isApprovedBetaEmail(userEmail, decodedToken.uid))) {
      return NextResponse.json(
        {
          error: 'Access denied. Contact the ActiQuest administrator to request beta access.',
        },
        { status: 403 }
      );
    }

    // 4. Return Free Hosted GitHub Release APK URL
    const apkUrl = process.env.BETA_APK_DOWNLOAD_URL || process.env.GITHUB_RELEASE_APK_URL;

    if (!apkUrl) {
      return NextResponse.json(
        { error: 'Beta APK URL is not configured. Please set BETA_APK_DOWNLOAD_URL in environment variables.' },
        { status: 500 }
      );
    }

    return NextResponse.json({
      success: true,
      downloadUrl: apkUrl,
      expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
      user: {
        email: decodedToken.email,
        uid: decodedToken.uid,
      },
    });
  } catch (error: any) {
    console.error('API Error verifying token:', error);
    return NextResponse.json(
      { error: 'Authentication failed or user session expired.', details: error.message },
      { status: 401 }
    );
  }
}
