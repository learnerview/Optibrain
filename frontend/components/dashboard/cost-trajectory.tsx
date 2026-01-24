"use client"

import { mockCloudData } from "@/lib/mock-data"
import { Card } from "@/components/ui/card"
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Legend, ResponsiveContainer, Tooltip } from "recharts"

interface CostTrajectoryProps {
  data?: any
}

export default function CostTrajectory({ data: propData }: CostTrajectoryProps) {
  const chartData = propData && propData.length > 0 ? propData : [
    { month: "Aug", cost: 94500 },
    { month: "Sep", cost: 101200 },
    { month: "Oct", cost: 110300 },
    { month: "Nov", cost: 118900 },
    { month: "Dec", cost: 125600 },
    { month: "Jan", cost: 128450 }
  ];

  return (
    <Card className="glass border-border/50 p-6">
      <div className="mb-4">
        <h2 className="text-xl font-bold text-foreground">Monthly Cost Trend</h2>
        <p className="text-sm text-muted-foreground mt-1">
          Historical view of cloud expenditures over the last 6 months
        </p>
      </div>

      <div className="h-80 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="colorCost" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="hsl(217.2 91.2% 59.8%)" stopOpacity={0.3} />
                <stop offset="100%" stopColor="hsl(217.2 91.2% 59.8%)" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
            <XAxis dataKey="month" stroke="rgba(255, 255, 255, 0.3)" />
            <YAxis stroke="rgba(255, 255, 255, 0.3)" tickFormatter={(value) => `₹${(value / 1000).toFixed(0)}K`} />
            <Tooltip
              contentStyle={{
                backgroundColor: "rgba(15, 23, 42, 0.9)",
                border: "1px solid rgba(51, 65, 85, 0.5)",
                borderRadius: "8px",
              }}
              formatter={(value: any) => [`₹${value.toLocaleString()}`, "Monthly Cost"]}
            />
            <Area
              type="monotone"
              dataKey="cost"
              stroke="hsl(217.2 91.2% 59.8%)"
              strokeWidth={3}
              fillOpacity={1}
              fill="url(#colorCost)"
              name="Monthly Cost"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </Card>
  )
}
