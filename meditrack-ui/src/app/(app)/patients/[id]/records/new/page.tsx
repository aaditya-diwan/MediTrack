"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Button, Card, Field, PageHeader } from "@/components/ui";

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
    try {
      const res = await fetch("/api/medical-records/", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ...data, patientId: id }),
      });
      if (!res.ok) {
        setError("root", { message: "Failed to create medical record." });
        return;
      }
      toast.success("Medical record saved.");
      router.push(`/patients/${id}`);
    } catch {
      setError("root", {
        message: "Could not reach the server. Please try again.",
      });
    }
  }

  return (
    <div className="max-w-lg">
      <PageHeader
        eyebrow="Patients"
        title="Add medical record"
        description="Document a diagnosis and treatment for this patient's chart."
      />
      <Card>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Field label="Diagnosis" required error={errors.diagnosis?.message}>
            {(ids) => (
              <input className="input" {...register("diagnosis")} {...ids} />
            )}
          </Field>
          <Field label="Treatment" required error={errors.treatment?.message}>
            {(ids) => (
              <textarea
                rows={4}
                className="input"
                {...register("treatment")}
                {...ids}
              />
            )}
          </Field>
          <Field label="Date" required error={errors.date?.message}>
            {(ids) => (
              <input
                type="date"
                className="input"
                {...register("date")}
                {...ids}
              />
            )}
          </Field>

          {errors.root && (
            <div className="rounded-lg border border-danger/25 bg-danger-tint px-3 py-2">
              <p className="text-sm text-danger">{errors.root.message}</p>
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <Button type="submit" loading={isSubmitting}>
              Save record
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
