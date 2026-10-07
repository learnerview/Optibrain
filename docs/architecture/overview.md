# Architecture

## Design principles

**One way to ask the cloud.** Every AWS call originates in `AwsClientFactory`
(`com.optibrain.cloud.aws`). Services depend on `CloudProviderPort`
(`com.optibrain.cloud.port`) and hold no SDK clients, select no region and read no
credential. This is what guarantees the sandbox endpoint override applies to all traffic
and that credentials never reach a service.

**Measure, or report absence.** Each read path distinguishes three outcomes: data
available, no data, and cannot reach the source. The third never renders as the second.
`CostReport` carries `status` (`AVAILABLE` when measured, `UNAVAILABLE` when Cost
Explorer could not be reached), `MetricsSeries` carries `DataStatus` (`EMPTY` when a
telemetry source returns no observations), an anomaly report carries `status`, the
copilot answers "unavailable", and the ML service returns `available: false`.

**One mutation path.** Services reach the provider only through `CloudProviderPort`;
`AwsCloudProviderAdapter#execute` is the only site that mutates AWS. The dry-run
interlock, the protected-resource guard and the audit write live there, so no service can
bypass them - a service that calls `execute` cannot opt out of the guards.

**Fault per scanner, not per request.** `AwsResourceScanner#scan` catches and logs each
scanner failure and returns an empty list, so an unavailable AWS service omits its section
from the inventory instead of failing the whole response.

## Request flow

```
Controller
   └── Service                     (analysis, policy, projection)
          └── CloudProviderPort
                 └── AwsCloudProviderAdapter
                        ├── AwsClientFactory      (endpoint, region, credentials)
                        ├── ResourceScanners      (one @Component per resource family)
                        ├── AwsPriceList          (list prices for explicit attribution)
                        └── AWS SDK
```

Controllers hold no AWS clients. `AwsClientFactory` serves SANDBOX (LocalStack) and AWS
(real endpoints) from the same code path: it overrides the endpoint in SANDBOX and resolves
tenant roles via STS in AWS mode.

## Resource model

`CloudResource` normalises every resource type to one shape: id, type, region, state,
name, tags, specs, hourly and monthly cost, creation time, and a protection flag set via
`ProtectionPolicy`.

`ResourceType` enumerates the supported types (34 constants). `ActionType` pairs each
operation with a `RiskLevel`, so blast radius is a property of the operation rather than
of the caller.

## Adding a resource type

1. Add the constant to `ResourceType`.
2. Add a new `@Component` scanner (e.g. `Ec2InstanceScanner`) that extends
   `AwsResourceScanner`, maps the SDK model to `CloudResource`, and sets the `protected`
   flag through `ProtectionPolicy`.
3. The adapter composes every registered scanner via the injected `List<ResourceScanner>`;
   no interface change is needed. `ResourceQuery` filters on type, state, region and
   tags, so a new type participates in filtering automatically.
4. For mutation support, add the `ActionType` case to the `execute` switch in
   `AwsCloudProviderAdapter` and to `RemediationService#stepsFor` / `#estimatedSaving`.

## Cost attribution

Cost Explorer groups spend by service, region and linked account; it does not attribute
spend to a resource id. `AwsPriceList` supplies us-east-1 on-demand list prices so a
single resource can be labelled with an estimate (`EbsScanner`, `AutoScalingScanner`,
`NetworkScanner`) when its spend is otherwise unattributed.

Totals always come from Cost Explorer; a price-table figure never contributes to a total.
Scanners that cannot price honestly (RDS, ECS, Lambda, DynamoDB, S3) report `null` cost.

## Anomaly detection

`AnomalyService` compares daily total spend and per-service spend against their own rolling
median and median absolute deviation, flagging only upper-tail spikes above
`3.0 * scaled MAD`. Services with fewer than seven days of history are skipped, and an
account with fewer than seven days returns `INSUFFICIENT_DATA` rather than a verdict.
Severity is `HIGH` when the increase is ≥ 300%.

`AlertService` derives a spend-spike alert from the last 30 days of the cost report
(last 7 vs. prior 23, ≥ 25% change; `HIGH` at ≥ 50%). Alerts are acknowledged in memory.

## Forecasting

Forecasting lives in the ML service (`ml-service/`), which runs as a separate FastAPI
process. The backend reaches it through `PyBridgeService`; when the ML service is down or
returns no data, the caller degrades to an explicit unavailable result rather than a
fabricated one. The ML service holds no trained forecaster - `/predict/forecast` returns a
persistence series and `/models/status` reports forecasting `available: false`.

## Scaling and client handling

AWS SDK clients come from `AwsClientFactory` and are used in try-with-resources; they are
thread-safe and connection-pool-backed. Scanners paginate, so inventory cost grows
linearly in resource count. The `DescribeInstanceTypes` catalogue is cached once per
process. Tenancy-bound STS clients are cached per tenant/role/external-id and reused until
expiry. Commitment coverage is refreshed once per cost report (Cost Explorer is the slowest
AWS API, so round trips are minimised).

## Database

Entities extend `BaseEntity` (UUID id, `tenantId`, `createdAt`, `updatedAt`). `Tenant`
stands alone because it owns tenancy rather than belonging to one, and carries `@Version`
for optimistic locking.

- Dev/default: SQLite (`data/optibrain-dev.db`), `ddl-auto=create-drop`.
- Product default (`application.properties`): SQLite, `ddl-auto=update`.
- Prod: PostgreSQL, `ddl-auto=validate` - the application refuses to start against a
  schema it does not expect.

SQLite does not honour `ALTER TABLE ... ADD CONSTRAINT ... UNIQUE`, so
`DbUniqueIndexInitializer` recreates the declared unique indexes
(`idx_app_users_username` on `app_users(username)`,
`uk_orphaned_resources_resource_id` on `orphaned_resources(resource_id)`) when the
database product is SQLite.

## Configuration

`CloudProperties` binds `cloud.*` (mode, dry-run, aws.region, aws.cost-explorer-region,
sandbox.endpoint). Binding is type-checked at startup, so an invalid `cloud.mode` fails
immediately rather than defaulting silently. Annotations in
`backend/src/main/resources/*.properties` document every environment variable
(`CLOUD_MODE`, `CLOUD_DRY_RUN`, `SANDBOX_ENDPOINT`, `AWS_REGION`, `JWT_SECRET`,
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `ALLOWED_ORIGINS`, ...).

## Multi-tenancy

`TenantFilter` (registered in `FilterConfig` after the Spring Security chain) derives the
`TenantContext` from the authenticated user's membership - never from a header. The
legacy `X-TENANT` / `X-USER` headers are tolerated only when they exactly match the
principal; a mismatch is rejected with 403. The context is cleared in a `finally` block.
In AWS mode, `AwsClientFactory` returns per-tenant clients authenticated via
`AssumeRole` with each tenant's `awsRoleArn` / `awsExternalId`.