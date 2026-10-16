# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in OptiBrain, please report it responsibly. Do not create a public GitHub issue containing exploit details. Open a GitHub Security Advisory if available, or contact the maintainers via your preferred secure channel.

## Security Controls

- **Authentication:** JWT-based authentication with Spring Security; the configured auth flow secures access to protected endpoints.
- **Authorization:** Method-level RBAC (@EnableMethodSecurity) enforces role-based access for approve/reject/execute operations (see docs/operations/security.md).
- **Secrets Management:** Per-tenant AWS credentials are handled via tenant configuration. Never commit secrets or keys to the repository.
- **Execution Guardrails:** All destructive operations flow through the single mutation path in AwsCloudProviderAdapter#execute, with the global dry-run interlock (cloud.dry-run), optibrain:protected tag enforcement, tenant protected-resource list enforcement, live-state re-verification, plan tokens, idempotency keys, and full audit logging.
- **Network & Transport:** Production deployments should use HTTPS/TLS and restrict actuator endpoints appropriately (see docs/operations/security.md).
- **Data Handling:** Cost/read paths distinguish measured, absent, and unreachable data. The platform does not fabricate figures. Recommendations are advisory and require human approval.

## Grounding & AI Outputs

OptiBrain includes ML/AI capabilities (forecasting, anomaly detection, cost optimization) via the ML service. Outputs are grounded in measured data where available and explicitly return INSUFFICIENT_DATA/UNAVAILABLE when data is missing. No automated execution is triggered by ML outputs; all writes require explicit approval and pass through guardrails.

## Responsible Disclosure

Please include steps to reproduce, potential impact, and affected versions. We will acknowledge receipt and coordinate a fix timeline with you.

## Secret Manager Integration

For production deployments, secrets should not be distributed as plain environment variables.
Instead, integrate a secret manager and inject values at startup:

- **HashiCorp Vault.** Use the Vault Agent sidecar or the Spring Cloud Vault starter to read
  `JWT_SECRET`, `EXTERNAL_AUTH_JWT_SECRET`, database credentials, and webhook signing keys from a
  mounted secret path at startup. The application continues to read from `System.getenv()`, so the
  sidecar can write values to a `.env` file or inject them directly into the container environment
  before the JVM starts.
- **AWS Secrets Manager.** Store secrets as JSON secret values. A small init container or entrypoint
  script fetches the secret via the AWS CLI/SDK and exports the individual keys as environment
  variables before launching the application. Never embed the secret value in the Docker image.
- **Azure Key Vault / GCP Secret Manager.** Equivalent pattern: init container fetches the named
  secret versions and exports them as environment variables; the application reads them via Spring's
  `${ENV_VAR:default}` property resolution.

**Rotation.** Rotate `JWT_SECRET` and webhook signing keys on a schedule. After rotation, restart
the application so the new key material is loaded. Old JWTs signed with the previous secret will
fail verification, which is the intended behavior for compromised-key rotation.

**Do not.** Do not commit `.env`, `.env.production`, `secrets.json`, or any file containing key
material to version control. `.env.example` may contain placeholder names only.

