"use client"

import { mockCloudData } from "@/lib/mock-data"
import { Card } from "@/components/ui/card"
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Legend, ResponsiveContainer, Tooltip } from "recharts"

interface CostTrajectoryProps {
  data?: any
}

export default function CostTrajectory({ data: propData }: CostTrajectoryProps) {
  const data = propData || mockCloudData.costTrajectory

  return (
    <Card className="glass border-border/50 p-6">
      <div className="mb-4">
        <h2 className="text-xl font-bold text-foreground">Cost Trajectory - Baseline vs Optimized vs Forecast</h2>
        <p className="text-sm text-muted-foreground mt-1">
          Multi-month projection showing cost impact of optimizations
        </p>
      </div>

      <div className="h-80 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="colorBaseline" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="hsl(0 84.2% 60.2%)" stopOpacity={0.3} />
                <stop offset="95%" stopColor="hsl(0 84.2% 60.2%)" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="colorForecast" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="hsl(240 5.9% 90%)" stopOpacity={0.3} />
                <stop offset="95%" stopColor="hsl(240 5.9% 90%)" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="colorOptimized" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="hsl(142.3 71.8% 56.2%)" stopOpacity={0.3} />
                <stop offset="95%" stopColor="hsl(142.3 71.8% 56.2%)" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(139, 92, 246, 0.1)" />
            <XAxis dataKey="date" stroke="rgba(139, 92, 246, 0.3)" />
            <YAxis stroke="rgba(139, 92, 246, 0.3)" />
            <Tooltip
              contentStyle={{
                backgroundColor: "rgba(10, 10, 25, 0.95)",
                border: "1px solid rgba(139, 92, 246, 0.3)",
                borderRadius: "8px",
              }}
            />
            <Legend />
            <Area
              type="monotone"
              dataKey="baseline"
              stroke="hsl(0 84.2% 60.2%)"
              fillOpacity={1}
              fill="url(#colorBaseline)"
              name="Baseline"
            />
            <Area
              type="monotone"
              dataKey="forecast"
              stroke="hsl(240 5.9% 90%)"
              fillOpacity={1}
              fill="url(#colorForecast)"
              name="Forecast"
            />
            <Area
              type="monotone"
              dataKey="optimized"
              stroke="hsl(142.3 71.8% 56.2%)"
              fillOpacity={1}
              fill="url(#colorOptimized)"
              name="Optimized"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </Card>
  )
}
