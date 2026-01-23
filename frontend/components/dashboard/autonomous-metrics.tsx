"use client"

import { mockCloudData } from "@/lib/mock-data"
import { Card } from "@/components/ui/card"
import { TrendingUp, TrendingDown, Activity, AlertTriangle, LucideIcon } from "lucide-react"

interface AutonomousMetricsProps {
  data?: any
}

export default function AutonomousMetrics({ data }: AutonomousMetricsProps) {
  const metrics = [
    {
      title: "Total Savings",
      value: data ? `$${(data.totalSaved / 1000).toFixed(1)}K` : `$${(mockCloudData.totalSavingsYTD / 1000).toFixed(1)}K`,
      change: data ? `+$${(data.potentialMonthlySavings / 100).toFixed(1)}K` : "+27%",
      icon: TrendingUp,
      color: "from-emerald-500 to-green-600",
    },
    {
      title: "Cost Reduction",
      value: data ? `${data.savingsPercentage}%` : "46.2%",
      change: "-7.8%",
      icon: TrendingDown,
      color: "from-cyan-500 to-blue-600",
      trend: "down",
    },
    {
      title: "Engine Health",
      value: data ? "OPTIMAL" : "OPTIMAL",
      change: "+12.7%",
      icon: Activity,
      color: "from-emerald-500 to-teal-600",
      status: "optimal",
    },
    {
      title: "AI Risk Score",
      value: data ? "0.08" : "0.11",
      change: "Lower is better",
      icon: AlertTriangle,
      color: "from-purple-500 to-pink-600",
      trend: "down",
    },
  ]

  return (
    <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
      {metrics.map((metric, idx) => {
        const Icon = metric.icon
        return (
          <Card key={idx} className="glass border-border/50 p-6 hover:border-primary/50 transition-all duration-300">
            <div className="flex items-start justify-between mb-4">
              <div
                className={`w-12 h-12 rounded-lg bg-gradient-to-br ${metric.color} p-2.5 flex items-center justify-center`}
              >
                <Icon className="w-6 h-6 text-white" />
              </div>
              <span
                className={`text-xs font-semibold px-2 py-1 rounded-full ${metric.trend === "down" || metric.status === "optimal"
                    ? "bg-emerald-500/20 text-emerald-400"
                    : "bg-amber-500/20 text-amber-400"
                  }`}
              >
                {metric.change}
              </span>
            </div>
            <h3 className="text-sm font-medium text-muted-foreground mb-1">{metric.title}</h3>
            <p className="text-2xl font-bold text-foreground">{metric.value}</p>
          </Card>
        )
      })}
    </div>
  )
}
