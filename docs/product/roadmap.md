# Roadmap

Effort estimates assume senior developer time on the Spring Boot backend and exclude UI
work. Items already shipped are recorded at the top so the backlog reflects what is
actually outstanding.

## Shipped

- **Recommendation lifecycle with RBAC.** `@EnableMethodSecurity` guards approve/reject
  (`CLOUD_INTELLIGENCE_ANALYST`, `ADMIN`, `OWNER`) and execute (`DEVOPS_ENGINEER`,
  `ADMIN`, `OWNER`); the pipeline persists each execution (`RemediationOperation`) and
  writes the audit log.
- **Advisory commitment recommendations.** `RI_OPTIMIZATION` / `SP_OPTIMIZATION` rows
  derived from a measured baseline with simulated discounts - advisory only, no purchase.
- **Orphan cleanup and rightsizing engine.** Unattached EBS/Elastic IP/snapshot and
  CPU/memory-based rightsizing, regenerated on every read from live state.
- **Anomaly detection and derived alerts.** Rolling-median detection
  (`INSUFFICIENT_DATA` before 7 days) plus a spend-spike alert; both honest about absence.
- **Tag governance.** `GET /api/tags/coverage` reports compliance, uncovered spend and
  offenders by cost.
- **Per-tenant execution roles.** `awsRoleArn` / `awsExternalId` on `Tenant`, resolved via
  STS in AWS mode.
- **Error envelope.** 404 for unknown routes, 403 for denial, typed 4xx otherwise - no
  route silently 500s.
- **SQLite unique-index consistency.** `DbUniqueIndexInitializer` recreates the declared
  unique indexes that the SQLite dialect cannot add.

## Backlog

### 1. Savings verification against billed spend

**Value.** Turn "projected savings" into "savings realised". Currently every execution
states its projected figure; the platform never compares it to what the invoice later
says.

**Scope.** Ingest Cost and Usage Report (Data Export to S3 Parquet, FOCUS mapping) into a
cost fact table; compare remediated resources' pre/post monthly cost; surface a verified
savings figure beside the projection.

**Effort.** 4 to 6 weeks. Blocks chargeback, showback and unit economics.

### 2. Model Context Protocol server

**Value.** Table stakes for 2026 and the cheapest credibility purchase available. Makes
the existing copilot and remediation surface agent-addressable.

**Scope.** Read tools mirroring existing copilot queries. Then a guarded write surface:
`plan_remediation` returns a `RemediationPlan` (never mutates) and `execute_remediation`
honours the dry-run interlock, the `optibrain:protected` tag, the plan token, idempotency
and the audit write - wrapping `CloudProviderPort` exactly as the REST controller does.

**Effort.** 1.5 weeks. No new AWS APIs.

### 3. Approval thresholds and actor-bound execution tokens

**Value.** The global approve/reject and binary dry-run are an approval mechanism; they
lack enterprise separation of duties.

**Scope.** Per-risk approval thresholds mapped onto the existing `ActionType.RiskLevel`;
a time-boxed single-use execution token bound to action, resource, approver and expiry;
audit entries extended with actor and approver. Finish before commitment purchasing.

**Effort.** 3 weeks.

### 4. Serverless, network and data-transfer coverage

**Value.** Closes several of the largest blind spots on a typical bill.

**Scope.** Lambda memory and DynamoDB capacity attribution; NAT gateway and
`DATA_TRANSFER` surfacing from Cost Explorer; Fargate task-level rightsizing. The
inventory already discovers Lambda, DynamoDB and S3.

**Effort.** 3 weeks.

### 5. Anomaly root-cause attribution

**Value.** Detection without attribution is a notification that gets muted.

**Scope.** Surface usage-driven versus rate-driven classification, then CloudTrail
correlation to the API call and IAM principal behind a spike.

**AWS APIs.** CloudTrail `LookupEvents`, CloudWatch Logs Insights `StartQuery`,
Cost Explorer anomaly `GetAnomalies`.

**Effort.** 1 week for the classifier, plus 2 weeks for CloudTrail correlation.

### 6. Budgets and alert delivery

**Value.** Recommendations and alerts that arrive where the team works get actioned.

**Scope.** Budget entity (actual/forecast/remaining), threshold alerts, forecast-variance
warnings; Slack webhook, email and generic webhook delivery on top of the existing
spend-spike alert derivation.

**Effort.** 2 weeks.

### 7. Tag governance per tenant

**Status.** Base shipped: per-tenant `requiredTagKeys` on `Tenant`, consumed by
`GET /api/tags/coverage` (falls back to `Environment` / `Owner` when unset; explicit
`keys` overrides both).

**Value.** Required-key policy currently defaults to `Environment` / `Owner` for everyone.

**Remaining scope.** Drift detection over time, budget-aware enforcement thresholds, and
enforcement guidance via AWS tag policies.

**Effort.** 1 week remaining.

### 8. Single sign-on

**Value.** Enterprise procurement gate alongside the RBAC work.

**Scope.** SAML or OIDC with group-to-role mapping onto the existing `Role` enum.

**Effort.** 2 weeks after item 3.

### 9. Graviton and architecture migration

**Value.** A different class of saving from rightsizing, often 20 to 40 percent for
equivalent work. AWS already ships this recommendation; surfacing it is cheap.

**Scope.** Cost Optimization Hub output, or `DescribeInstanceTypes` comparison against
supported architectures, appended to the recommendation surface.

**Effort.** 1 week once the analytics layer consumes Cost Optimization Hub output.

## Sequencing

**Immediately.** MCP (2) and approval tokens (3). MCP is cheap and makes the existing
write surface agent-addressable; approval tokens gate the larger purchase work.

**Next.** Savings verification (1) - it is the foundation for chargeback, showback and
unit economics, and currently blocks the most defensible claim the product could make.

**Then.** Serverless/network coverage (4), anomaly attribution (5), budgets (6), tag
policy (7), SSO (8).

## Under consideration

Graviton migration (9) is cheap and appends cleanly to the recommendations surface.
Kubernetes and Karpenter remain out of scope by decision, recorded in
[position.md](position.md).