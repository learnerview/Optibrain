"use client"

import {
  ScatterChart,
  Scatter,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  BarChart,
  Bar,
} from "recharts"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

const securityCostData = [
  { name: "WAF", risk: 8, cost: 1200, incidents: 3 },
  { name: "DDoS Protection", risk: 9, cost: 2800, incidents: 2 },
  { name: "Data Encryption", risk: 7, cost: 950, incidents: 0 },
  { name: "VPN Access", risk: 6, cost: 450, incidents: 1 },
  { name: "Compliance Scanning", risk: 8, cost: 1100, incidents: 5 },
  { name: "Identity Management", risk: 9, cost: 3200, incidents: 0 },
  { name: "Network Segmentation", risk: 7, cost: 800, incidents: 0 },
  { name: "Intrusion Detection", risk: 8, cost: 2100, incidents: 4 },
]

const riskMetrics = [
  { metric: "Critical Vulnerabilities", count: 12, cost: 4500, trend: "up" },
  { metric: "High Risk Resources", count: 28, cost: 8900, trend: "stable" },
  { metric: "Non-Compliant Assets", count: 45, cost: 12300, trend: "down" },
  { metric: "Exposed Credentials", count: 3, cost: 1200, trend: "down" },
]

export default function SecurityCostIntelligence() {
  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold mb-2">Security + Cost Intelligence</h1>
        <p className="text-muted-foreground">
          Visualize security investments and their financial impact across infrastructure
        </p>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {riskMetrics.map((metric, index) => (
          <Card key={index} className="glass p-6 border-primary/20">
            <p className="text-sm text-muted-foreground mb-1">{metric.metric}</p>
            <div className="flex items-end justify-between">
              <h3 className="text-3xl font-bold">{metric.count}</h3>
              <span
                className={`text-xs px-2 py-1 rounded-lg ${metric.trend === "up" ? "bg-red-500/20 text-red-400" : metric.trend === "down" ? "bg-green-500/20 text-green-400" : "bg-muted text-muted-foreground"}`}
              >
                {metric.trend === "up" ? "↑" : metric.trend === "down" ? "↓" : "→"} {metric.trend}
              </span>
            </div>
            <p className="text-xs text-primary mt-2 font-semibold">${metric.cost.toLocaleString()} monthly</p>
          </Card>
        ))}
      </div>

      {/* Security vs Cost Correlation */}
      <Card className="glass p-8 border-primary/20">
        <h3 className="text-xl font-bold mb-6">Security Investments vs Risk Level</h3>
        <ResponsiveContainer width="100%" height={400}>
          <ScatterChart margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(139, 92, 246, 0.1)" />
            <XAxis type="number" dataKey="cost" name="Monthly Cost ($)" stroke="rgba(255, 255, 255, 0.5)" />
            <YAxis type="number" dataKey="risk" name="Risk Level (0-10)" stroke="rgba(255, 255, 255, 0.5)" />
            <Tooltip
              cursor={{ strokeDasharray: "3 3" }}
              contentStyle={{ backgroundColor: "rgba(15, 23, 42, 0.9)", border: "1px solid rgba(139, 92, 246, 0.3)" }}
            />
            <Legend />
            <Scatter name="Security Services" data={securityCostData} fill="#22d3ee" />
          </ScatterChart>
        </ResponsiveContainer>
      </Card>

      {/* Compliance Cost Breakdown */}
      <Card className="glass p-8 border-primary/20">
        <h3 className="text-xl font-bold mb-6">Compliance Spend by Framework</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart
            data={[
              { framework: "SOC 2", cost: 2400, annual: 28800 },
              { framework: "HIPAA", cost: 1800, annual: 21600 },
              { framework: "PCI-DSS", cost: 1200, annual: 14400 },
              { framework: "GDPR", cost: 950, annual: 11400 },
              { framework: "ISO 27001", cost: 1100, annual: 13200 },
            ]}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(139, 92, 246, 0.1)" />
            <XAxis dataKey="framework" stroke="rgba(255, 255, 255, 0.5)" />
            <YAxis stroke="rgba(255, 255, 255, 0.5)" />
            <Tooltip
              contentStyle={{ backgroundColor: "rgba(15, 23, 42, 0.9)", border: "1px solid rgba(139, 92, 246, 0.3)" }}
            />
            <Legend />
            <Bar dataKey="cost" fill="#22d3ee" name="Monthly" />
            <Bar dataKey="annual" fill="#a78bfa" name="Annual" />
          </BarChart>
        </ResponsiveContainer>
      </Card>

      {/* Risk-Adjusted Recommendations */}
      <Card className="glass p-8 border-primary/20">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-bold">Risk-Adjusted Cost Optimization</h3>
          <Button size="sm">Implement All</Button>
        </div>
        <div className="space-y-4">
          {[
            { title: "Optimize WAF Rules", risk: "↓ 12%", cost: "↓ $180/mo", safe: true },
            { title: "Consolidate Identity Services", risk: "→ 0%", cost: "↓ $850/mo", safe: true },
            { title: "Automate Compliance Scanning", risk: "↓ 8%", cost: "↓ $320/mo", safe: true },
            { title: "Reduce Encryption Overhead", risk: "↑ 22%", cost: "↓ $420/mo", safe: false },
          ].map((rec, index) => (
            <div
              key={index}
              className="p-4 bg-secondary/30 rounded-lg border border-border/50 flex items-center justify-between"
            >
              <div>
                <p className="font-semibold">{rec.title}</p>
                <p className="text-sm text-muted-foreground">
                  Risk: <span className="text-primary">{rec.risk}</span> | Cost Savings:{" "}
                  <span className={rec.safe ? "text-green-400" : "text-orange-400"}>{rec.cost}</span>
                </p>
              </div>
              <Button size="sm" variant={rec.safe ? "default" : "outline"}>
                {rec.safe ? "Apply" : "Review"}
              </Button>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}
