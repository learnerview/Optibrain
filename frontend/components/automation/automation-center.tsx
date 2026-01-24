"use client"

import { useState } from "react"
import { Clock, Zap, Trash2, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"

const automationRules = [
  {
    id: 1,
    name: "Auto-terminate idle EC2 instances",
    description: "Automatically stop EC2 instances with <5% CPU for 7 days",
    enabled: true,
    schedule: "Daily at 2 AM UTC",
    savings: 320,
  },
  {
    id: 2,
    name: "Archive old S3 objects",
    description: "Move objects older than 90 days to Glacier",
    enabled: true,
    schedule: "Weekly, Sunday 1 AM UTC",
    savings: 180,
  },
  {
    id: 3,
    name: "Downsize underutilized RDS",
    description: "Automatically downsize RDS instances with <10% usage",
    enabled: false,
    schedule: "Bi-weekly",
    savings: 250,
  },
]

export default function AutomationCenter() {
  const [rules, setRules] = useState(automationRules)

  const toggleRule = (id: number) => {
    setRules(rules.map((rule) => (rule.id === id ? { ...rule, enabled: !rule.enabled } : rule)))
  }

  const totalSavings = rules.filter((r) => r.enabled).reduce((acc, r) => acc + r.savings, 0)

  return (
    <div className="space-y-6">
      {/* Summary */}
      <div className="grid md:grid-cols-3 gap-6">
        <div className="glass p-6 rounded-xl border border-border/50">
          <p className="text-sm text-muted-foreground mb-2">Active Rules</p>
          <p className="text-3xl font-bold">{rules.filter((r) => r.enabled).length}</p>
        </div>
        <div className="glass p-6 rounded-xl border border-border/50">
          <p className="text-sm text-muted-foreground mb-2">Monthly Savings</p>
          <p className="text-3xl font-bold text-green-500">${totalSavings}</p>
        </div>
        <div className="glass p-6 rounded-xl border border-border/50">
          <p className="text-sm text-muted-foreground mb-2">Last Run</p>
          <p className="text-lg font-semibold">Demo Mode</p>
          <p className="text-xs text-amber-400 mt-1">Execution engine in development</p>
        </div>
      </div>

      {/* Rules list */}
      <div className="glass rounded-xl border border-border/50 overflow-hidden">
        <div className="p-6 border-b border-border/50 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-semibold">Automation Rules</h3>
            <span className="px-2 py-1 text-xs bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded">Demo Mode</span>
          </div>
          <button
            className="px-3 py-1.5 text-sm bg-secondary/50 text-muted-foreground rounded-lg cursor-not-allowed opacity-50 flex items-center gap-2"
            disabled
            title="Demo Mode - Rule creation coming soon"
          >
            <Plus className="w-4 h-4" />
            Add Rule
          </button>
        </div>

        <div className="divide-y divide-border/50">
          {rules.map((rule) => (
            <div key={rule.id} className="p-6 hover:bg-secondary/30 transition">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-start gap-4 flex-1">
                  <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center flex-shrink-0">
                    <Zap className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <h4 className="font-semibold mb-1">{rule.name}</h4>
                    <p className="text-sm text-muted-foreground mb-3">{rule.description}</p>
                    <div className="flex items-center gap-4 text-xs text-muted-foreground">
                      <div className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        {rule.schedule}
                      </div>
                      <div className="flex items-center gap-1 text-green-500">
                        <Zap className="w-3 h-3" />
                        Save ${rule.savings}/month
                      </div>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Switch checked={rule.enabled} onChange={() => toggleRule(rule.id)} />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
