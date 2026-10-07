# Feature Reference

What the product does, and what each capability actually measures.

## Inventory

`GET /api/resources` enumerates the account through fault-isolated scanners. Each scanner
is a `@Component` `ResourceScanner`; an AWS service that is unavailable in the account
omits its section instead of failing the response. Cost is `null` where attribution is
deliberately avoided (see *Cost attribution*).

| Scanner | Type(s) produced | Source API | Cost attributed |
|---|---|---|---|
| `Ec2InstanceScanner` (`ec2:instances`) | `EC2_INSTANCE`, `GPU_INSTANCE` (accelerator families) | `DescribeInstances`, `DescribeInstanceTypes` (cached) | No — null by design |
| `EbsScanner` (`ec2:ebs`) | `EBS_VOLUME`, `EBS_SNAPSHOT` | `DescribeVolumes`, `DescribeSnapshots` (`ownerIds=self`) | Yes — per GiB-month list price |
| `NetworkScanner` (`ec2:network`) | `NAT_GATEWAY`, `ELASTIC_IP`, `LOAD_BALANCER`, `NETWORK_INTERFACE`, `SECURITY_GROUP` | `DescribeNatGateways`, `DescribeAddresses`, `DescribeLoadBalancers` + `DescribeTags`, `DescribeNetworkInterfaces`, `DescribeSecurityGroups` | NAT gateway fixed $0.045/hour; Elastic IP $0.005/hour only while unassociated; others none |
| `AutoScalingScanner` (`autoscaling:groups`) | `ASG` | `DescribeAutoScalingGroups` | Yes — sum of current member instance types |
| `RdsScanner` (`rds:database`) | `RDS_INSTANCE`, `RDS_CLUSTER` | `DescribeDBInstances`, `DescribeDBClusters` | No — list-price estimate avoided |
| `EcsScanner` (`ecs:services`) | `ECS_SERVICE` (incl. Fargate launch type) | `ListClusters`, `DescribeClusters`, `ListServices`, `DescribeServices` | No — task-geometry dependent |
| `ServerlessScanner` (`serverless:lambda,dynamodb,s3`) | `LAMBDA_FUNCTION`, `DYNAMODB_TABLE`, `S3_BUCKET` | `ListFunctions`; `ListTables` + `DescribeTable`; `ListBuckets` | No — billing is per GB-second / request unit, not list-priceable |

`ResourceType` also declares further constants (EKS and Kubernetes-family, CloudFront,
ElastiCache, OpenSearch, MSK, Redshift, Kinesis, Glue, Athena, SQS/SNS, ECR and others)
with no scanner behind them, so they do not produce rows today. LocalStack's community
image implements no ECS either, so in the sandbox the `ecs:services` section is empty.

Details worth recording:

- EC2 cost is deliberately `null`. Cost Explorer attributes spend to a service rather
  than a resource id, so a per-instance figure would be a list-price estimate presented
  beside measured data. Callers that want an explicit attribution use `AwsPriceList`.
- Snapshots backing an AMI are excluded (`EbsScanner#isAmiBacking`) - AWS removes them
  implicitly with the AMI, so they are not standalone waste.
- Snapshot storage is priced at the `standard` (magnetic) rate, not the source volume's
  rate, because that is how snapshot storage bills in us-east-1.
- Encryption, IOPS and throughput spec keys are omitted rather than emitted as `"null"`
  or empty when the volume type does not support them (`AwsResourceScanner#spec`).
- ECS cost is not estimated because Fargate pricing is task-geometry dependent; the
  cluster, launch type and desired/running/pending counts are surfaced as specs instead.

## Cost attribution

**Measured - totals only.** Totals, service breakdowns, regional breakdowns,
linked-account rollups and daily series come from Cost Explorer (`ce:GetCostAndUsage`,
metric `UnblendedCost`). Cost Explorer cannot attribute spend to a resource id, so a total
matches the invoice and no per-resource figure contributes to it.

**Estimated - labelled as such.** Per-resource monthly cost in the inventory comes from
us-east-1 list prices in `AwsPriceList`. A resource whose price is unknown (GPU
instances, RDS, Lambda, DynamoDB, S3) reports `null` rather than a fabricated figure.

