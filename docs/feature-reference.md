# Feature Reference

What the product does, and what each capability actually measures.

## Inventory

`GET /api/resources` enumerates these AWS resource types (each scanner is fault-isolated,
so a service that is unavailable in the account omits its section):

| Type | Source API | Cost attributed |
|---|---|---|
| `Instance` / `GPU_INSTANCE` | `DescribeInstances` | List-price estimate only |
| `Volume` / `EBS_SNAPSHOT` | `DescribeVolumes`, `DescribeSnapshots` | Storage priced per GiB-month |
| `NAT_GATEWAY` | `DescribeNatGateways` | Yes, fixed hourly |
| `ELASTIC_IP` | `DescribeAddresses` | Yes, charged only when unassociated |
| `LOAD_BALANCER` | `DescribeLoadBalancers` | No |
| `NetworkInterface` | `DescribeNetworkInterfaces` | No |
| `SecurityGroup` | `DescribeSecurityGroups` | No |
| `ASG` | `DescribeAutoScalingGroups` | Yes, per-member instance type |
| `RDS_INSTANCE` / `RDS_CLUSTER` | `DescribeDBInstances`, `DescribeDBClusters` | List-price estimate |
| `Function` | Lambda `ListFunctions` | No |
| `Table` | DynamoDB `ListTables` | No |
| `Bucket` | S3 `ListBuckets` | No |
| `Queue` | SQS `ListQueues` | No |

`ResourceType` also declares EKS/ECS, CloudFront, ElastiCache, OpenSearch, MSK, Redshift,
Kinesis, Glue, Athena, SQS/SNS (queue listed), and ECR. The corresponding scanners are
not implemented yet, so those enum values reserved for future coverage do not produce
rows today.

Snapshots backing an AMI are excluded. They are removed implicitly with their AMI and
are not standalone waste. Some EC2 implementations also ignore the owner filter, so
ownership and purpose are both checked rather than trusted to the API.

## Cost attribution

Cost Explorer groups spend by service, region and linked account. It does not attribute
spend to a resource id.

**Measured.** Totals, service breakdowns, regional breakdowns, daily series and
linked-account rollups. These come from Cost Explorer and match the invoice.

**Estimated.** Per-resource monthly cost, derived from us-east-1 list prices held in
`AwsPriceList`. A resource reports `null` where attribution fails rather than presenting an
estimate as a measurement.

**Commitments.** `CommitmentCoverage` measures how much recent spend a Savings Plan or
Reserved Instance absorbs. The savings projection carries both a raw figure priced from
on-demand rates and an effective figure (`effectiveMonthlySavings`) that discounts the
share already covered by a commitment, so a reader sees the realistic benefit and can
tell when coverage could be measured versus estimated (`commitmentAdjusted`).

AWS offers four Savings Plan families: Compute (up to 66 percent), EC2 Instance (72
percent), Database (35 percent) and SageMaker AI (64 percent).

## Recommendations

| Trigger | Evidence required |
|---|---|
| Unattached EBS volume | `attachmentCount` of zero and cost above zero |
| Unassociated Elastic IP | `associated` false and cost above zero |
| Rightsizing | Sustained CloudWatch CPU below threshold |

An instance with no telemetry produces no recommendation. Absence of data is not evidence
of waste.

## Anomaly detection

Daily spend is compared against its own rolling median and median absolute deviation. A
fixed percentage threshold flags a naturally volatile service every day; a median-relative
threshold does not.

With fewer than seven days of history the endpoint returns `status: INSUFFICIENT_DATA`
rather than a verdict. A day is flagged when spend exceeds the median by more than three
scaled median absolute deviations.

## Remediation

Each action carries a risk level:

| Action | Risk |
|---|---|
| `NOOP` | None |
| `STOP_INSTANCE`, `START_INSTANCE` | Low |
| `RELEASE_ELASTIC_IP` | Low |
| `SCALE_GROUP` | Low |
| `APPLY_TAGS` | Low |
| `RESIZE_INSTANCE` | Medium |
| `PURCHASE_COMMITMENT` | Medium |
| `DELETE_SNAPSHOT` | High |
| `DELETE_VOLUME` | High |
| `DELETE_BUCKET` | High |
| `DELETE_NETWORK_RESOURCE` | High |
| `DELETE_DATASTORE` | Critical |
| `TERMINATE_INSTANCE` | Critical |

Two guards apply to every action inside `AwsCloudProviderAdapter#execute`.

**Dry-run.** `cloud.dry-run` defaults to `true`. A dry-run produces the same result shape
as a real run with `applied: false`, so the interface renders both through one code path.

**Protection.** A resource tagged `optibrain:protected=true` rejects destructive actions.

## Tag governance

`GET /api/tags/coverage` reports compliance against required keys, defaulting to
`Environment` and `Owner`. It returns the compliant percentage, the attributed cost
belonging to non-compliant resources, violations by resource type, and individual offenders
ordered by cost.

The report is audit-only. AWS tag policies and service control policies perform
enforcement; implementing it in the application would duplicate a weaker version.

## Forecasting

`ForecastingService` fits models over supplied history. With insufficient history it
returns `available: false` and an empty prediction list.

The ML service holds no trained forecasting model, so `DeepLearningForecaster` reports
itself unavailable and exposes no `accuracy_score`, because none is measured.

`SpotPredictor` returns `risk_score: null`. AWS exposes no public API for spot market depth
or interruption notices.

## Copilot

`POST /api/copilot/chat` answers four intents from measurement: cost drivers, idle
resources, savings opportunities and inventory. An intent that cannot be answered returns
a statement that the data is unavailable, with no estimate attached.

## Tenants

The full lifecycle is available through `/api/tenants`, with optimistic locking so
concurrent edits do not overwrite each other. Deactivation is a soft delete, because audit
entries, remediation records and cleanup reports reference the tenant id.

A new tenant starts in `MANUAL` mode with approval required. Neither is enabled at
creation, so a fresh install cannot mutate infrastructure without a human decision.

No endpoint accepts or returns AWS credentials.

## Audit

Every remediation request records an entry in the audit log with its action, resource,
status, region, explanation and savings. The optimisation history endpoint reads from that
log, so an account with no recorded remediation shows an empty history rather than a
sample one.