import { AbnormalFlag } from "@/lib/types";

const flagConfig: Record<AbnormalFlag, { label: string; className: string }> = {
  NORMAL: { label: "Normal", className: "bg-green-100 text-green-800" },
  LOW: { label: "Low", className: "bg-yellow-100 text-yellow-800" },
  HIGH: { label: "High", className: "bg-orange-100 text-orange-800" },
  CRITICALLY_LOW: { label: "Critical Low", className: "bg-red-100 text-red-800 font-semibold" },
  CRITICALLY_HIGH: { label: "Critical High", className: "bg-red-100 text-red-800 font-semibold" },
  ABNORMAL: { label: "Abnormal", className: "bg-purple-100 text-purple-800" },
};

export default function LabResultBadge({ flag }: { flag: AbnormalFlag }) {
  const { label, className } = flagConfig[flag] ?? flagConfig.ABNORMAL;
  return (
    <span className={`inline-block px-2 py-0.5 rounded text-xs ${className}`}>
      {label}
    </span>
  );
}
