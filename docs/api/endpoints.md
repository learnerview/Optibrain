# API Reference

Base path `/api`. Every response uses the envelope:

```json
{ "success": true, "message": "...", "data": { }, "timestamp": "2026-10-06T08:00:00Z" }
```

Errors return `"success": false` with a `message` describing the cause. A 404 carries
`"Tenant not found"` or similar; a 409 signals a conflict such as a duplicate id.

All `/api/**` endpoints require a JWT in every profile. Only `POST /api/auth/login` and
`POST /api/auth/register` are public. The token is sent as `Authorization: Bearer`.

## Inventory

| Method | Path | Returns |
|---|---|---|
| GET | `/api/resources` | `Resource[]` |
| GET | `/api/resources/{id}` | `Resource` or 404 |
| GET | `/api/resources/status/{status}` | `Resource[]` |

```json
{
  "id": "i-0ceae4121f12a88f6",
  "type": "Instance",
  "name": "sandbox-i-0ceae4121f12a88f6",
  "status": "running",
  "region": "us-east-1",
  "monthlyCost": null,
  "hourlyCost": null,
  "protected": false,
  "specs": { "instanceType": "t3.micro", "vCPU": "2", "memoryGiB": "1.0" },
  "tags": { "Environment": "dev" }
}
```

`monthlyCost` and `hourlyCost` are `null` when cost cannot be attributed to the resource.
Cost Explorer groups by service rather than resource id, so `null` means "unattributed",
not "free".

Supported types: `Instance`, `GPU_INSTANCE`, `Volume`, `EBS_SNAPSHOT`, `NAT_GATEWAY`,
`ELASTIC_IP`, `LOAD_BALANCER`, `ASG`, `RDS_INSTANCE`.

## Cost

| Method | Path | Returns |
|---|---|---|
| GET | `/api/cost-explorer/explorer` | Spend with service, region and daily breakdowns |
| GET | `/api/cost-explorer/daily` | Daily series |
| GET | `/api/cost-explorer/breakdown` | Service breakdown |
| GET | `/api/costs/breakdown` | Service breakdown |
| GET | `/api/costs/forecast` | Forecast |
| GET | `/api/costs/idle-instances` | Instances below the CPU threshold |
| GET | `/api/reports/monthly` | Monthly report |

Query parameters on `/cost-explorer/explorer`: `from`, `to`, `service`, `region`.

```json
{
  "from": "2026-09-06", "to": "2026-10-06",
  "service": "ALL", "region": "ALL",
  "totalCost": 0.0, "currency": "USD",
  "costByService": {}, "costByRegion": {},
  "dailyCosts": [], "serviceBreakdown": [],
  "available": false
}
```

`available: false` indicates Cost Explorer returned nothing. This is the expected
response in the LocalStack sandbox, which does not implement Cost Explorer.

## Analysis

| Method | Path | Returns |
|---|---|---|
| GET | `/api/dashboard/overview` | Fleet summary |
| GET | `/api/anomalies` | Anomaly report |
| GET | `/api/alerts` | Derived alerts |
| GET | `/api/alerts/severity/{severity}` | Filtered alerts |
| PATCH | `/api/alerts/{id}/acknowledge` | Acknowledged alert |
| GET | `/api/recommendations` | Pending recommendations |
| GET | `/api/savings` | Savings projection |
| GET | `/api/optimizations/history` | Audit-derived history |
| GET | `/api/optimizations/status/{status}` | Filtered history |

Anomaly report:

```json
{
  "anomalies": [],
  "count": 0,
  "status": "INSUFFICIENT_DATA",
  "reason": "Need at least 7 days of daily spend; have 0."
}
```

`status` is `NOMINAL`, `ANOMALIES_DETECTED`, `INSUFFICIENT_DATA` or `UNAVAILABLE`.

Savings projection:

```json
{
  "monthlySavings": 0.0,
  "annualSavings": 0.0,
  "commitmentCoveragePercentage": 0.0,
  "confidence": "Calculated (0.0% commitment coverage)",
  "assumptions": ["..."]
}
```

