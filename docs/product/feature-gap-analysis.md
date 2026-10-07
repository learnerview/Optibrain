# Feature Gap Analysis

Assessment of OptiBrain against the 2026 cloud cost management market. Demand is rated 1
to 5, where 5 indicates a capability expected of any product in this category.

## Implemented

| Capability | Notes |
|---|---|
| Resource inventory | Scanners for EC2 (incl. GPU), EBS (volumes + snapshots), NAT gateway, Elastic IP, load balancers, network interfaces, security groups, Auto Scaling groups, RDS (instances + clusters), ECS services, Lambda, DynamoDB, S3 |
| Cost attribution | Measured totals (service/region/linked-account/daily) from Cost Explorer; per-resource list-price estimates where honest, `null` otherwise; cost forecast |
| Telemetry | Any CloudWatch namespace and dimension via `MetricQuery` (`GetMetricStatistics`) |
| Recommendation engine | Orphan cleanup (EBS/VOLUME, Elastic IP, snapshot), rightsizing (CPU/memory thresholds, priced), advisory RI/SP rows from a measured baseline |
| Remediation | Thirteen action types with risk levels; dry-run interlock, protection tag, live-state re-verification, plan tokens, idempotency keys, persisted audit |
| Approval workflow and RBAC | Method-level RBAC (`@EnableMethodSecurity`) on approve/reject/execute; recommendation lifecycle PENDING → APPROVED → EXECUTED, persisted per tenant so regeneration never clobbers decided rows |
| Anomaly detection | Rolling median with scaled MAD, upper-tail only, per-service |
| Derived alerts | Spend-spike alert (last 7 vs prior 23 days) with severity; in-memory acknowledgement |
| Forecasting surface | ML service `/predict/forecast`, explicit `available: false` without history |
| Copilot | Rule-based answers from measurement; refuses to estimate unavailable data |
| Tenant management | Full CRUD, optimistic locking, soft delete, per-tenant `awsRoleArn`/`awsExternalId` |
| Audit trail | `RemediationOperation` + `AuditLog` per execution; optimizations history from audit log |
| Tag governance | Coverage against required keys (per-tenant `requiredTagKeys`, fallback defaults), uncovered spend, offenders by cost (audit-only) |
| Sandbox | LocalStack, identical code path, re-runnable seed |

## Not implemented

| Capability | Demand | Current state | Assessment |
|---|---|---|---|
| Commitment purchasing | 5 | Advisory `RI_OPTIMIZATION` / `SP_OPTIMIZATION` rows exist; no automated purchase | Needs the approval workflow first; deliberately deferred |
| Savings verification | 5 | Projected savings are stated per execution; no compare against later bills | Requires Cost and Usage Report ingestion |
| Serverless cost attribution | 5 | Lambda/DynamoDB/S3 inventoried, cost `null` by design | A blind spot on a typical bill; `MetricQuery` needs no change |
| Data transfer and network cost | 5 | Absent | A top cause of billing surprise; NAT gateway hourly cost only |
| Chargeback and showback | 5 | Absent | Requires the cost fact table first |
| Unit economics | 5 | Absent | Customers supply the unit metric |
| Per-tenant tag policy + drift | 5 | Per-tenant `requiredTagKeys` shipped; no drift detection over time | Force multiplier for allocation and budgets |
| Budgets and alert delivery | 5 | Spend-spike alert exists; no external delivery (Slack/email/webhook) | Highest perceived value per engineering hour |
| Anomaly root-cause attribution | 5 | Detection only (backend local) + ML `/detect/anomalies` | An unexplained alert gets muted |
| AI and GPU cost | 4 | GPU instances detected as a type; no attribution | Fastest-growing line item |
| SSO (SAML/OIDC) | 5 | Form login with JWT only | Enterprise procurement gate |
| Approval thresholds and token binding | 4 | Global approve/reject; no per-risk thresholds, no actor-bound expiring tokens | Natural extension of the risk levels |
| MCP server | 5 | Absent | Table stakes in 2026; wraps existing plan/execute surface |
| Integrations | 4 | Absent | Where recommendations otherwise die |
| Data export | 4 | Absent | Finance analysts require warehouse output |
| Graviton and architecture migration | 4 | Absent | Cheap to add once analytics consume first-party recommendations |
| Storage lifecycle | 4 | Absent | AWS automates it; detection is the useful part |
| Spot orchestration | 4 | Auspicious surface reports `UNAVAILABLE` | Out of scope by decision; recorded in [position.md](position.md) |
| Kubernetes and EKS | 4 | Raw compute captured; ECS services inventoried, no cluster awareness | Out of scope by decision |
| Carbon and sustainability | 3 | Absent | Compliance-driven; a straightforward read-and-report feature |
| Confidence calibration | 3 | Deterministic scores only | No market leader; available ground |
| Non-production scheduling | 3 | Absent | Large saving in non-production accounts |
| Data platform cost | 3 | Absent | Newer category |
| SaaS and licence spend | 3 | Absent | Increasingly managed by FinOps teams |

## AI and agent infrastructure

Model Context Protocol is table stakes for 2026. OpenCost ships a server in its official
Helm chart. Vantage, CloudZero and Finout operate hosted servers. AWS, Azure and Google
ship managed cost servers, and the FinOps Foundation runs a dedicated working group.

AWS AI cost investigation is worth reading for its framing: it prefers an honest statement
of what it can and cannot confirm over a confident answer that may be wrong. That is the
same contract OptiBrain applies in code, including the ML service, which returns
`available: false` and an explicit reason wherever it has no model or data.

## Structural limits

**Cost Explorer cannot attribute spend to a resource id.** Every per-resource cost figure
in the inventory is a list-price estimate where one exists; instances, RDS, serverless and
ECS report `null` rather than a fabricated figure. Totals come from Cost Explorer and are
correct.

**Recommendation detail is unavailable per instance.** Commitment purchase
recommendations from Cost Explorer apply to a service and region, not to an identified
instance, so a rightsizing saving cannot be exactly netted against a commitment. The
commitment rows the platform derives are advisory simulations over a measured baseline.

**AWS provides the analytics for free.** Cost Optimization Hub, Compute Optimizer and Cost
Anomaly Detection improve continuously without a purchase. Competing on analytics depth
means competing against a first-party service at no customer cost. See
[position.md](position.md).