"use client"

import { mockCloudData } from "@/lib/mock-data"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { CheckCircle2, Clock } from "lucide-react"

import { SpotActionDTO } from "@/lib/api"

interface RecentDecisionsProps {
  data?: any[]
}

export default function RecentDecisions({ data }: RecentDecisionsProps) {
  const decisions = data ? data.map((d: any, idx: number) => ({
    id: `DEC-${idx}`,
    action: d.type || d.action,
    resource: d.resourceId || d.resource,
    savings: d.predictedSavings || d.savings || 0,
    confidence: d.confidence || 95,
    timestamp: d.createdAt || d.timestamp || new Date().toISOString(),
    status: (d.status || 'completed').toLowerCase()
  })) : mockCloudData.autonomousDecisions

  const actionLabels: Record<string, string> = {
    SPOT_MIGRATE: "SPOT_MIGRATE",
    RIGHT_SIZE: "RIGHT_SIZE",
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case "completed":
        return "bg-emerald-500/20 text-emerald-400 border-emerald-500/30"
      case "pending":
        return "bg-amber-500/20 text-amber-400 border-amber-500/30"
      default:
        return "bg-red-500/20 text-red-400 border-red-500/30"
    }
  }

  const getStatusIcon = (status: string) => {
    return status === "completed" ? <CheckCircle2 className="w-4 h-4" /> : <Clock className="w-4 h-4" />
  }

  return (
    <Card className="glass border-border/50 p-6">
      <div className="mb-4">
        <h2 className="text-lg font-bold text-foreground">Recent Decisions - Latest autonomous optimization actions</h2>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border/30 text-muted-foreground">
              <th className="text-left py-3 px-3 font-semibold">Decision ID</th>
              <th className="text-left py-3 px-3 font-semibold">Action</th>
              <th className="text-left py-3 px-3 font-semibold">Resource</th>
              <th className="text-right py-3 px-3 font-semibold">Savings</th>
              <th className="text-right py-3 px-3 font-semibold">Confidence</th>
              <th className="text-left py-3 px-3 font-semibold">Time</th>
              <th className="text-center py-3 px-3 font-semibold">Status</th>
            </tr>
          </thead>
          <tbody>
            {decisions.map((decision) => (
              <tr key={decision.id} className="border-b border-border/20 hover:bg-secondary/20 transition">
                <td className="py-3 px-3 font-mono text-primary">{decision.id}</td>
                <td className="py-3 px-3 font-medium text-foreground">
                  {actionLabels[decision.action] || decision.action}
                </td>
                <td className="py-3 px-3 text-muted-foreground">{decision.resource}</td>
                <td className="py-3 px-3 text-right text-emerald-400 font-semibold">${decision.savings.toFixed(2)}</td>
                <td className="py-3 px-3 text-right text-foreground font-medium">{decision.confidence}%</td>
                <td className="py-3 px-3 text-muted-foreground">
                  {new Date(decision.timestamp).toLocaleTimeString("en-US", {
                    hour: "2-digit",
                    minute: "2-digit",
                    hour12: true,
                  })}
                </td>
                <td className="py-3 px-3 text-center">
                  <Badge
                    variant="outline"
                    className={`capitalize inline-flex items-center gap-1.5 ${getStatusColor(decision.status)}`}
                  >
                    {getStatusIcon(decision.status)}
                    {decision.status}
                  </Badge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 text-right">
        <a href="/dashboard/decisions" className="text-primary hover:text-primary/80 text-sm font-medium">
          View all →
        </a>
      </div>
    </Card>
  )
}
