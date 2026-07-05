"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { navFor, type Role } from "@/lib/rbac";

const ROLE_LABEL: Record<Role, string> = {
  ADMIN: "Admin",
  DOCTOR: "Doctor",
  NURSE: "Nurse",
  LAB_TECH: "Lab Tech",
};

export default function Navbar({ role }: { role: Role | null }) {
  const pathname = usePathname();
  const router = useRouter();
  const links = navFor(role);

  async function handleLogout() {
    await fetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  }

  return (
    <nav className="bg-slate-800 text-white px-6 py-3 flex items-center gap-6">
      <Link href="/" className="font-bold text-lg tracking-tight mr-4">
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
      <div className="ml-auto flex items-center gap-4">
        {role && (
          <span className="text-xs px-2 py-0.5 rounded-full bg-slate-700 text-slate-200">
            {ROLE_LABEL[role]}
          </span>
        )}
        <button
          onClick={handleLogout}
          className="text-sm text-slate-400 hover:text-white transition-colors"
        >
          Logout
        </button>
      </div>
    </nav>
  );
}
