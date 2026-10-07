"use client";

import { Card, CardContent } from "@/components/ui/card";
import { AlertTriangle, RefreshCw, Database } from "lucide-react";

type EmptyStateProps = {
  title: string;
  description: string;
  /** Distinguishes "nothing found" from "could not ask", which need different wording. */
  variant?: "no-data" | "error";
  onRetry?: () => void;
};

/**
 * Shown when an endpoint returns nothing, or cannot be reached.
 *
 * <p>This exists because the honest answer is often "there is no data here", and the old
 * alternative was to render invented numbers instead. Each case now explains itself: an
 * unreachable backend is visibly different from an empty account, because one needs a
 * developer and the other needs nothing.
 */
export function EmptyState({
  title,
  description,
  variant = "no-data",
  onRetry,
}: EmptyStateProps) {
  const Icon = variant === "error" ? AlertTriangle : Database;

  return (
    <Card className="border-dashed">
      <CardContent className="flex flex-col items-center justify-center gap-3 py-12 text-center">
        <Icon
          className={`h-8 w-8 ${variant === "error" ? "text-amber-500" : "text-muted-foreground"}`}
          aria-hidden
        />
        <div className="space-y-1">
          <p className="font-medium">{title}</p>
          <p className="max-w-md text-sm text-muted-foreground">{description}</p>
        </div>
        {onRetry && variant === "error" && (
          <button
            type="button"
            onClick={onRetry}
            className="inline-flex items-center gap-2 rounded-md border px-3 py-1.5 text-sm hover:bg-accent"
          >
            <RefreshCw className="h-3.5 w-3.5" aria-hidden />
            Retry
          </button>
        )}
      </CardContent>
    </Card>
  );
}