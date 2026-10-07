"use client";

import { getCostExplorerData } from "@/lib/api";
import { useApi } from "@/hooks/use-api";
import { EmptyState } from "@/components/empty-state";
import { formatCompactCurrency, formatCurrency, formatPercent } from "@/lib/format";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import DashboardLayout from "@/components/dashboard/dashboard-layout";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ArrowRight } from "lucide-react";

export default function CostExplorerPage() {
  // The filter values are not sent because the backend's Cost Explorer call groups by
  // SERVICE and REGION server-side. A client-side filter would only hide rows, not
  // reduce what was queried, so the label states the real scope.
  const { data, loading, error, reload } = useApi(() => getCostExplorerData(), []);

  const currency = data?.currency ?? "USD";
  const daily = data?.dailyCosts ?? [];
  const breakdown = data?.serviceBreakdown ?? [];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Cost explorer</h1>
          <p className="text-sm text-muted-foreground">
            {data
              ? `${data.from} to ${data.to}, grouped by service across the whole account.`
              : "Spend attributed by AWS Cost Explorer."}
          </p>
        </div>

        {loading ? (
          <div className="h-72 animate-pulse rounded-lg bg-muted" />
        ) : error ? (
          <EmptyState
            variant="error"
            title="Could not load cost data"
            description={error}
            onRetry={reload}
          />
        ) : !data || !data.available ? (
          <EmptyState
            title="No cost data available"
            description="Cost Explorer returned no spend for this period. This is expected in the LocalStack sandbox, which does not implement Cost Explorer; run against a real AWS account to populate it."
          />
        ) : (
          <>
            <div className="grid gap-4 md:grid-cols-3">
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Total</CardDescription>
                  <CardTitle className="text-2xl">
                    {formatCompactCurrency(data.totalCost, currency)}
                  </CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Services billed</CardDescription>
                  <CardTitle className="text-2xl">{breakdown.length}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Largest service</CardDescription>
                  <CardTitle className="text-2xl">
                    {breakdown[0]?.service ?? "—"}
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-xs text-muted-foreground">
                    {breakdown[0]
                      ? `${formatCurrency(breakdown[0].cost, currency)} · ${formatPercent(breakdown[0].percentage)} of total`
                      : "No data"}
                  </p>
                </CardContent>
              </Card>
            </div>

            {daily.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle>Daily spend</CardTitle>
                  <CardDescription>Unblended cost per day</CardDescription>
                </CardHeader>
                <CardContent className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={daily}>
                      <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 11 }}
                        stroke="hsl(var(--muted-foreground))"
                      />
                      <YAxis
                        tick={{ fontSize: 11 }}
                        stroke="hsl(var(--muted-foreground))"
                        tickFormatter={(v: number) => formatCompactCurrency(v, currency)}
                      />
                      <Tooltip
                        formatter={(value: number) => formatCurrency(value, currency)}
                        contentStyle={{
                          background: "hsl(var(--popover))",
                          border: "1px solid hsl(var(--border))",
                          borderRadius: 8,
                        }}
                      />
                      <Line
                        type="monotone"
                        dataKey="cost"
                        stroke="hsl(var(--primary))"
                        strokeWidth={2}
                        dot={false}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            )}

            <Card>
              <CardHeader>
                <CardTitle>Spend by service</CardTitle>
                <CardDescription>
                  Percentages are of measured total, not of a fixed constant.
                </CardDescription>
              </CardHeader>
              <CardContent className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={breakdown} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis
                      type="number"
                      tick={{ fontSize: 11 }}
                      stroke="hsl(var(--muted-foreground))"
                      tickFormatter={(v: number) => formatCompactCurrency(v, currency)}
                    />
                    <YAxis
                      type="category"
                      dataKey="service"
                      tick={{ fontSize: 11 }}
                      stroke="hsl(var(--muted-foreground))"
                      width={110}
                    />
                    <Tooltip
                      formatter={(value: number) => formatCurrency(value, currency)}
                      contentStyle={{
                        background: "hsl(var(--popover))",
                        border: "1px solid hsl(var(--border))",
                        borderRadius: 8,
                      }}
                    />
                    <Bar dataKey="cost" fill="hsl(var(--primary))" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>

            {Object.keys(data.costByRegion).length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle>Spend by region</CardTitle>
                </CardHeader>
                <CardContent>
                  <ul className="space-y-2">
                    {Object.entries(data.costByRegion).map(([region, cost]) => (
                      <li
                        key={region}
                        className="flex items-center justify-between text-sm"
                      >
                        <span className="font-mono">{region}</span>
                        <span className="flex items-center gap-2">
                          {formatCurrency(cost, currency)}
                          <ArrowRight className="h-3 w-3 text-muted-foreground" aria-hidden />
                        </span>
                      </li>
                    ))}
                  </ul>
                </CardContent>
              </Card>
            )}
          </>
        )}
      </div>
    </DashboardLayout>
  );
}