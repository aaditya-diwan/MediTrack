"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm, type Resolver } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Relationship, PatientResponse } from "@/lib/types";
import { PatientPicker } from "@/components/PatientPicker";
import { Button, Card, Field, PageHeader } from "@/components/ui";

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

function SectionHeading({ children }: { children: React.ReactNode }) {
  return (
    <h3 className="font-display text-sm font-semibold uppercase tracking-wide text-ink-muted">
      {children}
    </h3>
  );
}

export default function NewInsurancePolicyPage() {
  const router = useRouter();
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const {
    register,
    handleSubmit,
    setValue,
    clearErrors,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormData>({ resolver: zodResolver(schema) as unknown as Resolver<FormData> });

  function handlePatientSelect(p: PatientResponse | null) {
    setPatient(p);
    setValue("patientId", p?.id ?? "", { shouldValidate: false });
    if (p) clearErrors("patientId");
  }

  async function onSubmit(data: FormData) {
    try {
      const res = await fetch("/api/insurance/policies/", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        setError("root", { message: "Failed to create policy." });
        return;
      }
      toast.success("Insurance policy created.");
      router.push(`/insurance?patientId=${data.patientId}`);
    } catch {
      setError("root", {
        message: "Could not reach the server. Please try again.",
      });
    }
  }

  const text = (name: keyof FormData, type = "text") => {
    return function TextInput(ids: {
      id: string;
      "aria-invalid": boolean | undefined;
      "aria-describedby": string | undefined;
    }) {
      return (
        <input type={type} className="input" {...register(name)} {...ids} />
      );
    };
  };

  return (
    <div className="max-w-2xl">
      <PageHeader
        eyebrow="Insurance"
        title="New insurance policy"
        description="Attach a payer policy to a patient, including subscriber and coverage details."
      />
      <Card>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
          <section className="space-y-4">
            <SectionHeading>Policy</SectionHeading>
            <input type="hidden" {...register("patientId")} />
            <div>
              <PatientPicker selected={patient} onSelect={handlePatientSelect} />
              {errors.patientId && (
                <p className="mt-1 text-xs text-danger">
                  Select a patient for this policy.
                </p>
              )}
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field
                label="Policy number"
                required
                error={errors.policyNumber?.message}
              >
                {text("policyNumber")}
              </Field>
              <Field
                label="Group number"
                required
                error={errors.groupNumber?.message}
              >
                {text("groupNumber")}
              </Field>
              <Field label="Payer ID" required error={errors.payerId?.message}>
                {text("payerId")}
              </Field>
              <Field
                label="Payer name"
                required
                error={errors.payerName?.message}
              >
                {text("payerName")}
              </Field>
              <Field label="Plan name" required error={errors.planName?.message}>
                {text("planName")}
              </Field>
            </div>
          </section>

          <section className="space-y-4">
            <SectionHeading>Subscriber</SectionHeading>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field
                label="Subscriber ID"
                required
                error={errors.subscriberId?.message}
              >
                {text("subscriberId")}
              </Field>
              <Field
                label="Subscriber name"
                required
                error={errors.subscriberName?.message}
              >
                {text("subscriberName")}
              </Field>
              <Field
                label="Relationship to patient"
                required
                error={errors.relationship?.message}
              >
                {(ids) => (
                  <select
                    className="input"
                    {...register("relationship")}
                    {...ids}
                  >
                    {RELATIONSHIPS.map((r) => (
                      <option key={r} value={r}>
                        {r.replaceAll("_", " ")}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <Field
                label="Effective date"
                required
                error={errors.effectiveDate?.message}
              >
                {text("effectiveDate", "date")}
              </Field>
              <Field
                label="Termination date"
                required
                error={errors.terminationDate?.message}
              >
                {text("terminationDate", "date")}
              </Field>
            </div>
          </section>

          <section className="space-y-4">
            <SectionHeading>Coverage amounts</SectionHeading>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <Field
                label="Copay ($)"
                required
                error={errors.copayAmount?.message}
              >
                {text("copayAmount", "number")}
              </Field>
              <Field
                label="Deductible ($)"
                required
                error={errors.deductibleAmount?.message}
              >
                {text("deductibleAmount", "number")}
              </Field>
              <Field
                label="Out-of-pocket max ($)"
                required
                error={errors.outOfPocketMax?.message}
              >
                {text("outOfPocketMax", "number")}
              </Field>
            </div>
          </section>

          {errors.root && (
            <div className="rounded-lg border border-danger/25 bg-danger-tint px-3 py-2">
              <p className="text-sm text-danger">{errors.root.message}</p>
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <Button type="submit" loading={isSubmitting}>
              Create policy
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => router.back()}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
