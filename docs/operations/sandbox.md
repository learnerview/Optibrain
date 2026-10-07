# LocalStack Sandbox

The sandbox runs the production AWS code against a local AWS API implementation. It is
not a mock: the backend uses the ordinary AWS SDK with ordinary request signatures. Only
the endpoint and credentials differ.

| | Sandbox | Production |
|---|---|---|
| Endpoint | `http://localhost:4566` | AWS |
| Credentials | `test` / `test` | Ambient credential chain |
| Code path | `AwsCloudProviderAdapter` | `AwsCloudProviderAdapter` |

## Setup

```bash
docker compose up -d localstack
docker compose --profile seed run --rm seed
```

The seed script is re-runnable: resources it has already created are reported as
already present rather than re-created. It is not fully idempotent - on the LocalStack
community image the idle-CPU datapoint and ELB steps are expected to fail with a sandbox
error, so the sandbox may be missing those two sections.

## Seeded resources

| Resource | Purpose |
|---|---|
| Baseline instance | A healthy resource that should raise no recommendation |
| Oversized idle instance | A rightsizing candidate |
| Unattached EBS volume | A cleanup candidate |
| Tags including `Environment` | Exercises tag-based filtering and policy |
| Snapshot | Exercises the snapshot scanner |
| CloudWatch alarm | Exercises the telemetry path |

## Service coverage

LocalStack's community licence covers these services:

`ec2`, `cloudwatch`, `autoscaling`, `rds`, `sts`, `s3`

Two capabilities behave differently, and the product states this in its responses rather
than papering over it:

**Cost Explorer is absent.** Cost endpoints return `available: false` with an explanation.
Savings, spend breakdowns, the monthly report and cost-driven anomaly detection all report
no data. This is a sandbox limitation, not a defect: against a real AWS account the same
endpoints return measured figures.

**Elastic Load Balancing v2 is absent.** Load balancer scans return an empty section.
Each scanner in `AwsCloudProviderAdapter` is individually guarded, so one unavailable
service omits its section rather than failing the whole request.

## Validating a change

```bash
# Inventory reflects the seeded account
curl -s localhost:8080/api/resources | jq '.data | length'

# Dry-run blocks mutations
# Change cloud.dry-run to false, then confirm the adapter still simulates
```

The dry-run interlock is independent of the sandbox. With `cloud.dry-run=true` no AWS
mutation is issued in either target.

## Troubleshooting

**`AwsClientFactory` fails at startup.** In `SANDBOX` mode it requires only the endpoint.
Confirm LocalStack is healthy:

```bash
curl -s localhost:4566/_localstack/health
```

**Empty inventory.** Run the seed script.

**Cost endpoints return `available: false`.** Expected. Cost Explorer is absent.

**Timeouts under load.** LocalStack is single-instance and not a throughput reference.
Measure throughput against a real account or a dedicated sandbox deployment.