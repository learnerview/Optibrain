/**
 * Formatting helpers.
 *
 * <p>Currency formatting was previously inlined in 13 places and, more seriously, used
 * the rupee sign for data the backend reports in USD. Cost Explorer returns USD, so the
 * correct symbol is the one Cost Explorer uses; the currency code from the response is
 * passed through so a non-USD account renders correctly too.
 */

const SYMBOLS: Record<string, string> = {
  USD: "$",
  EUR: "€",
  GBP: "£",
  INR: "₹",
  JPY: "¥",
};

/**
 * Formats an amount in the given currency.
 *
 * @param amount value in major units (dollars, not cents)
 * @param currency ISO code; defaults to USD because Cost Explorer reports USD
 */
export function formatCurrency(amount: number | null | undefined, currency = "USD"): string {
  if (amount === null || amount === undefined || Number.isNaN(amount)) return "—";
  const symbol = SYMBOLS[currency] ?? `${currency} `;
  const sign = amount < 0 ? "-" : "";
  return `${sign}${symbol}${Math.abs(amount).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

/**
 * Compact form for dashboard tiles: $12.4k, $1.2M.
 *
 * @param amount value in major units
 * @param currency ISO code
 */
export function formatCompactCurrency(amount: number | null | undefined, currency = "USD"): string {
  if (amount === null || amount === undefined || Number.isNaN(amount)) return "—";
  const symbol = SYMBOLS[currency] ?? `${currency} `;
  const abs = Math.abs(amount);
  const sign = amount < 0 ? "-" : "";

  if (abs >= 1_000_000) return `${sign}${symbol}${(abs / 1_000_000).toFixed(1)}M`;
  if (abs >= 1_000) return `${sign}${symbol}${(abs / 1_000).toFixed(1)}k`;
  return `${sign}${symbol}${abs.toFixed(2)}`;
}

/** Formats a 0-100 percentage. */
export function formatPercent(
  value: number | null | undefined,
  fractionDigits = 1
): string {
  if (value === null || value === undefined || Number.isNaN(value)) return "—";
  return `${value.toFixed(fractionDigits)}%`;
}

/** Formats an ISO instant as a readable local date. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

/** Formats an ISO instant as a readable local date and time. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Extracts a year/month key such as "2026-10" from an instant, for grouping.
 */
export function monthKey(iso: string): string {
  return iso.slice(0, 7);
}

/** Short month label such as "Oct" from a "2026-10" key. */
export function monthLabel(key: string): string {
  const date = new Date(`${key}-01T00:00:00Z`);
  if (Number.isNaN(date.getTime())) return key;
  return date.toLocaleDateString(undefined, { month: "short", timeZone: "UTC" });
}