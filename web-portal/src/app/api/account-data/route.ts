import { NextResponse } from 'next/server';
import { adminAuth, adminDb } from '@/lib/firebaseAdmin';

export const dynamic = 'force-dynamic';

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

    const [userDoc, submissionsSnap, notificationsSnap] = await Promise.all([
      adminDb.collection('users').doc(uid).get(),
      adminDb.collection('submissions').where('userId', '==', uid).get(),
      adminDb.collection('notifications').where('userId', '==', uid).get(),
    ]);

    const userData = userDoc.exists ? userDoc.data() : null;
    const submissions = submissionsSnap.docs.map((doc) => doc.data());
    const notifications = notificationsSnap.docs.map((doc) => doc.data());

    return NextResponse.json({
      exportDate: new Date().toISOString(),
      user: userData,
      submissions,
      notifications,
    });
  } catch (error: any) {
    console.error('Error exporting account data:', error);
    return NextResponse.json({ error: 'Failed to export account data.' }, { status: 500 });
  }
}

export async function DELETE(request: Request) {
  try {
    // 1. Check Query Parameters - Reject if any query parameter exists
    const url = new URL(request.url);
    if (url.searchParams.toString().length > 0) {
      return NextResponse.json(
        { error: 'Invalid request: Query parameters are forbidden for account deletion.' },
        { status: 400 }
      );
    }

    // 2. Check Authorization Header
    const authHeader = request.headers.get('authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json(
        { error: 'Unauthorized: Missing or invalid authentication token.' },
        { status: 401 }
      );
    }

    const idToken = authHeader.split('Bearer ')[1];

    // 3. Revocation-checked verified Firebase ID Token
    const decodedToken = await adminAuth.verifyIdToken(idToken, true);
    const uid = decodedToken.uid;

    // 4. Verify auth_time is within the last 10 minutes (Recent Login Required)
    const authTimeMs = (decodedToken.auth_time || 0) * 1000;
    const tenMinutesMs = 10 * 60 * 1000;
    if (Date.now() - authTimeMs > tenMinutesMs) {
      return NextResponse.json(
        { error: 'RECENT_LOGIN_REQUIRED', message: 'Recent authentication is required to delete your account.' },
        { status: 401 }
      );
    }

    // 5. Parse Body & Require Exact JSON Body `{"confirm":"DELETE"}`
    let body: any;
    try {
      body = await request.json();
    } catch {
      return NextResponse.json(
        { error: 'Invalid body: JSON payload expected.' },
        { status: 400 }
      );
    }

    const keys = Object.keys(body || {});
    if (keys.length !== 1 || body.confirm !== 'DELETE') {
      return NextResponse.json(
        { error: 'Invalid body: Body must strictly be {"confirm":"DELETE"} with no extra fields.' },
        { status: 400 }
      );
    }

    // 6. Delete User Documents & Storage Records in Firestore
    const batch = adminDb.batch();

    // Delete user profile
    batch.delete(adminDb.collection('users').doc(uid));

    // Delete submissions
    const submissionsSnap = await adminDb.collection('submissions').where('userId', '==', uid).get();
    submissionsSnap.docs.forEach((doc) => batch.delete(doc.ref));

    // Delete notifications
    const notificationsSnap = await adminDb.collection('notifications').where('userId', '==', uid).get();
    notificationsSnap.docs.forEach((doc) => batch.delete(doc.ref));

    await batch.commit();

    // 7. Delete User from Firebase Authentication
    await adminAuth.deleteUser(uid);

    return NextResponse.json({
      success: true,
      deletedUid: uid,
      message: 'Account and associated records successfully deleted.',
    });
  } catch (error: any) {
    console.error('Error during account deletion:', error);
    if (error.code === 'auth/id-token-revoked') {
      return NextResponse.json(
        { error: 'RECENT_LOGIN_REQUIRED', message: 'ID token was revoked. Please log in again.' },
        { status: 401 }
      );
    }
    return NextResponse.json(
      { error: 'Account deletion failed.', details: error.message },
      { status: 500 }
    );
  }
}
