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
  const [overviewData, setOverviewData] = useState<any>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/signin");
      return;
    }

    const fetchOverview = async () => {
      try {
        setLoading(true);
        const data = await getCostSummary(); // This is mapped to getDashboardOverview in api.ts
        setOverviewData(data);
      } catch (err) {
        console.error("Failed to fetch dashboard data:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchOverview();
  }, [isAuthenticated, router]);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground mb-2">
            Cost Overview
          </h1>
          <p className="text-muted-foreground">
            Cloud Cost Intelligence & Optimization Dashboard
          </p>
        </div>

        {/* Loading State - Subtle Skeleton Loader preferred, but keeping it simple for now */}
        {loading ? (
          <div className="grid gap-6 animate-pulse">
            <div className="h-32 bg-slate-800 rounded-lg"></div>
            <div className="h-80 bg-slate-800 rounded-lg"></div>
          </div>
        ) : (
          <>
            {/* Key Metrics */}
            <AutonomousMetrics data={overviewData} />

            {/* Cost Trajectory Chart */}
            <CostTrajectory data={overviewData?.trend || []} />

            <div className="grid lg:grid-cols-3 gap-6">
              {/* Top Services (Renamed from TodayImpact for clarity in this view) */}
              <TodayImpact data={overviewData} />

              {/* Recent Decisions / Context */}
              <div className="lg:col-span-2">
                <RecentDecisions data={[]} />
              </div>
            </div>
          </>
        )}
      </div>
    </DashboardLayout>
  );
}
