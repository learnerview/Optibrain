# Security

OptiBrain reads an AWS account and, when authorised to, stops, resizes and terminates
resources. This document describes the controls that constrain that capability.

## Method-level RBAC

`@EnableMethodSecurity` is enabled on `SecurityConfig`. The effective gates are:

| Endpoint | Required authority |
|---|---|
| `GET /api/recommendations` | any authenticated user |
| `POST /api/recommendations/{id}/approve`, `/reject` | `CLOUD_INTELLIGENCE_ANALYST`, `ADMIN`, `OWNER` |
| `POST /api/recommendations/{id}/execute` | `DEVOPS_ENGINEER`, `ADMIN`, `OWNER` |

A user without the required authority receives `403` with
`"You do not have permission to perform this action"`. The `Role` enum defines
`VIEWER_ONLY`, `DEVOPS_ENGINEER`, `CLOUD_INTELLIGENCE_ANALYST`, `ADMIN`, `OWNER`,
in that order. The development seed creates `admin` (`ROLE_ADMIN`) and `user`
(`ROLE_USER`) only; the analyst/engineer authorities must be granted to a user to unlock
approval and execution. Everything matched by `requestMatchers("/api/**").authenticated()`.

## Dry-run interlock

`cloud.dry-run` defaults to `true` (`CLOUD_DRY_RUN`). `AwsCloudProviderAdapter#execute`
applies the interlock - `dryRun = action.dryRun() || properties.isDryRun()` - and it is
the only site in the codebase that mutates AWS resources, so no service bypasses it.

A dry-run produces the same `ActionResult` shape as a real run with `applied: false`, so
the UI and audit trail render "would have stopped X" through the same code path that
renders a completed remediation. The `RemediationController#RemediationRequest` treats an
absent `dryRun` as `true`: applying a change is an opt-in and never the default.

## Resource protection

A resource carrying `optibrain:protected=true` (`ProtectionPolicy.TAG`/`VALUE`) rejects
every destructive action. The check lives in `com.optibrain.cloud.policy.ProtectionPolicy`,
shared by the scanners (which populate each resource's `protected` flag), the remediation
planner (which explains the block) and the provider's `execute`, the only place a mutation
can happen. The check is on the resource's live state, so a plan that predates a
protection tag is refused when it is submitted.

## Fail-closed execution

Beyond the protection tag, `execute` refuses a destructive action whose target cannot be
verified in the current inventory (`"Target resource could not be found in the inventory;
refusing to apply a change that cannot be verified"`), refuses `RESIZE_INSTANCE` on any
instance the inventory does not confirm as `stopped` (`"An instance must be stopped before
resizing..."`), and refuses a `planToken` that no longer matches live state (`"The plan is
stale: ... Recompute the plan and retry."`).
Repeated `idempotencyKey`s replay the stored outcome instead of mutating twice.

## Credentials

No endpoint accepts or returns AWS credentials. `AwsClientFactory` holds all credential
resolution:

- `SANDBOX` uses static `test`/`test` credentials and overrides the endpoint to
  `http://localhost:4566`.
- `AWS` resolves through `DefaultCredentialsProvider` - IAM role first, then environment
  variables, then the shared credentials file. A missing credential fails at startup with
  a message naming the environment variables and pointing at `CLOUD_MODE=SANDBOX`.
- In `AWS` mode, per-tenant roles are resolved via `sts:AssumeRole` from each tenant's
  `awsRoleArn` / `awsExternalId`, cached per tenant key until expiry.

`aws.access-key` and `aws.secret-key` are absent from every properties file - a credential
in a properties file is a credential in version control.

## Authentication

`POST /api/auth/login` verifies credentials against the Spring Security `DaoAuthenticationProvider`
(same store the filter chain uses). `400` for blank input, `401` for invalid credentials.
`POST /api/auth/register` creates a `ROLE_USER` account (bean-validation checked; `409`
"User already exists" for duplicates). `POST /api/auth/logout` revokes the presented
token; the revoked set is bounded at 1000.

Sessions are stateless JWTs issued by `JwtService`: `app.jwt.secret` (`JWT_SECRET`) with a
working default of `OPTIBRAIN_DEV_JWT_SECRET_MEAN_CHANGE_IN_PROD_000`,
`app.jwt.expiration-ms=86400000` (24 hours). Production must supply a non-default secret.
`JwtAuthenticationFilter` authenticates the `Authorization: Bearer` header; an invalid
token simply leaves the context unauthenticated (the route then requires login).

`POST /api/auth/login` and `POST /api/auth/register` are public; `GET /actuator/health`,
`GET /actuator/info`, `/error`, `/swagger-ui/**` and `/v3/api-docs/**` are public;
everything else is authenticated.

## Error envelopes

`GlobalExceptionHandler` returns a single envelope for failures: `404` for unknown routes
(`"No endpoint at <path>"`), `403` for method-level access denial, `400` for typed binding
and validation failures, `405` for wrong methods, and `500` only for unhandled exceptions.
Unknown routes are not silently swallowed as 500s.

## Tenant isolation

`TenantFilter` derives the `TenantContext` from the authenticated user's membership, never
from a header. The legacy `X-TENANT` / `X-USER` headers are tolerated only when they
exactly match the principal; a mismatch returns `403` with a JSON body
(`"X-TENANT does not match the authenticated user's tenant"`). Every persisted row carries
`tenantId`. Tenant deactivation is a soft delete so audit entries, remediation records and
cleanup reports continue to resolve; `@Version` optimistic locking prevents concurrent
edits from silently overwriting each other.

## Network and exposure

CORS origins are configured through `cors.allowed-origins` (default
`http://localhost:3000,http://localhost:4200`; `ALLOWED_ORIGINS` env). Note
`SavingsController` is annotated `@CrossOrigin("*")`. In production,
`ALLOWED_ORIGINS` should be restricted to the real frontend origin.

Actuator exposes `health`, `info`, `metrics` in all profiles; dev additionally exposes
`env`, which discloses configuration values and must not be used against a real account.
`management.endpoint.health.show-details=always` in dev, `when-authorized` in prod.

## Database

- Prod uses PostgreSQL with `spring.jpa.hibernate.ddl-auto=validate`, so the application
  refuses to start against an unexpected schema rather than mutating it.
- Dev uses SQLite (`data/optibrain-dev.db`) with `create-drop`.
- On SQLite, `DbUniqueIndexInitializer` recreates the declared unique indexes that the
  dialect cannot add via `ALTER TABLE`.

## Reporting a vulnerability

Report suspected vulnerabilities through the repository's private security advisory
channel rather than a public issue.