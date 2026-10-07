# OptiBrain

AWS cost intelligence that measures an account, recommends changes, and executes the ones
an operator approves - through one guarded mutation path.

## What problem does it solve?

Running on AWS, spend grows out of account. Unattached volumes keep billing, oversized
instances keep running, and the tools that spot this (AWS Cost Explorer, Compute
Optimizer, Cost Anomaly Detection) are read-only dashboards: they diagnose waste but
cannot fix it. Fixing means someone drills into the console, verifies the finding, and
performs a change that may be destructive.

OptiBrain closes that gap. It scans the same account, attributes cost, and surfaces a
small set of high-confidence findings as recommendations. When an operator approves one,
the platform executes it through a pipeline that re-verifies the target against the live
inventory, refuses resources tagged `optibrain:protected`, honours a global dry-run
interlock, and writes an audit record - so acting on a finding is as safe as the machine
can make it.

## What is OptiBrain?

OptiBrain is a self-contained, multi-tenant cost management platform with three moving
parts:

- a **Spring Boot backend** (`backend/`) that owns all AWS access, produces
  recommendations, derives alerts and anomalies, and executes approved remediation;
- a **Next.js frontend** (`frontend/`) that presents inventory, cost, recommendations,
  remediation, tenants and the AI Copilot;
- a **FastAPI ML service** (`ml-service/`) that exposes forecasting, anomaly detection
  and cost-optimisation surfaces.

Two properties define the product:

**An uncertainty contract.** Every read path distinguishes *data available*, *no data*,
and *source unreachable* - and never renders the third as the second. A cost report
without Cost Explorer access says so with `available: false`. An anomaly engine with fewer
than seven days of history returns `INSUFFICIENT_DATA`, not a verdict. An instance with no
telemetry produces no recommendation.

**An execution layer with guardrails.** `cloud.dry-run` defaults to `true` and blocks
every mutation in `AwsCloudProviderAdapter#execute`, the only site that mutates AWS.
Resources tagged `optibrain:protected=true` reject destructive actions. Every execution is
planned, verified against live state, protected, and audited.

## How does the system work?

```text
Next.js frontend (3000)
   │  /api/**  (JWT + X-TENANT)
   ▼
Spring Boot backend (8080)
   │
   ├── Auth / users / tenants        ─ login, JWT, tenant lifecycle
   ├── Resource inventory            ─ fault-isolated AWS scanners
   ├── Cost intelligence             ─ Cost Explorer measurements, price-table estimates
   ├── Recommendations               ─ derived from inventory + telemetry + cost
   ├── Anomalies & alerts            ─ rolling-median detection, spend-spike alerts
   ├── Copilot                       ─ rule-based answers from measured data
   ├── Remediation                   ─ plan → review → execute, one guarded path
   └── CloudProviderPort             ─ the single cloud boundary
        └── AwsCloudProviderAdapter
               ├── AwsClientFactory  ─ region, credentials, sandbox endpoint override
               └── AWS SDK  ─▶ AWS account  (or LocalStack on :4566)
```

On every request the backend reads the account through `AwsClientFactory`. Clients - not
services - hold SDK clients, so the sandbox endpoint override and the credential policy
apply to all traffic. `GET /api/recommendations` regenerates recommendations from the
current account state on every read; there is no cached stale ledger. Executing an
approved recommendation flows through the same remediation pipeline as a manual change.

## Core architecture

- **One cloud boundary.** All AWS access originates in `AwsClientFactory`
  (`com.optibrain.cloud.aws`). Services depend on `CloudProviderPort`
  (`com.optibrain.cloud.port`) and hold no SDK clients.
- **Fault-isolated scanners.** Each resource type is a `@Component` `ResourceScanner`
  (`ec2:instances`, `ec2:ebs`, `ec2:network`, `autoscaling:groups`, `rds:database`,
  `ecs:services`, `serverless:lambda,dynamodb,s3`). One unavailable service omits its
  section instead of failing the inventory (`AwsResourceScanner#scan`).
- **One mutation path.** `RemediationService` → `CloudProviderPort` →
  `AwsCloudProviderAdapter#execute`. The dry-run interlock, the protected-resource guard
  and the audit write all live in that adapter.
- **Normalised resource model.** `CloudResource` (id, type, region, state, name, tags,
  specs, hourly/monthly cost, protection flag) plus `ResourceType` and
  `ActionType`/`RiskLevel`.
- **In-memory recommendation store.** Recommendations are held in the singleton service
  and regenerated on read; approved/rejected statuses are per-session.
- **Persistent audit + operation log.** Remediation operations are stored
  (`RemediationOperation`) alongside `AuditLog` entries so retries and dashboards can
  refer to them; SQLite in development, PostgreSQL in production.

