"use client"

import { mockCloudData } from "@/lib/mock-data"
import { Card } from "@/components/ui/card"
import { Zap, Timer, Database, HardDrive } from "lucide-react"

interface TodayImpactProps {
  data?: any
}

export default function TodayImpact({ data }: TodayImpactProps) {
  const impact = data || mockCloudData.todayImpact

  const items = [
    { icon: Zap, label: impact.decisions_unit, value: impact.decisions_made, color: "from-yellow-500 to-orange-600" },
    { icon: Timer, label: impact.latency_unit, value: impact.avg_latency, color: "from-cyan-500 to-blue-600" },
    {
      icon: Database,
      label: impact.resources_unit,
      value: impact.resources_managed,
      color: "from-purple-500 to-pink-600",
    },
    { icon: HardDrive, label: impact.data_unit, value: impact.data_processed, color: "from-emerald-500 to-green-600" },
  ]

  return (
    <Card className="glass border-border/50 p-6">
      <h2 className="text-lg font-bold text-foreground mb-4">Today's Impact</h2>
      <div className="space-y-4">
        {items.map((item, idx) => {
          const Icon = item.icon
          return (
            <div key={idx} className="flex items-center gap-3 p-3 rounded-lg bg-secondary/20 border border-border/30">
              <div
                className={`w-10 h-10 rounded-lg bg-gradient-to-br ${item.color} p-2 flex items-center justify-center flex-shrink-0`}
              >
                <Icon className="w-5 h-5 text-white" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-muted-foreground">{item.label}</p>
                <p className="text-lg font-bold text-foreground">{item.value}</p>
              </div>
            </div>
          )
        })}
      </div>
    </Card>
  )
}
