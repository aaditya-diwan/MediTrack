import { PolicyResponse } from "@/lib/types";
import { Badge, Card, CardTitle, DetailLabel, DetailValue } from "@/components/ui";
import { format } from "date-fns";

const money = (n: number) => `$${Number(n).toLocaleString()}`;

export default function InsurancePolicyCard({
  policy,
}: {
  policy: PolicyResponse;
}) {
  return (
    <Card>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <CardTitle className="truncate">{policy.planName}</CardTitle>
          <p className="mt-0.5 text-sm text-ink-muted">{policy.payerName}</p>
        </div>
        <Badge tone={policy.active ? "ok" : "neutral"}>
          {policy.active ? "Active" : "Inactive"}
        </Badge>
      </div>

      <dl className="mt-4 grid grid-cols-2 gap-x-6 gap-y-3 sm:grid-cols-3">
        <div>
          <DetailLabel>Policy #</DetailLabel>
          <DetailValue mono>{policy.policyNumber}</DetailValue>
        </div>
        <div>
          <DetailLabel>Group #</DetailLabel>
          <DetailValue mono>{policy.groupNumber}</DetailValue>
        </div>
        <div>
          <DetailLabel>Subscriber</DetailLabel>
          <DetailValue>
            {policy.subscriberName}{" "}
            <span className="text-ink-faint">
              ({policy.relationship.replaceAll("_", " ").toLowerCase()})
            </span>
          </DetailValue>
        </div>
        <div>
          <DetailLabel>Effective</DetailLabel>
          <DetailValue mono>
            {format(new Date(policy.effectiveDate), "MMM d, yyyy")}
          </DetailValue>
        </div>
        {policy.terminationDate && (
          <div>
            <DetailLabel>Ends</DetailLabel>
            <DetailValue mono>
              {format(new Date(policy.terminationDate), "MMM d, yyyy")}
            </DetailValue>
          </div>
        )}
        <div>
          <DetailLabel>Copay</DetailLabel>
          <DetailValue mono>{money(policy.copayAmount)}</DetailValue>
        </div>
        <div>
          <DetailLabel>Deductible met</DetailLabel>
          <DetailValue mono>
            {money(policy.deductibleMet)} / {money(policy.deductibleAmount)}
          </DetailValue>
        </div>
        <div>
          <DetailLabel>Out-of-pocket met</DetailLabel>
          <DetailValue mono>
            {money(policy.outOfPocketMet)} / {money(policy.outOfPocketMax)}
          </DetailValue>
        </div>
      </dl>
    </Card>
  );
}
