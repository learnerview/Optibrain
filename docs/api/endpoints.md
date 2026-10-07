# API Reference

Base path `/api` (port 8080 by default). Every response uses the `ApiResponse` envelope:

```json
{
  "success": true,
  "message": "...",
  "data": {},
  "timestamp": "2026-10-07T09:00:00",
  "traceId": null
}
```

Errors return `"success": false` with a `message`. `traceId` is not populated today.

Path prefixes, added to all of the below:
- `/api/**` requires a valid JWT (`Authorization: Bearer <token>`) in every profile.
- Only `POST /api/auth/login` and `POST /api/auth/register` are public.
- `GET /actuator/health`, `GET /actuator/info`, `/error`, `/swagger-ui/**` and
  `/v3/api-docs/**` are public.
- The tenant a request runs under is the membership of the authenticated user, never the
  header. The legacy `X-TENANT` / `X-USER` headers are tolerated only when they exactly
  match the principal; a mismatch returns 403 with
  `{"success":false,"message":"X-TENANT does not match the authenticated user's tenant","data":null}`.

## Error envelope

Handled centrally by `GlobalExceptionHandler`:

| Status | Case | Example `message` |
|---|---|---|
| 400 | Missing/blank login input, invalid body, missing/invalid parameter | `"Username and password are required"` |
| 401 | Bad credentials | `"Invalid credentials"` |
| 403 | Method-level access denied (RBAC) | `"You do not have permission to perform this action"` |
| 403 | Tenant/header mismatch (TenantFilter) | `"X-TENANT does not match the authenticated user's tenant"` |
| 404 | Unknown route | `"No endpoint at /api/costs/foo"` |
| 404 | Missing resource | `"No active tenant with id 'acme'"` |
| 405 | Wrong method on a real route | `"GET is not supported for this path"` |
| 409 | Duplicate user / tenant | `"User already exists"` |
| 500 | Unhandled exception | `"Internal error"` |

## Authentication

| Method | Path | Behaviour |
|---|---|---|
| POST | `/api/auth/login` | `200` with user + token; `400` blank input; `401` invalid credentials |
| POST | `/api/auth/register` | `200` creates a `USER` and returns a token; `400` validation failure; `409` existing username |
| POST | `/api/auth/logout` | Revokes the presented token; always `200` |
| GET | `/api/auth/status` | Session state for the current request |

`login` response `data`:

```json
{
  "username": "admin",
  "authenticated": true,
  "authorities": ["ROLE_ADMIN"],
  "token": "<jwt>",
  "tenantId": "default"
}
```

`register` accepts `{ "username", "password", "company" }` (username/password validated
via bean validation) and assigns `ROLE_USER` plus the default tenant.

## Inventory

| Method | Path | Returns |
|---|---|---|
| GET | `/api/resources` | `CloudResource[]` |
| GET | `/api/resources/{id}` | One resource, or `404` |
| GET | `/api/resources/status/{status}` | Resources with that lifecycle state |

```json
{
  "id": "i-0ceae4121f12a88f6",
  "type": "EC2_INSTANCE",
  "name": "sandbox-i-0ceae4121f12a88f6",
  "region": "us-east-1",
  "state": "running",
  "tags": { "Environment": "dev", "Owner": "platform" },
  "specs": { "instanceType": "t3.micro", "vCPU": "2", "memoryGiB": "1.0" },
  "hourlyCost": null,
  "monthlyCost": null,
  "protected": false,
  "createdAt": "2026-10-07T09:00:00Z"
}
```

`hourlyCost` / `monthlyCost` are `null` when cost is not attributed rather than when the
resource is free (Cost Explorer attributes by service, not resource id). Type coverage is
listed in [`feature-reference.md`](../feature-reference.md).

## Cost

