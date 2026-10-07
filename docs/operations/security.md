# Security

OptiBrain reads an AWS account and, when authorised to, stops, resizes and terminates
resources. This document describes the controls that constrain that capability.

## Dry-run interlock

`cloud.dry-run` defaults to `true`.

`AwsCloudProviderAdapter#execute` applies the interlock. It is the only site in the
codebase that mutates AWS resources, so no service bypasses it: services depend on
`CloudProviderPort` and hold no SDK clients of their own.

A dry-run produces the same `ActionResult` shape as a real run, with `applied: false`.
The interface and audit trail therefore render "would have saved $X" through the same
code path that renders a completed remediation.

## Resource protection

A resource carrying the tag `optibrain:protected=true` rejects every destructive action.
The check lives in `com.optibrain.cloud.policy.ProtectionPolicy`, shared by the scanners
(which populate each resource's `protected` flag), the remediation planner (which
explains the block) and the provider's `execute`, the only place a mutation can happen.

## Credentials

No endpoint accepts or returns AWS credentials. `CloudCredentials` is absent from the
codebase.

`AwsClientFactory` resolves credentials through `DefaultCredentialsProvider`, which
covers, in order: environment variables, the shared credentials file, and IAM roles for
EC2, ECS, Lambda and EKS. An IAM role is the appropriate choice for a deployed workload.

`cloud.mode=AWS` fails at startup when no credential resolves, with a message naming the
environment variables and pointing at `CLOUD_MODE=SANDBOX` as the alternative.

## Authentication

`POST /api/auth/login` verifies credentials against the Spring Security user store and
rejects unknown users with 401. Blank input returns 400.

`POST /api/auth/login` and `POST /api/auth/register` are public; every other `/api/**`
endpoint requires a valid JWT in all profiles, including development. The frontend stores
the token in `localStorage` and sends it as `Authorization: Bearer`, so the sandbox is
usable only after a login.

Production user credentials come from the `ADMIN_PASSWORD` and `USER_PASSWORD`
environment variables. Startup fails when either is absent in `prod`, rather than
falling back to a default.

## Secrets in configuration

`aws.access-key` and `aws.secret-key` are absent from every properties file. A
credential in a properties file is a credential in version control.

## Database

`prod` uses PostgreSQL with `spring.jpa.hibernate.ddl-auto=validate`, so the application
refuses to start against a schema it does not expect rather than mutating it.

`dev` and the default profile use SQLite (`data/optibrain-dev.db` and
`data/optibrain.db`), so state survives a restart. `prod` uses PostgreSQL.

## Tenant isolation

Every persisted row carries `tenantId`. Tenant deactivation is a soft delete, so audit
entries, remediation records and cleanup reports continue to resolve.

Tenant records carry `@Version` for optimistic locking, so two concurrent edits do not
silently overwrite each other.

## Network

CORS origins are configured through `cors.allowed-origins` and default to
`http://localhost:3000` outside production.

Actuator exposes `health`, `info` and `metrics` in all profiles. The `dev` profile
additionally exposes `env`, which discloses configuration values and must not be used
against a real account.

## Reporting a vulnerability

Report suspected vulnerabilities through the repository's private security advisory
channel rather than a public issue.