"use client";

import { useRouter } from "next/navigation";
import { useForm, type Resolver } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Relationship } from "@/lib/types";

const RELATIONSHIPS: Relationship[] = [
  "SELF",
  "SPOUSE",
  "CHILD",
  "PARENT",
  "DOMESTIC_PARTNER",
  "OTHER",
];

const schema = z.object({
  patientId: z.string().uuid(),
  policyNumber: z.string().min(1),
  payerId: z.string().min(1),
  payerName: z.string().min(1),
  planName: z.string().min(1),
  groupNumber: z.string().min(1),
  subscriberId: z.string().min(1),
  subscriberName: z.string().min(1),
  relationship: z.enum(["SELF", "SPOUSE", "CHILD", "PARENT", "DOMESTIC_PARTNER", "OTHER"]),
  effectiveDate: z.string().min(1),
  terminationDate: z.string().min(1),
  copayAmount: z.coerce.number().min(0),
  deductibleAmount: z.coerce.number().min(0),
  outOfPocketMax: z.coerce.number().min(0),
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
      <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
      <input
        type={type}
        {...register(name)}
        className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
      />
      {error && <p className="text-red-500 text-xs mt-1">{error}</p>}
    </div>
  );
}

export default function NewInsurancePolicyPage() {
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormData>({ resolver: zodResolver(schema) as unknown as Resolver<FormData> });

  async function onSubmit(data: FormData) {
    const res = await fetch("/api/insurance/policies/", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      setError("root", { message: "Failed to create policy." });
      return;
    }
    router.push(`/insurance?patientId=${data.patientId}`);
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">New Insurance Policy</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <Field label="Patient ID (UUID)" name="patientId" register={register} error={errors.patientId?.message} />
          <Field label="Policy Number" name="policyNumber" register={register} error={errors.policyNumber?.message} />
          <Field label="Payer ID" name="payerId" register={register} error={errors.payerId?.message} />
          <Field label="Payer Name" name="payerName" register={register} error={errors.payerName?.message} />
          <Field label="Plan Name" name="planName" register={register} error={errors.planName?.message} />
          <Field label="Group Number" name="groupNumber" register={register} error={errors.groupNumber?.message} />
          <Field label="Subscriber ID" name="subscriberId" register={register} error={errors.subscriberId?.message} />
          <Field label="Subscriber Name" name="subscriberName" register={register} error={errors.subscriberName?.message} />
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Relationship</label>
            <select
              {...register("relationship")}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
            >
              {RELATIONSHIPS.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
            {errors.relationship && (
              <p className="text-red-500 text-xs mt-1">{errors.relationship.message}</p>
            )}
          </div>
          <Field label="Effective Date" name="effectiveDate" type="date" register={register} error={errors.effectiveDate?.message} />
          <Field label="Termination Date" name="terminationDate" type="date" register={register} error={errors.terminationDate?.message} />
          <Field label="Copay Amount ($)" name="copayAmount" type="number" register={register} error={errors.copayAmount?.message} />
          <Field label="Deductible Amount ($)" name="deductibleAmount" type="number" register={register} error={errors.deductibleAmount?.message} />
          <Field label="Out-of-Pocket Max ($)" name="outOfPocketMax" type="number" register={register} error={errors.outOfPocketMax?.message} />
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
            {isSubmitting ? "Creating…" : "Create Policy"}
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