| Method | Path | Returns |
|---|---|---|
| GET | `/api/cost-explorer/explorer` | Spend with service/region/daily breakdowns |
| GET | `/api/cost-explorer/daily` | Daily cost series |
| GET | `/api/cost-explorer/breakdown` | Service breakdown |
| GET | `/api/costs/report/daily?days=7` | Measured `CostReport`, daily granularity |
| GET | `/api/costs/report/weekly?weeks=4` | Measured `CostReport`, weekly granularity |
| GET | `/api/costs/forecast?days=30` | Cost forecast over the view window |
| GET | `/api/costs/breakdown` | Service breakdown for a date range (`startDate`, `endDate`) |
| GET | `/api/costs/idle-instances` | Instances below the idle CPU threshold |
| GET | `/api/costs/stopped-instances` | Stopped instances |
| GET | `/api/reports/monthly` | Monthly report |
| GET | `/api/savings` | Savings projection |

`/api/cost-explorer/explorer` query params: `from`, `to` (ISO dates, default last 30
days), `service`, `region`. It returns an explorer shape with `totalCost`, `currency`,
`costByService`, `costByRegion`, `dailyCosts` (`[{ "date", "cost" }]`),
`serviceBreakdown` (`[{ "service", "cost", "percentage" }]`), and two flags anyone can
read: `available` (`true` only when the report is measured spend) and `status`
(`AVAILABLE` | `UNAVAILABLE`). `/api/cost-explorer/daily` returns the `dailyCosts`
list; `/api/cost-explorer/breakdown` returns the `serviceBreakdown` list.

`/api/costs/report/daily` / `/api/costs/report/weekly` return a different DTO
(`CostReportService.CostReport`): `reportType`, `startDate`, `endDate`, `totalCost`,
`costByService`, `costTimeSeries` (`[{ "date", "cost" }]`), `topServices`, plus
`status` - `"AVAILABLE"` on a successful Cost Explorer round trip, even when the total
is 0.0 - and `"UNAVAILABLE"` with an `error` field carrying the failure when Cost
Explorer cannot be reached.

`/api/costs/forecast` returns `CostReportService.CostForecast`: `startDate`, `endDate`,
`forecastedCost`, `meanValue`, `predictionIntervalLowerBound` /
`predictionIntervalUpperBound` (both 0.0 today), `status` (`"AVAILABLE"` |
`"UNAVAILABLE"`) and `error`.

Every cost response distinguishes measured spend from failure: `UNAVAILABLE` or
`available: false` never renders as a measured zero. In the LocalStack sandbox, which
has no Cost Explorer implementation, these calls fail, so the report/forecast return
`UNAVAILABLE` with the underlying `error` and the explorer endpoint reports
`available: false`.

## Dashboard, anomalies and alerts

| Method | Path | Returns |
|---|---|---|
| GET | `/api/dashboard/overview` | Fleet and cost summary |
| GET | `/api/anomalies` | Anomaly report |
| GET | `/api/alerts` | Derived spend-spike alerts |
| GET | `/api/alerts/severity/{severity}` | Alerts filtered by `HIGH` / `MEDIUM` |
| PATCH | `/api/alerts/{id}/acknowledge` | Acknowledge an alert (in-memory) |

Anomaly report `status`: `UNAVAILABLE`, `INSUFFICIENT_DATA` (fewer than seven days),
`NOMINAL` or `ANOMALIES_DETECTED`. A spike carries `service`, `date`,
`description` and `severity (MEDIUM | HIGH)`; detection never fires on dips.

## Recommendations

| Method | Path | Required role | Behaviour |
|---|---|---|---|
| GET | `/api/recommendations` | authenticated | Regenerates and returns pending recommendations |
| POST | `/api/recommendations/{id}/approve` | `CLOUD_INTELLIGENCE_ANALYST`, `ADMIN`, `OWNER` | `true` when approved, `false` when not pending |
| POST | `/api/recommendations/{id}/reject` | same | `true` when rejected, `false` when not pending |
| POST | `/api/recommendations/{id}/execute` | `DEVOPS_ENGINEER`, `ADMIN`, `OWNER` | `true` when applied or simulated, `false` when blocked or not approved |

Row shape:

