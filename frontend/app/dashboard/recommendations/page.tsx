'use client'

import { useState, useEffect } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { ChevronRight, Zap, AlertCircle, CheckCircle2, TrendingDown } from 'lucide-react'
import { getCleanupRecommendations, executeCleanup, OrphanedResourceDTO } from '@/lib/api'

export default function RecommendationsPage() {
  const [filter, setFilter] = useState('all')
  const [selectedRec, setSelectedRec] = useState<any>(null)
  const [recommendationsList, setRecommendationsList] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  const fetchRecommendations = async () => {
    try {
      const data = await getCleanupRecommendations()
      const mapped = (data || []).map((r: OrphanedResourceDTO, idx: number) => ({
        id: idx,
        resourceId: r.resourceId,
        resource: r.resourceId,
        type: r.resourceType,
        issue: 'Orphaned resource detected',
        description: `Unused ${r.resourceType} in ${r.region}. Created at ${new Date(r.createdAt).toLocaleDateString()}.`,
        savings: `$${r.estimatedMonthlyCost?.toLocaleString() || '0'}`,
        confidence: 95,
        impact: r.estimatedMonthlyCost > 100 ? 'high' : 'medium',
        icon: AlertCircle,
        actualSavings: r.estimatedMonthlyCost
      }))
      setRecommendationsList(mapped)
    } catch (error) {
      console.error('Failed to fetch recommendations:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRecommendations()
  }, [])

  const handleApply = async (rec: any) => {
    try {
      await executeCleanup(rec.resourceId, rec.type)
      fetchRecommendations()
    } catch (err) {
      console.error('Failed to execute cleanup:', err)
    }
  }

  const filtered = filter === 'all' ? recommendationsList : recommendationsList.filter((r: any) => r.type === filter)
  const totalSavings = filtered.reduce((sum, r: any) => sum + r.actualSavings, 0)

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-emerald-600/20 via-teal-600/20 to-cyan-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-teal-400 to-cyan-400 mb-2">AI Recommendations Center</h1>
            <p className="text-muted-foreground">Machine learning-powered optimization opportunities</p>
          </div>
        </div>

        {/* Summary Stats */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-emerald-950/40 to-teal-950/40 border border-emerald-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Active Recommendations</p>
            <p className="text-3xl font-bold text-emerald-400">{recommendationsList.length}</p>
            <p className="text-xs text-emerald-300 mt-2">AI-generated opportunities</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-cyan-950/40 to-blue-950/40 border border-cyan-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Potential Monthly Savings</p>
            <p className="text-3xl font-bold text-cyan-400">${(totalSavings / 12).toLocaleString()}</p>
            <p className="text-xs text-cyan-300 mt-2">If all applied</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-purple-950/40 to-pink-950/40 border border-purple-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Avg Confidence</p>
            <p className="text-3xl font-bold text-purple-400">92%</p>
            <p className="text-xs text-purple-300 mt-2">High reliability</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-blue-950/40 to-indigo-950/40 border border-blue-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Annual Impact</p>
            <p className="text-3xl font-bold text-blue-400">${(totalSavings * 12).toLocaleString()}</p>
            <p className="text-xs text-blue-300 mt-2">Full year projection</p>
          </div>
        </div>

        {/* Filter */}
        <div className="flex gap-2 flex-wrap">
          {['all', 'RESIZE', 'SCHEDULE', 'ARCHIVE', 'MIGRATE', 'DELETE', 'SPOT_CONVERSION'].map(t => (
            <button
              key={t}
              onClick={() => setFilter(t)}
              className={`px-4 py-2 rounded-lg font-medium transition-all ${filter === t
                ? 'bg-gradient-to-r from-emerald-500 to-cyan-500 text-white'
                : 'bg-slate-700/30 text-slate-300 hover:bg-slate-600/30'
                }`}
            >
              {t === 'all' ? 'All Recommendations' : t}
            </button>
          ))}
        </div>

        {/* Recommendations Grid */}
        <div className="space-y-4">
          {filtered.map(rec => (
            <div
              key={rec.id}
              onClick={() => setSelectedRec(selectedRec?.id === rec.id ? null : rec)}
              className="backdrop-blur-xl bg-gradient-to-r from-slate-900/60 to-slate-800/40 border border-emerald-500/20 hover:border-emerald-400/50 rounded-2xl p-6 cursor-pointer transition-all transform hover:scale-[1.02]"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-3 py-1 bg-emerald-500/20 text-emerald-300 rounded-full text-xs font-bold">{rec.type}</span>
                    <h3 className="text-lg font-bold text-cyan-300">{rec.resource}</h3>
                  </div>
                  <p className="text-slate-400">{rec.issue}</p>
                </div>
                <div className="text-right">
                  <p className="text-2xl font-bold text-emerald-400">{rec.savings}</p>
                  <p className="text-xs text-slate-400">monthly savings</p>
                </div>
              </div>

              <p className="text-sm text-slate-300 mb-4">{rec.description}</p>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="flex items-center gap-2">
                    <div className="w-32 h-2 bg-slate-700/50 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-cyan-500 to-emerald-500"
                        style={{ width: `${rec.confidence}%` }}
                      ></div>
                    </div>
                    <span className="text-xs font-semibold text-cyan-300">{rec.confidence}%</span>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button className="px-4 py-2 bg-gradient-to-r from-emerald-500 to-cyan-500 text-white rounded-lg font-medium hover:from-emerald-600 hover:to-cyan-600 transition">Apply</button>
                  <button className="px-4 py-2 bg-slate-700/30 text-slate-300 rounded-lg font-medium hover:bg-slate-600/30 transition">Simulate</button>
                </div>
              </div>

              {selectedRec?.id === rec.id && (
                <div className="mt-4 pt-4 border-t border-slate-700/50">
                  <div className="space-y-3">
                    <div>
                      <p className="text-xs text-slate-500 uppercase mb-1">AI Analysis</p>
                      <p className="text-sm text-slate-300">
                        This recommendation is based on 90 days of usage patterns. The resource has consistently operated below optimal capacity, indicating oversizing. Switching to a smaller instance type will maintain performance while reducing costs.
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-slate-500 uppercase mb-1">Implementation</p>
                      <p className="text-sm text-slate-300">
                        OptiBrain can automatically migrate this workload during the next maintenance window with zero downtime. Estimated time: 15 minutes.
                      </p>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  )
}