Recommendation values are priced from on-demand list rates. Actual benefit is lower where
a workload is already covered by a Savings Plan or Reserved Instance, which
`commitmentCoveragePercentage` quantifies.

## Governance

| Method | Path | Returns |
|---|---|---|
| GET | `/api/tags/coverage` | Tag compliance and uncovered spend |

Optional parameter `keys` overrides the required tag keys, which default to
`Environment` and `Owner`.

```json
{
  "requiredKeys": ["Environment", "Owner"],
  "resourcesAssessed": 5,
  "compliantResources": 2,
  "coveragePercentage": 40.0,
  "attributedMonthlyCost": 6.77,
  "uncoveredMonthlyCost": 6.77,
  "uncoveredShareOfSpend": 100.00,
  "uncostedResourceCount": 2,
  "violationsByService": { "Volume": 3 },
  "uncoveredByService": { "Volume": 6.77 },
  "offenders": [
    { "resourceId": "vol-...", "resourceType": "Volume",
      "region": "us-east-1", "monthlyCost": 5.84,
      "missingKeys": ["Owner"] }
  ],
  "enforcement": "AWS tag policies and service control policies perform enforcement. This report is audit-only.",
  "available": true
}
```

`offenders` is ordered by cost, so the expensive resources appear first. Monetary values
are rounded to two decimal places.

`available: false` indicates the inventory is empty, so no assessment is possible. That is
distinct from a measured report showing zero coverage.

The report is audit-only by design. AWS tag policies and service control policies perform
enforcement; implementing it in the application would duplicate a weaker version.

## Remediation

| Method | Path | Behaviour |
|---|---|---|
| POST | `/api/remediation/plan` | Preview only: target, risk, steps, blocked/executable, estimated saving. Mutates nothing. |
| POST | `/api/remediation/execute` | Apply, or evaluate with `dryRun: true`. Honours the global `cloud.dry-run` interlock and refuses resources carrying the `optibrain:protected=true` tag. |

Planning returns `executable: false` when a change is blocked or the global interlock is
on; that flag is derived from the blocking facts rather than re-stated, so a client cannot
read an inconsistent pair. Destructive plans state data loss and irreversibility in the
steps. Estimated savings are `null` when the target's monthly cost is unattributed.

## Tenants

| Method | Path | Behaviour |
|---|---|---|
| GET | `/api/tenants` | Active tenants |
| GET | `/api/tenants/{id}` | One tenant, or 404 |
| POST | `/api/tenants` | 201 on create, 400 invalid, 409 duplicate |
| PUT | `/api/tenants/{id}` | Partial update |
| DELETE | `/api/tenants/{id}` | Soft delete |
| POST | `/api/tenants/{id}/reactivate` | Restore a soft-deleted tenant |

```json
{
  "id": "acme",
  "name": "Acme Corp",
  "active": true,
  "cloudProvider": "AWS",
  "mode": "MANUAL",
  "autonomousMode": false,
  "requireApprovalForChanges": true,
  "plan": "PRO",
  "monthlyBudgetLimit": 5000.0,
  "createdAt": "2026-10-06T08:00:00Z",
  "lastActivityAt": null
}
```

Deactivation is a soft delete. Audit entries, remediation records and cleanup reports
reference the tenant id and remain resolvable.

No endpoint accepts or returns AWS credentials.

## Authentication

| Method | Path | Behaviour |
|---|---|---|
| POST | `/api/auth/login` | 200 with authorities, 400 blank, 401 invalid |
| POST | `/api/auth/logout` | Invalidates the session |
| GET | `/api/auth/status` | Current session state |

## Copilot

| Method | Path | Behaviour |
|---|---|---|
| POST | `/api/copilot/chat` | Answers from measured data |

Body: `{"message": "..."}`. Recognised intents are cost drivers, idle resources, savings
opportunities and inventory. An intent that cannot be answered from measurement returns a
statement that the data is unavailable.

## Operational

| Method | Path |
|---|---|
| GET | `/actuator/health` |
| GET | `/actuator/info` |
| GET | `/actuator/metrics` |