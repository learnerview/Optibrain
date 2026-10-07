# Roadmap

Effort estimates assume senior developer time on the Spring Boot backend and exclude UI
work.

## Backlog

### 1. Commitment management, advisory only

**Value.** The largest untapped dollar pool. The majority of AWS organisations hold no
Savings Plan or Reserved Instance, and Database Savings Plans (up to 35%) apply to the RDS
instances this product already inventories.

**Scope.** Coverage percentage by service, family and region. Utilisation with unused and
expiring-within-30-days flags. Purchase recommendations with estimated monthly saving
and an explicit do-nothing option. Read-only: automated purchasing requires approval
workflows first.

**AWS APIs.** `DescribeSavingsPlans`, `ListSavingsPlans`, `DescribeSavingsPlanOfferings`,
`DescribeReservedInstances`, `DescribeReservedInstancesOfferings`,
`GetSavingsPlansUtilization`, `GetReservationUtilization`,
`GetSavingsPlansPurchaseRecommendation`, `GetReservationPurchaseRecommendation`.

**Effort.** 3 weeks. Builds on `CommitmentCoverage`, which is in place.

### 2. Model Context Protocol server

**Value.** Table stakes for 2026 and the cheapest credibility purchase available. Makes
the existing copilot surface agent-addressable.

**Scope.** Read tools mirroring existing copilot queries. Then a guarded write surface:
`plan_remediation` returning a dry-run plan without mutation, and `execute_remediation`
requiring an approved token, honouring `optibrain:protected` and writing an audit entry.

**Effort.** 1.5 weeks. No new AWS APIs; wraps `CloudProviderPort`.

### 3. Serverless, network and data-transfer coverage

**Value.** Closes three of the largest blind spots on a typical bill with no new
abstraction, because `MetricQuery` already accepts any namespace and dimension.

**Scope.** Lambda and Fargate scanners. Lambda memory rightsizing from `Duration` against
`MemorySize`, confirmed with `MaxMemoryUsed`. Fargate task-level rightsizing. NAT gateway
and data-transfer surfacing from the Cost Explorer `DATA_TRANSFER` category. Lambda memory
and Fargate resize actions in the `ActionType` switch.

**AWS APIs.** `ListFunctions`, `GetFunctionConfiguration`, `ListTasks` (ECS),
`PutFunctionConcurrency`. CloudWatch `AWS/Lambda` metrics and `AWS/Fargate`.

**Effort.** 3 weeks.

### 4. Anomaly root-cause attribution

**Value.** Detection without attribution is a notification that gets muted.

**Scope.** Surface AWS's own root causes alongside local detection. Add resource-level
attribution by indexing `ResourceInventory` by service and category. A usage-driven versus
rate-driven classifier distinguishes a traffic change from a pricing change.

**AWS APIs.** `GetAnomalyMonitor`, `GetAnomalies`, `LookupLinkedAccounts`, CloudTrail
`LookupEvents`, CloudWatch Logs Insights `StartQuery`.

**Effort.** 1 week for the first two items, plus 2 weeks for CloudTrail correlation.

### 5. Tag governance and uncovered spend

**Status.** Implemented. `GET /api/tags/coverage` reports compliance against required
keys, uncovered spend, violations by resource type, and offenders ordered by cost.

**Remaining.** Compliance is evaluated against fixed default keys (`Environment`,
`Owner`). Per-tenant required-key policy and drift detection over time remain
outstanding.

**AWS APIs.** `tag:GetResources`, `tag:GetComplianceSummary`, the Resource Groups Tagging
API `GetResources`, and AWS Organizations tag policies for enforcement.

**Effort.** 2 weeks.

### 6. Budgets and alerting

**Value.** Recommendations that arrive where the team works get actioned; recommendations
in a queue do not.

**Scope.** Budget entity, actual/forecast/remaining, threshold alerts, forecast variance
warnings. Slack webhook, email and generic webhook delivery.

**AWS APIs.** `CreateBudget`, `DescribeBudgets`, `ModifyBudget`, SNS `Publish`, and
Cost Explorer's `GetCostForecast`.

**Effort.** 2 weeks.

### 7. Approval workflows and role-based access control

**Value.** The enterprise procurement gate. The dry-run interlock is already an approval
mechanism; it is binary and global.

**Scope.** Roles for admin, analyst, viewer and approver. Per-action-type approval
thresholds, which map directly onto the existing `RiskLevel` on `ActionType`. An approval
request produces a time-boxed single-use execution token bound to action, resource, approver
and expiry. Audit entries extended with actor and approver. SAML or OIDC with group-to-role
mapping.

**Effort.** 3 weeks for roles and approval, 2 weeks for SSO.

### 8. Cost and Usage Report ingestion

**Value.** The foundation for accurate allocation, unit economics, chargeback and
savings verification. Cost Explorer cannot attribute spend to a resource id, which is the
structural limit on everything per-resource.

**Scope.** Data Export creation, S3 Parquet output, mapping to the FinOps FOCUS
specification columns, backing a cost fact table.

**Effort.** 4 to 6 weeks. Blocks chargeback and verified-savings reporting, so it precedes
those items rather than following them.

### 9. Graviton and architecture migration

**Value.** A different class of saving from rightsizing, often 20 to 40 percent for
equivalent work. AWS already ships this recommendation; surfacing it is cheap.

**AWS APIs.** Cost Optimization Hub, or `DescribeInstanceTypes` comparison against
supported architectures.

**Effort.** 1 week once the analytics layer consumes Cost Optimization Hub output.

## Sequencing

**Immediately.** Commitments (1) and MCP (2). Both are high value and independent.

**Next.** Serverless coverage (3) and anomaly attribution (4). Each closes a blind spot
the product currently has.

**Then.** Budgets (6), approval workflows (7) and the data foundation (8). Approval
workflows gate automated purchasing.

**After the fact table.** Unit economics, showback and chargeback.

## Under consideration

Graviton migration (9) is cheap and appends cleanly to the recommendations surface.
Kubernetes and Karpenter remain out of scope by decision, recorded in
[position.md](position.md).