**Commitments.** `CommitmentCoverage` measures how much recent spend a Savings Plan or
Reserved Instance absorbs each time a cost report is generated. The savings projection
carries both a raw figure priced from on-demand rates and an effective figure
(`effectiveMonthlySavings`) that discounts the share already covered by a commitment, so a
reader sees the realistic benefit. When coverage cannot be measured,
`commitmentAdjusted` and `confidence` say so.

## Recommendations

`GET /api/recommendations` regenerates the pending rows on every read from current state
(`refresh()` re-runs the generators), so rows reflect the account as it is now. Rows are
persisted per tenant in a `recommendations` table; only `PENDING` rows are replaced by a
regeneration, so operator decisions (`APPROVED`, `REJECTED`, `EXECUTED`, `FAILED`) and
their `executedAt` timestamps are never clobbered by a later read.

Restart durability follows the schema lifecycle: the development profile recreates the
database on every boot (`ddl-auto=create-drop`), so dev data is ephemeral; profiles that
run against a persistent store (`ddl-auto=update` on SQLite, `validate` on PostgreSQL in
`prod`) retain decision rows across restarts.

| Generator | Trigger | Action | Confidence |
|---|---|---|---|
| Orphan cleanup | Unattached EBS volume (`attachmentCount` 0), unassociated Elastic IP, or orphan snapshot - each with cost above zero | `ORPHAN_CLEANUP` | 0.9 |
| Rightsizing | 7-day CloudWatch window, CPU < 30% and memory < 40% | `RIGHTSIZE` (downsize) - only when a cheaper instance type exists | scored |
| Rightsizing | CPU > 80% or memory > 85% | `RIGHTSIZE` (upsize) - only when a larger type exists | scored |
| Commitment (advisory) | Account-level measured hourly baseline (attributed spend / 730) above zero | `RI_OPTIMIZATION` (simulated 30% discount), `SP_OPTIMIZATION` (simulated 22%), both `basis: simulated_baseline` | 0.68 / 0.64 |

An instance with no telemetry produces no recommendation. Absence of data is not evidence
of waste. A commitment row is an advisory simulation over a measured baseline - it states
the simulated discount, never a verified saving.

Each row carries `id`, `resourceId`, `tenantId`, `action`, `recommendedType`, `reason`,
`monthlySavings`, `confidence`, `status` and `createdAt`. Lifecycle is
`PENDING → APPROVED / REJECTED → EXECUTED / FAILED`; the row is written to the
`recommendations` table in every transition, so the state of every decision survives a
restart. `execute` routes through the remediation pipeline, so the live-inventory
check, protection guard and dry-run interlock apply exactly as for a manual change.

## Anomaly detection

`GET /api/anomalies` compares daily total spend and each service's daily spend against
their own rolling median and median absolute deviation. Only upper-tail spikes are flagged
- a dip is a bill improvement, not an anomaly.

- Fewer than seven days of history returns `status: INSUFFICIENT_DATA`.
- A day is flagged when spend exceeds the median by more than `3.0 * scaled MAD`;
  severity is `HIGH` when the increase is ≥ 300%, otherwise `MEDIUM`.
- A service with fewer than seven days is skipped rather than judged.
- `status` is one of `UNAVAILABLE`, `INSUFFICIENT_DATA`, `NOMINAL`,
  `ANOMALIES_DETECTED`.

## Alerts

`GET /api/alerts` derives a spend-spike alert from the cost report's last 30 days: the
last 7 days are compared with the preceding 23, and a change of ≥ 25% produces an alert
(`HIGH` at ≥ 50%). Alerts carry a fixed `id` (`spend-spike`), a snapshot of contributors,
and an in-memory acknowledged flag (kept only for the session). No usable cost data
produces no alert.

## Remediation

Every action type carries a risk level. Actions are planned and executed through the same
port; those without an AWS implementation in `AwsCloudProviderAdapter#execute` are planned
transparently and refused at execution:

