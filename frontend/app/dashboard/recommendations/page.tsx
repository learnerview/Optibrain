'use client'

import { useEffect, useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getRecommendations } from '@/lib/api'
import { Brain, TrendingDown, CheckCircle2, AlertCircle } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

export default function RecommendationsPage() {
  const [recommendations, setRecommendations] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchRecommendations = async () => {
      try {
        setLoading(true)
        const data = await getRecommendations()
        setRecommendations(data)
      } catch (err) {
        console.error("Failed to fetch recommendations", err)
      } finally {
        setLoading(false)
      }
    }
    fetchRecommendations()
  }, [])

  const totalMonthlySavings = recommendations.reduce((sum, rec) => sum + rec.monthlySavings, 0)

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-bold text-foreground mb-2">AI Optimization Recommendations</h1>
            <p className="text-muted-foreground">Intelligent insights based on historical usage patterns and AI heuristics</p>
          </div>
          <div className="text-right hidden md:block">
            <p className="text-sm text-muted-foreground mb-1 uppercase tracking-wider">Total Potential Monthly Savings</p>
            <p className="text-3xl font-bold text-emerald-400">₹{totalMonthlySavings.toLocaleString()}</p>
          </div>
        </div>

        {loading ? (
          <div className="grid gap-6 animate-pulse">
            <div className="h-40 bg-slate-800 rounded-xl"></div>
            <div className="h-40 bg-slate-800 rounded-xl"></div>
          </div>
        ) : (
          <div className="grid gap-6">
            {recommendations.map((rec, idx) => (
              <Card key={idx} className="glass border-emerald-500/20 hover:border-emerald-500/40 p-6 transition-all group">
                <div className="flex flex-col md:flex-row gap-6">
                  {/* Icon & Savings */}
                  <div className="flex md:flex-col items-center justify-between md:justify-center p-4 bg-emerald-500/5 rounded-xl border border-emerald-500/10 min-w-[150px]">
                    <div className="w-12 h-12 bg-emerald-500/20 rounded-full flex items-center justify-center mb-0 md:mb-3">
                      <TrendingDown className="w-6 h-6 text-emerald-400" />
                    </div>
                    <div className="text-right md:text-center">
                      <p className="text-xl font-bold text-emerald-400">₹{rec.monthlySavings.toLocaleString()}</p>
                      <p className="text-[10px] text-emerald-400/60 uppercase font-bold tracking-widest">Savings / Mo</p>
                    </div>
                  </div>

                  {/* Details */}
                  <div className="flex-1 space-y-3">
                    <div className="flex items-center gap-2">
                      <h3 className="text-lg font-bold text-foreground group-hover:text-emerald-400 transition-colors">{rec.resource}</h3>
                    </div>
                    <div className="flex flex-col gap-2">
                      <div className="flex items-start gap-2 text-sm">
                        <AlertCircle className="w-4 h-4 text-amber-400 mt-0.5" />
                        <span className="text-muted-foreground font-medium w-32 shrink-0">Problem:</span>
                        <span className="text-foreground">{rec.issue}</span>
                      </div>
                      <div className="flex items-start gap-2 text-sm">
                        <CheckCircle2 className="w-4 h-4 text-emerald-400 mt-0.5" />
                        <span className="text-muted-foreground font-medium w-32 shrink-0">Recommendation:</span>
                        <span className="text-foreground font-semibold">{rec.recommendation}</span>
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex md:flex-col justify-end gap-3 min-w-[140px]">
                    <Button className="bg-emerald-500 hover:bg-emerald-600 text-white font-bold w-full">
                      Apply Now
                    </Button>
                    <Button variant="outline" className="border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/10 w-full">
                      Details
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}

        <div className="p-4 bg-blue-500/5 border border-blue-500/20 rounded-lg">
          <div className="flex gap-3">
            <Brain className="w-5 h-5 text-blue-400" />
            <p className="text-xs text-blue-300 leading-relaxed italic">
              "Recommendations are generated using historical usage patterns and AI-based heuristics. Impact estimates are based on current regional spot and on-demand pricing."
            </p>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
