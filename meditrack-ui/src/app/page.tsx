import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { landingFor, roleFromJwt } from "@/lib/rbac";

export default async function Home() {
  const cookieStore = await cookies();
  const role = roleFromJwt(cookieStore.get("token")?.value);
  redirect(landingFor(role));
}
