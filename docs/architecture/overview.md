# Architecture

## Design principles

**One way to ask the cloud.** Every AWS call originates in `AwsClientFactory`. Services
depend on `CloudProviderPort`. No service constructs an SDK client, selects a region or
reads a credential. This is what guarantees the sandbox endpoint override applies to all
traffic.

**Measure, or report absence.** Each read path distinguishes three outcomes: data
available, no data, and cannot reach the source. The third never renders as the second.
`CostReport` carries an `available` flag; `AnomalyReport` carries `status`; the copilot
answers "unavailable" instead of estimating.

**One mutation path.** `RemediationExecutor.execute` receives a `ResourceAction`. The
dry-run interlock, the protected-resource guard and the audit write live in
`AwsCloudProviderAdapter#execute`. Adding a mutation surface requires no change to any
service.

**Fail per scanner, not per request.** `guarded()` wraps each resource scanner, so an
unavailable AWS service omits its section from the inventory instead of failing the
whole response.

## Request flow

```
Controller
   └── Service            (analysis, policy, projection)
          └── CloudProviderPort
                 └── AwsCloudProviderAdapter
                        ├── AwsClientFactory     (endpoint, region, credentials)
                        ├── AwsPriceList         (list prices for attribution)
                        └── AWS SDK
```

## Resource model

`CloudResource` normalises every resource type to one shape: id, type, region, state,
name, tags, specs, hourly and monthly cost, creation time, protection flag.

`ResourceType` enumerates the supported types. `ActionType` pairs each operation with a
`RiskLevel`, so blast radius is a property of the operation rather than of the caller.

## Adding a resource type

1. Add the constant to `ResourceType`.
2. Add a new `ResourceScanner` (e.g. `Ec2InstanceScanner`) that extends
   `AwsResourceScanner` and maps the SDK model to `CloudResource`, using
   `ProtectionPolicy` to set the `protected` flag. Annotate it `@Component` so the
   adapter composes it automatically.
3. The adapter calls every registered scanner via its injected `List<ResourceScanner>`;
   no interface change is needed. `ResourceQuery` filters on type, state, region and
   tags, so a newly added type participates in filtering automatically.
4. Add cleanup and remediation actions to the `execute` switch and to the
   `ActionType`-driven `RemediationService` messaging.

## Cost attribution

Cost Explorer groups spend by service, region and linked account. It does not attribute
spend to a resource id. `AwsPriceList` supplies us-east-1 on-demand list prices so a
single resource can be labelled with an estimate when its spend is otherwise unattributed.

Totals always come from Cost Explorer. A price-table figure never contributes to a total.

## Anomaly detection

`AnomalyServiceImpl` compares daily spend against its own rolling median and median
absolute deviation. A fixed percentage threshold flags a naturally volatile service on
every day; a median-relative threshold does not.

With fewer than seven days of history the endpoint returns `status: INSUFFICIENT_DATA`
rather than a verdict.

## Forecasting

`ForecastingService` fits models over supplied history. With insufficient history it
returns `available: false` and an empty prediction list.

The ML service holds no trained forecasting model, so `DeepLearningForecaster` reports
itself unavailable. It exposes no `accuracy_score` because none is measured.

## Scaling

AWS SDK clients are created per call and used in try-with-resources. Each client is
thread-safe and pool-backed by default, and short-lived clients avoid unbounded
connection accumulation under load.

Scanners paginate, so inventory cost grows linearly in resource count rather than
requiring the entire account in one response.

`DescribeInstanceTypes` results are cached per process. The catalogue changes only when
AWS launches hardware, so an un-cached call re-requests the full catalogue on every
inventory refresh.

## Database

Entities extend `BaseEntity`, which supplies a UUID id, `tenantId`, `createdAt` and
`updatedAt`. `Tenant` stands alone because it owns tenancy rather than belonging to a
tenant, and carries `@Version` for optimistic locking.

## Configuration

`CloudProperties` binds `cloud.*`. Binding is type-checked at startup, so an invalid
`cloud.mode` fails immediately rather than defaulting silently.