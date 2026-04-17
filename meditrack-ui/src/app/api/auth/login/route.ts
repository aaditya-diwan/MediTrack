import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

export async function POST(req: NextRequest) {
  const body = await req.json();
  const res = await fetch(
    `${process.env.PATIENT_SERVICE_URL}/api/v1/auth/authenticate`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }
  );
  if (!res.ok) {
    return NextResponse.json({ error: "Invalid credentials" }, { status: 401 });
  }
  const { jwt } = await res.json();
  const cookieStore = await cookies();
  cookieStore.set("token", jwt, {
    httpOnly: true,
    path: "/",
    sameSite: "lax",
    maxAge: 86400,
  });
  return NextResponse.json({ ok: true });
}
