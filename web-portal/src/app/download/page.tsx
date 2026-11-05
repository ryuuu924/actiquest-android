'use client';

import { useState, useEffect } from 'react';
import { auth, googleProvider } from '@/lib/firebase';
import { signInWithPopup, signOut, onAuthStateChanged, User as FirebaseUser } from 'firebase/auth';
import { Download, ShieldCheck, LogOut, Lock, AlertCircle, CheckCircle2 } from 'lucide-react';
import Link from 'next/link';

export default function DownloadPage() {
  const [user, setUser] = useState<FirebaseUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [downloadSuccess, setDownloadSuccess] = useState(false);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  const handleGoogleSignIn = async () => {
    try {
      setErrorMessage(null);
      await signInWithPopup(auth, googleProvider);
    } catch (err: any) {
      setErrorMessage(err.message || 'Google Sign-In failed.');
    }
  };

  const handleDownloadAPK = async () => {
    if (!user) return;
    try {
      setDownloading(true);
      setErrorMessage(null);
      setDownloadSuccess(false);

      const idToken = await user.getIdToken();
      const res = await fetch('/api/download-apk', {
        headers: {
          Authorization: `Bearer ${idToken}`,
        },
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'Failed to fetch secure download link.');
      }

      // Initiate download
      window.location.href = data.downloadUrl;
      setDownloadSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.message || 'Download error occurred. Please verify your account.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 flex flex-col justify-between">
      {/* Top Header */}
      <header className="border-b border-slate-800 bg-[#0B0F19]/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-4xl mx-auto px-6 h-16 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-emerald-500 flex items-center justify-center font-black text-lg">
              AQ
            </div>
            <span className="font-bold text-lg">ActiQuest Portal</span>
          </Link>

          {user && (
            <button
              onClick={() => signOut(auth)}
              className="text-xs font-semibold text-slate-400 hover:text-white flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-800 hover:bg-slate-800 transition-colors"
            >
              <LogOut className="w-3.5 h-3.5" /> Sign Out
            </button>
          )}
        </div>
      </header>

      {/* Main Auth & Download Card */}
      <main className="max-w-xl mx-auto px-6 py-16 w-full flex-1 flex flex-col justify-center">
        <div className="bg-slate-900/80 border border-slate-800 rounded-3xl p-8 shadow-2xl backdrop-blur-xl">
          <div className="text-center mb-8">
            <div className="w-16 h-16 rounded-2xl bg-blue-500/10 border border-blue-500/20 text-blue-400 flex items-center justify-center mx-auto mb-4">
              <ShieldCheck className="w-8 h-8" />
            </div>
            <h1 className="text-2xl font-bold text-white mb-2">Authenticated Beta Download</h1>
            <p className="text-sm text-slate-400">
              Sign in with your verified institutional Google Account to access the latest signed Android APK build.
            </p>
          </div>

          {errorMessage && (
            <div className="mb-6 p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center gap-3">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {downloadSuccess && (
            <div className="mb-6 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs flex items-center gap-3">
              <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
              <span>Download started! Check your browser downloads for <strong>actiquest-beta-latest.apk</strong>.</span>
            </div>
          )}

          {loading ? (
            <div className="text-center py-8 text-sm text-slate-500 animate-pulse">
              Verifying security session...
            </div>
          ) : !user ? (
            <div className="space-y-4">
              <button
                onClick={handleGoogleSignIn}
                className="w-full py-3.5 px-6 rounded-2xl bg-white hover:bg-slate-100 text-slate-900 font-bold text-sm flex items-center justify-center gap-3 shadow-lg transition-all hover:scale-[1.02]"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24">
                  <path
                    fill="#4285F4"
                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  />
                  <path
                    fill="#34A853"
                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                  />
                  <path
                    fill="#EA4335"
                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                  />
                </svg>
                Sign in with Institutional Google Account
              </button>
              <p className="text-[11px] text-center text-slate-500">
                Only verified accounts can generate secure APK download links.
              </p>
            </div>
          ) : (
            <div className="space-y-6">
              <div className="p-4 rounded-2xl bg-slate-800/50 border border-slate-700/60 flex items-center justify-between">
                <div>
                  <p className="text-xs text-slate-400">Authenticated User</p>
                  <p className="text-sm font-bold text-white">{user.email}</p>
                </div>
                <span className="px-2.5 py-1 rounded-md bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 text-[10px] font-bold">
                  VERIFIED
                </span>
              </div>

              <button
                onClick={handleDownloadAPK}
                disabled={downloading}
                className="w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-500 hover:to-emerald-400 text-white font-bold text-sm flex items-center justify-center gap-3 shadow-xl shadow-blue-600/25 transition-all hover:scale-[1.02] disabled:opacity-50"
              >
                {downloading ? (
                  <span className="animate-pulse">Generating Secure Download Link...</span>
                ) : (
                  <>
                    <Download className="w-5 h-5" /> Download ActiQuest Beta APK (v1.2.0)
                  </>
                )}
              </button>

              <div className="text-[11px] text-slate-400 space-y-1 bg-slate-900 p-3 rounded-xl border border-slate-800">
                <p>• <strong>File:</strong> <code>ActiQuest-Beta-v1.2.0.apk</code> (~28 MB)</p>
                <p>• <strong>Requirements:</strong> Android 7.0 (API 24) or higher</p>
                <p>• <strong>Security:</strong> Link expires automatically in 15 minutes</p>
              </div>
            </div>
          )}
        </div>
      </main>

      <footer className="py-6 text-center text-xs text-slate-600">
        © 2026 ActiQuest. Authenticated Distribution Portal.
      </footer>
    </div>
  );
}
