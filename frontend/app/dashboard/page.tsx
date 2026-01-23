"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/store";
import {
  getCostSummary,
  getCostTrend,
  getServiceBreakdown,
  getForecast,
  getAnomalies,
} from "@/lib/api";
import DashboardLayout from "@/components/dashboard/dashboard-layout";
import AutonomousMetrics from "@/components/dashboard/autonomous-metrics";
import CostTrajectory from "@/components/dashboard/cost-trajectory";
import TodayImpact from "@/components/dashboard/today-impact";
import RecentDecisions from "@/components/dashboard/recent-decisions";
import { Card } from "@/components/ui/card";

export default function DashboardPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [costData, setCostData] = useState<any>(null);
  const [trendData, setTrendData] = useState<any>(null);
  const [breakdownData, setBreakdownData] = useState<any>(null);
  const [forecastData, setForecastData] = useState<any>(null);
  const [anomalyData, setAnomalyData] = useState<any>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/signin");
      return;
    }

    // Phase 4 & 5: Fetch real API data
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        setError(null);

        // Fetch all metrics in parallel (Phase 4 + Phase 5)
        const [
          costSummary,
          trendResponse,
          serviceBreakdown,
          forecast,
          anomalies,
        ] = await Promise.all([
          getCostSummary(),
          getCostTrend(30, "daily"),
          getServiceBreakdown(),
          getForecast(), // Phase 5: Forecast
          getAnomalies(), // Phase 5: Anomalies
        ]);

        setCostData(costSummary);
        setTrendData(trendResponse);
        setBreakdownData(serviceBreakdown);
        setForecastData(forecast); // Phase 5
        setAnomalyData(anomalies); // Phase 5
      } catch (err) {
        console.error("Failed to fetch dashboard data:", err);
        setError(
          err instanceof Error
            ? err.message
            : "Failed to load dashboard data. Please try again.",
        );
        // Continue with rendering - components will use fallback mock data
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, [isAuthenticated, router]);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground mb-2">
            Autonomous Dashboard
          </h1>
          <p className="text-muted-foreground">
            Real-time overview of your cloud optimization engine
          </p>
        </div>

        {/* Error Display */}
        {error && (
          <Card className="border-red-500/50 bg-red-500/10 p-4">
            <p className="text-sm text-red-700 dark:text-red-400">⚠️ {error}</p>
            <p className="text-xs text-red-600/70 dark:text-red-400/70 mt-1">
              Displaying mock data. Real data will load when the API is
              available.
            </p>
          </Card>
        )}

        {/* Loading State */}
        {loading && (
          <Card className="border-blue-500/50 bg-blue-500/10 p-4">
            <p className="text-sm text-blue-700 dark:text-blue-400">
              ⏳ Loading real cost data & predictions...
            </p>
          </Card>
        )}

        {/* Key Metrics */}
        <AutonomousMetrics data={costData} />

        {/* Cost Trajectory Chart */}
        <CostTrajectory data={trendData} />

        <div className="grid lg:grid-cols-3 gap-6">
          {/* Today's Impact */}
          <TodayImpact data={costData} />

          {/* Recent Decisions */}
          <div className="lg:col-span-2">
            <RecentDecisions data={anomalyData || []} />
          </div>
        </div>

        {/* Debug Info - Phase 4 & 5 Development */}
        {process.env.NODE_ENV === "development" && (
          <Card className="p-4 border-dashed border-gray-300 dark:border-gray-700">
            <p className="text-xs text-gray-600 dark:text-gray-400 mb-2 font-mono">
              Phase 4 & 5 Debug - API Data:
            </p>
            <div className="space-y-2">
              <details className="text-xs text-gray-600 dark:text-gray-400">
                <summary className="cursor-pointer font-mono hover:text-gray-800 dark:hover:text-gray-200">
                  Cost Summary
                </summary>
                <pre className="bg-gray-100 dark:bg-gray-900 p-2 rounded mt-1 overflow-auto text-xs">
                  {JSON.stringify(costData, null, 2)}
                </pre>
              </details>

              {/* Phase 5: Forecast Debug */}
              <details className="text-xs text-gray-600 dark:text-gray-400">
                <summary className="cursor-pointer font-mono hover:text-gray-800 dark:hover:text-gray-200">
                  Forecast (Phase 5)
                </summary>
                <pre className="bg-gray-100 dark:bg-gray-900 p-2 rounded mt-1 overflow-auto text-xs">
                  {JSON.stringify(forecastData, null, 2)}
                </pre>
              </details>

              {/* Phase 5: Anomalies Debug */}
              <details className="text-xs text-gray-600 dark:text-gray-400">
                <summary className="cursor-pointer font-mono hover:text-gray-800 dark:hover:text-gray-200">
                  Anomalies (Phase 5)
                </summary>
                <pre className="bg-gray-100 dark:bg-gray-900 p-2 rounded mt-1 overflow-auto text-xs">
                  {JSON.stringify(anomalyData, null, 2)}
                </pre>
              </details>
            </div>
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
}
