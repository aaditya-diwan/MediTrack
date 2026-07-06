"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { EcgTrace } from "@/components/ui/Ecg";

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setLoading(true);
    const form = new FormData(e.currentTarget);
    let res: Response;
    try {
      res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: form.get("username"),
          password: form.get("password"),
        }),
      });
    } catch {
      setLoading(false);
      setError("Could not reach the server. Check that the platform is running.");
      return;
    }
    setLoading(false);
    if (!res.ok) {
      setError("Invalid username or password.");
      return;
    }
    const data = await res.json().catch(() => ({}));
    router.push(data.landing ?? "/dashboard");
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-page px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <EcgTrace animate className="mx-auto h-8 w-48 text-brand" />
          <h1 className="mt-4 font-display text-3xl font-semibold tracking-tight text-ink">
            MediTrack
          </h1>
          <p className="mt-1 text-sm text-ink-muted">
            Hospital operations, in one place.
          </p>
        </div>
        <div className="rounded-2xl border border-line bg-card p-8 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label
                htmlFor="username"
                className="mb-1 block text-xs font-medium text-ink-muted"
              >
                Username
              </label>
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                required
                className="input"
              />
            </div>
            <div>
              <label
                htmlFor="password"
                className="mb-1 block text-xs font-medium text-ink-muted"
              >
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                required
                className="input"
              />
            </div>
            {error && (
              <p role="alert" className="text-sm text-danger">
                {error}
              </p>
            )}
            <Button type="submit" loading={loading} className="w-full">
              Sign in
            </Button>
          </form>
        </div>
        <p className="mt-6 text-center text-xs text-ink-faint">
          Staff access only. All activity is logged.
        </p>
      </div>
    </div>
  );
}
