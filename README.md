# OptiBrain

AWS cost optimisation: measure an account, recommend changes, execute the approved ones.

Every displayed figure comes from a measurement. Where a measurement is unavailable, the
product reports that fact rather than substituting an estimate.

## Documentation

Full documentation lives in [`docs/`](docs/README.md).

| | |
|---|---|
| [Product and architecture](docs/README.md) | Overview, quick start, design commitments |
| [API reference](docs/api/endpoints.md) | Every endpoint and response shape |
| [Architecture](docs/architecture/overview.md) | Principles, request flow, extension points |
| [Security](docs/operations/security.md) | Safety controls and credentials |
| [IAM policy](docs/operations/iam-policy.md) | Least-privilege permissions |
| [Sandbox](docs/operations/sandbox.md) | LocalStack setup and coverage gaps |
| [ML service](docs/operations/ml-service.md) | Endpoints and data contract |
| [Roadmap](docs/product/roadmap.md) | Prioritised backlog |
| [Market position](docs/product/position.md) | Competitive analysis and strategy |
| [Feature gaps](docs/product/feature-gap-analysis.md) | Capability assessment |

## Services

| Component | Port | Purpose |
|---|---|---|
| `backend` | 8080 | API, AWS integration, remediation |
| `frontend` | 3000 | Dashboard |
| `ml-service` | 8000 | Forecasting, anomaly detection |
| LocalStack | 4566 | AWS API sandbox |

## Quick start

```bash
docker compose up -d localstack
docker compose --profile seed run --rm seed

cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Two properties

**An uncertainty contract.** Every read path distinguishes data available, no data, and
source unreachable. The third never renders as the second.

**An execution layer with guardrails.** `cloud.dry-run` defaults to `true` and blocks every
mutation in `AwsCloudProviderAdapter#execute`, the only site that mutates AWS. Resources
tagged `optibrain:protected=true` reject destructive actions.