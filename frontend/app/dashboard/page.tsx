"use client";

import { getDashboardOverview } from "@/lib/api";
import { useApi } from "@/hooks/use-api";
import { EmptyState } from "@/components/empty-state";
import {
  formatCompactCurrency,
  formatCurrency,
  formatDateTime,
  formatPercent,
} from "@/lib/format";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import DashboardLayout from "@/components/dashboard/dashboard-layout";
import {
  Activity,
  CircleDollarSign,
  TrendingUp,
  Unplug,
} from "lucide-react";

function Loading() {
  return (
    <div className="space-y-4">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-28 animate-pulse rounded-lg bg-muted" />
        ))}
      </div>
      <div className="h-64 animate-pulse rounded-lg bg-muted" />
    </div>
  );
}

export default function DashboardPage() {
  const { data, loading, error, reload } = useApi(getDashboardOverview, []);

  if (loading) {
    return (
      <DashboardLayout>
        <Loading />
      </DashboardLayout>
    );
  }

  if (error || !data) {
    return (
      <DashboardLayout>
        <EmptyState
          variant="error"
          title="Could not load the dashboard"
          description={error ?? "The backend did not return dashboard data."}
          onRetry={reload}
        />
      </DashboardLayout>
    );
  }

  const { executiveSummary, realTimeMetrics, topPerformers } = data;

  const metrics = [
    {
      label: "Realised savings",
      value: formatCompactCurrency(executiveSummary.realisedSavings),
      hint: "Recorded by completed remediation actions",
      icon: TrendingUp,
    },
    {
      label: "Monthly spend",
      value: executiveSummary.spendAvailable
        ? formatCompactCurrency(executiveSummary.monthlySpend)
        : "No data",
      hint: executiveSummary.spendAvailable
        ? "From Cost Explorer, last 30 days"
        : "Cost Explorer returned no spend for this account",
      icon: CircleDollarSign,
    },
    {
      label: "Accounts monitored",
      value: String(executiveSummary.totalTenantsMonitored),
      hint: `${executiveSummary.activeAutomations} recorded automation(s)`,
      icon: Activity,
    },
    {
      label: "Cloud connection",
      value: realTimeMetrics.connectedToCloud ? "Connected" : "Unreachable",
      hint: realTimeMetrics.connectedToCloud
        ? "Inventory discovery succeeded"
        : "The provider adapter failed to answer discovery",
      icon: realTimeMetrics.connectedToCloud ? Activity : Unplug,
    },
  ];

  const hasAnything =
    executiveSummary.realisedSavings > 0 ||
    executiveSummary.monthlySpend > 0 ||
    topPerformers.length > 0;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {metrics.map(({ label, value, hint, icon: Icon }) => (
            <Card key={label}>
              <CardHeader className="pb-2">
                <CardDescription className="flex items-center gap-2">
                  <Icon className="h-4 w-4" aria-hidden />
                  {label}
                </CardDescription>
                <CardTitle className="text-2xl">{value}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-muted-foreground">{hint}</p>
              </CardContent>
            </Card>
          ))}
        </div>

        {!hasAnything && (
          <EmptyState
            title="No activity recorded yet"
            description="Nothing has been measured for this account. Once the backend can reach Cost Explorer and a remediation is applied, figures appear here."
          />
        )}

        {executiveSummary.roiRatio !== null && (
          <Card>
            <CardHeader>
              <CardTitle>Return on spend</CardTitle>
              <CardDescription>
                Realised savings divided by measured monthly spend.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-semibold">
                {formatPercent(executiveSummary.roiRatio * 100)}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                {formatCurrency(executiveSummary.realisedSavings)} saved against{" "}
                {formatCurrency(executiveSummary.monthlySpend)} monthly spend
              </p>
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader>
            <CardTitle>Accounts</CardTitle>
            <CardDescription>
              Last updated {formatDateTime(data.lastUpdated)}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {topPerformers.length === 0 ? (
              <p className="text-sm text-muted-foreground">No accounts found.</p>
            ) : (
              <ul className="space-y-2">
                {topPerformers.map((performer) => (
                  <li
                    key={performer.tenantName}
                    className="flex items-center justify-between text-sm"
                  >
                    <span>{performer.tenantName}</span>
                    <Badge variant="secondary">
                      {formatCurrency(performer.savings)} saved
                    </Badge>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}