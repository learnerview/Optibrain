import Link from "next/link";

export default function LandingPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-16">
      <h1 className="text-3xl font-semibold tracking-tight">OptiBrain</h1>
      <p className="mt-4 text-lg text-muted-foreground">
        AWS-first cost and remediation workspace. Default is LocalStack sandbox,
        dry-run remediation, and explicit protected-resource safeguards.
      </p>

      <section className="mt-10 space-y-3 text-sm">
        <h2 className="text-lg font-medium">What it does</h2>
        <ul className="list-disc space-y-2 pl-5 text-muted-foreground">
          <li>Inventories EC2, EBS, RDS, NAT gateways, Elastic IPs, load balancers, network interfaces, security groups, autoscaling groups, Lambda, DynamoDB, S3, and SQS where the configured account supports them.</li>
          <li>Refreshes cost, tag, and remediation data from the backend; the frontend shows what the backend returns or a clear explanation of what it could not.</li>
          <li>Provides a backend-mediated remediation workflow: plan a change, review the exact risk and steps, then execute with a server-side dry-run default.</li>
          <li>Supports multiple tenants and SQLite/PostgreSQL-backed persistence.</li>
        </ul>
      </section>

      <section className="mt-10 space-y-3 text-sm">
        <h2 className="text-lg font-medium">Current limits</h2>
        <ul className="list-disc space-y-2 pl-5 text-muted-foreground">
          <li>Cost Explorer is not implemented by LocalStack, so against the sandbox cost pages report that no spend data is available.</li>
          <li>ECS service scanning is implemented but unavailable against the community LocalStack edition.</li>
          <li>The browser cannot change AWS credentials; connection mode is configured server-side.</li>
        </ul>
      </section>

      <section className="mt-10 space-y-3 text-sm">
        <h2 className="text-lg font-medium">Run locally</h2>
        <pre className="overflow-x-auto rounded-md border bg-muted p-3 text-xs">
{`docker compose up -d localstack
docker compose --profile seed run --rm seed
.\\run-all.ps1`}
        </pre>
        <div className="flex gap-3 pt-2">
          <Link className="text-primary underline" href="/auth/signin">
            Sign in
          </Link>
          <Link className="text-primary underline" href="/onboarding">
            Check connection
          </Link>
          <Link className="text-primary underline" href="/dashboard">
            Dashboard
          </Link>
        </div>
      </section>
    </div>
  );
}
