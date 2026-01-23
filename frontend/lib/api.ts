/**
 * API Client for Phase 4 & Refined Multi-Tenant Architecture
 * Handles communication with backend using refined DTOs and header-based auth
 */

const API_BASE_URL = "http://localhost:8080/api";
const DEFAULT_HEADERS = {
  "Content-Type": "application/json",
  "X-TENANT": "demo-tenant",
  "X-USER": "demo-user"
};

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface AuditLogDTO {
  action: string;
  resourceId: string;
  status: string;
  explanation: string;
  savings: number;
  score: number;
  createdAt: string;
}

export interface FinancialReportDTO {
  period: string;
  currentCost: number;
  previousCost: number;
  forecastedCost: number;
  potentialSavings: number;
  topSpenders: any[];
  source: string;
  createdAt: string;
}

export interface OrphanedResourceDTO {
  resourceId: string;
  resourceType: string;
  region: string;
  estimatedMonthlyCost: number;
  resolved: boolean;
  createdAt: string;
}

export interface SpotActionDTO {
  type: string;
  resourceId: string;
  status: string;
  predictedSavings: number;
  createdAt: string;
}

// Analytics & Dashboard
export async function getCostSummary(): Promise<any> {
  const res = await fetch(`${API_BASE_URL}/v1/metrics/cost-summary`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data;
}

export async function getCostTrend(daysBack: number = 30, granularity: string = "daily"): Promise<any> {
  const res = await fetch(`${API_BASE_URL}/v1/metrics/cost-trend?daysBack=${daysBack}&granularity=${granularity}`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data;
}

export async function getServiceBreakdown(): Promise<any> {
  const res = await fetch(`${API_BASE_URL}/v1/metrics/service-breakdown`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data;
}

export async function getForecast(): Promise<any> {
  const tenantId = DEFAULT_HEADERS["X-TENANT"];
  const res = await fetch(`${API_BASE_URL}/dashboard/predictions/${tenantId}`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data;
}

export async function getAnomalies(): Promise<any> {
  const tenantId = DEFAULT_HEADERS["X-TENANT"];
  const res = await fetch(`${API_BASE_URL}/dashboard/predictions/${tenantId}`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data?.anomalies || [];
}

export async function getFinanceReports(): Promise<FinancialReportDTO[]> {
  const res = await fetch(`${API_BASE_URL}/analytics/reports`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data;
}

// Compliance / Audit
export async function getAuditLogs(): Promise<AuditLogDTO[]> {
  const res = await fetch(`${API_BASE_URL}/audit/logs`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data;
}

// Cleanup / Recommendations
export async function getCleanupRecommendations(): Promise<OrphanedResourceDTO[]> {
  const res = await fetch(`${API_BASE_URL}/cleanup/recommendations`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data;
}

export async function executeCleanup(resourceId: string, resourceType: string): Promise<string> {
  const res = await fetch(`${API_BASE_URL}/cleanup/execute/${resourceId}?resourceType=${resourceType}`, {
    method: 'POST',
    headers: DEFAULT_HEADERS
  });
  const json = await res.json();
  return json.data;
}

// Spot Scaling
export async function getSpotActions(): Promise<SpotActionDTO[]> {
  const res = await fetch(`${API_BASE_URL}/autoscaling/spot/actions`, { headers: DEFAULT_HEADERS });
  const json = await res.json();
  return json.data;
}

export async function executeSpotMigration(actions: SpotActionDTO[]): Promise<SpotActionDTO[]> {
  const res = await fetch(`${API_BASE_URL}/autoscaling/spot/migrate`, {
    method: 'POST',
    headers: DEFAULT_HEADERS,
    body: JSON.stringify(actions)
  });
  const json = await res.json();
  return json.data;
}
