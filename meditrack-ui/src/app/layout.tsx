import type { Metadata } from "next";
import { Geist, Geist_Mono, Bricolage_Grotesque } from "next/font/google";
import { Toaster } from "sonner";
import "./globals.css";

const geist = Geist({ subsets: ["latin"], variable: "--font-geist" });
const geistMono = Geist_Mono({ subsets: ["latin"], variable: "--font-geist-mono" });
const display = Bricolage_Grotesque({
  subsets: ["latin"],
  variable: "--font-display",
});

export const metadata: Metadata = {
  title: "MediTrack",
  description: "Hospital operations platform",
};

/**
 * Resolves the theme before first paint: explicit choice from localStorage,
 * otherwise the OS preference. Inline so there is no flash of wrong theme.
 */
const themeInit = `(function(){try{var t=localStorage.getItem("mt-theme");if(t!=="light"&&t!=="dark"){t=window.matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light";}document.documentElement.setAttribute("data-theme",t);}catch(e){document.documentElement.setAttribute("data-theme","light");}})();`;

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="en"
      className={`${geist.variable} ${geistMono.variable} ${display.variable} h-full`}
      suppressHydrationWarning
    >
      <body className="min-h-full bg-page font-sans text-ink antialiased">
        <script dangerouslySetInnerHTML={{ __html: themeInit }} />
        {children}
        <Toaster richColors position="top-right" />
      </body>
    </html>
  );
}
