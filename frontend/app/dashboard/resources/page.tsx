"use client";

import { getResources } from "@/lib/api";
import { useApi } from "@/hooks/use-api";
import { EmptyState } from "@/components/empty-state";
import { formatCompactCurrency, formatDate } from "@/lib/format";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import DashboardLayout from "@/components/dashboard/dashboard-layout";
import { useMemo, useState } from "react";
import { ShieldCheck } from "lucide-react";

export default function ResourcesPage() {
  const { data, loading, error, reload } = useApi(getResources, []);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("all");

  const resources = data ?? [];

  // Status options come from what the account actually contains rather than a fixed
  // ["Running", "Idle", "Orphaned"] list, which described a demo account, not any
  // particular one.
  const statuses = useMemo(
    () => Array.from(new Set(resources.map((r) => r.status))).sort(),
    [resources]
  );

  const filtered = useMemo(
    () =>
      resources.filter((resource) => {
        if (status !== "all" && resource.status !== status) return false;
        if (!search) return true;
        const needle = search.toLowerCase();
        return (
          resource.id.toLowerCase().includes(needle) ||
          resource.name.toLowerCase().includes(needle) ||
          resource.type.toLowerCase().includes(needle)
        );
      }),
    [resources, search, status]
  );

  const totalMonthly = resources.reduce(
    (sum, r) => sum + (r.monthlyCost ?? 0),
    0
  );
  const unattachedVolumes = resources.filter(
    (r) => r.type === "Volume" && r.specs?.attachmentCount === "0"
  ).length;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Resources</h1>
          <p className="text-sm text-muted-foreground">
            Live inventory discovered from the configured AWS account.
          </p>
        </div>

        {loading ? (
          <div className="h-64 animate-pulse rounded-lg bg-muted" />
        ) : error ? (
          <EmptyState
            variant="error"
            title="Could not load resources"
            description={error}
            onRetry={reload}
          />
        ) : resources.length === 0 ? (
          <EmptyState
            title="No resources found"
            description="The inventory scan returned nothing. In the LocalStack sandbox this usually means the sandbox has not been seeded - run `docker compose --profile seed run --rm seed`."
          />
        ) : (
          <>
            <div className="grid gap-4 md:grid-cols-3">
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Total resources</CardDescription>
                  <CardTitle className="text-2xl">{resources.length}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Attributed monthly cost</CardDescription>
                  <CardTitle className="text-2xl">
                    {totalMonthly > 0
                      ? formatCompactCurrency(totalMonthly)
                      : "No data"}
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-xs text-muted-foreground">
                    {totalMonthly > 0
                      ? "Sum of per-resource estimates"
                      : "Cost Explorer attributes no spend in this account"}
                  </p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Unattached volumes</CardDescription>
                  <CardTitle className="text-2xl">{unattachedVolumes}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-xs text-muted-foreground">
                    Eligible for cleanup review
                  </p>
                </CardContent>
              </Card>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <Input
                placeholder="Search by id, name or type"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                className="max-w-xs"
                aria-label="Search resources"
              />
              <div className="flex flex-wrap gap-2">
                <Button
                  size="sm"
                  variant={status === "all" ? "default" : "outline"}
                  onClick={() => setStatus("all")}
                >
                  All
                </Button>
                {statuses.map((value) => (
                  <Button
                    key={value}
                    size="sm"
                    variant={status === value ? "default" : "outline"}
                    onClick={() => setStatus(value)}
                  >
                    {value}
                  </Button>
                ))}
              </div>
            </div>

            {filtered.length === 0 ? (
              <EmptyState
                title="No matches"
                description="No resource matches the current search and status filter."
              />
            ) : (
              <div className="grid gap-3">
                {filtered.map((resource) => (
                  <Card key={resource.id}>
                    <CardHeader className="pb-2">
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <CardTitle className="text-base">
                            {resource.name || resource.id}
                          </CardTitle>
                          <CardDescription className="font-mono text-xs">
                            {resource.id}
                          </CardDescription>
                        </div>
                        <div className="flex items-center gap-2">
                          {resource.protected && (
                            <Badge variant="outline">
                              <ShieldCheck className="mr-1 h-3 w-3" aria-hidden />
                              Protected
                            </Badge>
                          )}
                          <Badge variant="secondary">{resource.status}</Badge>
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent className="text-sm text-muted-foreground">
                      <dl className="grid grid-cols-2 gap-x-4 gap-y-1 sm:grid-cols-4">
                        <div>
                          <dt className="text-xs uppercase">Type</dt>
                          <dd>{resource.type}</dd>
                        </div>
                        <div>
                          <dt className="text-xs uppercase">Region</dt>
                          <dd>{resource.region}</dd>
                        </div>
                        <div>
                          <dt className="text-xs uppercase">Monthly cost</dt>
                          <dd>
                            {resource.monthlyCost !== null
                              ? formatCompactCurrency(resource.monthlyCost)
                              : "Unattributed"}
                          </dd>
                        </div>
                        <div>
                          <dt className="text-xs uppercase">Created</dt>
                          <dd>{formatDate(resource.specs?.createdAt)}</dd>
                        </div>
                      </dl>
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