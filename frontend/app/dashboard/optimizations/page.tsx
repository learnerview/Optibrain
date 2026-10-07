"use client";

import { getOptimizationHistory } from "@/lib/api";
import { useApi } from "@/hooks/use-api";
import { EmptyState } from "@/components/empty-state";
import { formatCurrency, formatDateTime } from "@/lib/format";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import DashboardLayout from "@/components/dashboard/dashboard-layout";
import { useMemo, useState } from "react";

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  APPLIED: "default",
  PENDING: "secondary",
  FAILED: "destructive",
  REJECTED: "outline",
};

export default function OptimizationsPage() {
  const { data, loading, error, reload } = useApi(getOptimizationHistory, []);
  const [status, setStatus] = useState("all");

  const optimizations = data ?? [];

  const statuses = useMemo(
    () => Array.from(new Set(optimizations.map((o) => o.status))).sort(),
    [optimizations]
  );

  const filtered = useMemo(
    () =>
      status === "all"
        ? optimizations
        : optimizations.filter((o) => o.status === status),
    [optimizations, status]
  );

  const totalSavings = optimizations.reduce(
    (sum, o) => sum + (o.estimatedSavings ?? 0),
    0
  );

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Optimisation history</h1>
          <p className="text-sm text-muted-foreground">
            Remediation actions recorded in the audit log.
          </p>
        </div>

        {loading ? (
          <div className="h-64 animate-pulse rounded-lg bg-muted" />
        ) : error ? (
          <EmptyState
            variant="error"
            title="Could not load optimisation history"
            description={error}
            onRetry={reload}
          />
        ) : optimizations.length === 0 ? (
          <EmptyState
            title="No optimisations recorded"
            description="No remediation has been applied to this account. Actions appear here once a recommendation is executed or simulated."
          />
        ) : (
          <>
            <Card>
              <CardHeader className="pb-2">
                <CardDescription>Recorded savings across {optimizations.length} action(s)</CardDescription>
                <CardTitle className="text-2xl">
                  {formatCurrency(totalSavings)}
                </CardTitle>
              </CardHeader>
            </Card>

            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => setStatus("all")}
                className={`rounded-md border px-3 py-1.5 text-sm ${
                  status === "all" ? "bg-primary text-primary-foreground" : "hover:bg-accent"
                }`}
              >
                All
              </button>
              {statuses.map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setStatus(value)}
                  className={`rounded-md border px-3 py-1.5 text-sm ${
                    status === value ? "bg-primary text-primary-foreground" : "hover:bg-accent"
                  }`}
                >
                  {value}
                </button>
              ))}
            </div>

            {filtered.length === 0 ? (
              <EmptyState
                title="No matching actions"
                description={`No recorded action has status "${status}".`}
              />
            ) : (
              <div className="grid gap-3">
                {filtered.map((optimization) => (
                  <Card key={optimization.id}>
                    <CardHeader className="pb-2">
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <CardTitle className="text-base">
                            {optimization.action}
                          </CardTitle>
                          <CardDescription className="font-mono text-xs">
                            {optimization.resourceId ?? "no resource recorded"}
                            {optimization.region ? ` · ${optimization.region}` : ""}
                          </CardDescription>
                        </div>
                        <div className="flex items-center gap-2">
                          <Badge variant={STATUS_VARIANT[optimization.status] ?? "secondary"}>
                            {optimization.status}
                          </Badge>
                          <span className="text-sm font-medium">
                            {formatCurrency(optimization.estimatedSavings)}
                          </span>
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent className="text-sm text-muted-foreground">
                      {optimization.explanation && <p>{optimization.explanation}</p>}
                      <p className="mt-1 text-xs">
                        {formatDateTime(optimization.timestamp)}
                      </p>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </DashboardLayout>
  );
}