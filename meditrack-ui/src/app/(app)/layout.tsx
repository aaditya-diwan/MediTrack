import { cookies } from "next/headers";
import Navbar from "@/components/Navbar";
import { roleFromJwt } from "@/lib/rbac";

export default async function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const cookieStore = await cookies();
  const role = roleFromJwt(cookieStore.get("token")?.value);

  return (
    <div className="min-h-screen flex flex-col">
      <Navbar role={role} />
      <main className="flex-1 max-w-5xl mx-auto w-full px-6 py-8">
        {children}
      </main>
    </div>
  );
}
