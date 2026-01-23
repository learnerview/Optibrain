"use client"

import { mockCloudData } from "@/lib/mock-data"
import { AlertTriangle, Zap, Target, DollarSign, TrendingDown } from "lucide-react"
import { Card } from "@/components/ui/card"

export default function OverviewMetrics() {
  const metrics = [
    {
      label: "Current Monthly Spend",
      value: `$${mockCloudData.monthlySpend.toLocaleString("en-US", { maximumFractionDigits: 0 })}`,
      icon: DollarSign,
      color: "from-blue-500 to-cyan-500",
      trend: mockCloudData.providers.AWS.trend,
      trendValue: "+2.3%",
      subtext: "Real-time tracked spend",
    },
    {
      label: "Forecasted Annual",
      value: `$${mockCloudData.forecastedAnnual.toLocaleString("en-US", { maximumFractionDigits: 0 })}`,
      icon: Target,
      color: "from-purple-500 to-pink-500",
      trend: "up",
      trendValue: "+8.2%",
      subtext: "Based on 12-month projection",
    },
    {
      label: "Optimization Potential",
      value: `$${mockCloudData.optimizationPotential.toLocaleString("en-US", { maximumFractionDigits: 0 })}`,
      icon: Zap,
      color: "from-green-500 to-emerald-500",
      trend: "down",
      trendValue: "-15%",
      subtext: "Available this quarter",
    },
    {
      label: "Savings Achieved YTD",
      value: `$${mockCloudData.totalSavingsYTD.toLocaleString("en-US", { maximumFractionDigits: 0 })}`,
      icon: TrendingDown,
      color: "from-orange-500 to-red-500",
      trend: "up",
      trendValue: "+22%",
      subtext: "Year-to-date total",
    },
    {
      label: "Cost Per Employee",
      value: `$${mockCloudData.costPerEmployee.toLocaleString("en-US", { maximumFractionDigits: 2 })}`,
      icon: AlertTriangle,
      color: "from-indigo-500 to-blue-500",
      trend: "stable",
      trendValue: "±0%",
      subtext: "Monthly per headcount",
    },
    {
      label: "Active Anomalies",
      value: mockCloudData.anomalies.length,
      icon: AlertTriangle,
      color: "from-red-500 to-orange-500",
      trend: "down",
      trendValue: "-2",
      subtext: "Requiring attention",
    },
  ]

  return (
    <div>
      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        {metrics.map((metric, index) => {
          const Icon = metric.icon
          return (
            <Card key={index} className="glass p-6 border-primary/20 hover:border-primary/40 transition-all group">
              <div className="flex items-start justify-between mb-4">
                <div
                  className={`w-12 h-12 rounded-lg bg-gradient-to-br ${metric.color} flex items-center justify-center shadow-lg`}
                >
                  <Icon className="w-6 h-6 text-white" />
                </div>
                {/* Trend Badge */}
                <div
                  className={`text-xs font-semibold px-2 py-1 rounded-lg ${
                    metric.trend === "up"
                      ? "bg-red-500/20 text-red-400"
                      : metric.trend === "down"
                        ? "bg-green-500/20 text-green-400"
                        : "bg-muted text-muted-foreground"
                  }`}
                >
                  {metric.trendValue}
                </div>
              </div>
              <p className="text-xs text-muted-foreground uppercase tracking-wide mb-1">{metric.label}</p>
              <h3 className="text-3xl font-bold mb-2">{metric.value}</h3>
              <p className="text-xs text-muted-foreground">{metric.subtext}</p>
            </Card>
          )
        })}
      </div>

      {/* Multi-Cloud Provider Breakdown */}
      <Card className="glass p-8 border-primary/20">
        <h3 className="text-xl font-bold mb-6">Multi-Cloud Spend Distribution</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {Object.entries(mockCloudData.providers).map(([provider, data]) => (
            <div key={provider} className="space-y-4">
              <div className="flex items-center justify-between mb-2">
                <h4 className="font-semibold text-foreground">{provider}</h4>
                <span className="text-xs bg-primary/20 text-primary px-2 py-1 rounded-lg">{data.percentage}%</span>
              </div>
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-muted-foreground">Monthly Spend</span>
                  <span className="font-bold">${data.spend.toLocaleString("en-US", { maximumFractionDigits: 0 })}</span>
                </div>
                <div className="w-full h-2 bg-secondary rounded-full overflow-hidden">
                  <div
                    className={`h-full bg-gradient-to-r ${data.percentage > 40 ? "from-red-500 to-orange-500" : data.percentage > 25 ? "from-yellow-500 to-orange-500" : "from-green-500 to-emerald-500"}`}
                    style={{ width: `${data.percentage}%` }}
                  ></div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}
