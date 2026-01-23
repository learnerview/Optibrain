"use client"

import { useState } from "react"
import { mockCloudData } from "@/lib/mock-data"
import { AlertTriangle, TrendingUp, CheckCircle2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts"

const anomalyTimeline = [
  { date: "Jan 10", value: 395 },
  { date: "Jan 11", value: 420 },
  { date: "Jan 12", value: 445 },
  { date: "Jan 13", value: 480 },
  { date: "Jan 14", value: 520 },
  { date: "Jan 15", value: 535 },
  { date: "Jan 16", value: 630 }, // Spike
]

export default function AnomalyDetection() {
  const [selectedAnomaly, setSelectedAnomaly] = useState(0)

  const selected = mockCloudData.anomalies[selectedAnomaly]

  return (
    <div className="grid lg:grid-cols-3 gap-6">
      {/* Timeline chart */}
      <div className="lg:col-span-2 glass p-6 rounded-xl border border-border/50">
        <h3 className="text-lg font-semibold mb-6">Anomaly Timeline</h3>

        <div className="h-80 mb-8">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={anomalyTimeline}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
              <XAxis dataKey="date" stroke="rgba(255,255,255,0.5)" />
              <YAxis stroke="rgba(255,255,255,0.5)" />
              <Tooltip contentStyle={{ backgroundColor: "rgba(15, 23, 42, 0.9)" }} formatter={(value) => `$${value}`} />
              <Line
                type="monotone"
                dataKey="value"
                stroke="#22D3EE"
                dot={{ fill: "#22D3EE", r: 4 }}
                activeDot={{ r: 6 }}
              />
              {/* Highlight anomaly point */}
              <Line
                type="monotone"
                dataKey="value"
                stroke="transparent"
                dot={({ cx, cy, payload }) => {
                  if (payload.date === "Jan 16") {
                    return (
                      <g key={`anomaly-${payload.date}`}>
                        <circle cx={cx} cy={cy} r={8} fill="none" stroke="#EF4444" strokeWidth={2} />
                        <circle cx={cx} cy={cy} r={4} fill="#EF4444" />
                      </g>
                    )
                  }
                  return null
                }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="p-4 rounded-lg bg-orange-500/10 border border-orange-500/30 flex gap-3">
          <AlertTriangle className="w-5 h-5 text-orange-500 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold text-sm">Anomaly Detected</p>
            <p className="text-xs text-muted-foreground">
              Unusual 18.7% cost spike on Jan 16, up from typical daily average of $465
            </p>
          </div>
        </div>
      </div>

      {/* Anomalies list */}
      <div className="glass p-6 rounded-xl border border-border/50 flex flex-col">
        <h3 className="text-lg font-semibold mb-4">Detected Anomalies</h3>

        <div className="space-y-3 flex-1">
          {mockCloudData.anomalies.map((anomaly, index) => {
            const Icon =
              anomaly.severity === "high" ? AlertTriangle : anomaly.severity === "medium" ? TrendingUp : CheckCircle2
            return (
              <button
                key={anomaly.id}
                onClick={() => setSelectedAnomaly(index)}
                className={`w-full text-left p-3 rounded-lg transition ${
                  selectedAnomaly === index
                    ? "bg-primary/20 border border-primary/50"
                    : "bg-secondary/30 border border-border/50 hover:border-primary/50"
                }`}
              >
                <div className="flex items-start gap-2">
                  <Icon
                    className={`w-4 h-4 flex-shrink-0 mt-0.5 ${
                      anomaly.severity === "high"
                        ? "text-destructive"
                        : anomaly.severity === "medium"
                          ? "text-orange-500"
                          : "text-green-500"
                    }`}
                  />
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-sm leading-tight">{anomaly.title}</p>
                    <p className="text-xs text-muted-foreground mt-1">{anomaly.timestamp}</p>
                  </div>
                </div>
              </button>
            )
          })}
        </div>

        <div className="mt-6 pt-6 border-t border-border/50">
          <h4 className="font-semibold text-sm mb-3">Recommended Action</h4>
          <p className="text-xs text-muted-foreground mb-4">{selected.details}</p>
          <Button className="w-full" size="sm">
            Investigate
          </Button>
        </div>
      </div>
    </div>
  )
}
