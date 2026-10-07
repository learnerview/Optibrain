"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getDashboardOverview } from "@/lib/api";
import { useApi } from "@/hooks/use-api";
import { useAuthStore, useCloudStore } from "@/lib/store";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { CheckCircle2, XCircle, Loader2, ExternalLink } from "lucide-react";

/**
 * Connection setup.
 *
 * <p>This replaces a four-step wizard that asked the user to "connect" Azure and GCP
 * accounts and to grant nine permission scopes, then stored the result in localStorage
 * with `connected: true` - without contacting AWS, the backend, or any validation. The
 * user was shown a successful connection to clouds the product does not implement, and
 * permissions that were never enforced anywhere.
 *
 * <p>What the product actually needs is one thing: a working AWS connection, configured
 * server-side via `cloud.mode` and the AWS credential chain. So that is what this page
 * reports - verified by asking the backend, not asserted locally.
 */
export default function OnboardingPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const upsertAccount = useCloudStore((state) => state.upsertAccount);

  // Discovery succeeding is the only honest test of connectivity.
  const { data, loading, error, reload } = useApi(getDashboardOverview, []);

  const connected = Boolean(data?.realTimeMetrics?.connectedToCloud);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/signin");
    }
  }, [isAuthenticated, router]);

  useEffect(() => {
    if (connected) {
      upsertAccount({
        id: "aws-primary",
        accountId: "configured-server-side",
        accountName: "AWS",
        executionTarget: data?.executiveSummary.spendAvailable ? "AWS" : "SANDBOX",
        regions: [],
        connected: true,
        lastChecked: new Date().toISOString(),
      });
    }
  }, [connected, data, upsertAccount]);

  return (
    <div className="min-h-screen bg-background">
      <div className="mx-auto max-w-2xl px-4 py-16">
        <div className="mb-10 text-center">
          <h1 className="text-3xl font-bold">Connect OptiBrain</h1>
          <p className="mt-2 text-muted-foreground">
            OptiBrain reads your AWS account directly. There is nothing to connect from
            the browser - the connection is configured on the server.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              {loading && <Loader2 className="h-5 w-5 animate-spin" aria-hidden />}
              {error && <XCircle className="h-5 w-5 text-red-400" aria-hidden />}
              {connected && <CheckCircle2 className="h-5 w-5 text-emerald-400" aria-hidden />}
              AWS connection
            </CardTitle>
            <CardDescription>
              Verified by asking the backend to enumerate the account.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {loading && <p className="text-sm text-muted-foreground">Checking…</p>}

            {error && (
              <div className="space-y-3">
                <p className="text-sm text-red-400">{error}</p>
                <Button variant="outline" onClick={reload}>
                  Retry
                </Button>
              </div>
            )}

            {connected && !loading && (
              <div className="space-y-3">
                <p className="text-sm">
                  Connected. Inventory discovery succeeded.
                </p>
                {!data?.executiveSummary.spendAvailable && (
                  <p className="rounded-md border border-amber-500/30 bg-amber-500/5 p-3 text-sm text-amber-300">
                    Cost Explorer returned no spend. This is expected against the
                    LocalStack sandbox, which does not implement it. Cost features
                    will report no data until the backend runs against a real AWS
                    account.
                  </p>
                )}
                <Button onClick={() => router.push("/dashboard")}>
                  Open dashboard
                  <ExternalLink className="ml-2 h-4 w-4" aria-hidden />
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="mt-6">
          <CardHeader>
            <CardTitle>Running against the LocalStack sandbox?</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm text-muted-foreground">
            <p>
              From the repository root:
            </p>
            <pre className="overflow-x-auto rounded-md bg-muted p-3 text-xs">
{`docker compose up -d localstack
docker compose --profile seed run --rm seed`}
            </pre>
            <p>
              The seed script is re-runnable: resources it has already created are
              reported as already present rather than duplicated. Not every step is
              protected on the LocalStack community image (the idle-CPU datapoint and
              ELB steps may fail), so the sandbox may lack those sections.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}