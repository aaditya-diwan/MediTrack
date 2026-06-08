"use client";

import { useEffect, useState } from "react";
import { use } from "react";
import { format } from "date-fns";
import { toast } from "sonner";
import {
  AppointmentResponse,
  AppointmentStatus,
  PatientResponse,
  PrescriptionResponse,
  MedicationDraft,
  PrescriptionLabOrderDraft,
} from "@/lib/types";

type Tab = "patient" | "prescribe" | "history";

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-700",
  CONFIRMED: "bg-blue-100 text-blue-700",
  IN_PROGRESS: "bg-indigo-100 text-indigo-700",
  COMPLETED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-600",
  NO_SHOW: "bg-slate-100 text-slate-500",
};

const BLANK_MED: MedicationDraft = {
  medicationName: "", genericName: "", dosage: "", frequency: "",
  duration: "", route: "", instructions: "",
};
const BLANK_LAB: PrescriptionLabOrderDraft = {
  testCode: "", testName: "", clinicalIndication: "", urgency: "ROUTINE",
};

export default function ConsultationPage({
  params,
}: {
  params: Promise<{ appointmentId: string }>;
}) {
  const { appointmentId } = use(params);
  const [appt, setAppt] = useState<AppointmentResponse | null>(null);
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [prescriptions, setPrescriptions] = useState<PrescriptionResponse[]>([]);
  const [tab, setTab] = useState<Tab>("patient");
  const [loading, setLoading] = useState(true);

  const [consultationNotes, setConsultationNotes] = useState("");
  const [diagnosisCodes, setDiagnosisCodes] = useState("");
  const [medications, setMedications] = useState<MedicationDraft[]>([]);
  const [labOrders, setLabOrders] = useState<PrescriptionLabOrderDraft[]>([]);
  const [newMed, setNewMed] = useState<MedicationDraft>({ ...BLANK_MED });
  const [newLab, setNewLab] = useState<PrescriptionLabOrderDraft>({ ...BLANK_LAB });
  const [showMedForm, setShowMedForm] = useState(false);
  const [showLabForm, setShowLabForm] = useState(false);
  const [activePrescription, setActivePrescription] = useState<PrescriptionResponse | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetch(`/api/appointments/${appointmentId}`)
      .then((r) => r.json())
      .then(async (a: AppointmentResponse) => {
        setAppt(a);
        const [patRes, rxRes] = await Promise.all([
          fetch(`/api/patients/${a.patientId}`),
          fetch(`/api/prescriptions/patient/${a.patientId}`),
        ]);
        if (patRes.ok) setPatient(await patRes.json());
        if (rxRes.ok) {
          const rxData = await rxRes.json();
          setPrescriptions(Array.isArray(rxData) ? rxData : []);
        }
        setLoading(false);
      });
  }, [appointmentId]);

  async function updateApptStatus(status: AppointmentStatus) {
    const res = await fetch(`/api/appointments/${appointmentId}/status?status=${status}`, {
      method: "PUT",
    });
    if (!res.ok) { toast.error("Failed to update status."); return; }
    setAppt(await res.json());
    toast.success(`Marked as ${status.replace(/_/g, " ")}`);
  }

  function addMedication() {
    if (!newMed.medicationName || !newMed.dosage || !newMed.frequency) {
      toast.error("Name, dosage, and frequency are required.");
      return;
    }
    setMedications((m) => [...m, { ...newMed }]);
    setNewMed({ ...BLANK_MED });
    setShowMedForm(false);
  }

  function addLabOrder() {
    if (!newLab.testCode || !newLab.testName) {
      toast.error("Test code and name are required.");
      return;
    }
    setLabOrders((l) => [...l, { ...newLab }]);
    setNewLab({ ...BLANK_LAB });
    setShowLabForm(false);
  }

  async function createDraft() {
    if (!appt) return;
    setSaving(true);
    const res = await fetch("/api/prescriptions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        patientId: appt.patientId,
        doctorId: appt.doctorId,
        appointmentId: appt.id,
        consultationNotes,
        diagnosisCodes,
        medications,
        labOrders,
      }),
    });
    setSaving(false);
    if (!res.ok) { toast.error("Failed to create prescription."); return; }
    setActivePrescription(await res.json());
    toast.success("Draft created.");
  }

  async function issuePrescription() {
    if (!activePrescription) return;
    setSaving(true);
    const res = await fetch(`/api/prescriptions/${activePrescription.id}/issue`, { method: "POST" });
    setSaving(false);
    if (!res.ok) { toast.error("Failed to issue."); return; }
    const rx = await res.json();
    setActivePrescription(rx);
    setPrescriptions((prev) => [rx, ...prev.filter((p) => p.id !== rx.id)]);
    toast.success("Prescription issued!");
  }

  async function sendToPharmacy() {
    if (!activePrescription) return;
    const res = await fetch(`/api/prescriptions/${activePrescription.id}/send-pharmacy`, { method: "POST" });
    if (!res.ok) { toast.error("Failed to send to pharmacy."); return; }
    setActivePrescription(await res.json());
    toast.success("Sent to pharmacy.");
  }

  async function sendToLab() {
    if (!activePrescription) return;
    const res = await fetch(`/api/prescriptions/${activePrescription.id}/send-lab`, { method: "POST" });
    if (!res.ok) { toast.error("Failed to send to lab."); return; }
    setActivePrescription(await res.json());
    toast.success("Sent to lab.");
  }

  function downloadPdf() {
    if (!activePrescription) return;
    const a = document.createElement("a");
    a.href = `/api/prescriptions/${activePrescription.id}/pdf`;
    a.download = `prescription-${activePrescription.id.slice(0, 8)}.pdf`;
    a.click();
  }

  if (loading) return <p className="text-slate-500">Loading…</p>;
  if (!appt) return <p className="text-red-500">Appointment not found.</p>;

  const isIssued = activePrescription && activePrescription.status !== "DRAFT";

  return (
    <div>
      <div className="flex items-start justify-between mb-4">
        <div>
          <h1 className="text-xl font-bold text-slate-800">
            {patient ? `${patient.firstName} ${patient.lastName}` : `Patient …${appt.patientId.slice(-6)}`}
          </h1>
          <p className="text-slate-500 text-sm mt-0.5">
            {format(new Date(appt.scheduledAt), "MMM d, yyyy 'at' h:mm a")} ·{" "}
            {appt.type.replace(/_/g, " ").toLowerCase()}
            {appt.reasonForVisit ? ` · ${appt.reasonForVisit}` : ""}
          </p>
        </div>
        <span className={`text-xs px-3 py-1 rounded-full font-medium ${STATUS_STYLES[appt.status]}`}>
          {appt.status.replace(/_/g, " ")}
        </span>
      </div>

      <div className="flex gap-2 mb-5">
        {appt.status === "CONFIRMED" && (
          <button
            onClick={() => updateApptStatus("IN_PROGRESS")}
            className="text-xs px-3 py-1.5 rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-colors"
          >
            Start Consultation
          </button>
        )}
        {appt.status === "IN_PROGRESS" && (
          <button
            onClick={() => updateApptStatus("COMPLETED")}
            className="text-xs px-3 py-1.5 rounded-lg bg-green-600 text-white hover:bg-green-700 transition-colors"
          >
            Mark Completed
          </button>
        )}
      </div>

      <div className="flex gap-1 border-b border-slate-200 mb-5">
        {(["patient", "prescribe", "history"] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 text-sm transition-colors ${
              tab === t
                ? "border-b-2 border-slate-800 text-slate-800 font-medium"
                : "text-slate-500 hover:text-slate-700"
            }`}
          >
            {t === "prescribe" ? "Write Prescription" : t === "history" ? "Rx History" : "Patient"}
          </button>
        ))}
      </div>

      {tab === "patient" && (
        <div className="space-y-4">
          {patient ? (
            <>
              <dl className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
                {[
                  ["MRN", patient.mrn],
                  ["DOB", format(new Date(patient.dateOfBirth), "MMM d, yyyy")],
                  ["Email", patient.email],
                  ["Phone", patient.phoneNumber],
                  ["Address", patient.address],
                  ["Insurance", patient.insuranceProvider],
                ].map(([label, val]) => (
                  <div key={label}>
                    <dt className="text-slate-400">{label}</dt>
                    <dd className="text-slate-800 font-medium">{val}</dd>
                  </div>
                ))}
              </dl>
              {patient.medicalHistory && patient.medicalHistory.length > 0 && (
                <div>
                  <h3 className="text-sm font-semibold text-slate-700 mb-2 mt-4">Medical History</h3>
                  <div className="space-y-2">
                    {patient.medicalHistory.slice(0, 5).map((r) => (
                      <div key={r.recordId} className="text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white">
                        <span className="font-medium text-slate-800">{r.diagnosis}</span>
                        <span className="text-slate-400 ml-2 text-xs">{r.date}</span>
                        {r.treatment && <p className="text-slate-500 mt-0.5 text-xs">{r.treatment}</p>}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : (
            <p className="text-slate-500 text-sm">Patient data unavailable.</p>
          )}
        </div>
      )}

      {tab === "prescribe" && (
        <div className="space-y-5 max-w-2xl">
          {activePrescription && (
            <div className={`text-sm px-4 py-2 rounded-lg border ${
              activePrescription.status === "DRAFT"
                ? "border-yellow-300 bg-yellow-50 text-yellow-800"
                : "border-green-300 bg-green-50 text-green-800"
            }`}>
              Prescription <span className="font-mono text-xs">{activePrescription.id.slice(0, 8)}…</span>{" "}
              — <span className="font-medium">{activePrescription.status}</span>
            </div>
          )}

          {!activePrescription && (
            <>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Consultation Notes</label>
                <textarea rows={4} value={consultationNotes} onChange={(e) => setConsultationNotes(e.target.value)}
                  placeholder="SOAP notes, clinical observations…" className="input" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Diagnosis Codes (ICD-10)</label>
                <input value={diagnosisCodes} onChange={(e) => setDiagnosisCodes(e.target.value)}
                  placeholder="e.g. J06.9, E11.9" className="input" />
              </div>

              {/* Medications */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-semibold text-slate-700">Medications</h3>
                  <button onClick={() => setShowMedForm(!showMedForm)}
                    className="text-xs border border-slate-300 rounded-lg px-2 py-1 hover:bg-slate-100">
                    + Add Medication
                  </button>
                </div>
                {showMedForm && (
                  <div className="border border-slate-200 rounded-xl p-4 bg-slate-50 space-y-3 mb-3">
                    <div className="grid grid-cols-2 gap-3">
                      {[
                        { label: "Name *", key: "medicationName", placeholder: "e.g. Metformin" },
                        { label: "Generic Name", key: "genericName", placeholder: "e.g. Metformin HCl" },
                        { label: "Dosage *", key: "dosage", placeholder: "e.g. 500mg" },
                        { label: "Frequency *", key: "frequency", placeholder: "e.g. Twice daily" },
                        { label: "Duration", key: "duration", placeholder: "e.g. 30 days" },
                        { label: "Route", key: "route", placeholder: "e.g. Oral" },
                      ].map(({ label, key, placeholder }) => (
                        <div key={key}>
                          <label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>
                          <input value={(newMed as unknown as Record<string, string>)[key]}
                            onChange={(e) => setNewMed({ ...newMed, [key]: e.target.value })}
                            placeholder={placeholder} className="input text-xs" />
                        </div>
                      ))}
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">Instructions</label>
                      <input value={newMed.instructions}
                        onChange={(e) => setNewMed({ ...newMed, instructions: e.target.value })}
                        placeholder="e.g. Take with food" className="input text-xs" />
                    </div>
                    <div className="flex gap-2">
                      <button onClick={addMedication} className="bg-slate-800 text-white text-xs px-3 py-1.5 rounded-lg">Add</button>
                      <button onClick={() => setShowMedForm(false)} className="text-slate-500 text-xs px-3 py-1.5 rounded-lg">Cancel</button>
                    </div>
                  </div>
                )}
                {medications.length === 0
                  ? <p className="text-slate-400 text-xs">No medications added.</p>
                  : (
                    <div className="space-y-1">
                      {medications.map((m, i) => (
                        <div key={i} className="flex items-center justify-between text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white">
                          <div>
                            <span className="font-medium">{m.medicationName}</span>
                            <span className="text-slate-400 ml-2 text-xs">{m.dosage} · {m.frequency}{m.duration ? ` · ${m.duration}` : ""}</span>
                          </div>
                          <button onClick={() => setMedications((p) => p.filter((_, j) => j !== i))}
                            className="text-red-400 hover:text-red-600 text-xs ml-4">remove</button>
                        </div>
                      ))}
                    </div>
                  )}
              </div>

              {/* Lab Orders */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-sm font-semibold text-slate-700">Lab Orders</h3>
                  <button onClick={() => setShowLabForm(!showLabForm)}
                    className="text-xs border border-slate-300 rounded-lg px-2 py-1 hover:bg-slate-100">
                    + Add Lab Order
                  </button>
                </div>
                {showLabForm && (
                  <div className="border border-slate-200 rounded-xl p-4 bg-slate-50 space-y-3 mb-3">
                    <div className="grid grid-cols-2 gap-3">
                      {[
                        { label: "Test Code *", key: "testCode", placeholder: "e.g. CBC" },
                        { label: "Test Name *", key: "testName", placeholder: "e.g. Complete Blood Count" },
                        { label: "Clinical Indication", key: "clinicalIndication", placeholder: "e.g. Suspected anaemia" },
                      ].map(({ label, key, placeholder }) => (
                        <div key={key}>
                          <label className="block text-xs font-medium text-slate-600 mb-1">{label}</label>
                          <input value={(newLab as unknown as Record<string, string>)[key]}
                            onChange={(e) => setNewLab({ ...newLab, [key]: e.target.value })}
                            placeholder={placeholder} className="input text-xs" />
                        </div>
                      ))}
                      <div>
                        <label className="block text-xs font-medium text-slate-600 mb-1">Urgency</label>
                        <select value={newLab.urgency} onChange={(e) => setNewLab({ ...newLab, urgency: e.target.value })} className="input text-xs">
                          <option value="ROUTINE">Routine</option>
                          <option value="URGENT">Urgent</option>
                          <option value="STAT">STAT</option>
                        </select>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button onClick={addLabOrder} className="bg-slate-800 text-white text-xs px-3 py-1.5 rounded-lg">Add</button>
                      <button onClick={() => setShowLabForm(false)} className="text-slate-500 text-xs px-3 py-1.5 rounded-lg">Cancel</button>
                    </div>
                  </div>
                )}
                {labOrders.length === 0
                  ? <p className="text-slate-400 text-xs">No lab orders added.</p>
                  : (
                    <div className="space-y-1">
                      {labOrders.map((l, i) => (
                        <div key={i} className="flex items-center justify-between text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white">
                          <div>
                            <span className="font-medium">{l.testName}</span>
                            <span className="text-slate-400 ml-2 text-xs">{l.testCode}</span>
                            <span className={`ml-2 text-xs px-1.5 py-0.5 rounded ${
                              l.urgency === "STAT" ? "bg-red-100 text-red-600"
                              : l.urgency === "URGENT" ? "bg-amber-100 text-amber-700"
                              : "bg-slate-100 text-slate-500"
                            }`}>{l.urgency}</span>
                          </div>
                          <button onClick={() => setLabOrders((p) => p.filter((_, j) => j !== i))}
                            className="text-red-400 hover:text-red-600 text-xs ml-4">remove</button>
                        </div>
                      ))}
                    </div>
                  )}
              </div>

              <button onClick={createDraft} disabled={saving}
                className="bg-slate-800 text-white text-sm px-6 py-2 rounded-lg hover:bg-slate-700 disabled:opacity-40 transition-colors">
                {saving ? "Saving…" : "Create Prescription Draft"}
              </button>
            </>
          )}

          {activePrescription && (
            <div className="flex gap-3 flex-wrap">
              {activePrescription.status === "DRAFT" && (
                <button onClick={issuePrescription} disabled={saving}
                  className="bg-green-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-green-700 disabled:opacity-40">
                  {saving ? "…" : "Issue Prescription"}
                </button>
              )}
              {isIssued && (
                <>
                  <button onClick={sendToPharmacy}
                    className="text-sm px-4 py-2 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100">
                    Send to Pharmacy
                  </button>
                  {activePrescription.labOrders && activePrescription.labOrders.length > 0 && (
                    <button onClick={sendToLab}
                      className="text-sm px-4 py-2 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100">
                      Send to Lab
                    </button>
                  )}
                  <button onClick={downloadPdf}
                    className="text-sm px-4 py-2 rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-100">
                    Download PDF
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      )}

      {tab === "history" && (
        <div className="space-y-3 max-w-2xl">
          {prescriptions.length === 0
            ? <p className="text-slate-500 text-sm">No prescriptions on file.</p>
            : prescriptions
                .slice()
                .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                .map((rx) => (
                  <div key={rx.id} className="border border-slate-200 rounded-xl p-4 bg-white text-sm">
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-mono text-xs text-slate-400">{rx.id.slice(0, 8)}…</span>
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        rx.status === "ISSUED" || rx.status === "FULFILLED" ? "bg-green-100 text-green-700"
                        : rx.status === "DRAFT" ? "bg-yellow-100 text-yellow-700"
                        : "bg-blue-100 text-blue-700"
                      }`}>{rx.status}</span>
                    </div>
                    {rx.consultationNotes && (
                      <p className="text-slate-600 text-xs mb-2 line-clamp-2">{rx.consultationNotes}</p>
                    )}
                    {rx.medications && rx.medications.length > 0 && (
                      <div className="flex flex-wrap gap-1">
                        {rx.medications.map((m) => (
                          <span key={m.id} className="bg-slate-100 text-slate-600 text-xs px-2 py-0.5 rounded">
                            {m.medicationName} {m.dosage}
                          </span>
                        ))}
                      </div>
                    )}
                    <p className="text-slate-400 text-xs mt-2">
                      {format(new Date(rx.createdAt), "MMM d, yyyy")}
                    </p>
                  </div>
                ))}
        </div>
      )}
    </div>
  );
}
