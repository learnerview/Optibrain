"use client";

import {
  Tenant,
  createTenant,
  deactivateTenant,
  listTenants,
  reactivateTenant,
} from "@/lib/api";
import { useApi } from "@/hooks/use-api";
import { EmptyState } from "@/components/empty-state";
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
import { useState } from "react";

export default function TenantsPage() {
  const { data, loading, error, reload } = useApi(listTenants, []);
  const [name, setName] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);

  async function create() {
    setActionError(null);
    try {
      await createTenant({ id: name.trim().toLowerCase().replace(/\s+/g, "-"), name });
      setName("");
      reload();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "Create failed");
    }
  }

  async function toggle(tenant: Tenant) {
    setActionError(null);
    try {
      if (tenant.active) {
        await deactivateTenant(tenant.id);
      } else {
        await reactivateTenant(tenant.id);
      }
      reload();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "Action failed");
    }
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Tenants</h1>
          <p className="text-sm text-muted-foreground">
            Organisations whose cloud accounts this deployment manages.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Add tenant</CardTitle>
            <CardDescription>Name becomes the id in lower-case-slug form.</CardDescription>
          </CardHeader>
          <CardContent className="flex items-end gap-3">
            <Input
              placeholder="e.g. Acme Analytics"
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
            <Button onClick={create} disabled={!name.trim()}>
              Create
            </Button>
          </CardContent>
        </Card>

        {actionError ? (
          <EmptyState variant="error" title="Request failed" description={actionError} />
        ) : null}

        {loading ? (
          <div className="h-64 animate-pulse rounded-lg bg-muted" />
        ) : error ? (
          <EmptyState variant="error" title="Could not load tenants" description={error} onRetry={reload} />
        ) : !data || data.length === 0 ? (
          <EmptyState title="No tenants" description="Create the first tenant to get started." />
        ) : (
          <Card>
            <CardContent className="p-0">
              <table className="w-full text-sm">
                <thead className="border-b text-left text-muted-foreground">
                  <tr>
                    <th className="p-3 font-medium">Name</th>
                    <th className="p-3 font-medium">Id</th>
                    <th className="p-3 font-medium">Plan</th>
                    <th className="p-3 font-medium">Mode</th>
                    <th className="p-3 font-medium">Status</th>
                    <th className="p-3 text-right font-medium">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((tenant) => (
                    <tr key={tenant.id} className="border-b last:border-0">
                      <td className="p-3">{tenant.name}</td>
                      <td className="p-3 font-mono text-xs">{tenant.id}</td>
                      <td className="p-3">{tenant.plan ?? "—"}</td>
                      <td className="p-3">{tenant.mode}</td>
                      <td className="p-3">
                        <Badge variant={tenant.active ? "default" : "outline"}>
                          {tenant.active ? "active" : "deactivated"}
                        </Badge>
                      </td>
                      <td className="p-3 text-right">
                        <Button variant="outline" size="sm" onClick={() => toggle(tenant)}>
                          {tenant.active ? "Deactivate" : "Reactivate"}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
}
