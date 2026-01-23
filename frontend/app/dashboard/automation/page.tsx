'use client'

import { useState, useEffect } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { Zap, Clock, TrendingDown, Play, Pause, Trash2 } from 'lucide-react'
import { getSpotActions, SpotActionDTO } from '@/lib/api'

export default function AutomationPage() {
  const [selectedRule, setSelectedRule] = useState<any>(null)
  const [automationRulesList, setAutomationRulesList] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function fetchData() {
      try {
        const data = await getSpotActions()
        const mapped = (data || []).map((a: SpotActionDTO, idx: number) => ({
          id: idx,
          name: `${a.type} Optimization`,
          trigger: 'ML Analysis',
          action: a.type,
          savings: `$${a.predictedSavings}/mo`,
          status: a.status.toLowerCase(),
          executions: 1,
          lastRun: new Date(a.createdAt).toLocaleDateString(),
          successRate: 100
        }))
        setAutomationRulesList(mapped)
      } catch (error) {
        console.error('Failed to fetch automation actions:', error)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-purple-600/20 via-pink-600/20 to-red-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-purple-400 via-pink-400 to-red-400 mb-2">Autonomous Automation Center</h1>
            <p className="text-muted-foreground">24/7 cost optimization engine with AI-powered execution</p>
          </div>
        </div>

        {/* Key Metrics */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-purple-950/40 to-pink-950/40 border border-purple-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Active Rules</p>
            <p className="text-3xl font-bold text-purple-400">{automationRulesList.filter((r: any) => r.status === 'active' || r.status === 'completed' || r.status === 'executing').length}</p>
            <p className="text-xs text-purple-300 mt-2">Running continuously</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-pink-950/40 to-red-950/40 border border-pink-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Monthly Savings</p>
            <p className="text-3xl font-bold text-pink-400">$12,180</p>
            <p className="text-xs text-pink-300 mt-2">From automation</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-red-950/40 to-orange-950/40 border border-red-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Success Rate</p>
            <p className="text-3xl font-bold text-red-400">99.3%</p>
            <p className="text-xs text-red-300 mt-2">Last 7 days</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-orange-950/40 to-yellow-950/40 border border-orange-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Total Executions</p>
            <p className="text-3xl font-bold text-orange-400">2,291</p>
            <p className="text-xs text-orange-300 mt-2">Since activation</p>
          </div>
        </div>

        {/* Execution History */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">Weekly Execution History</h2>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={executionHistory}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,200,255,0.1)" />
              <XAxis dataKey="date" stroke="rgba(200,200,200,0.6)" />
              <YAxis stroke="rgba(200,200,200,0.6)" />
              <Tooltip contentStyle={{ backgroundColor: 'rgba(15,23,42,0.9)', border: '1px solid rgba(100,200,255,0.3)' }} />
              <Bar dataKey="successful" fill="#10b981" />
              <Bar dataKey="failed" fill="#ef4444" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Active Rules */}
        <div className="space-y-4">
          <h2 className="text-xl font-bold text-cyan-300">Active Automation Rules</h2>
          {automationRulesList.map((rule: any) => (
            <div
              key={rule.id}
              onClick={() => setSelectedRule(selectedRule?.id === rule.id ? null : rule)}
              className={`backdrop-blur-xl rounded-xl p-6 cursor-pointer transition-all border ${rule.status === 'active'
                ? 'bg-gradient-to-r from-purple-950/40 to-pink-950/40 border-purple-500/20 hover:border-purple-400/50'
                : 'bg-gradient-to-r from-slate-950/40 to-slate-900/40 border-slate-700/30 opacity-60'
                }`}
            >
              <div className="flex items-start justify-between mb-3">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <Zap className={`w-5 h-5 ${rule.status === 'active' ? 'text-yellow-400' : 'text-slate-500'}`} />
                    <h3 className="text-lg font-bold text-cyan-300">{rule.name}</h3>
                    <span className={`px-2 py-1 rounded-full text-xs font-bold ${rule.status === 'active'
                      ? 'bg-green-500/20 text-green-300'
                      : 'bg-slate-600/20 text-slate-400'
                      }`}>
                      {rule.status.toUpperCase()}
                    </span>
                  </div>
                </div>
                <span className="text-2xl font-bold text-emerald-400">{rule.savings}</span>
              </div>

              <div className="grid md:grid-cols-3 gap-4 mb-4 text-sm">
                <div className="flex items-center gap-2">
                  <Clock className="w-4 h-4 text-slate-400" />
                  <span className="text-slate-400">Trigger: <span className="text-slate-300">{rule.trigger}</span></span>
                </div>
                <div className="flex items-center gap-2">
                  <Zap className="w-4 h-4 text-slate-400" />
                  <span className="text-slate-400">Action: <span className="text-slate-300">{rule.action}</span></span>
                </div>
                <div className="flex items-center gap-2">
                  <TrendingDown className="w-4 h-4 text-slate-400" />
                  <span className="text-slate-400">Last: <span className="text-slate-300">{rule.lastRun}</span></span>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div>
                    <p className="text-xs text-slate-400">Executions: <span className="text-slate-300 font-bold">{rule.executions}</span></p>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-20 h-2 bg-slate-700/50 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-emerald-500 to-cyan-500"
                        style={{ width: `${rule.successRate}%` }}
                      ></div>
                    </div>
                    <span className="text-xs text-emerald-300">{rule.successRate}%</span>
                  </div>
                </div>
                <div className="flex gap-2">
                  {rule.status === 'active' ? (
                    <button className="p-2 bg-red-500/20 text-red-400 rounded-lg hover:bg-red-500/30 transition">
                      <Pause className="w-4 h-4" />
                    </button>
                  ) : (
                    <button className="p-2 bg-emerald-500/20 text-emerald-400 rounded-lg hover:bg-emerald-500/30 transition">
                      <Play className="w-4 h-4" />
                    </button>
                  )}
                  <button className="p-2 bg-slate-600/20 text-slate-400 rounded-lg hover:bg-slate-600/30 transition">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Suggested Automations */}
        <div className="space-y-4">
          <h2 className="text-xl font-bold text-cyan-300">AI-Suggested Automations</h2>
          <div className="grid md:grid-cols-3 gap-4">
            {suggestedAutomations.map((auto, idx) => (
              <div key={idx} className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-cyan-500/20 rounded-xl p-6 hover:border-cyan-400/50 transition-all">
                <h3 className="font-bold text-cyan-300 mb-2">{auto.title}</h3>
                <p className="text-sm text-slate-400 mb-4">{auto.description}</p>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xl font-bold text-emerald-400">{auto.potential}</span>
                  <span className="text-xs bg-emerald-500/20 text-emerald-300 px-2 py-1 rounded">{auto.confidence}%</span>
                </div>
                <button className="w-full px-4 py-2 bg-gradient-to-r from-cyan-500 to-blue-500 text-white rounded-lg font-medium hover:from-cyan-600 hover:to-blue-600 transition">
                  Enable
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
