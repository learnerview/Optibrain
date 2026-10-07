/**
 * OptiBrain API client.
 *
 * <p>Every request goes to the real backend. There is no demo fixture and no in-memory
 * fallback: if the backend is unreachable, these functions reject so the UI can say so.
 * Silently substituting invented figures was the previous design, and it made a broken
 * deployment indistinguishable from a healthy one.
 */

/**
 * Requests are proxied through the Next.js origin by the rewrite in
 * `next.config.mjs`, so this is a relative path by default. The absolute form remains
 * available for callers running outside the dev server.
 */
const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "/api";

/**
 * Error thrown for a non-2xx response, carrying the backend's own error envelope so a
 * caller can surface the real reason instead of "something went wrong".
 */
export class ApiError extends Error {
  readonly status: number;
  readonly endpoint: string;

  constructor(message: string, status: number, endpoint: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.endpoint = endpoint;
  }
}

/**
 * Performs one request and unwraps the `ApiResponse` envelope.
 *
 * <p>Every endpoint answers `{ success, message, data, timestamp }`. The previous
 * implementation read `json.data` without checking the envelope's `success` flag, so a
 * backend that answered 200-with-failure (a service error envelope) resolved as an
 * empty success and every page silently rendered "no data". A failed envelope is now an
 * error, and a revoked/expired session clears the stored token and returns the caller
 * to sign-in instead of leaving a dead dashboard on screen.
 */
async function apiRequest<T>(endpoint: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    const token =
      typeof window !== "undefined"
        ? window.localStorage.getItem("optibrain_token")
        : null;
    response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...init,
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(init?.headers ?? {}),
      },
    });
  } catch (cause) {
    throw new ApiError(
      `Cannot reach the OptiBrain backend at ${API_BASE_URL}. Is it running?`,
      0,
      endpoint
    );
  }

  let body: { success?: boolean; message?: string; data?: unknown } | null = null;
  try {
    body = await response.json();
  } catch {
    // Non-JSON error body.
  }

  if (!response.ok) {
    if (response.status === 401 && typeof window !== "undefined") {
      window.localStorage.removeItem("optibrain_token");
      window.localStorage.removeItem("auth-store");
      window.localStorage.removeItem("cloud-store");
      if (!window.location.pathname.startsWith("/auth/")) {
        window.location.assign("/auth/signin");
      }
    }
    throw new ApiError(
      body?.message || response.statusText,
      response.status,
      endpoint
    );
  }

  if (body === null || body.success === false) {
    throw new ApiError(
      body?.message || "The backend reported a failure without a message",
      response.status,
      endpoint
    );
  }
  return body.data as T;
}

// --- Response shapes -------------------------------------------------------
// These mirror the backend DTOs as they are actually serialised, verified against a
// running instance rather than assumed.

export interface ExecutiveSummary {
  realisedSavings: number;
  monthlySpend: number;
  /** null when Cost Explorer returned no spend, since a ratio against zero is meaningless. */
  roiRatio: number | null;
  totalTenantsMonitored: number;
  activeAutomations: number;
  spendAvailable: boolean;
  lastCalculated: string;
}

export interface RealTimeMetrics {
  liveSavingsCounter: number;
  pendingOptimizations: number;
  connectedToCloud: boolean;
}

export interface TopPerformer {
  tenantName: string;
  savings: number;
}

export interface DashboardOverview {
  lastUpdated: string;
  executiveSummary: ExecutiveSummary;
  realTimeMetrics: RealTimeMetrics;
  topPerformers: TopPerformer[];
}

export interface Anomaly {
  title: string;
  description: string;
  severity: string;
  timestamp: string;
  status: string;
}

export interface AnomalyReport {
  anomalies: Anomaly[];
  count: number;
  /** NOMINAL | ANOMALIES_DETECTED | INSUFFICIENT_DATA | UNAVAILABLE */
  status: string;
  /** Present when status is INSUFFICIENT_DATA or UNAVAILABLE. */
  reason?: string;
  method?: string;
  daysAnalysed?: number;
  timestamp: string;
}

export interface Recommendation {
  id?: string;
  resourceId?: string;
  action?: string;
  recommendedType?: string;
  reason?: string;
  monthlySavings?: number;
  confidence?: number;
  status?: string;
  createdAt?: string;
}

export interface SavingsProjection {
  monthlySavings: number;
  annualSavings: number;
  confidence: string;
  assumptions: string[];
}

export interface CostExplorerBreakdownRow {
  service: string;
  cost: number;
  percentage: number;
}

export type DataStatus = "AVAILABLE" | "EMPTY" | "UNAVAILABLE";

