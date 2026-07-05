import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { canAccess, landingFor, roleFromJwt } from "@/lib/rbac";

export function proxy(req: NextRequest) {
  const token = req.cookies.get("token")?.value;
  const { pathname } = req.nextUrl;

  // Unauthenticated: only the login page is reachable.
  if (!token) {
    if (pathname.startsWith("/login")) return NextResponse.next();
    return NextResponse.redirect(new URL("/login", req.url));
  }

  // Authenticated: enforce role-based section access. The role is read from
  // the JWT itself (source of truth), not a separate spoofable cookie.
  const role = roleFromJwt(token);
  if (role && !canAccess(role, pathname)) {
    return NextResponse.redirect(new URL(landingFor(role), req.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next|favicon.ico|api/auth).*)"],
};
