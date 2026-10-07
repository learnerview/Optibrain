"use client";

import {
  ActionType,
  ActionResult,
  executeRemediation,
  planRemediation,
  RemediationPlan,
} from "@/lib/api";
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
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import DashboardLayout from "@/components/dashboard/dashboard-layout";
import { useState } from "react";

const ACTION_TYPES: ActionType[] = [
  "STOP_INSTANCE",
  "START_INSTANCE",
  "TERMINATE_INSTANCE",
  "DELETE_VOLUME",
  "DELETE_SNAPSHOT",
  "RELEASE_ELASTIC_IP",
  "APPLY_TAGS",
  "SCALE_GROUP",
  "RESIZE_INSTANCE",
];

export default function RemediationPage() {
  const [actionType, setActionType] = useState<ActionType>("STOP_INSTANCE");
  const [resourceId, setResourceId] = useState("");
  const [preview, setPreview] = useState<RemediationPlan | null>(null);
  const [result, setResult] = useState<ActionResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function plan() {
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      setPreview(await planRemediation({ actionType, resourceId }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Request failed");
    } finally {
      setBusy(false);
    }
  }

  async function execute(dryRun: boolean) {
    setBusy(true);
    setError(null);
    try {
      setResult(await executeRemediation({ actionType, resourceId, dryRun }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Request failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Remediation</h1>
          <p className="text-sm text-muted-foreground">
            Plan a change before approving it. Execution honours the global dry-run
            interlock and refuses protected resources.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Action</CardTitle>
            <CardDescription>
              Identifiers come from Resources. Planning never mutates the account.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-wrap items-end gap-3">
            <select
              className="rounded-md border bg-background px-3 py-2 text-sm"
              value={actionType}
              onChange={(event) => {
                setActionType(event.target.value as ActionType);
                setPreview(null);
                setResult(null);
              }}
            >
              {ACTION_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
            <Input
              placeholder="Resource id, e.g. i-0123456789abcdef0"
              value={resourceId}
              onChange={(event) => setResourceId(event.target.value)}
              className="w-96 font-mono text-sm"
            />
            <Button onClick={plan} disabled={busy || !resourceId.trim()}>
              {busy ? "Working…" : "Plan"}
            </Button>
            {preview && (
              <>
                <Button
                  variant="outline"
                  disabled={busy || !preview.executable}
                  onClick={() => execute(false)}
                >
                  Execute
                </Button>
                <Button
                  variant="outline"
                  disabled={busy}
                  onClick={() => execute(true)}
                >
                  Dry-run execute
                </Button>
              </>
            )}
          </CardContent>
        </Card>

        {error ? (
          <EmptyState variant="error" title="Remediation request failed" description={error} />
        ) : null}

        {preview ? <PlanCard plan={preview} /> : null}
        {result ? <ResultCard result={result} /> : null}
      </div>
    </DashboardLayout>
  );
}

function PlanCard({ plan }: { plan: RemediationPlan }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Plan</CardTitle>
        <CardDescription>
          {plan.resourceType} in {plan.region} · state {plan.currentState}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex flex-wrap gap-2">
          <Badge>{plan.actionType}</Badge>
          <Badge variant="outline">{plan.risk} risk</Badge>
          {plan.destructive ? <Badge variant="destructive">destructive</Badge> : null}
          {plan.protectedResource ? <Badge variant="destructive">protected</Badge> : null}
          {plan.sandboxed ? <Badge variant="outline">sandbox</Badge> : null}
          {plan.dryRunOnly ? <Badge variant="outline">global dry-run</Badge> : null}
          <Badge variant={plan.executable ? "default" : "destructive"}>
            {plan.executable ? "executable" : "blocked"}
          </Badge>
        </div>
        {plan.blockedReason ? (
          <p className="text-sm text-red-600">{plan.blockedReason}</p>
        ) : null}
        <div className="text-sm">
          <p>Monthly cost: {plan.monthlyCost != null ? formatCompactCurrency(plan.monthlyCost) : "unattributed"}</p>
          <p>Estimated saving: {plan.estimatedMonthlySavings != null ? formatCompactCurrency(plan.estimatedMonthlySavings) : "not stated"}</p>
        </div>
        <ol className="list-inside list-decimal text-sm text-muted-foreground">
          {plan.steps.map((step) => (
            <li key={step}>{step}</li>
          ))}
        </ol>
      </CardContent>
    </Card>
  );
}

function ResultCard({ result }: { result: ActionResult }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Outcome</CardTitle>
        <CardDescription>{result.message}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="flex flex-wrap gap-2">
          <Badge variant={result.success ? "default" : "destructive"}>
            {result.success ? "success" : "not applied"}
          </Badge>
          <Badge variant={result.applied ? "default" : "outline"}>
            {result.applied ? "applied" : "dry-run"}
          </Badge>
          {result.error ? <Badge variant="destructive">{result.error}</Badge> : null}
        </div>
      </CardContent>
    </Card>
  );
}
