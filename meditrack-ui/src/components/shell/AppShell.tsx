"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  CalendarDays,
  ClipboardList,
  FlaskConical,
  LayoutDashboard,
  LogOut,
  Menu,
  Pill,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  Users,
  X,
  type LucideIcon,
} from "lucide-react";
import { navFor, type Role } from "@/lib/rbac";
import { EcgTrace } from "@/components/ui/Ecg";
import { ThemeToggle } from "./ThemeToggle";

const ROLE_LABEL: Record<Role, string> = {
  ADMIN: "Admin",
  DOCTOR: "Doctor",
  NURSE: "Nurse",
  LAB_TECH: "Lab tech",
};

const NAV_ICONS: Record<string, LucideIcon> = {
  "/dashboard": LayoutDashboard,
  "/patients": Users,
  "/doctors": Stethoscope,
  "/appointments": CalendarDays,
  "/doctor-dashboard": ClipboardList,
  "/prescriptions": Pill,
  "/lab/orders": FlaskConical,
  "/ai": Sparkles,
  "/insurance": ShieldCheck,
};

function NavLinks({
  role,
  onNavigate,
}: {
  role: Role | null;
  onNavigate?: () => void;
}) {
  const pathname = usePathname();
  return (
    <nav aria-label="Main" className="flex-1 space-y-0.5 px-3">
      {navFor(role).map((item) => {
        const Icon = NAV_ICONS[item.href] ?? ClipboardList;
        const active =
          pathname === item.href || pathname.startsWith(item.href + "/");
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            aria-current={active ? "page" : undefined}
            className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors ${
              active
                ? "bg-brand-tint font-medium text-brand-ink"
                : "text-ink-muted hover:bg-card-2 hover:text-ink"
            }`}
          >
            <Icon className="size-4 shrink-0" aria-hidden />
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}

function Brand() {
  return (
    <Link
      href="/"
      className="flex items-center gap-2 px-6 py-5 font-display text-lg font-semibold tracking-tight text-ink"
    >
      <EcgTrace className="h-5 w-12 text-brand" />
      MediTrack
    </Link>
  );
}

export function AppShell({
  role,
  children,
}: {
  role: Role | null;
  children: React.ReactNode;
}) {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const router = useRouter();

  async function handleLogout() {
    await fetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  }

  const sidebarBody = (
    <>
      <Brand />
      <NavLinks role={role} onNavigate={() => setDrawerOpen(false)} />
      <div className="border-t border-line px-6 py-4">
        {role && (
          <p className="text-xs text-ink-faint">
            Signed in as{" "}
            <span className="font-medium text-ink-muted">{ROLE_LABEL[role]}</span>
          </p>
        )}
      </div>
    </>
  );

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[240px_1fr]">
      {/* Desktop sidebar */}
      <aside className="sticky top-0 hidden h-screen flex-col border-r border-line bg-card lg:flex">
        {sidebarBody}
      </aside>

      {/* Mobile drawer */}
      {drawerOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button
            aria-label="Close menu"
            className="absolute inset-0 bg-ink/40"
            onClick={() => setDrawerOpen(false)}
          />
          <div className="absolute inset-y-0 left-0 flex w-64 flex-col border-r border-line bg-card shadow-xl">
            <button
              onClick={() => setDrawerOpen(false)}
              aria-label="Close menu"
              className="absolute right-3 top-4 rounded p-1.5 text-ink-muted hover:text-ink"
            >
              <X className="size-4" aria-hidden />
            </button>
            {sidebarBody}
          </div>
        </div>
      )}

      <div className="flex min-h-screen flex-col">
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-line bg-page/90 px-4 py-2.5 backdrop-blur sm:px-6">
          <button
            onClick={() => setDrawerOpen(true)}
            aria-label="Open menu"
            className="rounded-lg p-2 text-ink-muted hover:bg-card-2 hover:text-ink lg:hidden"
          >
            <Menu className="size-4" aria-hidden />
          </button>
          <div className="ml-auto flex items-center gap-1">
            <ThemeToggle />
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-ink-muted transition-colors hover:bg-card-2 hover:text-ink"
            >
              <LogOut className="size-4" aria-hidden />
              Log out
            </button>
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">
          {children}
        </main>
      </div>
    </div>
  );
}
