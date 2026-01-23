"use client"

import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { ChevronRight } from "lucide-react"

const rightsizingData = [
  { instance: "t3.xlarge", current: 65, recommended: 35, savings: 450, region: "us-east-1" },
  { instance: "m5.2xlarge", current: 42, recommended: 28, savings: 320, region: "us-east-1" },
  { instance: "c5.4xlarge", current: 88, recommended: 72, savings: 520, region: "eu-west-1" },
  { instance: "r5.xlarge", current: 78, recommended: 55, savings: 280, region: "ap-southeast-1" },
  { instance: "i3.2xlarge", current: 45, recommended: 25, savings: 620, region: "us-west-2" },
]

const utilizationTrend = [
  { day: "Mon", average: 65, peak: 95, recommended: 72 },
  { day: "Tue", average: 62, peak: 88, recommended: 70 },
  { day: "Wed", average: 68, peak: 92, recommended: 75 },
  { day: "Thu", average: 71, peak: 98, recommended: 78 },
  { day: "Fri", average: 58, peak: 85, recommended: 65 },
  { day: "Sat", average: 42, peak: 68, recommended: 50 },
  { day: "Sun", average: 39, peak: 62, recommended: 45 },
]

export default function RightsizingEngine() {
  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold mb-2">Intelligent Rightsizing</h1>
        <p className="text-muted-foreground">Optimize compute instances based on actual usage patterns</p>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="glass p-6 border-primary/20">
          <p className="text-sm text-muted-foreground mb-1">Total Opportunities</p>
          <h3 className="text-3xl font-bold text-primary">45</h3>
          <p className="text-xs text-muted-foreground mt-2">Instances to resize</p>
        </Card>
        <Card className="glass p-6 border-primary/20">
          <p className="text-sm text-muted-foreground mb-1">Monthly Savings</p>
          <h3 className="text-3xl font-bold text-green-400">$12,500</h3>
          <p className="text-xs text-muted-foreground mt-2">If all implemented</p>
        </Card>
        <Card className="glass p-6 border-primary/20">
          <p className="text-sm text-muted-foreground mb-1">Avg Utilization</p>
          <h3 className="text-3xl font-bold text-orange-400">52%</h3>
          <p className="text-xs text-muted-foreground mt-2">Across recommendations</p>
        </Card>
        <Card className="glass p-6 border-primary/20">
          <p className="text-sm text-muted-foreground mb-1">Confidence Level</p>
          <h3 className="text-3xl font-bold text-primary">92%</h3>
          <p className="text-xs text-muted-foreground mt-2">Based on 30-day analysis</p>
        </Card>
      </div>

      {/* Utilization Trend */}
      <Card className="glass p-8 border-primary/20">
        <h3 className="text-xl font-bold mb-6">Weekly CPU Utilization Trend</h3>
        <ResponsiveContainer width="100%" height={400}>
          <LineChart data={utilizationTrend}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(139, 92, 246, 0.1)" />
            <XAxis dataKey="day" stroke="rgba(255, 255, 255, 0.5)" />
            <YAxis stroke="rgba(255, 255, 255, 0.5)" />
            <Tooltip
              contentStyle={{ backgroundColor: "rgba(15, 23, 42, 0.9)", border: "1px solid rgba(139, 92, 246, 0.3)" }}
            />
            <Legend />
            <Line type="monotone" dataKey="average" stroke="#22d3ee" strokeWidth={2} name="Average" />
            <Line type="monotone" dataKey="peak" stroke="#ef4444" strokeWidth={2} name="Peak" />
            <Line
              type="monotone"
              dataKey="recommended"
              stroke="#a78bfa"
              strokeWidth={2}
              strokeDasharray="5 5"
              name="Recommended"
            />
          </LineChart>
        </ResponsiveContainer>
      </Card>

      {/* Rightsizing Recommendations Table */}
      <Card className="glass p-8 border-primary/20">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-bold">Top Rightsizing Opportunities</h3>
          <Button size="sm">View All</Button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border/50">
                <th className="text-left py-4 px-4 text-sm font-semibold text-muted-foreground">Instance</th>
                <th className="text-center py-4 px-4 text-sm font-semibold text-muted-foreground">Current Size</th>
                <th className="text-center py-4 px-4 text-sm font-semibold text-muted-foreground">Recommended</th>
                <th className="text-center py-4 px-4 text-sm font-semibold text-muted-foreground">Current Util.</th>
                <th className="text-center py-4 px-4 text-sm font-semibold text-muted-foreground">Monthly Savings</th>
                <th className="text-center py-4 px-4 text-sm font-semibold text-muted-foreground">Region</th>
                <th className="text-center py-4 px-4 text-sm font-semibold text-muted-foreground">Action</th>
              </tr>
            </thead>
            <tbody>
              {rightsizingData.map((item, index) => (
                <tr key={index} className="border-b border-border/50 hover:bg-secondary/20 transition">
                  <td className="py-4 px-4 font-medium">{item.instance}</td>
                  <td className="py-4 px-4 text-center text-sm">
                    <span className="bg-red-500/20 text-red-400 px-3 py-1 rounded-lg">{item.current}%</span>
                  </td>
                  <td className="py-4 px-4 text-center text-sm">
                    <span className="bg-green-500/20 text-green-400 px-3 py-1 rounded-lg">{item.recommended}%</span>
                  </td>
                  <td className="py-4 px-4 text-center text-sm">
                    <div className="w-24 h-2 bg-secondary rounded-full overflow-hidden mx-auto">
                      <div
                        className="h-full bg-gradient-to-r from-orange-500 to-red-500"
                        style={{ width: `${item.current}%` }}
                      ></div>
                    </div>
                  </td>
                  <td className="py-4 px-4 text-center font-semibold text-green-400">
                    ${item.savings.toLocaleString()}
                  </td>
                  <td className="py-4 px-4 text-center text-sm text-muted-foreground">{item.region}</td>
                  <td className="py-4 px-4 text-center">
                    <Button size="sm" variant="outline" className="text-xs bg-transparent">
                      Resize <ChevronRight className="ml-1 w-3 h-3" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
