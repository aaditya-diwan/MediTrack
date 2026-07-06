import { cookies } from "next/headers";
import { AppShell } from "@/components/shell/AppShell";
import { roleFromJwt } from "@/lib/rbac";

export default async function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const cookieStore = await cookies();
  const role = roleFromJwt(cookieStore.get("token")?.value);

  return <AppShell role={role}>{children}</AppShell>;
}
