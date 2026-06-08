"use client";

import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import {
  DoctorResponse,
  AvailabilitySlotResponse,
  AppointmentType,
  SPECIALIZATIONS,
  formatSpecialization,
} from "@/lib/types";

const DAY_ORDER = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

function generateTimeSlots(startTime: string, endTime: string, durationMins: number): string[] {
  const parse = (t: string) => {
    const [h, m] = t.split(":").map(Number);
    return h * 60 + m;
  };
  const slots: string[] = [];
  let cur = parse(startTime);
  const end = parse(endTime);
  while (cur + durationMins <= end) {
    slots.push(`${String(Math.floor(cur / 60)).padStart(2, "0")}:${String(cur % 60).padStart(2, "0")}`);
    cur += durationMins;
  }
  return slots;
}

function fmtTime(t: string) {
  const [h, m] = t.split(":").map(Number);
  return `${h % 12 || 12}:${String(m).padStart(2, "0")} ${h >= 12 ? "PM" : "AM"}`;
}

function getDayOfWeek(dateStr: string): string {
  const d = new Date(dateStr + "T00:00:00");
  const idx = d.getDay(); // 0=Sun
  return DAY_ORDER[idx === 0 ? 6 : idx - 1];
}

function minDate(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
}

function BookForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const preselectedDoctorId = searchParams.get("doctorId") ?? "";
  const preselectedPatientId = searchParams.get("patientId") ?? "";

  const [step, setStep] = useState<1 | 2 | 3 | 4>(preselectedDoctorId ? 3 : 1);
  const [specialization, setSpecialization] = useState("");
  const [doctors, setDoctors] = useState<DoctorResponse[]>([]);
  const [doctorsLoading, setDoctorsLoading] = useState(false);

  const [selectedDoctor, setSelectedDoctor] = useState<DoctorResponse | null>(null);
  const [slots, setSlots] = useState<AvailabilitySlotResponse[]>([]);

  const [selectedDate, setSelectedDate] = useState("");
  const [selectedTime, setSelectedTime] = useState("");
  const [patientId, setPatientId] = useState(preselectedPatientId);
  const [visitType, setVisitType] = useState<AppointmentType>("FIRST_VISIT");
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!preselectedDoctorId) return;
    fetch(`/api/doctors/${preselectedDoctorId}`)
      .then((r) => r.json())
      .then((doc) => {
        setSelectedDoctor(doc);
        return fetch(`/api/doctors/${preselectedDoctorId}/slots`);
      })
      .then((r) => r.json())
      .then((sl) => setSlots(Array.isArray(sl) ? sl : []));
  }, [preselectedDoctorId]);

  async function searchDoctors() {
    setDoctorsLoading(true);
    const url = specialization ? `/api/doctors?specialization=${specialization}` : `/api/doctors`;
    const res = await fetch(url);
    setDoctorsLoading(false);
    if (res.ok) setDoctors(await res.json());
  }

  async function selectDoctor(doctor: DoctorResponse) {
    setSelectedDoctor(doctor);
    const res = await fetch(`/api/doctors/${doctor.id}/slots`);
    const sl = await res.json();
    setSlots(Array.isArray(sl) ? sl : []);
    setStep(3);
  }

  const availableDays = slots.filter((s) => s.available).map((s) => s.dayOfWeek);
  const slotForDate = slots.find(
    (s) => selectedDate && s.dayOfWeek === getDayOfWeek(selectedDate) && s.available
  );
  const timeSlots = slotForDate
    ? generateTimeSlots(slotForDate.startTime, slotForDate.endTime, slotForDate.slotDurationMinutes)
    : [];

  async function submit() {
    if (!selectedDoctor || !selectedDate || !selectedTime || !patientId) {
      toast.error("Please fill all required fields.");
      return;
    }
    setSubmitting(true);
    const res = await fetch("/api/appointments", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        patientId,
        doctorId: selectedDoctor.id,
        type: visitType,
        reasonForVisit: reason || undefined,
        scheduledAt: `${selectedDate}T${selectedTime}:00`,
      }),
    });
    setSubmitting(false);
    if (!res.ok) {
      const body = await res.text();
      toast.error(body || "Failed to book appointment.");
      return;
    }
    const appt = await res.json();
    toast.success("Appointment booked!");
    router.push(`/appointments/${appt.id}`);
  }

  const STEPS = ["Choose Specialty", "Choose Doctor", "Pick Date & Time", "Confirm"];

  return (
    <div className="max-w-xl">
      {/* Step indicator */}
      <div className="flex items-center gap-1 mb-8">
        {STEPS.map((label, i) => (
          <div key={i} className="flex items-center gap-1">
            <div
              className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 ${
                step >= i + 1 ? "bg-slate-800 text-white" : "bg-slate-200 text-slate-500"
              }`}
            >
              {i + 1}
            </div>
            {i < STEPS.length - 1 && (
              <div className={`h-0.5 w-10 ${step > i + 1 ? "bg-slate-800" : "bg-slate-200"}`} />
            )}
          </div>
        ))}
        <span className="ml-3 text-xs text-slate-500">{STEPS[step - 1]}</span>
      </div>

      {/* Step 1 — Specialty */}
      {step === 1 && (
        <div className="space-y-4">
          <h2 className="text-lg font-semibold text-slate-800">Choose a Specialty</h2>
          <select
            value={specialization}
            onChange={(e) => setSpecialization(e.target.value)}
            className="input"
          >
            <option value="">All Specializations</option>
            {SPECIALIZATIONS.map((s) => (
              <option key={s} value={s}>{formatSpecialization(s)}</option>
            ))}
          </select>
          <button
            onClick={() => { searchDoctors(); setStep(2); }}
            className="bg-slate-800 text-white text-sm px-6 py-2 rounded-lg hover:bg-slate-700 transition-colors"
          >
            Find Doctors →
          </button>
        </div>
      )}

      {/* Step 2 — Doctor */}
      {step === 2 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-800">Choose a Doctor</h2>
            <button onClick={() => setStep(1)} className="text-sm text-slate-500 hover:text-slate-700">← Back</button>
          </div>
          {doctorsLoading && <p className="text-slate-500 text-sm">Loading…</p>}
          {!doctorsLoading && doctors.length === 0 && (
            <p className="text-slate-500 text-sm">No doctors found.</p>
          )}
          <div className="space-y-2">
            {doctors.map((d) => (
              <button
                key={d.id}
                onClick={() => selectDoctor(d)}
                className="w-full text-left border border-slate-200 rounded-xl p-4 hover:border-slate-800 transition-all bg-white"
              >
                <p className="font-semibold text-slate-800">{d.fullName}</p>
                <p className="text-sm text-slate-500">
                  {formatSpecialization(d.specialization)} · {d.yearsOfExperience}y exp
                </p>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Step 3 — Date & Time */}
      {step === 3 && selectedDoctor && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-800">Pick Date & Time</h2>
            {!preselectedDoctorId && (
              <button onClick={() => setStep(2)} className="text-sm text-slate-500 hover:text-slate-700">← Back</button>
            )}
          </div>

          <div className="border border-slate-200 rounded-lg px-4 py-3 bg-slate-50 text-sm">
            <span className="font-medium">{selectedDoctor.fullName}</span>
            {availableDays.length > 0 && (
              <span className="text-slate-500 ml-2">
                Available:{" "}
                {availableDays.map((d) => d.charAt(0) + d.slice(1).toLowerCase()).join(", ")}
              </span>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Date</label>
            <input
              type="date"
              min={minDate()}
              value={selectedDate}
              onChange={(e) => { setSelectedDate(e.target.value); setSelectedTime(""); }}
              className="input"
            />
            {selectedDate && !slotForDate && (
              <p className="text-amber-600 text-xs mt-1">
                Not available on {getDayOfWeek(selectedDate).charAt(0) + getDayOfWeek(selectedDate).slice(1).toLowerCase()}s.
              </p>
            )}
          </div>

          {timeSlots.length > 0 && (
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Time Slot</label>
              <div className="grid grid-cols-4 gap-2">
                {timeSlots.map((t) => (
                  <button
                    key={t}
                    onClick={() => setSelectedTime(t)}
                    className={`text-sm py-2 rounded-lg border transition-colors ${
                      selectedTime === t
                        ? "bg-slate-800 text-white border-slate-800"
                        : "border-slate-300 text-slate-700 hover:border-slate-600"
                    }`}
                  >
                    {fmtTime(t)}
                  </button>
                ))}
              </div>
            </div>
          )}

          <button
            onClick={() => setStep(4)}
            disabled={!selectedDate || !selectedTime}
            className="bg-slate-800 text-white text-sm px-6 py-2 rounded-lg hover:bg-slate-700 disabled:opacity-40 transition-colors"
          >
            Continue →
          </button>
        </div>
      )}

      {/* Step 4 — Confirm */}
      {step === 4 && selectedDoctor && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-800">Confirm Appointment</h2>
            <button onClick={() => setStep(3)} className="text-sm text-slate-500 hover:text-slate-700">← Back</button>
          </div>

          <div className="border border-slate-200 rounded-xl p-4 bg-white space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-slate-500">Doctor</span>
              <span className="font-medium">{selectedDoctor.fullName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Date & Time</span>
              <span className="font-medium">{selectedDate} at {fmtTime(selectedTime)}</span>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Patient ID <span className="text-red-500">*</span>
            </label>
            <input
              value={patientId}
              onChange={(e) => setPatientId(e.target.value)}
              placeholder="Patient UUID"
              className="input"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Visit Type</label>
            <select
              value={visitType}
              onChange={(e) => setVisitType(e.target.value as AppointmentType)}
              className="input"
            >
              <option value="FIRST_VISIT">First Visit</option>
              <option value="FOLLOW_UP">Follow-up</option>
              <option value="EMERGENCY">Emergency</option>
              <option value="TELECONSULTATION">Teleconsultation</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Reason for Visit</label>
            <textarea
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Briefly describe your symptoms…"
              className="input"
            />
          </div>

          <button
            onClick={submit}
            disabled={submitting || !patientId}
            className="w-full bg-slate-800 text-white text-sm py-2.5 rounded-lg hover:bg-slate-700 disabled:opacity-40 transition-colors font-medium"
          >
            {submitting ? "Booking…" : "Confirm Appointment"}
          </button>
        </div>
      )}
    </div>
  );
}

export default function BookAppointmentPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-6">Book Appointment</h1>
      <Suspense fallback={<p className="text-slate-500 text-sm">Loading…</p>}>
        <BookForm />
      </Suspense>
    </div>
  );
}
