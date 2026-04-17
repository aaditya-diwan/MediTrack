"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

const schema = z.object({
  diagnosis: z.string().min(1),
  treatment: z.string().min(1),
  date: z.string().min(1),
});

type FormData = z.infer<typeof schema>;

export default function NewMedicalRecordPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    const res = await fetch("/api/medical-records/", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...data, patientId: id }),
    });
    if (!res.ok) {
      setError("root", { message: "Failed to create medical record." });
      return;
    }
    router.push(`/patients/${id}`);
  }

  return (
    <div className="max-w-lg">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">
        Add Medical Record
      </h1>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Diagnosis
          </label>
          <input
            {...register("diagnosis")}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
          />
          {errors.diagnosis && (
            <p className="text-red-500 text-xs mt-1">{errors.diagnosis.message}</p>
          )}
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Treatment
          </label>
          <textarea
            {...register("treatment")}
            rows={4}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
          />
          {errors.treatment && (
            <p className="text-red-500 text-xs mt-1">{errors.treatment.message}</p>
          )}
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Date
          </label>
          <input
            type="date"
            {...register("date")}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
          />
          {errors.date && (
            <p className="text-red-500 text-xs mt-1">{errors.date.message}</p>
          )}
        </div>
        {errors.root && (
          <p className="text-red-500 text-sm">{errors.root.message}</p>
        )}
        <div className="flex gap-3 pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="bg-slate-800 text-white text-sm px-6 py-2 rounded-lg hover:bg-slate-700 disabled:opacity-50 transition-colors"
          >
            {isSubmitting ? "Saving…" : "Save Record"}
          </button>
          <button
            type="button"
            onClick={() => router.back()}
            className="text-sm px-6 py-2 rounded-lg border border-slate-300 hover:bg-slate-100 transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
