# LocalStack Sandbox

The sandbox runs the production AWS code against a local AWS API implementation. It is
not a mock: the backend uses the ordinary AWS SDK with ordinary request signatures. Only
the endpoint and credentials differ.

| | Sandbox | Production |
|---|---|---|
| Endpoint | `http://localhost:4566` | AWS |
| Credentials | `test` / `test` | Ambient credential chain |
| `cloud.mode` | `SANDBOX` (dev default) | `AWS` (prod profile) |
| Code path | `AwsCloudProviderAdapter` | `AwsCloudProviderAdapter` |

## Setup

```bash
docker compose up -d localstack
docker compose --profile seed run --rm seed
```

`compose.yaml` runs `localstack/localstack:4` with `SERVICES=ec2,cloudwatch,autoscaling,rds,sts,s3,lambda,dynamodb,sqs`
on port 4566, pins `PERSISTENCE=0` by default, and provides a health check. The `seed`
service runs `scripts/sandbox/seed.sh` with an AWS CLI image.

## Seeded resources

The seed script builds a small, deliberately imperfect account - at least one of every
type the product analyses, including the cases that should produce a recommendation:

| Resource | Purpose |
|---|---|
| VPC (`10.0.0.0/16`) + public subnet (`us-east-1a`) | Networking for instances and gateways |
| Baseline instance (`t3.micro`, running, tagged) | Healthy resource that raises no recommendation |
| Idle instance (`m5.4xlarge`, running) | Rightsizing candidate |
| Unattached EBS volume (100 GiB) | `ORPHAN_CLEANUP` candidate; tagged `orphan-candidate`, `Environment=dev` |
| The same volume, tagged `optibrain:protected=true` | Exercises the destructive-action guard |
| Snapshot of that volume | Exercises the snapshot scanner |
| CloudWatch alarm + flat low-CPU datapoint | Makes rightsizing produce a real recommendation |
| Lambda function `optibrain-sandbox-handler` | Inventory coverage |
| DynamoDB table `optibrain-sandbox-table` | Inventory coverage |
| S3 bucket `optibrain-sandbox-bucket` | Inventory coverage |
| SQS queue `optibrain-sandbox-queue` | Inventory coverage |

All running instances are tagged `Name=sandbox-<id>`, `Environment=dev`, `Owner=platform`
so tag-based filtering and governance have data to work on.

## Re-running

The seed script is re-runnable. Steps are individually fault-tolerant: an "already
exists" outcome is reported as `ok: ... (already present)`, and any step that fails for a
reason other than duplication prints `SKIPPED` with the tail of the error. Two steps are
known not to complete on the LocalStack community image:

- the ELBv2 load balancer (`elb create-load-balancer`) - ELBv2 is outside the free
  licence, exactly as `compose.yaml` documents;
- the idle-CPU datapoint and high-CPU alarm may also skip on some versions.

The backend's scanners are guarded, so omitted sections render empty rather than failing,
and a partially seeded account is still usable.

## Service coverage

The free licence covers the services listed in `compose.yaml`. Three capabilities behave
differently, and the product states each in its responses rather than papering over it:

- **Cost Explorer is absent.** Cost Explorer calls fail, so cost endpoints return
  `status: UNAVAILABLE` (report/forecast) or `available: false` (explorer flags) rather
  than a measured zero. Savings, spend breakdowns, the monthly report, commitment
  coverage and cost-driven anomaly detection all report no data. This is a sandbox
  limitation, not a defect: against a real account the same endpoints return measured
  figures.
- **Elastic Load Balancing v2 and ECS are absent.** Load balancer and ECS scans return
  empty sections. Each scanner is individually guarded, so one unavailable service omits
  its section rather than failing the request.
- **Anomaly detection returns `INSUFFICIENT_DATA`** rather than a verdict while it holds
  fewer than seven days of history.

Recommendations still flow: orphan cleanup, rightsizing (from the seeded CloudWatch
datapoint) and the simulated commitment rows are generated from inventory and telemetry by
the normal code path.

## Validating a change

```bash
# Inventory reflects the seeded account
curl -s localhost:8080/api/resources | jq '.data | length'

# Plan a destructive action against the protected volume - it must come back blocked
TOKEN="..."  # from POST /api/auth/login
curl -s -X POST localhost:8080/api/remediation/plan \
  -H "Authorization: Bearer $TOKEN" -H "X-TENANT: default" \
  -d '{"actionType":"DELETE_VOLUME","resourceId":"vol-<protected>"}' \
  | jq '.data | {blocked, blockedReason, executable}'
```

With `cloud.dry-run=true` no AWS mutation is issued in either target; executions come back
`succeeded=false ... applied=false` with "Would ... (dry-run: no change applied)".

## Troubleshooting

**`AwsClientFactory` fails at startup.** In `SANDBOX` mode it requires only the endpoint.
Confirm LocalStack is healthy:

```bash
curl -s localhost:4566/_localstack/health
```

**Empty inventory.** Run the seed script.

**Cost endpoints return `status: UNAVAILABLE` or `available: false`.** Expected. Cost
Explorer is absent from LocalStack.

**ECS or load balancer sections empty.** Expected on the community image.

**Timeouts under load.** LocalStack is single-instance and not a throughput reference.
Measure throughput against a real account or a dedicated sandbox deployment.