| Action | Risk | AWS implementation |
|---|---|---|
| `NOOP` | None | Rejected ("has no AWS implementation") |
| `STOP_INSTANCE` / `START_INSTANCE` | Low | `ec2:StopInstances` / `StartInstances` |
| `RELEASE_ELASTIC_IP` | Low | `ec2:ReleaseAddress` (requires `allocationId`) |
| `SCALE_GROUP` | Low | `autoscaling:UpdateAutoScalingGroup` (requires `desiredCapacity`) |
| `APPLY_TAGS` | Low | `ec2:CreateTags` (`key`/`value`); the `optibrain:protected` key is refused |
| `RESIZE_INSTANCE` | Medium | `ec2:ModifyInstanceAttribute` (requires `instanceType`) |
| `PURCHASE_COMMITMENT` | Medium | None - planned, refused at execution |
| `DELETE_SNAPSHOT` | High | `ec2:DeleteSnapshot` |
| `DELETE_VOLUME` | High | `ec2:DeleteVolume` |
| `DELETE_BUCKET` | High | None - planned, refused at execution |
| `DELETE_NETWORK_RESOURCE` | High | None - planned, refused at execution |
| `DELETE_DATASTORE` | Critical | None - planned, refused at execution |
| `TERMINATE_INSTANCE` | Critical | `ec2:TerminateInstances` |

Two guards apply inside `AwsCloudProviderAdapter#execute`, the only mutation site:

- **Dry-run.** `cloud.dry-run` defaults to `true`. A dry-run produces the same result
  shape as a real run with `applied: false`, so the interface renders both through one
  code path.
- **Protection.** A resource tagged `optibrain:protected=true` rejects destructive
  actions. The check is shared via `ProtectionPolicy` by the scanners (which set the
  `protected` flag), the planner (which explains the block) and the executor (which
  enforces it).

Execution also re-derives the plan from live state before mutating, refuses destructive
actions whose target cannot be verified, replays repeated `idempotencyKey`s, and refuses a
`planToken` that no longer matches (stale plan). Each execution writes a
`RemediationOperation` and an `AuditLog` entry. See
[`operations/security.md`](operations/security.md) and
[`api/endpoints.md`](api/endpoints.md).

## Tag governance

`GET /api/tags/coverage` reports compliance against required keys. The required keys are
the calling tenant's configured keys (stored as `requiredTagKeys` on the tenant); a tenant
without its own keys falls back to the platform defaults `Environment` and `Owner`, and an
explicit `keys` query parameter overrides both. It returns the compliant percentage, the
attributed monthly cost of non-compliant resources, violations by resource type, and
individual offenders ordered by cost. `available: false` means the inventory is empty,
which is distinct from a measured report showing zero coverage.

The report is audit-only. AWS tag policies and service control policies perform
enforcement; implementing it in the application would duplicate a weaker version.

## Forecasting (ML service)

`POST /predict/forecast` fits a model over supplied history. With fewer than eight points
it returns `available: false` and an empty prediction list. With eight or more it returns a
persistence forecast (the last observed value repeated) with a ±10% confidence band, and
`model_metadata.method: persistence`. `/models/status` reports forecasting as
`available: false` with the reason "persistence baseline only; train a forecaster for
seasonal behavior" - there is no trained forecaster in the service.

## Copilot

`POST /api/copilot/chat` answers four intents from measurement: cost drivers, idle
resources, savings opportunities and inventory. An intent that cannot be answered from
current data returns an explicit unavailable statement with no estimate attached. `status`
reports `{ online: true, mode: "local-copilot-surface" }`.

## Automation

`/api/automation` exposes a demo rule surface: three seeded rules with mutable
`enabled`/schedule/savings, in-memory CRUD, and a single simulated execution-history row
(`status: SIMULATED`, message "Demo Mode - Execution engine in development"). Rules do not
run anything; the surface demonstrates the API shape.

## Tenants

The full lifecycle is available through `/api/tenants`, with `@Version` optimistic locking
so concurrent edits do not overwrite each other. Deactivation is a soft delete, because
audit entries, remediation records and cleanup reports reference the tenant id.

A new tenant starts in `MANUAL` mode with `requireApprovalForChanges: true`. Neither
autonomous mode nor approval bypass is enabled at creation. The invariant
`AUTONOMOUS ⇒ requireApprovalForChanges` is enforced on update.

No endpoint accepts or returns AWS credentials. Per-tenant execution roles are resolved in
AWS mode from `awsRoleArn`/`awsExternalId` via STS by `AwsClientFactory`.

## Audit

Every remediation request records a `RemediationOperation` (with idempotency key,
action, resource, dry-run flag, status, message and savings) and an `AuditLog` entry
(action, resource, status, region, explanation, savings, reason). Recommendation lifecycle
transitions also write audit entries. The optimisation history endpoint
(`/api/optimizations/history`) reads from the audit log via
`OptimizationHistoryService`, so an account with no recorded activity shows an empty
history rather than a sample one.