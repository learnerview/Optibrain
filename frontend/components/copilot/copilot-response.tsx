"use client"

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts"
import { AlertCircle, CheckCircle2 } from "lucide-react"

interface CopilotResponseProps {
  response: {
    type: string
    data?: any
  }
}

export default function CopilotResponse({ response }: CopilotResponseProps) {
  if (response.type === "text") {
    return <p className="text-sm whitespace-pre-wrap">{response.data.text}</p>
  }

  if (response.type === "chart") {
    return (
      <div>
        <p className="text-sm font-medium mb-4">{response.data.text}</p>
        <div className="h-64 mb-4">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={response.data.items} layout="vertical" margin={{ left: 100, right: 20, top: 5, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
              <XAxis type="number" stroke="rgba(255,255,255,0.5)" />
              <YAxis dataKey="name" type="category" stroke="rgba(255,255,255,0.5)" width={100} />
              <Tooltip
                contentStyle={{
                  backgroundColor: "rgba(15, 23, 42, 0.9)",
                  border: "1px solid rgba(139, 92, 246, 0.2)",
                }}
                formatter={(value) => `$${value}`}
              />
              <Bar dataKey="cost" fill="#22D3EE" radius={[0, 8, 8, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    )
  }

  if (response.type === "analysis") {
    return (
      <div>
        <p className="text-sm font-medium mb-4">{response.data.text}</p>
        <div className="space-y-3">
          {response.data.anomalies.map((anomaly: any, index: number) => (
            <div key={index} className="p-3 rounded-lg bg-background/50 border border-border/50">
              <div className="flex items-start gap-2 mb-2">
                <AlertCircle
                  className={`w-4 h-4 flex-shrink-0 mt-0.5 ${
                    anomaly.severity === "high" ? "text-destructive" : "text-orange-500"
                  }`}
                />
                <div>
                  <p className="font-semibold text-sm">{anomaly.title}</p>
                  <p className="text-xs text-muted-foreground">{anomaly.description}</p>
                  <p className="text-xs text-primary mt-1">✓ {anomaly.recommendation}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  if (response.type === "recommendation") {
    return (
      <div>
        <p className="text-sm font-medium mb-4">{response.data.text}</p>
        <div className="space-y-3">
          {response.data.recommendations.map((rec: any, index: number) => (
            <div key={index} className="p-3 rounded-lg bg-background/50 border border-border/50">
              <div className="flex items-start gap-2 mb-2">
                <CheckCircle2 className="w-4 h-4 text-accent flex-shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold text-sm">{rec.title}</p>
                  <p className="text-xs text-muted-foreground">{rec.details}</p>
                  <p className="text-xs text-green-500 font-medium mt-1">Potential Savings: ${rec.savings}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  return null
}
