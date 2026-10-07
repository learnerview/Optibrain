"use client";

import { getTagCoverage, TagCoverageReport } from "@/lib/api";
import { useApi } from "@/hooks/use-api";
import { EmptyState } from "@/components/empty-state";
import { formatCompactCurrency } from "@/lib/format";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import DashboardLayout from "@/components/dashboard/dashboard-layout";

export default function TagsPage() {
  const { data, loading, error, reload } = useApi(getTagCoverage, []);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Tag governance</h1>
          <p className="text-sm text-muted-foreground">
            Which resources carry the required tags, and what the rest cost.
          </p>
        </div>

        {loading ? (
          <div className="h-64 animate-pulse rounded-lg bg-muted" />
        ) : error ? (
          <EmptyState
            variant="error"
            title="Could not load tag coverage"
            description={error}
            onRetry={reload}
          />
        ) : !data?.available ? (
          <EmptyState
            title="No inventory to assess"
            description={
              data?.reason ??
              "The connected account returned no resources to tag-assess."
            }
          />
        ) : (
          <TagCoverageContent data={data} />
        )}
      </div>
    </DashboardLayout>
  );
}

function TagCoverageContent({ data }: { data: TagCoverageReport }) {
  return (
    <>
      <div className="grid gap-4 md:grid-cols-4">
        <MetricCard
          title="Coverage"
          value={`${data.coveragePercentage}%`}
          subtitle={`${data.compliantResources}/${data.resourcesAssessed} compliant`}
        />
        <MetricCard
          title="Uncovered spend"
          value={formatCompactCurrency(data.uncoveredMonthlyCost ?? 0)}
          subtitle="attributed monthly cost"
        />
        <MetricCard
          title="Share of spend uncovered"
          value={`${data.uncoveredShareOfSpend ?? 0}%`}
          subtitle="of attributed cost"
        />
        <MetricCard
          title="Uncosted resources"
          value={String(data.uncostedResourceCount)}
          subtitle="cost not attributable"
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Attribution</CardTitle>
          <CardDescription>
            Required keys: {data.requiredKeys.join(", ")} · enforcement:{" "}
            {data.enforcement}
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {data.offenders.length === 0 ? (
            <p className="p-6 text-sm text-muted-foreground">
              Every resource carries the required tags.
            </p>
          ) : (
            <table className="w-full text-sm">
              <thead className="border-b text-left text-muted-foreground">
                <tr>
                  <th className="p-3 font-medium">Resource</th>
                  <th className="p-3 font-medium">Type</th>
                  <th className="p-3 font-medium">Region</th>
                  <th className="p-3 font-medium">Missing</th>
                  <th className="p-3 text-right font-medium">Monthly cost</th>
                </tr>
              </thead>
              <tbody>
                {data.offenders.map((offender) => (
                  <tr key={offender.resourceId} className="border-b last:border-0">
                    <td className="p-3 font-mono text-xs">
                      {offender.resourceId}
                    </td>
                    <td className="p-3">{offender.resourceType}</td>
                    <td className="p-3">{offender.region}</td>
                    <td className="p-3">
                      <div className="flex gap-1">
                        {offender.missingKeys.map((key) => (
                          <Badge key={key} variant="outline">
                            {key}
                          </Badge>
                        ))}
                      </div>
                    </td>
                    <td className="p-3 text-right">
                      {offender.monthlyCost != null
                        ? formatCompactCurrency(offender.monthlyCost)
                        : "unattributed"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </>
  );
}

function MetricCard({
  title,
  value,
  subtitle,
}: {
  title: string;
  value: string;
  subtitle: string;
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardDescription>{title}</CardDescription>
        <CardTitle className="text-2xl">{value}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-xs text-muted-foreground">{subtitle}</p>
      </CardContent>
    </Card>
  );
}