export interface CostExplorerData {
  from: string;
  to: string;
  service: string;
  region: string;
  totalCost: number;
  currency: string;
  costByService: Record<string, number>;
  costByRegion: Record<string, number>;
  dailyCosts: { date: string; cost: number }[];
  serviceBreakdown: CostExplorerBreakdownRow[];
  /**
   * Honest data status: AVAILABLE when figures were measured, EMPTY when the account
   * returned no spend for the period, UNAVAILABLE when the query failed or the provider
   * could not be reached. Rendering should never treat the last two as zero spend.
   */
  status: DataStatus;
  /** false when the status is not AVAILABLE; kept for the existing empty-state logic. */
  available: boolean;
}

export interface Resource {
  id: string;
  type: string;
  name: string;
  status: string;
  region: string;
  monthlyCost: number | null;
  hourlyCost: number | null;
  protected: boolean;
  specs: Record<string, string>;
  tags: Record<string, string>;
}

export interface Alert {
  id: string;
  severity: string;
  title: string;
  message: string;
  acknowledged: boolean;
  timestamp: string;
}

export interface Optimization {
  id: string;
  action: string;
  resourceId?: string;
  region?: string;
  status: string;
  estimatedSavings: number;
  explanation?: string;
  score?: number;
  timestamp: string;
}

export interface MonthlyReport {
  reportMonth: string;
  reportYear: number;
  generatedDate: string;
  currency: string;
  summary: {
    totalCost: number;
    resourceCount: number;
    costByRegion: Record<string, number>;
  };
  topCostServices: { service: string; cost: number }[];
  dailySpend: { date: string; cost: number }[];
  status: DataStatus;
  available: boolean;
}

export interface CopilotReply {
  type: string;
  text: string;
  data?: unknown;
  available?: boolean;
}

// --- Endpoints -------------------------------------------------------------

export const getDashboardOverview = () =>
  apiRequest<DashboardOverview>("/dashboard/overview");

export const getAnomalies = () => apiRequest<AnomalyReport>("/anomalies");

export const getRecommendations = () =>
  apiRequest<Recommendation[]>("/recommendations");

export const getSavingsProjection = () =>
  apiRequest<SavingsProjection>("/savings");

export function getCostExplorerData(params: {
  from?: string;
  to?: string;
  service?: string;
  region?: string;
} = {}): Promise<CostExplorerData> {
  const query = new URLSearchParams();
  if (params.from) query.append("from", params.from);
  if (params.to) query.append("to", params.to);
  if (params.service) query.append("service", params.service);
  if (params.region) query.append("region", params.region);
  const suffix = query.toString() ? `?${query.toString()}` : "";
  // The path is /cost-explorer/explorer. The frontend previously requested
  // /costs/explorer, which does not exist, so this page always fell through.
  return apiRequest<CostExplorerData>(`/cost-explorer/explorer${suffix}`);
}

export const getResources = () => apiRequest<Resource[]>("/resources");

export const getResourceById = (id: string) =>
  apiRequest<Resource>(`/resources/${encodeURIComponent(id)}`);

export const getResourcesByStatus = (status: string) =>
  apiRequest<Resource[]>(`/resources/status/${encodeURIComponent(status)}`);

export const getOptimizationHistory = () =>
  apiRequest<Optimization[]>("/optimizations/history");

export const getOptimizationsByStatus = (status: string) =>
  apiRequest<Optimization[]>(
    `/optimizations/status/${encodeURIComponent(status)}`
  );

export const getAlerts = () => apiRequest<Alert[]>("/alerts");

export const getAlertsBySeverity = (severity: string) =>
  apiRequest<Alert[]>(`/alerts/severity/${encodeURIComponent(severity)}`);

export const acknowledgeAlert = (id: string) =>
  apiRequest<Alert>(`/alerts/${encodeURIComponent(id)}/acknowledge`, {
    method: "PATCH",
  });

export const getMonthlyReport = () => apiRequest<MonthlyReport>("/reports/monthly");

export const askCopilot = (message: string) =>
  apiRequest<CopilotReply>("/copilot/chat", {
    method: "POST",
    body: JSON.stringify({ message }),
  });

export const register = (username: string, password: string, company: string) =>
  apiRequest<{ username: string; authenticated: boolean; authorities: string[]; token: string }>(
    "/auth/register",
    { method: "POST", body: JSON.stringify({ username, password, company }) }
  );

export const login = (username: string, password: string) =>
  apiRequest<{ username: string; authenticated: boolean; authorities: string[]; token: string }>(
    "/auth/login",
    { method: "POST", body: JSON.stringify({ username, password }) }
  );

export const logout = () =>
  apiRequest<null>("/auth/logout", { method: "POST" });

// Tag governance, remediation and tenant management

export interface TagOffender {
  resourceId: string;
  resourceType: string;
  region: string;
  monthlyCost: number | null;
  missingKeys: string[];
}

