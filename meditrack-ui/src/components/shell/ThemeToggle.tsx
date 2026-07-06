"use client";

import { Moon, Sun } from "lucide-react";

/**
 * Stateless toggle: the visible icon is driven purely by the [data-theme]
 * attribute via the custom `dark:` variant, so server and client markup
 * always agree and no effect is needed.
 */
export function ThemeToggle() {
  function toggle() {
    const next =
      document.documentElement.getAttribute("data-theme") === "dark"
        ? "light"
        : "dark";
    document.documentElement.setAttribute("data-theme", next);
    try {
      localStorage.setItem("mt-theme", next);
    } catch {
      // private mode — theme just won't persist
    }
  }

  return (
    <button
      type="button"
      onClick={toggle}
      aria-label="Toggle color theme"
      className="rounded-lg p-2 text-ink-muted transition-colors hover:bg-card-2 hover:text-ink"
    >
      <Sun className="hidden size-4 dark:block" aria-hidden />
      <Moon className="size-4 dark:hidden" aria-hidden />
    </button>
  );
}
