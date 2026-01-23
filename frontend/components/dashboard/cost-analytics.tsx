"use client"

import { mockCloudData } from "@/lib/mock-data"
import { Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts"

export default function CostAnalytics() {
  const COLORS = ["#22D3EE", "#A855F7", "#EF4444"]

  const providerData = Object.entries(mockCloudData.providers).map(([name, data]: any) => ({
    name,
    value: data.spend,
    color: COLORS[Object.keys(mockCloudData.providers).indexOf(name)],
  }))

  return (
    <div className="glass p-6 rounded-xl border border-border/50">
      <h3 className="text-lg font-semibold mb-6">Provider Breakdown</h3>

      <div className="mb-8 h-64">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={providerData}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              outerRadius={80}
              fill="#8884d8"
              dataKey="value"
            >
              {providerData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => `$${value}`} />
          </PieChart>
        </ResponsiveContainer>
      </div>

      <div className="space-y-3">
        {providerData.map((provider, index) => (
          <div key={index} className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: provider.color }}></div>
              <span className="text-sm text-muted-foreground">{provider.name}</span>
            </div>
            <span className="font-semibold">${provider.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
