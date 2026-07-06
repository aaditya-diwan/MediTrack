// --- Role-based access control (RBAC) ---
//
// IMPORTANT: This is a UI-convenience layer only. It controls which nav links
// and pages a logged-in staff member sees, and where they land after login.
// It is NOT a security boundary. The real authorization must be enforced
// server-side by each backend service when it validates the JWT. A user can
// tamper with their own browser/token; only the services can verify the
// signature. Treat everything here as cosmetic gating.

export type Role = "ADMIN" | "DOCTOR" | "NURSE" | "LAB_TECH";

export const ALL_ROLES: Role[] = ["ADMIN", "DOCTOR", "NURSE", "LAB_TECH"];

export interface NavItem {
  href: string;
  label: string;
  /** Roles allowed to see and access this section. */
  roles: Role[];
}

// Access model: admin-vs-clinician.
// ADMIN sees everything. All clinical roles share the full clinical set
// except Insurance, which is ADMIN-only.
const CLINICAL: Role[] = ["ADMIN", "DOCTOR", "NURSE", "LAB_TECH"];

export const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Overview", roles: CLINICAL },
  { href: "/patients", label: "Patients", roles: CLINICAL },
  { href: "/doctors", label: "Doctors", roles: CLINICAL },
  { href: "/appointments", label: "Appointments", roles: CLINICAL },
  { href: "/doctor-dashboard", label: "Doctor Dashboard", roles: CLINICAL },
  { href: "/prescriptions", label: "Prescriptions", roles: CLINICAL },
  { href: "/lab/orders", label: "Lab Orders", roles: CLINICAL },
  { href: "/ai", label: "AI CDSS", roles: CLINICAL },
  { href: "/insurance", label: "Insurance", roles: ["ADMIN"] },
];

/** Where each role is sent after login and from the app root. */
export const LANDING: Record<Role, string> = {
  ADMIN: "/dashboard",
  DOCTOR: "/doctor-dashboard",
  NURSE: "/dashboard",
  LAB_TECH: "/lab/orders",
};

export function landingFor(role: Role | null): string {
  return role ? LANDING[role] : "/dashboard";
}

/** Nav links visible to the given role (unknown role => empty). */
export function navFor(role: Role | null): NavItem[] {
  if (!role) return [];
  return NAV_ITEMS.filter((i) => i.roles.includes(role));
}

/**
 * Whether a role may access a pathname. Matches the most specific nav item
 * by href prefix; paths under no guarded section are allowed for any role.
 */
export function canAccess(role: Role, pathname: string): boolean {
  const match = NAV_ITEMS
    .filter((i) => pathname === i.href || pathname.startsWith(i.href + "/"))
    .sort((a, b) => b.href.length - a.href.length)[0];
  return match ? match.roles.includes(role) : true;
}

/** Map a JWT authority string (e.g. "ROLE_ADMIN") to a Role, or null. */
export function primaryRole(roles: string[]): Role | null {
  for (const raw of roles) {
    const short = raw.startsWith("ROLE_") ? raw.slice(5) : raw;
    if ((ALL_ROLES as string[]).includes(short)) return short as Role;
  }
  return null;
}

/**
 * Read the role out of a JWT WITHOUT verifying its signature. Verification is
 * the backend services' job; here we only need the claim to drive the UI.
 */
export function roleFromJwt(jwt: string | undefined | null): Role | null {
  if (!jwt) return null;
  try {
    const payload = jwt.split(".")[1];
    if (!payload) return null;
    const json = Buffer.from(payload, "base64url").toString("utf8");
    const claims = JSON.parse(json) as { roles?: unknown };
    const roles = Array.isArray(claims.roles)
      ? claims.roles.filter((x): x is string => typeof x === "string")
      : [];
    return primaryRole(roles);
  } catch {
    return null;
  }
}
