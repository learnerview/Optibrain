'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { CheckCircle2, AlertCircle, Clock, DollarSign, TrendingDown, Eye } from 'lucide-react'
import { useState, useEffect } from 'react'

export default function DecisionsPage() {
  const [selectedDecision, setSelectedDecision] = useState(null)
  const [filter, setFilter] = useState('all')
  const [decisionsList, setDecisionsList] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function fetchDecisions() {
      try {
        const response = await fetch('http://localhost:8080/api/audit')
        const result = await response.json()
        if (result.success) {
          const mapped = result.data.map((d: any) => ({
            id: d.decisionId,
            type: d.action.toLowerCase().includes('terminate') ? 'terminate' : 'optimize',
            resource: d.resourceId,
            service: d.service || 'Cloud Engine',
            region: d.region || 'Global',
            action: d.action,
            savings: `$${d.savings?.toLocaleString() || '0'}`,
            confidence: Math.round(d.score * 100) || 0,
            status: d.status.toLowerCase(),
            timestamp: new Date(d.timestamp).toLocaleString(),
            impact: d.explanation,
            riskLevel: d.score > 0.8 ? 'low' : 'medium',
            automationRule: d.providerSource || 'OptiBrain Autonomous'
          }))
          setDecisionsList(mapped)
        }
      } catch (error) {
        console.error('Failed to fetch decisions:', error)
      } finally {
        setLoading(false)
      }
    }
    fetchDecisions()
  }, [])

  const filteredDecisions = filter === 'all' ? decisionsList : decisionsList.filter((d: any) => d.status === filter)

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header with gradient background */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 via-cyan-600/20 to-teal-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-cyan-400 to-teal-400 mb-2">Optimization Decisions</h1>
            <p className="text-muted-foreground">View all autonomous optimization decisions made by OptiBrain</p>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-emerald-950/40 to-teal-950/40 border border-emerald-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Total Decisions</p>
            <p className="text-3xl font-bold text-emerald-400">{decisionsList.length}</p>
            <p className="text-xs text-emerald-300 mt-2">All time</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-green-950/40 to-emerald-950/40 border border-green-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Executed</p>
            <p className="text-3xl font-bold text-green-400">{decisionsList.filter((d: any) => d.status === 'executed' || d.status === 'success').length}</p>
            <p className="text-xs text-green-300 mt-2">Completed</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-yellow-950/40 to-orange-950/40 border border-yellow-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Pending Approval</p>
            <p className="text-3xl font-bold text-yellow-400">{decisionsList.filter((d: any) => d.status === 'pending').length}</p>
            <p className="text-xs text-yellow-300 mt-2">Awaiting confirmation</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-cyan-950/40 to-blue-950/40 border border-cyan-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Total Monthly Savings</p>
            <p className="text-3xl font-bold text-cyan-400">
              ${decisionsList.reduce((acc: number, d: any) => acc + (parseFloat(d.savings.replace('$', '').replace(',', '')) || 0), 0).toLocaleString()}
            </p>
            <p className="text-xs text-cyan-300 mt-2">From all decisions</p>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="flex gap-2">
          {['all', 'executed', 'pending'].map(f => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-4 py-2 rounded-lg font-medium transition ${filter === f
                  ? 'bg-gradient-to-r from-cyan-500 to-blue-500 text-white'
                  : 'bg-slate-700/30 text-slate-300 hover:bg-slate-600/30'
                }`}
            >
              {f.charAt(0).toUpperCase() + f.slice(1)}
            </button>
          ))}
        </div>

        {/* Decisions Table */}
        <div className="space-y-4">
          {filteredDecisions.map(decision => (
            <div
              key={decision.id}
              onClick={() => setSelectedDecision(selectedDecision?.id === decision.id ? null : decision)}
              className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-cyan-500/20 rounded-xl p-6 hover:border-cyan-400/50 transition-all cursor-pointer"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    {decision.status === 'executed' ? (
                      <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                    ) : (
                      <Clock className="w-5 h-5 text-yellow-400" />
                    )}
                    <h3 className="text-lg font-bold text-cyan-300">{decision.action}</h3>
                    <span className={`px-2 py-1 rounded-full text-xs font-bold ${decision.status === 'executed'
                        ? 'bg-emerald-500/20 text-emerald-300'
                        : 'bg-yellow-500/20 text-yellow-300'
                      }`}>
                      {decision.status.toUpperCase()}
                    </span>
                  </div>

                  <div className="grid md:grid-cols-5 gap-3 text-sm">
                    <div className="text-slate-400">
                      Decision: <span className="text-cyan-300 font-mono">{decision.id}</span>
                    </div>
                    <div className="text-slate-400">
                      Resource: <span className="text-slate-300">{decision.resource}</span>
                    </div>
                    <div className="text-slate-400">
                      Service: <span className="text-slate-300">{decision.service}</span>
                    </div>
                    <div className="text-slate-400">
                      Region: <span className="text-slate-300">{decision.region}</span>
                    </div>
                    <div className="text-slate-400">
                      {decision.timestamp}
                    </div>
                  </div>
                </div>

                <div className="text-right ml-4">
                  <div className="flex items-baseline gap-2 mb-2">
                    <span className="text-3xl font-bold text-emerald-400">{decision.savings}</span>
                    <span className="text-sm text-slate-400">/month</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-16 h-2 bg-slate-700/50 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-blue-500 to-cyan-500"
                        style={{ width: `${decision.confidence}%` }}
                      ></div>
                    </div>
                    <span className="text-xs text-cyan-300">{decision.confidence}%</span>
                  </div>
                </div>
              </div>

              {/* Expanded Details */}
              {selectedDecision?.id === decision.id && (
                <div className="mt-6 pt-6 border-t border-slate-700/50 space-y-4">
                  <div className="grid md:grid-cols-2 gap-4">
                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">Analysis</p>
                      <p className="text-sm text-slate-300">{decision.impact}</p>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">Automation Rule</p>
                      <p className="text-sm text-cyan-300 font-medium">{decision.automationRule}</p>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">Risk Level</p>
                      <p className={`text-sm font-bold ${decision.riskLevel === 'low' ? 'text-emerald-400'
                          : decision.riskLevel === 'medium' ? 'text-yellow-400'
                            : 'text-red-400'
                        }`}>
                        {decision.riskLevel.toUpperCase()}
                      </p>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">Confidence Score</p>
                      <p className="text-sm text-cyan-300 font-bold">{decision.confidence}%</p>
                    </div>
                  </div>

                  {decision.status === 'pending' && (
                    <div className="flex gap-2 pt-4">
                      <button className="px-4 py-2 bg-emerald-500/20 text-emerald-400 rounded-lg font-medium hover:bg-emerald-500/30 transition">
                        Approve
                      </button>
                      <button className="px-4 py-2 bg-red-500/20 text-red-400 rounded-lg font-medium hover:bg-red-500/30 transition">
                        Reject
                      </button>
                      <button className="px-4 py-2 bg-slate-600/20 text-slate-400 rounded-lg font-medium hover:bg-slate-600/30 transition">
                        Review Later
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  )
}
