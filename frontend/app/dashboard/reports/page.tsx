"use client";

import { getMonthlyReport } from "@/lib/api";
import { useApi } from "@/hooks/use-api";
import { EmptyState } from "@/components/empty-state";
import { formatCompactCurrency, formatCurrency, formatDate } from "@/lib/format";
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

export default function ReportsPage() {
  const { data, loading, error, reload } = useApi(getMonthlyReport, []);

  const currency = data?.currency ?? "USD";

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Monthly report</h1>
          <p className="text-sm text-muted-foreground">
            {data
              ? `${data.reportMonth} ${data.reportYear}, generated ${formatDate(data.generatedDate)}.`
              : "Spend summary derived from Cost Explorer."}
          </p>
        </div>

        {loading ? (
          <div className="h-72 animate-pulse rounded-lg bg-muted" />
        ) : error ? (
          <EmptyState
            variant="error"
            title="Could not load the monthly report"
            description={error}
            onRetry={reload}
          />
        ) : !data || data.status === "UNAVAILABLE" ? (
          <EmptyState
            variant="error"
            title="Report unavailable"
            description="The provider could not be reached, so the report is not produced. A placeholder zero-total would misrepresent the account period; retry when Cost Explorer is reachable."
            onRetry={reload}
          />
        ) : !data.available || data.status === "EMPTY" ? (
          <EmptyState
            title="No spend measured"
            description="Cost Explorer returned no spend for the reporting period, so there is nothing to summarise. Expected in the LocalStack sandbox."
          />
        ) : (
          <>
            <div className="grid gap-4 md:grid-cols-3">
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Total spend</CardDescription>
                  <CardTitle className="text-2xl">
                    {formatCompactCurrency(data.summary.totalCost, currency)}
                  </CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Resources discovered</CardDescription>
                  <CardTitle className="text-2xl">{data.summary.resourceCount}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Regions billed</CardDescription>
                  <CardTitle className="text-2xl">
                    {Object.keys(data.summary.costByRegion).length}
                  </CardTitle>
                </CardHeader>
              </Card>
            </div>

            {data.dailySpend.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle>Daily spend</CardTitle>
                </CardHeader>
                <CardContent className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={data.dailySpend}>
                      <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                      <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
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
                      <Line type="monotone" dataKey="cost" stroke="hsl(var(--primary))" strokeWidth={2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            )}

            {data.topCostServices.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle>Top services</CardTitle>
                </CardHeader>
                <CardContent className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={data.topCostServices} layout="vertical">
                      <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                      <XAxis
                        type="number"
                        tick={{ fontSize: 11 }}
                        stroke="hsl(var(--muted-foreground))"
                        tickFormatter={(v: number) => formatCompactCurrency(v, currency)}
                      />
                      <YAxis type="category" dataKey="service" tick={{ fontSize: 11 }} width={110} stroke="hsl(var(--muted-foreground))" />
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
            )}
          </>
        )}
      </div>
    </DashboardLayout>
  );
}