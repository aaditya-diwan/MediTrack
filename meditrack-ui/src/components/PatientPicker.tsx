"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Search, X } from "lucide-react";
import type { PatientResponse as Patient } from "@/lib/types";

/**
 * Search-as-you-type patient selector backed by /api/patients/search.
 * Replaces every "paste a patient UUID" input in the app.
 */
export function PatientPicker({
  onSelect,
  selected,
  label = "Patient",
  placeholder = "Search by name or MRN…",
}: {
  onSelect: (patient: Patient | null) => void;
  selected: Patient | null;
  label?: string;
  placeholder?: string;
}) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Patient[]>([]);
  const [open, setOpen] = useState(false);
  const [searching, setSearching] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const listboxId = useId();

  useEffect(() => {
    if (query.trim().length < 2) return;
    const controller = new AbortController();
    const t = setTimeout(async () => {
      setSearching(true);
      try {
        const res = await fetch(
          `/api/patients/search?query=${encodeURIComponent(query.trim())}`,
          { signal: controller.signal },
        );
        if (res.ok) {
          const data = (await res.json()) as Patient[];
          setResults(Array.isArray(data) ? data.slice(0, 8) : []);
          setOpen(true);
        }
      } catch {
        // aborted or network failure — keep previous results
      } finally {
        setSearching(false);
      }
    }, 250);
    return () => {
      controller.abort();
      clearTimeout(t);
    };
  }, [query]);

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, []);

  if (selected) {
    return (
      <div>
        <span className="mb-1 block text-xs font-medium text-ink-muted">
          {label}
        </span>
        <div className="flex items-center justify-between rounded-lg border border-brand/30 bg-brand-tint px-3 py-2">
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-ink">
              {selected.firstName} {selected.lastName}
            </p>
            <p className="tabular text-xs text-ink-muted">MRN {selected.mrn}</p>
          </div>
          <button
            type="button"
            onClick={() => onSelect(null)}
            aria-label="Clear selected patient"
            className="rounded p-1 text-ink-muted hover:text-ink"
          >
            <X className="size-4" aria-hidden />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div ref={rootRef} className="relative">
      <label className="mb-1 block text-xs font-medium text-ink-muted">
        {label}
        <div className="relative mt-1">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-faint"
            aria-hidden
          />
          <input
            type="text"
            role="combobox"
            aria-expanded={open}
            aria-controls={listboxId}
            aria-autocomplete="list"
            value={query}
            onChange={(e) => {
              const next = e.target.value;
              setQuery(next);
              if (next.trim().length < 2) {
                setResults([]);
                setOpen(false);
              }
            }}
            onFocus={() => results.length > 0 && setOpen(true)}
            placeholder={placeholder}
            className="input pl-9"
          />
        </div>
      </label>
      {open && (
        <ul
          id={listboxId}
          role="listbox"
          className="absolute z-20 mt-1 max-h-72 w-full overflow-auto rounded-lg border border-line bg-card py-1 shadow-lg"
        >
          {searching && results.length === 0 && (
            <li className="px-3 py-2 text-sm text-ink-faint">Searching…</li>
          )}
          {!searching && results.length === 0 && (
            <li className="px-3 py-2 text-sm text-ink-faint">
              No patients match “{query.trim()}”.
            </li>
          )}
          {results.map((p) => (
            <li key={p.id} role="option" aria-selected={false}>
              <button
                type="button"
                onClick={() => {
                  onSelect(p);
                  setQuery("");
                  setOpen(false);
                }}
                className="flex w-full items-baseline justify-between gap-3 px-3 py-2 text-left hover:bg-card-2"
              >
                <span className="truncate text-sm text-ink">
                  {p.firstName} {p.lastName}
                </span>
                <span className="tabular shrink-0 text-xs text-ink-muted">
                  {p.mrn}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
