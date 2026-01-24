"use client"

import { Check, X } from "lucide-react"

const comparison = [
  { feature: "Multi-cloud support (AWS/Azure/GCP)", optibrain: true, awsExplorer: false, azureCost: false },
  { feature: "AI-powered recommendations", optibrain: true, awsExplorer: false, azureCost: false },
  { feature: "Autonomous optimization", optibrain: true, awsExplorer: false, azureCost: false },
  { feature: "Predictive forecasting", optibrain: true, awsExplorer: true, azureCost: true },
  { feature: "Anomaly detection", optibrain: true, awsExplorer: true, azureCost: true },
  { feature: "Security + cost correlation", optibrain: true, awsExplorer: false, azureCost: false },
  { feature: "Native cloud integration", optibrain: true, awsExplorer: true, azureCost: true },
  { feature: "Conversational Cloud Cost Intelligence AI", optibrain: true, awsExplorer: false, azureCost: false },
  { feature: "Policy-as-code automation", optibrain: true, awsExplorer: false, azureCost: false },
  { feature: "Real-time cost tracking", optibrain: true, awsExplorer: true, azureCost: true },
]

export default function ComparisonSection() {
  return (
    <section className="py-24 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold mb-4">OptiBrain vs The Rest</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            How OptiBrain compares to native cloud tools and competitors
          </p>
        </div>

        {/* Comparison Table */}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-4 px-6 font-bold text-foreground">Feature</th>
                <th className="text-center py-4 px-6 font-bold">
                  <div className="flex flex-col items-center gap-2">
                    <div className="w-10 h-10 bg-primary rounded-lg flex items-center justify-center">
                      <span className="text-primary-foreground font-bold text-sm">OB</span>
                    </div>
                    <span className="text-sm">OptiBrain</span>
                  </div>
                </th>
                <th className="text-center py-4 px-6 font-bold">
                  <div className="flex flex-col items-center gap-2">
                    <div className="w-10 h-10 bg-yellow-600 rounded-lg flex items-center justify-center text-white text-xs font-bold">
                      AWS
                    </div>
                    <span className="text-sm">AWS Cost Explorer</span>
                  </div>
                </th>
                <th className="text-center py-4 px-6 font-bold">
                  <div className="flex flex-col items-center gap-2">
                    <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center text-white text-xs font-bold">
                      AZ
                    </div>
                    <span className="text-sm">Azure Cost Mgmt</span>
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {comparison.map((row, index) => (
                <tr key={index} className="border-b border-border/50 hover:bg-secondary/20 transition">
                  <td className="py-4 px-6 font-medium">{row.feature}</td>
                  <td className="py-4 px-6 text-center">
                    {row.optibrain ? (
                      <Check className="w-5 h-5 text-primary mx-auto" />
                    ) : (
                      <X className="w-5 h-5 text-muted-foreground/50 mx-auto" />
                    )}
                  </td>
                  <td className="py-4 px-6 text-center">
                    {row.awsExplorer ? (
                      <Check className="w-5 h-5 text-primary mx-auto" />
                    ) : (
                      <X className="w-5 h-5 text-muted-foreground/50 mx-auto" />
                    )}
                  </td>
                  <td className="py-4 px-6 text-center">
                    {row.azureCost ? (
                      <Check className="w-5 h-5 text-primary mx-auto" />
                    ) : (
                      <X className="w-5 h-5 text-muted-foreground/50 mx-auto" />
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
