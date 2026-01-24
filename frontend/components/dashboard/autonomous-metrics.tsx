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
      title: "Total Monthly Cost",
      value: data ? `${data.currency} ${data.totalMonthlyCost.toLocaleString()}` : "INR 128,450",
      change: data ? "+4.2%" : "+4.2%",
      icon: Activity,
      color: "from-blue-500 to-indigo-600",
      trend: "up",
    },
    {
      title: "Active Services",
      value: data ? data.topServices.length : "4",
      change: "Monitoring",
      icon: Activity,
      color: "from-emerald-500 to-teal-600",
      status: "optimal",
    },
    {
      title: "Cost Potential Savings",
      value: "INR 13,000",
      change: "AI-Optimized",
      icon: TrendingDown,
      color: "from-cyan-500 to-blue-600",
      trend: "down",
    },
    {
      title: "Intelligence Health",
      value: "99.9%",
      change: "Stable",
      icon: AlertTriangle,
      color: "from-purple-500 to-pink-600",
      trend: "none",
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
