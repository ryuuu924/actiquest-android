import { NextResponse } from 'next/server';
import { adminAuth, adminDb } from '@/lib/firebaseAdmin';

export const dynamic = 'force-dynamic';

const CANONICAL_TERMS_VERSION = 'v1.2.0-beta';
const CANONICAL_PRIVACY_VERSION = 'v1.2.0-beta';

export async function GET(request: Request) {
  try {
    const authHeader = request.headers.get('authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json(
        { error: 'Unauthorized: Missing or invalid authentication token.' },
        { status: 401 }
      );
    }

    const idToken = authHeader.split('Bearer ')[1];
    const decodedToken = await adminAuth.verifyIdToken(idToken, true);
    const uid = decodedToken.uid;

    const consentDoc = await adminDb.collection('users').doc(uid).collection('consent').doc('current').get();
    if (!consentDoc.exists) {
      return NextResponse.json({
        accepted: false,
        isAdult: false,
        termsVersion: CANONICAL_TERMS_VERSION,
        privacyVersion: CANONICAL_PRIVACY_VERSION,
      });
    }

    return NextResponse.json(consentDoc.data());
  } catch (error: any) {
    console.error('Error fetching beta consent:', error);
    return NextResponse.json({ error: 'Failed to verify consent status.' }, { status: 401 });
  }
}

export async function POST(request: Request) {
  try {
    const authHeader = request.headers.get('authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json(
        { error: 'Unauthorized: Missing or invalid authentication token.' },
        { status: 401 }
      );
    }

    const idToken = authHeader.split('Bearer ')[1];
    const decodedToken = await adminAuth.verifyIdToken(idToken, true);
    const uid = decodedToken.uid;

    const body = await request.json();
    const { termsVersion, privacyVersion, accepted, isAdult } = body || {};

    if (!accepted) {
      return NextResponse.json({ error: 'Policy acceptance is required.' }, { status: 400 });
    }

    if (!isAdult) {
      return NextResponse.json(
        { error: 'Self-activation is restricted to adults (18+). Minors require verified guardian consent.' },
        { status: 403 }
      );
    }

    const consentRecord = {
      uid,
      email: decodedToken.email || '',
      accepted: true,
      isAdult: true,
      termsVersion: termsVersion || CANONICAL_TERMS_VERSION,
      privacyVersion: privacyVersion || CANONICAL_PRIVACY_VERSION,
      consentTimestamp: new Date().toISOString(),
    };

    await adminDb.collection('users').doc(uid).collection('consent').doc('current').set(consentRecord);
    await adminDb.collection('users').doc(uid).set(
      {
        consentGiven: true,
        termsVersion: consentRecord.termsVersion,
        privacyVersion: consentRecord.privacyVersion,
        consentTimestamp: consentRecord.consentTimestamp,
      },
      { merge: true }
    );

    return NextResponse.json({ success: true, consent: consentRecord });
  } catch (error: any) {
    console.error('Error recording beta consent:', error);
    return NextResponse.json({ error: 'Failed to record policy consent.' }, { status: 500 });
  }
}