export interface TagCoverageReport {
  requiredKeys: string[];
  resourcesAssessed: number;
  compliantResources: number;
  coveragePercentage: number;
  attributedMonthlyCost: number | null;
  uncoveredMonthlyCost: number | null;
  uncoveredShareOfSpend: number | null;
  uncostedResourceCount: number;
  violationsByService: Record<string, number>;
  uncoveredByService: Record<string, number>;
  offenders: TagOffender[];
  enforcement: string;
  available: boolean;
  reason: string | null;
}

export type ActionType =
  | "STOP_INSTANCE"
  | "START_INSTANCE"
  | "TERMINATE_INSTANCE"
  | "DELETE_VOLUME"
  | "DELETE_SNAPSHOT"
  | "RELEASE_ELASTIC_IP"
  | "APPLY_TAGS"
  | "SCALE_GROUP"
  | "RESIZE_INSTANCE";

export interface RemediationPlan {
  actionType: ActionType;
  resourceId: string;
  resourceType: string;
  region: string;
  currentState: string;
  protectedResource: boolean;
  risk: string;
  destructive: boolean;
  monthlyCost: number | null;
  estimatedMonthlySavings: number | null;
  blocked: boolean;
  blockedReason: string | null;
  dryRunOnly: boolean;
  sandboxed: boolean;
  steps: string[];
  executable: boolean;
  /**
   * Digest of the plan inputs that can change while the plan sits on a desk. Execute
   * must echo it back so the backend can refuse to apply a stale plan.
   */
  token: string;
}

export interface ActionResult {
  success: boolean;
  applied: boolean;
  dryRun: boolean;
  action: ActionType;
  resourceId: string;
  message: string;
  estimatedMonthlySavings: number | null;
  error: string | null;
  executedAt: string;
}

export interface Tenant {
  id: string;
  name: string;
  active: boolean;
  cloudProvider: string;
  mode: string;
  plan: string | null;
  autonomousMode: boolean;
  requireApprovalForChanges: boolean;
  monthlyBudgetLimit: number;
  protectedResourcesCsv: string | null;
  createdAt: string;
  lastActivityAt: string | null;
  version: number;
}

export const getTagCoverage = (keys?: string[]) => {
  const suffix =
    keys && keys.length > 0 ? `?keys=${keys.map(encodeURIComponent).join(",")}` : "";
  return apiRequest<TagCoverageReport>(`/tags/coverage${suffix}`);
};

export const planRemediation = (body: {
  actionType: ActionType;
  resourceId: string;
  parameters?: Record<string, string>;
}) =>
  apiRequest<RemediationPlan>("/remediation/plan", {
    method: "POST",
    body: JSON.stringify(body),
  });

export const executeRemediation = (body: {
  actionType: ActionType;
  resourceId: string;
  parameters?: Record<string, string>;
  dryRun?: boolean;
  /**
   * Correlation id. Resubmitting the same key for the same tenant replays the stored
   * outcome instead of mutating the account a second time.
   */
  idempotencyKey?: string;
  /** Token from a previously fetched plan; guards against executing a stale plan. */
  planToken?: string;
}) =>
  apiRequest<ActionResult>("/remediation/execute", {
    method: "POST",
    body: JSON.stringify(body),
  });

export interface RemediationActionSummary {
  type: ActionType;
  risk: string;
  destructive: boolean;
}

export interface RemediationExecutionRecord {
  type: ActionType;
  resourceId: string;
  applied: boolean;
  dryRun: boolean;
  success: boolean;
  message: string;
  executedAt: string;
}

export const listRemediationActions = () =>
  apiRequest<RemediationActionSummary[]>("/remediation/actions");

export const getRemediationHistory = () =>
  apiRequest<RemediationExecutionRecord[]>("/remediation/history");

export const approveRecommendation = (id: string) =>
  apiRequest<boolean>(`/recommendations/${encodeURIComponent(id)}/approve`, {
    method: "POST",
  });

export const rejectRecommendation = (id: string) =>
  apiRequest<boolean>(`/recommendations/${encodeURIComponent(id)}/reject`, {
    method: "POST",
  });

export const executeRecommendation = (id: string) =>
  apiRequest<boolean>(`/recommendations/${encodeURIComponent(id)}/execute`, {
    method: "POST",
  });

export const listTenants = () => apiRequest<Tenant[]>("/tenants");

export const createTenant = (tenant: Partial<Tenant>) =>
  apiRequest<Tenant>("/tenants", {
    method: "POST",
    body: JSON.stringify(tenant),
  });

export const updateTenant = (id: string, changes: Partial<Tenant>) =>
  apiRequest<Tenant>(`/tenants/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(changes),
  });

export const deactivateTenant = (id: string) =>
  apiRequest<null>(`/tenants/${encodeURIComponent(id)}`, { method: "DELETE" });

export const reactivateTenant = (id: string) =>
  apiRequest<Tenant>(`/tenants/${encodeURIComponent(id)}/reactivate`, { method: "POST" });

export { API_BASE_URL };