Full detail: [`docs/architecture/overview.md`](docs/architecture/overview.md).

## Cost intelligence

- **Measured (authoritative).** Totals, service/regional/linked-account breakdowns and
  daily series come from Cost Explorer (`ce:GetCostAndUsage`). Cost Explorer attributes
  spend to a service, not a resource id, so totals always match the invoice.
- **Estimated (labelled).** Per-resource monthly cost is derived from us-east-1 list
  prices in `AwsPriceList` (EBS per GiB-month, NAT gateway, unassociated Elastic IP,
  ASG member instance types). No estimate feeds a total.
- **Commitment coverage.** Measured via Cost Explorer; the savings projection reports
  `effectiveMonthlySavings` (discounted by existing coverage) and says when coverage
  could not be measured (`commitmentAdjusted`).
- **Anomalies.** Daily and per-service spend against a rolling median ± 3.0 scaled median
  absolute deviations, with `INSUFFICIENT_DATA` before seven days of history.
- **Alerts.** A spend-spike alert fires when the last 7 days exceed the prior 23 by ≥ 25%
  (`MEDIUM`) or ≥ 50% (`HIGH`).
- **The ML service** exposes `/predict/forecast`, `/detect/anomalies` and
  `/optimize/cost`, and returns `available: false` where it has no model or no data - it
  never invents a figure.

Full detail: [`docs/feature-reference.md`](docs/feature-reference.md).

## Recommendation / decision engine

`GET /api/recommendations` re-derives the list from the current account:

| Trigger | Recommendation | Confidence |
|---|---|---|
| Unattached EBS volume, unassociated Elastic IP, or orphan snapshot with cost > 0 | `ORPHAN_CLEANUP` (delete / release) | 0.9 |
| CPU < 30% and memory < 40% (7-day CloudWatch window) | `RIGHTSIZE` (downsize), only when a cheaper type exists | scored |
| CPU > 80% or memory > 85% | `RIGHTSIZE` (upsize), only when a larger type exists | scored |
| Measured hourly baseline > 0 at account level | `RI_OPTIMIZATION` / `SP_OPTIMIZATION` (advisory: simulated 30% / 22% discount, `basis: simulated_baseline`) | 0.68 / 0.64 |

An instance with no telemetry produces no recommendation - absence of data is not evidence
of waste. Rows are reconciled on every list: a resource that stops qualifying disappears.

Lifecycle: recommendations start `PENDING`; an analyst/approver approves or rejects; only
`APPROVED` rows are executable. Execution routes through the remediation pipeline, so the
live-inventory check, protection guard, dry-run interlock and audit apply exactly as they
do for a manual change.

## Safety + remediation

`POST /api/remediation/plan` mutates nothing and returns everything a reviewer needs -
target state, risk, estimated saving, steps, whether policy blocks the action, and a
`planToken`. `POST /api/remediation/execute` applies it, subject to guards re-derived from
live state (a plan sitting on a desk may be stale):

1. **Dry-run interlock** - `cloud.dry-run` defaults to `true` (env `CLOUD_DRY_RUN`); a
   request is a dry-run unless the caller explicitly sends `dryRun: false` while the
   interlock is off.
2. **Protected-resource guard** - anything tagged `optibrain:protected=true` refuses
   destructive actions.
3. **Fail closed** - a destructive action whose target cannot be verified in the current
   inventory is refused, not executed blind.
4. **Staleness check** - `planToken` re-hashes live state; a mismatch refuses the change
   ("the plan is stale").
5. **Idempotency** - resubmitting an `idempotencyKey` replays the stored outcome instead
   of mutating the account twice.
6. **Audit** - every execution writes a `RemediationOperation` and an `AuditLog` entry.

Every `ActionType` carries a `RiskLevel` (`DELETE_VOLUME`, `DELETE_SNAPSHOT` high;
`TERMINATE_INSTANCE`, `DELETE_DATASTORE` critical; commitment purchase medium). Actions
with no AWS implementation (commitment purchase, bucket deletion, network-resource
deletion) are planned transparently and refused at execution rather than half-implemented.

Full detail: [`docs/operations/security.md`](docs/operations/security.md).

## Multi-tenancy / security

- **Tenants are first-class.** The full lifecycle is exposed through `/api/tenants`
  (create, partial update, soft-delete, reactivate) with `@Version` optimistic locking.
  Every persisted row carries `tenantId`.
- **Tenant scoping.** The `X-TENANT` header selects the tenant for a session; a mismatch
  with the authenticated user's tenant returns 403.
- **Per-tenant credentials (AWS mode).** `cloud.mode=AWS` resolves each tenant's role
  through its `awsRoleArn`/`awsExternalId` via STS, cached per tenant. No endpoint
  accepts or returns AWS keys.
