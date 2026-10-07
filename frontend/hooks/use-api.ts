"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";

type UseApiResult<T> = {
  data: T | null;
  loading: boolean;
  /** Set when the request failed. Distinct from "succeeded but empty". */
  error: string | null;
  reload: () => void;
};

/**
 * Fetches from the backend once per dependency change, with the loading and error states
 * every dashboard page was duplicating by hand.
 *
 * <p>Previously each page had its own copy of `useState(null)` + `useEffect` +
 * `try/catch/finally`, and on failure they all silently kept the previous value, so a
 * broken backend looked identical to an empty account.
 */
export function useApi<T>(
  loader: () => Promise<T>,
  deps: readonly unknown[] = []
): UseApiResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [nonce, setNonce] = useState(0);

  const reload = useCallback(() => setNonce((n) => n + 1), []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    loader()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((cause: unknown) => {
        if (cancelled) return;
        setData(null);
        setError(
          cause instanceof ApiError
            ? cause.message
            : "Unexpected error while contacting the OptiBrain backend."
        );
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, nonce]);

  return { data, loading, error, reload };
}