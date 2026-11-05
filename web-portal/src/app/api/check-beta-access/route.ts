import { NextResponse } from 'next/server';
import { adminAuth } from '@/lib/firebaseAdmin';
import { isApprovedBetaEmail, isBetaCheckRateLimited } from '@/lib/betaAccess';

export const dynamic = 'force-dynamic';

export async function POST(request: Request) {
  if (isBetaCheckRateLimited(request)) {
    return NextResponse.json(
      { error: 'Too many access checks. Please wait a minute and try again.' },
      { status: 429, headers: { 'Cache-Control': 'no-store' } }
    );
  }

  try {
    const body: unknown = await request.json();
    if (!body || typeof body !== 'object') {
      return NextResponse.json({ error: 'Invalid request.' }, { status: 400 });
    }

    const { email, idToken } = body as { email?: unknown; idToken?: unknown };
    if (typeof email !== 'string' || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      return NextResponse.json({ error: 'Enter a valid email address.' }, { status: 400 });
    }

    let uid: string | undefined;
    if (typeof idToken === 'string' && idToken.length > 0) {
      const decodedToken = await adminAuth.verifyIdToken(idToken);
      if (decodedToken.email?.toLowerCase() !== email.trim().toLowerCase()) {
        return NextResponse.json({ error: 'Account email mismatch.' }, { status: 403 });
      }
      uid = decodedToken.uid;
    }

    const approved = await isApprovedBetaEmail(email, uid);
    return NextResponse.json(
      { approved },
      { headers: { 'Cache-Control': 'no-store' } }
    );
  } catch (error) {
    console.error('Beta access verification failed:', error);
    return NextResponse.json(
      { error: 'Unable to verify beta access right now.' },
      { status: 503, headers: { 'Cache-Control': 'no-store' } }
    );
  }
}
