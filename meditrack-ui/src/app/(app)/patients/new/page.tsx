"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { CreatePatientRequest } from "@/lib/types";

const schema = z.object({
  mrn: z.string().min(1),
  ssn: z.string().min(1),
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  dateOfBirth: z.string().min(1),
  email: z.string().email(),
  phoneNumber: z.string().min(1),
  address: z.string().min(1),
  insuranceProvider: z.string().min(1),
  insurancePolicyNumber: z.string().min(1),
});

type FormData = z.infer<typeof schema>;

function Field({
  label,
  name,
  type = "text",
  register,
  error,
}: {
  label: string;
  name: keyof FormData;
  type?: string;
  register: ReturnType<typeof useForm<FormData>>["register"];
  error?: string;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1">
        {label}
      </label>
      <input
        type={type}
        {...register(name)}
        className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
      />
      {error && <p className="text-red-500 text-xs mt-1">{error}</p>}
    </div>
  );
}

export default function NewPatientPage() {
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    const res = await fetch("/api/patients/", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data as CreatePatientRequest),
    });
    if (!res.ok) {
      setError("root", { message: "Failed to create patient." });
      return;
    }
    const patient = await res.json();
    router.push(`/patients/${patient.id}`);
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">New Patient</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <Field label="First Name" name="firstName" register={register} error={errors.firstName?.message} />
          <Field label="Last Name" name="lastName" register={register} error={errors.lastName?.message} />
          <Field label="MRN" name="mrn" register={register} error={errors.mrn?.message} />
          <Field label="SSN" name="ssn" register={register} error={errors.ssn?.message} />
          <Field label="Date of Birth" name="dateOfBirth" type="date" register={register} error={errors.dateOfBirth?.message} />
          <Field label="Phone" name="phoneNumber" register={register} error={errors.phoneNumber?.message} />
          <Field label="Email" name="email" type="email" register={register} error={errors.email?.message} />
          <Field label="Insurance Provider" name="insuranceProvider" register={register} error={errors.insuranceProvider?.message} />
          <Field label="Insurance Policy #" name="insurancePolicyNumber" register={register} error={errors.insurancePolicyNumber?.message} />
        </div>
        <Field label="Address" name="address" register={register} error={errors.address?.message} />
        {errors.root && (
          <p className="text-red-500 text-sm">{errors.root.message}</p>
        )}
        <div className="flex gap-3 pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="bg-slate-800 text-white text-sm px-6 py-2 rounded-lg hover:bg-slate-700 disabled:opacity-50 transition-colors"
          >
            {isSubmitting ? "Creating…" : "Create Patient"}
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
