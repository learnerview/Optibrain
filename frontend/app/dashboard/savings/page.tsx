'use client'

import { useEffect, useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getSavingsProjection } from '@/lib/api'
import { TrendingUp, ShieldCheck, ListChecks, ArrowUpRight } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

export default function SavingsPage() {
  const [savings, setSavings] = useState<any>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchSavings = async () => {
      try {
        setLoading(true)
        const data = await getSavingsProjection()
        setSavings(data)
      } catch (err) {
        console.error("Failed to fetch savings projection", err)
      } finally {
        setLoading(false)
      }
    }
    fetchSavings()
  }, [])

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground mb-2">Savings Projection</h1>
          <p className="text-muted-foreground">Quantified impact of AI-driven cost optimization strategies</p>
        </div>

        {loading ? (
          <div className="grid md:grid-cols-2 gap-6 animate-pulse">
            <div className="h-40 bg-slate-800 rounded-xl"></div>
            <div className="h-40 bg-slate-800 rounded-xl"></div>
            <div className="h-60 bg-slate-800 rounded-xl md:col-span-2"></div>
          </div>
        ) : (
          <div className="grid gap-6">
            {/* Main Stats */}
            <div className="grid md:grid-cols-2 gap-6">
              <Card className="glass border-emerald-500/20 p-8 flex flex-col justify-center relative overflow-hidden group">
                <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
                  <TrendingUp className="w-24 h-24 text-emerald-400" />
                </div>
                <div className="relative z-10">
                  <p className="text-sm font-medium text-emerald-400/80 uppercase tracking-widest mb-2">Estimated Monthly Savings</p>
                  <h2 className="text-5xl font-extrabold text-foreground mb-4">₹{savings?.monthlySavings.toLocaleString()}</h2>
                  <div className="flex items-center gap-2 text-emerald-400 font-bold">
                    <ArrowUpRight className="w-5 h-5" />
                    <span>Immediate Impact</span>
                  </div>
                </div>
              </Card>

              <Card className="glass border-blue-500/20 p-8 flex flex-col justify-center relative overflow-hidden group">
                <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
                  <ArrowUpRight className="w-24 h-24 text-blue-400" />
                </div>
                <div className="relative z-10">
                  <p className="text-sm font-medium text-blue-400/80 uppercase tracking-widest mb-2">Projected Annual Savings</p>
                  <h2 className="text-5xl font-extrabold text-foreground mb-4">₹{savings?.annualSavings.toLocaleString()}</h2>
                  <div className="flex items-center gap-2 text-blue-400 font-bold">
                    <ShieldCheck className="w-5 h-5" />
                    <span>Long-term Sustainability</span>
                  </div>
                </div>
              </Card>
            </div>

            {/* Confidence & Assumptions */}
            <div className="grid md:grid-cols-3 gap-6">
              <Card className="glass border-border/50 p-6 md:col-span-1">
                <h3 className="text-sm font-medium text-muted-foreground mb-6 uppercase tracking-wider">Intelligence Confidence</h3>
                <div className="flex flex-col items-center justify-center space-y-4 py-4">
                  <div className="relative w-32 h-32 flex items-center justify-center">
                    <svg className="w-full h-full transform -rotate-90">
                      <circle cx="64" cy="64" r="58" stroke="currentColor" strokeWidth="8" fill="transparent" className="text-slate-800" />
                      <circle cx="64" cy="64" r="58" stroke="currentColor" strokeWidth="8" fill="transparent" className="text-emerald-500" strokeDasharray="364" strokeDashoffset={364 * (1 - 0.94)} />
                    </svg>
                    <span className="absolute text-2xl font-bold">{savings?.confidence}</span>
                  </div>
                  <Badge className="bg-emerald-500/20 text-emerald-400 border-emerald-500/30 px-4 py-1">
                    94% RELIABILITY
                  </Badge>
                </div>
              </Card>

              <Card className="glass border-border/50 p-6 md:col-span-2">
                <div className="flex items-center gap-2 mb-6">
                  <ListChecks className="w-5 h-5 text-blue-400" />
                  <h3 className="text-sm font-medium text-muted-foreground uppercase tracking-wider">Key Assumptions</h3>
                </div>
                <div className="grid gap-4">
                  {savings?.assumptions.map((assumption: string, idx: number) => (
                    <div key={idx} className="flex items-center gap-4 p-4 bg-slate-900/40 border border-slate-800 rounded-xl hover:border-blue-500/30 transition-colors">
                      <div className="w-6 h-6 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400 font-bold text-xs shrink-0">
                        {idx + 1}
                      </div>
                      <p className="text-sm text-foreground font-medium">{assumption}</p>
                    </div>
                  ))}
                </div>
              </Card>
            </div>

            <div className="p-6 bg-amber-500/5 border border-amber-500/20 rounded-xl text-center">
              <p className="text-sm text-amber-300">
                ⚠️ All projections are based on historical cloud usage patterns (last 180 days). Savings are estimated using current provider pricing models.
              </p>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
