"use client"

import { mockCloudData } from "@/lib/mock-data"
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts"

export default function ServiceBreakdown() {
  return (
    <div className="glass p-6 rounded-xl border border-border/50">
      <h3 className="text-lg font-semibold mb-6">Daily Cost Trend</h3>

      <div className="h-64 mb-8">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={mockCloudData.dailySpend}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
            <XAxis dataKey="date" stroke="rgba(255,255,255,0.5)" />
            <YAxis stroke="rgba(255,255,255,0.5)" />
            <Tooltip
              contentStyle={{ backgroundColor: "rgba(15, 23, 42, 0.9)", border: "1px solid rgba(139, 92, 246, 0.2)" }}
            />
            <Legend />
            <Bar dataKey="AWS" fill="#22D3EE" radius={[8, 8, 0, 0]} />
            <Bar dataKey="Azure" fill="#A855F7" radius={[8, 8, 0, 0]} />
            <Bar dataKey="GCP" fill="#EF4444" radius={[8, 8, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="space-y-2">
        <h4 className="font-semibold text-sm mb-3">Top Services</h4>
        {mockCloudData.services.map((service, index) => (
          <div
            key={index}
            className="flex items-center justify-between p-3 rounded-lg bg-secondary/30 hover:bg-secondary/50 transition"
          >
            <div>
              <p className="text-sm font-medium">{service.name}</p>
              <p className="text-xs text-muted-foreground">{service.region}</p>
            </div>
            <div className="text-right">
              <p className="font-semibold">${service.cost}</p>
              <p
                className={`text-xs font-medium ${
                  service.risk === "high"
                    ? "text-destructive"
                    : service.risk === "medium"
                      ? "text-orange-500"
                      : "text-green-500"
                }`}
              >
                {service.risk.charAt(0).toUpperCase() + service.risk.slice(1)} Risk
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
