import Link from 'next/link';
import { ShieldCheck, Download, Award, Flame, Users, ArrowRight, Smartphone, Lock } from 'lucide-react';

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 flex flex-col justify-between">
      {/* Navigation Header */}
      <header className="border-b border-slate-800 bg-[#0B0F19]/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-emerald-500 flex items-center justify-center font-black text-xl shadow-lg shadow-blue-500/20">
              AQ
            </div>
            <span className="font-bold text-xl tracking-tight bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent">
              ActiQuest
            </span>
          </div>

          <nav className="flex items-center gap-6 text-sm font-semibold">
            <Link href="#features" className="hover:text-blue-400 transition-colors hidden sm:block">
              Features
            </Link>
            <Link href="#adoption" className="hover:text-blue-400 transition-colors hidden sm:block">
              University Adoption
            </Link>
            <Link
              href="/download"
              className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-xl flex items-center gap-2 shadow-md shadow-blue-600/30 transition-all hover:scale-105"
            >
              <Download className="w-4 h-4" />
              Download Beta APK
            </Link>
          </nav>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative overflow-hidden py-24 px-6 max-w-6xl mx-auto text-center">
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-bold mb-8">
          <ShieldCheck className="w-4 h-4" /> Verified Institutional Beta Release v1.2.0
        </div>

        <h1 className="text-4xl sm:text-6xl font-black tracking-tight leading-tight max-w-4xl mx-auto mb-6 bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
          Gamified Campus Fitness & Verification Platform for Universities
        </h1>

        <p className="text-lg sm:text-xl text-slate-400 max-w-2xl mx-auto mb-10 leading-relaxed">
          Transform campus wellness with real-time QR video quests, pedometer verification, institutional class leaderboards, and Philippine Time streak integrity.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link
            href="/download"
            className="w-full sm:w-auto px-8 py-4 bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-500 hover:to-emerald-400 text-white font-bold rounded-2xl flex items-center justify-center gap-3 shadow-xl shadow-blue-600/25 transition-all hover:scale-105"
          >
            <Download className="w-5 h-5" /> Get Beta APK for Android
          </Link>
          <a
            href="https://github.com"
            target="_blank"
            rel="noopener noreferrer"
            className="w-full sm:w-auto px-8 py-4 bg-slate-800/80 hover:bg-slate-700 text-slate-200 border border-slate-700 font-bold rounded-2xl flex items-center justify-center gap-2 transition-all"
          >
            Explore Open Source Codebase <ArrowRight className="w-4 h-4" />
          </a>
        </div>
      </section>

      {/* Feature Grid */}
      <section id="features" className="py-20 px-6 max-w-6xl mx-auto border-t border-slate-800/60">
        <div className="text-center mb-16">
          <h2 className="text-3xl font-extrabold text-white mb-3">Enterprise-Grade Campus Fitness</h2>
          <p className="text-slate-400">Built natively in Kotlin & Jetpack Compose for Android 7.0+</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="p-8 rounded-2xl bg-slate-900/60 border border-slate-800 hover:border-blue-500/40 transition-all">
            <div className="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center mb-6">
              <Smartphone className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">QR Video Quests</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Campus checkpoints with automated evidence duration validation, local in-app replay, and Google Drive proof backup.
            </p>
          </div>

          <div className="p-8 rounded-2xl bg-slate-900/60 border border-slate-800 hover:border-emerald-500/40 transition-all">
            <div className="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-6">
              <Award className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Class Leaderboards</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Real-time Firestore class standings per college and degree program, complete with partial points approval and verification desks.
            </p>
          </div>

          <div className="p-8 rounded-2xl bg-slate-900/60 border border-slate-800 hover:border-purple-500/40 transition-all">
            <div className="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-400 flex items-center justify-center mb-6">
              <Flame className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">Philippine Time Streaks</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Strict timezone-aware streak tracking (Asia/Manila) with zero false baseline fallbacks and immutable server timestamps.
            </p>
          </div>
        </div>
      </section>

      {/* Adoption Section */}
      <section id="adoption" className="py-20 px-6 max-w-6xl mx-auto border-t border-slate-800/60">
        <div className="p-10 rounded-3xl bg-gradient-to-br from-blue-900/30 to-purple-900/30 border border-blue-500/30 text-center md:text-left flex flex-col md:flex-row items-center justify-between gap-8">
          <div>
            <h2 className="text-2xl font-bold text-white mb-2">Ready to Adopt ActiQuest for Your School?</h2>
            <p className="text-slate-300 max-w-xl text-sm">
              We provide open-source white-label configurations, dynamic college catalogs, and isolated multi-tenant Firestore security rules for university IT departments.
            </p>
          </div>
          <a
            href="mailto:yuriyabut924@gmail.com"
            className="px-6 py-3 bg-white text-slate-900 font-bold rounded-xl whitespace-nowrap hover:bg-slate-100 transition-colors shadow-lg"
          >
            Contact Integration Team
          </a>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-8 px-6 text-center text-slate-500 text-sm">
        <p>© 2026 ActiQuest. Open-Source Campus Gamified Fitness Project. Distributed under Apache 2.0 License.</p>
      </footer>
    </div>
  );
}
