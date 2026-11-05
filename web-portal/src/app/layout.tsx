import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'ActiQuest — University Gamified Fitness Portal',
  description: 'Official authenticated distribution portal for ActiQuest Android app beta releases, developer tools, and campus adoption.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="bg-[#0B0F19] text-white antialiased">
        {children}
      </body>
    </html>
  );
}
