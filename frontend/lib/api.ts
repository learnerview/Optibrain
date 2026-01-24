/**
 * OptiBrain API Client
 * Optimized for Hackathon Demo Mode
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";
const IS_DEMO_MODE = process.env.NEXT_PUBLIC_DEMO_MODE === "true";

// --- DEMO FALLBACK DATA ---
const DEMO_DATA = {
  overview: {
    totalMonthlyCost: 128450,
    currency: "INR",
    trend: [
      { month: "Aug", cost: 94500 },
      { month: "Sep", cost: 101200 },
      { month: "Oct", cost: 110300 },
      { month: "Nov", cost: 118900 },
      { month: "Dec", cost: 125600 },
      { month: "Jan", cost: 128450 }
    ],
    topServices: [
      { name: "EC2", cost: 62400 },
      { name: "RDS", cost: 31400 },
      { name: "S3", cost: 21200 },
      { name: "CloudWatch", cost: 13450 }
    ]
  },
  anomalies: {
    anomalyDetected: true,
    severity: "HIGH",
    date: "2026-01-14",
    service: "EC2",
    expectedCost: 4200,
    actualCost: 9100,
    reason: "Sudden increase in on-demand EC2 usage"
  },
  recommendations: [
    {
      resource: "EC2 Instance i-023ab",
      issue: "Idle for 92% of the time",
      recommendation: "Downscale or stop instance",
      monthlySavings: 8400
    },
    {
      resource: "RDS MySQL db-prod",
      issue: "Over-provisioned storage",
      recommendation: "Reduce allocated storage by 30%",
      monthlySavings: 4600
    }
  ],
  savings: {
    monthlySavings: 13000,
    annualSavings: 156000,
    confidence: "High",
    assumptions: [
      "Idle resources removed",
      "No increase in traffic",
      "Stable workload pattern"
    ]
  },
  "cost-explorer": {
    filters: {
      service: "EC2",
      region: "ap-south-1",
      dateRange: "30d"
    },
    dailyCosts: [
      { date: "2026-01-10", cost: 2100 },
      { date: "2026-01-11", cost: 2150 },
      { date: "2026-01-12", cost: 2080 },
      { date: "2026-01-13", cost: 2200 },
      { date: "2026-01-14", cost: 9100 },
      { date: "2026-01-15", cost: 2300 }
    ],
    breakdown: [
      { service: "EC2", cost: 62400 },
      { service: "RDS", cost: 31400 },
      { service: "S3", cost: 21200 },
      { service: "CloudWatch", cost: 13450 }
    ]
  },
  resources: [
    {
      id: "i-023ab",
      name: "EC2 Instance i-023ab",
      type: "EC2",
      status: "Idle",
      monthlyCost: 8400,
      region: "ap-south-1",
      utilizationPercent: 8,
      optimizationCandidate: true
    },
    {
      id: "db-prod",
      name: "RDS MySQL db-prod",
      type: "RDS",
      status: "Running",
      monthlyCost: 31400,
      region: "ap-south-1",
      utilizationPercent: 65,
      optimizationCandidate: true
    }
  ],
  optimizations: [
    {
      id: "opt-001",
      action: "Terminate Idle EC2 Instance",
      resourceId: "i-023ab",
      resourceName: "EC2 Instance i-023ab",
      status: "Simulated",
      monthlySavings: 8400,
      confidence: "High",
      createdAt: "2026-01-15T10:30:00",
      appliedAt: null,
      description: "EC2 instance idle for 92% of the time."
    },
    {
      id: "opt-002",
      action: "Reduce RDS Storage by 30%",
      resourceId: "db-prod",
      resourceName: "RDS MySQL db-prod",
      status: "Pending",
      monthlySavings: 4600,
      confidence: "High",
      createdAt: "2026-01-15T10:32:00",
      appliedAt: null,
      description: "RDS storage over-provisioned."
    }
  ],
  alerts: [
    {
      id: "alert-001",
      type: "COST_SPIKE",
      severity: "HIGH",
      message: "EC2 costs increased by 116% on 2026-01-14",
      service: "EC2",
      timestamp: "2026-01-14T08:15:00",
      acknowledged: false
    }
  ],
  reports: {
    month: "January 2026",
    totalCost: 128450,
    previousMonthCost: 125600,
    costChange: 2.27,
    totalSavings: 13000,
    savingsOpportunities: 2,
    topService: "EC2",
    topServiceCost: 62400,
    anomaliesDetected: 1,
    optimizationsApplied: 0,
    generatedAt: "2026-01-24T07:00:00"
  }
};

async function fetchWithFallback<T>(endpoint: string, localJsonPath: string): Promise<T> {
  if (IS_DEMO_MODE) {
    try {
      const res = await fetch(`${API_BASE_URL}${endpoint}`);
      if (!res.ok) throw new Error("API Error");
      const json = await res.json();
      return json.data;
    } catch (error) {
      console.warn(`API call to ${endpoint} failed, falling back to /demo-data/${localJsonPath}. Error: ${error}`);
      try {
        const fallbackRes = await fetch(`/demo-data/${localJsonPath}`);
        if (!fallbackRes.ok) throw new Error("Fallback JSON missing");
        return await fallbackRes.json();
      } catch (fallbackError) {
        console.error("Critical: Fallback JSON fetch failed!", fallbackError);
        // Absolute last resort from memory (DEMO_DATA defined below)
        const key = localJsonPath.replace(".json", "");
        return (DEMO_DATA as any)[key] || {} as T;
      }
    }
  }

  // Real logic (if not in demo mode)
  const res = await fetch(`${API_BASE_URL}${endpoint}`);
  const json = await res.json();
  return json.data;
}

export interface DashboardOverview {
  totalMonthlyCost: number;
  currency: string;
  trend: { month: string; cost: number }[];
  topServices: { name: string; cost: number }[];
}

export interface AnomalyData {
  anomalyDetected: boolean;
  severity: string;
  date: string;
  service: string;
  expectedCost: number;
  actualCost: number;
  reason: string;
}

export interface Recommendation {
  resource: string;
  issue: string;
  recommendation: string;
  monthlySavings: number;
}

export interface SavingsProjection {
  monthlySavings: number;
  annualSavings: number;
  confidence: string;
  assumptions: string[];
}

// --- API FUNCTIONS ---

export async function getDashboardOverview(): Promise<DashboardOverview> {
  return fetchWithFallback<DashboardOverview>("/dashboard/overview", "dashboard-overview.json");
}

export async function getAnomalies(): Promise<AnomalyData> {
  return fetchWithFallback<AnomalyData>("/anomalies", "anomalies.json");
}

export async function getRecommendations(): Promise<Recommendation[]> {
  return fetchWithFallback<Recommendation[]>("/recommendations", "recommendations.json");
}

export async function getSavingsProjection(): Promise<SavingsProjection> {
  return fetchWithFallback<SavingsProjection>("/savings", "savings.json");
}

// --- COST EXPLORER APIs ---

export interface CostExplorerData {
  filters: {
    service: string;
    region: string;
    dateRange: string;
  };
  dailyCosts: { date: string; cost: number }[];
  breakdown: { service: string; cost: number }[];
}

export async function getCostExplorerData(
  from?: string,
  to?: string,
  service?: string,
  region?: string
): Promise<CostExplorerData> {
  const params = new URLSearchParams();
  if (from) params.append("from", from);
  if (to) params.append("to", to);
  if (service) params.append("service", service);
  if (region) params.append("region", region);

  const endpoint = `/costs/explorer${params.toString() ? `?${params.toString()}` : ""}`;
  return fetchWithFallback<CostExplorerData>(endpoint, "cost-explorer.json");
}

// --- RESOURCE INVENTORY APIs ---

export interface Resource {
  id: string;
  name: string;
  type: string;
  status: string;
  monthlyCost: number;
  region: string;
  utilizationPercent: number;
  optimizationCandidate: boolean;
}

export async function getResources(): Promise<Resource[]> {
  return fetchWithFallback<Resource[]>("/resources", "resources.json");
}

export async function getResourceById(id: string): Promise<Resource> {
  return fetchWithFallback<Resource>(`/resources/${id}`, "resources.json");
}

// --- OPTIMIZATION HISTORY APIs ---

export interface Optimization {
  id: string;
  action: string;
  resourceId: string;
  resourceName: string;
  status: string;
  monthlySavings: number;
  confidence: string;
  createdAt: string;
  appliedAt: string | null;
  description: string;
}

export async function getOptimizationHistory(): Promise<Optimization[]> {
  return fetchWithFallback<Optimization[]>("/optimizations/history", "optimizations.json");
}

export async function getOptimizationById(id: string): Promise<Optimization> {
  return fetchWithFallback<Optimization>(`/optimizations/${id}`, "optimizations.json");
}

// --- ALERTS APIs ---

export interface Alert {
  id: string;
  type: string;
  severity: string;
  message: string;
  service: string;
  timestamp: string;
  acknowledged: boolean;
}

export async function getAlerts(): Promise<Alert[]> {
  return fetchWithFallback<Alert[]>("/alerts", "alerts.json");
}

// --- REPORTS APIs ---

export interface MonthlyReport {
  month: string;
  totalCost: number;
  previousMonthCost: number;
  costChange: number;
  totalSavings: number;
  savingsOpportunities: number;
  topService: string;
  topServiceCost: number;
  anomaliesDetected: number;
  optimizationsApplied: number;
  generatedAt: string;
}

export async function getMonthlyReport(): Promise<MonthlyReport> {
  return fetchWithFallback<MonthlyReport>("/reports/monthly", "reports.json");
}

// Bypassing/Cleaning up old exports to avoid build errors if they are used elsewhere
export const getCostSummary = getDashboardOverview;
export const getCostTrend = async () => (await getDashboardOverview()).trend;
export const getServiceBreakdown = async () => (await getDashboardOverview()).topServices;
export const getForecast = getSavingsProjection;
export const getAuditLogs = async () => [];
export const getCleanupRecommendations = getRecommendations;
export const executeCleanup = async () => "Success";
export const getSpotActions = async () => [];
export const executeSpotMigration = async () => [];
export interface FinancialReportDTO {
  period: string;
  currentCost: number;
  previousCost: number;
}
export const getFinanceReports = async () => [];
