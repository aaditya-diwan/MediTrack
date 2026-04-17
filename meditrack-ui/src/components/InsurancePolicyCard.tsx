import { PolicyResponse } from "@/lib/types";
import { format } from "date-fns";

export default function InsurancePolicyCard({
  policy,
}: {
  policy: PolicyResponse;
}) {
  return (
    <div className="border border-slate-200 rounded-lg p-4 text-sm space-y-2">
      <div className="flex justify-between items-start">
        <div>
          <p className="font-semibold text-slate-800">{policy.planName}</p>
          <p className="text-slate-500">{policy.payerName}</p>
        </div>
        <span
          className={`px-2 py-0.5 rounded text-xs font-medium ${
            policy.active
              ? "bg-green-100 text-green-800"
              : "bg-slate-100 text-slate-500"
          }`}
        >
          {policy.active ? "Active" : "Inactive"}
        </span>
      </div>
      <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-slate-600">
        <span className="text-slate-400">Policy #</span>
        <span>{policy.policyNumber}</span>
        <span className="text-slate-400">Group #</span>
        <span>{policy.groupNumber}</span>
        <span className="text-slate-400">Subscriber</span>
        <span>
          {policy.subscriberName} ({policy.relationship})
        </span>
        <span className="text-slate-400">Effective</span>
        <span>{format(new Date(policy.effectiveDate), "MMM d, yyyy")}</span>
        {policy.terminationDate && (
          <>
            <span className="text-slate-400">Ends</span>
            <span>{format(new Date(policy.terminationDate), "MMM d, yyyy")}</span>
          </>
        )}
        <span className="text-slate-400">Copay</span>
        <span>${policy.copayAmount}</span>
        <span className="text-slate-400">Deductible</span>
        <span>
          ${policy.deductibleMet} / ${policy.deductibleAmount}
        </span>
        <span className="text-slate-400">Out-of-Pocket</span>
        <span>
          ${policy.outOfPocketMet} / ${policy.outOfPocketMax}
        </span>
      </div>
    </div>
  );
}
