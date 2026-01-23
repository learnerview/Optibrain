"use client"

import { recommendations } from "@/lib/mock-data"
import { CheckCircle2, TrendingDown } from "lucide-react"
import { Button } from "@/components/ui/button"

export default function RecommendationsWidget() {
  return (
    <div className="glass p-6 rounded-xl border border-border/50">
      <h3 className="text-lg font-semibold mb-6">Top Recommendations</h3>

      <div className="space-y-3">
        {recommendations.map((rec, index) => (
          <div
            key={index}
            className="flex items-start justify-between p-4 rounded-lg bg-secondary/30 hover:bg-secondary/50 transition border border-border/50"
          >
            <div className="flex items-start gap-4 flex-1">
              <div className="w-10 h-10 rounded-lg bg-accent/20 flex items-center justify-center flex-shrink-0">
                <TrendingDown className="w-5 h-5 text-accent" />
              </div>
              <div>
                <h4 className="font-semibold text-sm mb-1">{rec.title}</h4>
                <div className="flex gap-4 text-xs text-muted-foreground">
                  <span>Savings: ${rec.savings}</span>
                  <span>Effort: {rec.effort}</span>
                  <span>Impact: {rec.impact}</span>
                </div>
              </div>
            </div>
            <Button size="sm" variant="outline" className="flex-shrink-0 ml-4 bg-transparent">
              <CheckCircle2 className="w-4 h-4 mr-1" />
              Apply
            </Button>
          </div>
        ))}
      </div>

      <Button className="w-full mt-4 bg-transparent" variant="outline">
        View All Recommendations
      </Button>
    </div>
  )
}
