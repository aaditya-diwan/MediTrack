"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";

const links = [
  { href: "/patients", label: "Patients" },
  { href: "/lab/orders", label: "Lab Orders" },
  { href: "/lab/results/critical", label: "Critical Results" },
  { href: "/insurance", label: "Insurance" },
];

export default function Navbar() {
  const pathname = usePathname();
  const router = useRouter();

  async function handleLogout() {
    await fetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  }

  return (
    <nav className="bg-slate-800 text-white px-6 py-3 flex items-center gap-6">
      <Link href="/patients" className="font-bold text-lg tracking-tight mr-4">
        MediTrack
      </Link>
      {links.map((l) => (
        <Link
          key={l.href}
          href={l.href}
          className={`text-sm hover:text-slate-200 transition-colors ${
            pathname.startsWith(l.href) ? "text-white font-medium" : "text-slate-400"
          }`}
        >
          {l.label}
        </Link>
      ))}
      <button
        onClick={handleLogout}
        className="ml-auto text-sm text-slate-400 hover:text-white transition-colors"
      >
        Logout
      </button>
    </nav>
  );
}
