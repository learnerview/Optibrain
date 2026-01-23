"use client"

import DashboardLayout from "@/components/dashboard/dashboard-layout"
import PredictiveForecasting from "@/components/analytics/predictive-forecasting"

export default function ForecastingPage() {
  return (
    <DashboardLayout>
      <div>
        <h1 className="text-3xl font-bold mb-2">Predictive Forecasting</h1>
        <p className="text-muted-foreground mb-8">ML-powered cost forecasting and what-if scenario analysis.</p>
        <PredictiveForecasting />
      </div>
    </DashboardLayout>
  )
}