```json
{
  "id": "...", "resourceId": "vol-...", "tenantId": "acme", "action": "ORPHAN_CLEANUP",
  "recommendedType": "EBS_VOLUME", "reason": "EBS_VOLUME is unattached and still incurring cost",
  "monthlySavings": 5.84, "confidence": 0.9, "status": "PENDING",
  "createdAt": "2026-10-07T09:00:00"
}
```

`GET` calls `refresh()`, so the list is derived from the live account on every request;
rows whose resource stopped qualifying disappear. Rows are persisted per tenant; a
regeneration replaces only `PENDING` rows, so approved/rejected/executed decisions (and
`executedAt`) are retained. See
[`feature-reference.md`](../feature-reference.md) for the generator rules and the note on
restart durability by profile.

## Remediation

| Method | Path | Behaviour |
|---|---|---|
| POST | `/api/remediation/plan` | Preview only - mutates nothing |
| POST | `/api/remediation/execute` | Apply, or evaluate with `dryRun: true` |
| GET | `/api/remediation/actions` | Supported actions with their blast radius |
| GET | `/api/remediation/history` | Session execution history, most recent first |

`plan` request:

```json
{ "actionType": "DELETE_VOLUME", "resourceId": "vol-...", "parameters": {} }
```

`parameters` vary by action (`instanceType` for `RESIZE_INSTANCE`,
`desiredCapacity` for `SCALE_GROUP`, `key`/`value` for `APPLY_TAGS`, `allocationId`
for `RELEASE_ELASTIC_IP`). Missing `actionType` or `resourceId` returns `400`.

`plan` response `data` (`RemediationPlan`):

```json
{
  "actionType": "DELETE_VOLUME",
  "resourceId": "vol-...",
  "resourceType": "EBS_VOLUME",
  "region": "us-east-1",
  "currentState": "available",
  "protectedResource": true,
  "risk": "HIGH",
  "destructive": true,
  "monthlyCost": 5.84,
  "estimatedMonthlySavings": 5.84,
  "blocked": true,
  "blockedReason": "Resource is protected by the optibrain:protected tag",
  "dryRunOnly": true,
  "sandboxed": true,
  "steps": ["Delete the volume", "Data on the volume is destroyed and cannot be recovered"],
  "token": "0f4b... (16 hex chars)",
  "executable": false
}
```

`executable` is derived (`!blocked && !dryRunOnly`) so the client can never read an
inconsistent pair. Savings are stated only for cost-removing actions on attributed
targets; otherwise `estimatedMonthlySavings` is `null`.

`execute` request:

```json
{
  "actionType": "STOP_INSTANCE",
  "resourceId": "i-...",
  "parameters": {},
  "dryRun": true,
  "idempotencyKey": "order-123",
  "planToken": "0f4b..."
}
```

`dryRun` defaults to `true` when absent; applying a change is always an opt-in (and
still requires `cloud.dry-run=false`). `idempotencyKey` replays a stored outcome on
re-submission. `planToken` re-hashes live state; a mismatch refuses the change with
`"The plan is stale: the resource state no longer matches it. Recompute the plan and retry."`.

Outcome (`ActionResult`): `{ "type", "resourceId", "success", "applied", "dryRun",
"message", "estimatedMonthlySavings", "error" }`. A rejected action returns `200` with
`success: false` - refusal is a client-visible outcome, not a transport failure.
`history` returns `ExecutionRecord[]` (`type`, `resourceId`, `applied`, `dryRun`,
`success`, `message`, `executedAt`) capped at the most recent 500.

## Optimisation history

| Method | Path | Returns |
|---|---|---|
| GET | `/api/optimizations/history` | Audit-derived history (most recent 100) |
| GET | `/api/optimizations/{id}` | One entry, or an error envelope |
| GET | `/api/optimizations/status/{status}` | Entries filtered by `APPLIED` / `FAILED` / `PENDING` / `UNKNOWN` |

Rows map `AuditLog` entries to `{ id, action, resourceId, region, status,
estimatedSavings, explanation, score, timestamp }`.

## Tag governance

