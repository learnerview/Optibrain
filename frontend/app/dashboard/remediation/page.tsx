"use client";

import {
  ActionType,
  ActionResult,
  executeRemediation,
  getRemediationHistory,
  listRemediationActions,
  planRemediation,
  RemediationActionSummary,
  RemediationExecutionRecord,
  RemediationPlan,
} from "@/lib/api";
import { EmptyState } from "@/components/empty-state";
import { formatCompactCurrency, formatDateTime } from "@/lib/format";
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
import { useEffect, useState } from "react";

const FALLBACK_ACTION_TYPES: ActionType[] = [
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

/** Best-effort client correlation id; real UUIDs when crypto.randomUUID exists. */
function newIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export default function RemediationPage() {
  const [actionTypes, setActionTypes] = useState<RemediationActionSummary[] | null>(null);
  const [actionType, setActionType] = useState<ActionType>("STOP_INSTANCE");
  const [resourceId, setResourceId] = useState("");
  const [preview, setPreview] = useState<RemediationPlan | null>(null);
  const [result, setResult] = useState<ActionResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [idempotencyKey, setIdempotencyKey] = useState<string | null>(null);
  const [history, setHistory] = useState<RemediationExecutionRecord[]>([]);

  async function refreshHistory() {
    try {
      setHistory(await getRemediationHistory());
    } catch {
      setHistory([]);
    }
  }

  useEffect(() => {
    let cancelled = false;
    listRemediationActions()
      .then((actions) => {
        if (cancelled) return;
        setActionTypes(actions);
        if (!actions.some((a) => a.type === actionType)) {
          setActionType(actions[0]?.type ?? "STOP_INSTANCE");
        }
      })
      .catch(() => {
        if (!cancelled) setActionTypes([]);
      });
    refreshHistory();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function plan() {
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      // Rotate the correlation id each time a fresh plan is made, so re-planning after
      // the resource changed still performs a new execution instead of replaying the old.
      setIdempotencyKey(newIdempotencyKey());
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
      setResult(
        await executeRemediation({
          actionType,
          resourceId,
          dryRun,
          idempotencyKey: idempotencyKey ?? undefined,
          // The plan token pins the execution to the state the reviewer saw.
          planToken: preview?.token,
        }),
      );
      await refreshHistory();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Request failed");
    } finally {
      setBusy(false);
    }
  }

  const availableActions = actionTypes?.length
    ? actionTypes.map((a) => a.type)
    : FALLBACK_ACTION_TYPES;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Remediation</h1>
          <p className="text-sm text-muted-foreground">
            Plan a change before approving it. Execution honours the global dry-run
            interlock, refuses protected resources, refuses stale plans, and replays a
            repeated request instead of mutating the account twice.
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
              {availableActions.map((type) => (
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
          {preview ? (
            <CardContent className="border-t pt-4 text-xs text-muted-foreground">
              <span className="font-mono">
                plan token {preview.token} · idempotency key {idempotencyKey}
              </span>
              <p className="mt-1">
                Execution echoes the token back, so a plan drawn on stale state is
                refused. A repeated request with the same key replays this plan&rsquo;s
                stored outcome rather than running again.
              </p>
            </CardContent>
          ) : null}
        </Card>

        {error ? (
          <EmptyState variant="error" title="Remediation request failed" description={error} />
        ) : null}

        {preview ? <PlanCard plan={preview} /> : null}
        {result ? <ResultCard result={result} /> : null}

        {history.length > 0 ? (
          <HistoryPanel history={history} />
        ) : (
          <p className="text-sm text-muted-foreground">
            No remediations have run in this session.
          </p>
        )}
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
  const rejectedBeforeDispatch = !result.success && !!result.error;
  return (
    <Card>
      <CardHeader>
        <CardTitle>Outcome</CardTitle>
        <CardDescription>{result.message}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="flex flex-wrap gap-2">
          <Badge variant={result.success ? "default" : "destructive"}>
            {result.success ? "success" : result.dryRun ? "simulated" : "not applied"}
          </Badge>
          <Badge variant={result.applied ? "default" : "outline"}>
            {result.applied ? "applied" : "dry-run"}
          </Badge>
          {result.estimatedMonthlySavings != null ? (
            <Badge variant="outline">
              saves {formatCompactCurrency(result.estimatedMonthlySavings)}/mo
            </Badge>
          ) : null}
          {rejectedBeforeDispatch ? (
            <Badge variant="destructive">refused before dispatch</Badge>
          ) : null}
          {result.error ? <p className="text-sm text-red-600">{result.error}</p> : null}
        </div>
      </CardContent>
    </Card>
  );
}

function HistoryPanel({ history }: { history: RemediationExecutionRecord[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Session history</CardTitle>
        <CardDescription>Actions requested this session, most recent first.</CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="space-y-2">
          {history.map((entry) => (
            <li
              key={`${entry.executedAt}-${entry.resourceId}`}
              className="flex flex-wrap items-center gap-2 text-sm"
            >
              <Badge variant="outline">{entry.type}</Badge>
              <span className="font-mono text-xs">{entry.resourceId}</span>
              <Badge variant={entry.success ? "default" : "destructive"}>
                {entry.success ? "success" : "failed"}
              </Badge>
              {entry.dryRun ? <Badge variant="outline">dry-run</Badge> : null}
              {entry.applied ? <Badge variant="outline">applied</Badge> : null}
              <span className="text-xs text-muted-foreground">
                {formatDateTime(entry.executedAt)}
              </span>
              {entry.message ? <span className="text-xs text-muted-foreground">{entry.message}</span> : null}
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}