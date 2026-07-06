"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { CreatePatientRequest } from "@/lib/types";
import { Button, Card, Field, PageHeader } from "@/components/ui";

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

export default function NewPatientPage() {
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    try {
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
      toast.success("Patient registered.");
      router.push(`/patients/${patient.id}`);
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
        eyebrow="Patients"
        title="New patient"
        description="Register a patient to create their chart and MRN record."
      />
      <Card>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="First name" required error={errors.firstName?.message}>
              {text("firstName")}
            </Field>
            <Field label="Last name" required error={errors.lastName?.message}>
              {text("lastName")}
            </Field>
            <Field
              label="MRN"
              required
              hint="Medical record number"
              error={errors.mrn?.message}
            >
              {text("mrn")}
            </Field>
            <Field label="SSN" required error={errors.ssn?.message}>
              {text("ssn")}
            </Field>
            <Field
              label="Date of birth"
              required
              error={errors.dateOfBirth?.message}
            >
              {text("dateOfBirth", "date")}
            </Field>
            <Field label="Phone" required error={errors.phoneNumber?.message}>
              {text("phoneNumber", "tel")}
            </Field>
            <Field label="Email" required error={errors.email?.message}>
              {text("email", "email")}
            </Field>
            <Field
              label="Insurance provider"
              required
              error={errors.insuranceProvider?.message}
            >
              {text("insuranceProvider")}
            </Field>
            <Field
              label="Insurance policy #"
              required
              error={errors.insurancePolicyNumber?.message}
            >
              {text("insurancePolicyNumber")}
            </Field>
          </div>
          <Field label="Address" required error={errors.address?.message}>
            {text("address")}
          </Field>

          {errors.root && (
            <div className="rounded-lg border border-danger/25 bg-danger-tint px-3 py-2">
              <p className="text-sm text-danger">{errors.root.message}</p>
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <Button type="submit" loading={isSubmitting}>
              Create patient
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