| Method | Path | Returns |
|---|---|---|
| GET | `/api/tags/coverage?keys=Environment&keys=Owner` | Compliance + uncovered spend |

`keys` is optional and, when present, overrides the effective required keys. Without it,
the effective keys are the calling tenant's configured keys (the `requiredTagKeys` set on
the tenant); a tenant without its own keys falls back to `Environment`, `Owner`. The
report contains `requiredKeys`, `resourcesAssessed`, `compliantResources`,
`coveragePercentage`, `attributedMonthlyCost`, `uncoveredMonthlyCost`,
`uncoveredShareOfSpend`, `uncostedResourceCount`, `violationsByService`,
`uncoveredByService`, `offenders` (ordered by cost) and `available`.
`available: false` means the inventory is empty.
The report is audit-only; enforcement belongs to AWS tag policies.

## Tenants

| Method | Path | Behaviour |
|---|---|---|
| GET | `/api/tenants` | All tenants |
| GET | `/api/tenants/{id}` | One tenant, or 404 |
| POST | `/api/tenants` | `201` created; `400` invalid; `409` conflict |
| PUT | `/api/tenants/{id}` | Partial update |
| DELETE | `/api/tenants/{id}` | Soft delete (deactivate) |
| POST | `/api/tenants/{id}/reactivate` | Restore a deactivated tenant |

```json
{
  "id": "acme",
  "name": "Acme Corp",
  "active": true,
  "cloudProvider": "AWS",
  "mode": "MANUAL",
  "plan": null,
  "autonomousMode": false,
  "requireApprovalForChanges": true,
  "monthlyBudgetLimit": 5000.0,
  "protectedResourcesCsv": null,
  "requiredTagKeys": ["AppID"],
  "awsRoleArn": "arn:aws:iam::111122223333:role/optibrain",
  "awsExternalId": null,
  "version": 0,
  "createdAt": "2026-10-07T09:00:00Z",
  "lastActivityAt": null
}
```

`requiredTagKeys` is a partial-updatable list of tag keys (via `PUT /api/tenants/{id}`)
that becomes the tag-governance requirement for that tenant; see the tag-governance
section above.

`PUT` accepts a partial `TenantUpdateRequest`. The invariant
`autonomousMode ⇒ requireApprovalForChanges` is enforced on update. Deactivation is a
soft delete so that audit entries, remediation records and cleanup reports stay
resolvable. No endpoint accepts or returns AWS credentials; per-tenant roles come from
`awsRoleArn` / `awsExternalId` and are resolved via STS in AWS mode by
`AwsClientFactory`.

## Copilot

| Method | Path | Returns |
|---|---|---|
| GET | `/api/copilot/status` | `{ online: true, mode: "local-copilot-surface" }` |
| POST | `/api/copilot/chat` | Answer from measured data |

`chat` body: `{ "message": "..." }`. Intents: cost drivers, idle resources, savings
opportunities, inventory. An intent that cannot be answered from current data returns an
explicit unavailable statement with no estimated figure.

## Automation

| Method | Path | Behaviour |
|---|---|---|
| GET | `/api/automation/rules` | Demo rules (3 seeded) + any created this session |
| POST | `/api/automation/rules` | Create a rule (in-memory) |
| PATCH | `/api/automation/rules/{id}/toggle` | Toggle `enabled` |
| DELETE | `/api/automation/rules/{id}` | Remove a rule |
| GET | `/api/automation/history` | Single simulated execution row |

This surface is demo-only: rules are held in memory and do not trigger execution.

## Operational

| Method | Path | Notes |
|---|---|---|
| GET | `/actuator/health` | Public; `show-details=always` in dev, `when-authorized` in prod |
| GET | `/actuator/info` | Public in all profiles |
| GET | `/actuator/metrics` | Authenticated |
| GET | `/actuator/env` | Dev profile only - discloses configuration; never expose against a real account |

Swagger UI is served at `/swagger-ui/index.html` and the OpenAPI document at
`/v3/api-docs` (both public).