- **JWT sessions.** `POST /api/auth/login` and `/register` are the only public routes;
  everything else requires `Authorization: Bearer`. Tokens last 24 hours; logout revokes
  (revoked set capped at 1000).
- **Method-level RBAC.** `@EnableMethodSecurity` is enforced: recommendation approve /
  reject require `CLOUD_INTELLIGENCE_ANALYST`, `ADMIN` or `OWNER`; execute requires
  `DEVOPS_ENGINEER`, `ADMIN` or `OWNER`. A plain `USER` gets 403 on both.
- **Honest errors.** Unknown routes return 404, denied access 403, and invalid input a
  typed 4xx envelope - through `dbUniqueIndexInitializer`, the SQLite schema is kept
  consistent with the declared unique constraints.

Full detail: [`docs/operations/security.md`](docs/operations/security.md).

## Example workflow

```bash
# 1. Stand up the sandbox (LocalStack) and seed a deliberately imperfect account
docker compose up -d localstack
docker compose --profile seed run --rm seed

# 2. Start the backend, frontend and ML service
cd backend && mvn spring-boot:run          # :8080
cd frontend && npm install && npm run dev  # :3000
cd ml-service  && python -m uvicorn main:app --port 8000

# 3. Login as the seeded admin (admin/admin123 in development)
#    GET /api/recommendations now reflects the seeded waste:
#    an orphan volume, an idle m5.4xlarge, and the simulated commitment rows.

# 4. Approve + execute without risk (dry-run stays on)
curl -s -X POST localhost:8080/api/recommendations/{id}/approve \
  -H "Authorization: Bearer $TOKEN" -H "X-TENANT: default"
curl -s -X POST localhost:8080/api/recommendations/{id}/execute \
  -H "Authorization: Bearer $TOKEN" -H "X-TENANT: default"
# → success: true, data: true, message: "Recommendation executed"
#   (with cloud.dry-run=true the change is simulated, never applied)

# 5. Inspect what happened
curl -s localhost:8080/api/remediation/history -H "Authorization: Bearer $TOKEN"
#   → the protected volume was refused; the audit trail shows why
```

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA, AWS SDK v2 |
| Database | SQLite (dev, `data/optibrain-dev.db`), PostgreSQL (prod, `ddl-auto=validate`) |
| Frontend | Next.js 16 (App Router), React 19, Tailwind CSS 4, TypeScript |
| ML service | Python 3.14, FastAPI, numpy |
| Cloud | AWS (EC2, EBS, ELBv2, ASG, RDS, ECS, Lambda, DynamoDB, S3, CloudWatch, Cost Explorer, STS) |
| Sandbox | LocalStack (`compose.yaml`, services: ec2, cloudwatch, autoscaling, rds, sts, s3, lambda, dynamodb, sqs) |

Maven is required (`mvn`); the repository has no wrapper script. `run-all.ps1` (and its
`run-all.sh` shim) orchestrates the backend, ML service and frontend on Windows.

## Run locally

**Prerequisites.** JDK 17+, Maven, Node 18+, Python 3.14+, Docker (for the sandbox), and
the AWS CLI (used by the seed script).

```bash
# Sandbox (LocalStack + seeded account)
docker compose up -d localstack
docker compose --profile seed run --rm seed

# Backend (dev profile: SANDBOX mode, dry-run on, SQLite)
cd backend && mvn spring-boot:run          # http://localhost:8080, health at /actuator/health

# Frontend
cd frontend && npm install && npm run dev  # http://localhost:3000

# ML service (optional - the backend degrades without it)
cd ml-service && python -m uvicorn main:app --port 8000  # http://localhost:8000/docs
```

Development users are seeded from the `ADMIN_PASSWORD` / `USER_PASSWORD` environment
variables (`admin/admin123` and `user/user123` by default). See the environment variable
table in `backend/src/main/resources/application.properties` for the full configuration
surface.

**Verification.** `cd backend && mvn verify` (tests are green: `37/37`), plus
`cd frontend && npm run typecheck` (type errors fail the build).

## Documentation

- **Feature reference** - [`docs/feature-reference.md`](docs/feature-reference.md)
- **API reference** - [`docs/api/endpoints.md`](docs/api/endpoints.md)
- **Architecture** - [`docs/architecture/overview.md`](docs/architecture/overview.md)
- **Security** - [`docs/operations/security.md`](docs/operations/security.md)
- **IAM policy** - [`docs/operations/iam-policy.md`](docs/operations/iam-policy.md)
- **Sandbox** - [`docs/operations/sandbox.md`](docs/operations/sandbox.md)
- **ML service** - [`docs/operations/ml-service.md`](docs/operations/ml-service.md)
- **Roadmap / position / gaps** - [`docs/product/`](docs/product/)