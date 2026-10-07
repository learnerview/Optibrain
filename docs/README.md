# OptiBrain Documentation

OptiBrain analyses AWS infrastructure cost, recommends changes, and executes approved
remediation. Every displayed figure comes from a measurement. Where a measurement is
unavailable, the product reports that fact rather than substituting an estimate.

## Documentation map

| Document | Contents |
|---|---|
| [feature-reference.md](feature-reference.md) | What each capability measures, and its limits |
| [architecture/overview.md](architecture/overview.md) | Design principles, request flow, extension points, scaling |
| [api/endpoints.md](api/endpoints.md) | Every endpoint with request and response shapes |
| [operations/security.md](operations/security.md) | Safety controls, credentials, authentication, tenant isolation |
| [operations/iam-policy.md](operations/iam-policy.md) | Least-privilege IAM policy, split read versus remediation |
| [operations/sandbox.md](operations/sandbox.md) | LocalStack setup, seeding, coverage gaps |
| [operations/ml-service.md](operations/ml-service.md) | ML service endpoints and data-honesty contract |
| [product/position.md](product/position.md) | Market position and competitive strategy |
| [product/feature-gap-analysis.md](product/feature-gap-analysis.md) | Capability assessment against the 2026 market |
| [product/roadmap.md](product/roadmap.md) | Prioritised backlog with effort estimates |

## Components

| Component | Port | Purpose |
|---|---|---|
| `backend` | 8080 | API, AWS integration, remediation |
| `frontend` | 3000 | Dashboard |
| `ml-service` | 8000 | Forecasting and anomaly detection |
| LocalStack | 4566 | AWS API sandbox |

## Quick start

```bash
docker compose up -d localstack
docker compose --profile seed run --rm seed

cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

The ML service runs separately and the backend degrades without it:

```bash
cd ml-service && python -m uvicorn main:app --port 8000
```

## Design commitments

**Measure, or report absence.** Every read path distinguishes three outcomes: data
available, no data, and source unreachable. The third never renders as the second. A
`CostReport` carries `available`; an `AnomalyReport` carries `status`; the copilot answers
"unavailable".

**One way to ask the cloud.** All AWS access originates in `AwsClientFactory`. Services
depend on `CloudProviderPort` and hold no SDK clients. This is what guarantees the sandbox
endpoint override applies to all traffic and that credentials never reach a service.

**One mutation path.** `RemediationExecutor.execute` receives a `ResourceAction`. The
dry-run interlock, the protected-resource guard and the audit write live in
`AwsCloudProviderAdapter#execute`. No service bypasses them because no service owns an AWS
client.

## Repository layout

```
backend/          Spring Boot API, AWS adapter, remediation engine
frontend/         Next.js dashboard
ml-service/       FastAPI forecasting and anomaly detection
scripts/sandbox/  Re-runnable LocalStack seeding
compose.yaml      LocalStack service definition
docs/             This documentation set
```

## Verification

```bash
cd backend && mvn clean verify     # compile and run tests
cd frontend && npm run typecheck   # tsc --noEmit
cd frontend && npm run build       # production build
```

Type errors fail the frontend build. `next.config.mjs` sets
`typescript.ignoreBuildErrors: false`.