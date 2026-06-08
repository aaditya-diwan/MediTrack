import { NextRequest, NextResponse } from "next/server";

const BASE = process.env.PRESCRIPTION_SERVICE_URL;

async function proxy(req: NextRequest, path: string[]) {
  const token = req.cookies.get("token")?.value;
  const url = `${BASE}/api/v1/prescriptions/${path.join("/")}${req.nextUrl.search}`;
  const res = await fetch(url, {
    method: req.method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body:
      req.method !== "GET" && req.method !== "DELETE"
        ? await req.text()
        : undefined,
  });

  // PDF responses — pass through binary with original headers
  const contentType = res.headers.get("content-type") ?? "";
  if (contentType.includes("application/pdf")) {
    const buf = await res.arrayBuffer();
    return new NextResponse(buf, {
      status: res.status,
      headers: {
        "Content-Type": "application/pdf",
        "Content-Disposition":
          res.headers.get("content-disposition") ??
          `attachment; filename="prescription.pdf"`,
      },
    });
  }

  const data = await res.text();
  return new NextResponse(data, {
    status: res.status,
    headers: { "Content-Type": "application/json" },
  });
}

type Ctx = { params: Promise<{ path: string[] }> };

export async function GET(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
export async function POST(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
export async function PUT(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
export async function DELETE(req: NextRequest, ctx: Ctx) {
  return proxy(req, (await ctx.params).path);
}
