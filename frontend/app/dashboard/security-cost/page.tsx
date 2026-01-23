'use client'

import { useState, useEffect } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { AlertTriangle, Shield, TrendingUp, Lock } from 'lucide-react'
import { getAuditLogs, AuditLogDTO } from '@/lib/api'

export default function SecurityCostPage() {
  const [anomalies, setAnomalies] = useState<AuditLogDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedAnomaly, setSelectedAnomaly] = useState<AuditLogDTO | null>(null)

  useEffect(() => {
    async function fetchData() {
      try {
        const data = await getAuditLogs()
        setAnomalies(data || [])
      } catch (error) {
        console.error('Failed to fetch audit logs:', error)
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
          <div className="absolute inset-0 bg-gradient-to-r from-red-600/20 via-pink-600/20 to-orange-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-red-400 via-pink-400 to-orange-400 mb-2">Security & Cost Intelligence</h1>
            <p className="text-muted-foreground">Detect security threats through cost anomaly analysis</p>
          </div>
        </div>

        {/* Key Metrics */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-red-950/40 to-pink-950/40 border border-red-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Active Threats</p>
            <p className="text-3xl font-bold text-red-400">{anomalies.length}</p>
            <p className="text-xs text-red-300 mt-2">Requiring action</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-pink-950/40 to-orange-950/40 border border-pink-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Cost Impact</p>
            <p className="text-3xl font-bold text-pink-400">$8,183</p>
            <p className="text-xs text-pink-300 mt-2">From threats</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-orange-950/40 to-amber-950/40 border border-orange-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Overall Risk Score</p>
            <p className="text-3xl font-bold text-orange-400">72/100</p>
            <p className="text-xs text-orange-300 mt-2">High risk</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-amber-950/40 to-yellow-950/40 border border-amber-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Last 24 Hours</p>
            <p className="text-3xl font-bold text-yellow-400">$47,283</p>
            <p className="text-xs text-yellow-300 mt-2">Potential exposure</p>
          </div>
        </div>

        {/* Cost-Security Correlation */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-red-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">24-Hour Cost vs Security Score Correlation</h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={costSecurityCorrelation}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,200,255,0.1)" />
              <XAxis dataKey="hour" stroke="rgba(200,200,200,0.6)" />
              <YAxis yAxisId="left" stroke="rgba(200,200,200,0.6)" />
              <YAxis yAxisId="right" orientation="right" stroke="rgba(200,200,200,0.6)" />
              <Tooltip contentStyle={{ backgroundColor: 'rgba(15,23,42,0.9)', border: '1px solid rgba(100,200,255,0.3)' }} />
              <Legend />
              <Line yAxisId="left" type="monotone" dataKey="cost" stroke="#ef4444" strokeWidth={2} name="Cost ($)" />
              <Line yAxisId="right" type="monotone" dataKey="securityScore" stroke="#10b981" strokeWidth={2} name="Security Score" />
            </LineChart>
          </ResponsiveContainer>
          <p className="text-sm text-slate-400 mt-4">Notice the inverse correlation: when security score drops, costs spike dramatically, indicating potential breaches or abuse.</p>
        </div>

        {/* Active Threats */}
        <div className="space-y-4">
          <div className="flex items-center gap-3 mb-4">
            <AlertTriangle className="w-6 h-6 text-red-400" />
            <h2 className="text-2xl font-bold text-cyan-300">Active Security Threats</h2>
          </div>

          <div className="space-y-4">
            {anomalies.map(anomaly => (
              <div
                key={anomaly.id}
                onClick={() => setSelectedAnomaly(selectedAnomaly?.id === anomaly.id ? null : anomaly)}
                className={`backdrop-blur-xl rounded-xl p-6 cursor-pointer transition-all border ${anomaly.severity === 'critical'
                  ? 'bg-gradient-to-r from-red-950/40 to-pink-950/40 border-red-500/30 hover:border-red-400/50'
                  : 'bg-gradient-to-r from-orange-950/40 to-red-950/40 border-orange-500/30 hover:border-orange-400/50'
                  }`}
              >
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <Shield className="w-5 h-5 text-red-400" />
                      <h3 className="text-lg font-bold text-red-300">{anomaly.title}</h3>
                      <span className={`px-3 py-1 rounded-full text-xs font-bold ${anomaly.severity === 'critical'
                        ? 'bg-red-500/20 text-red-300'
                        : 'bg-orange-500/20 text-orange-300'
                        }`}>
                        {anomaly.severity.toUpperCase()}
                      </span>
                    </div>
                    <p className="text-sm text-slate-400">{anomaly.description}</p>
                  </div>
                  <div className="text-right ml-4">
                    <p className="text-2xl font-bold text-red-400">{anomaly.cost}</p>
                    <p className="text-xs text-slate-400">cost impact</p>
                  </div>
                </div>

                <div className="grid md:grid-cols-4 gap-3 mb-4">
                  <div className="p-2 bg-slate-800/30 rounded border border-slate-700/50">
                    <p className="text-xs text-slate-400">Resource</p>
                    <p className="text-sm font-mono text-cyan-300">{anomaly.resource}</p>
                  </div>
                  <div className="p-2 bg-slate-800/30 rounded border border-slate-700/50">
                    <p className="text-xs text-slate-400">Time</p>
                    <p className="text-sm text-slate-300">{anomaly.time}</p>
                  </div>
                  <div className="p-2 bg-slate-800/30 rounded border border-slate-700/50">
                    <p className="text-xs text-slate-400">Risk Score</p>
                    <p className="text-sm font-bold text-red-400">{anomaly.riskScore}/100</p>
                  </div>
                  <div className="p-2 bg-slate-800/30 rounded border border-slate-700/50">
                    <p className="text-xs text-slate-400">Priority</p>
                    <p className="text-sm font-bold text-orange-400">{anomaly.action}</p>
                  </div>
                </div>

                {selectedAnomaly?.id === anomaly.id && (
                  <div className="mt-4 pt-4 border-t border-slate-700/50">
                    <div className="space-y-3">
                      <div>
                        <p className="text-xs text-slate-500 uppercase mb-1">Analysis</p>
                        <p className="text-sm text-slate-300">
                          This anomaly was detected through advanced correlation analysis between cost patterns and security signals. The spike in costs coincides with unusual resource behavior patterns that indicate potential compromise or misuse.
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-500 uppercase mb-1">Recommended Action</p>
                        <p className="text-sm text-slate-300">
                          1. Immediately isolate affected resource(s). 2. Review security logs for unauthorized access. 3. Check for data exfiltration. 4. Rotate credentials for affected systems. 5. Enable enhanced monitoring.
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <button className="px-4 py-2 bg-red-500/20 text-red-400 rounded-lg font-medium hover:bg-red-500/30 transition">Quarantine</button>
                        <button className="px-4 py-2 bg-orange-500/20 text-orange-400 rounded-lg font-medium hover:bg-orange-500/30 transition">Alert Team</button>
                        <button className="px-4 py-2 bg-slate-600/20 text-slate-400 rounded-lg font-medium hover:bg-slate-600/30 transition">Dismiss</button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Threat Categories */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">Threat Types & Financial Impact</h2>
          <div className="space-y-3">
            {threatsByCategory.map((threat, idx) => (
              <div key={idx} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50 hover:border-red-500/30 transition">
                <div>
                  <p className="font-bold text-cyan-300">{threat.category}</p>
                  <p className="text-sm text-slate-400">{threat.incidents} incidents detected</p>
                </div>
                <div className="text-right">
                  <p className="text-2xl font-bold text-red-400">{threat.cost}</p>
                  <p className="text-xs text-slate-400">total exposure</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Security Compliance */}
        <div className="grid md:grid-cols-2 gap-6">
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-blue-500/20 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <Lock className="w-6 h-6 text-blue-400" />
              <h3 className="text-lg font-bold text-cyan-300">Security Recommendations</h3>
            </div>
            <ul className="space-y-3">
              <li className="flex items-start gap-3">
                <span className="w-2 h-2 mt-1.5 rounded-full bg-emerald-400"></span>
                <span className="text-sm text-slate-300">Enable real-time cost monitoring with alerting</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-2 h-2 mt-1.5 rounded-full bg-emerald-400"></span>
                <span className="text-sm text-slate-300">Implement network segmentation & access controls</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-2 h-2 mt-1.5 rounded-full bg-yellow-400"></span>
                <span className="text-sm text-slate-300">Deploy DLP (Data Loss Prevention) solutions</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-2 h-2 mt-1.5 rounded-full bg-red-400"></span>
                <span className="text-sm text-slate-300">Reduce blast radius with resource isolation</span>
              </li>
            </ul>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-green-500/20 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <TrendingUp className="w-6 h-6 text-green-400" />
              <h3 className="text-lg font-bold text-cyan-300">Financial Impact Mitigation</h3>
            </div>
            <ul className="space-y-3">
              <li className="flex items-start gap-3">
                <span className="w-2 h-2 mt-1.5 rounded-full bg-emerald-400"></span>
                <span className="text-sm text-slate-300">Potential savings: $47,283 if threats resolved</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-2 h-2 mt-1.5 rounded-full bg-emerald-400"></span>
                <span className="text-sm text-slate-300">Cost baseline normalization estimated 48 hours</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-2 h-2 mt-1.5 rounded-full bg-yellow-400"></span>
                <span className="text-sm text-slate-300">Monthly savings: $180K with proactive monitoring</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="w-2 h-2 mt-1.5 rounded-full bg-green-400"></span>
                <span className="text-sm text-slate-300">ROI on security: 3.8x through cost prevention</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
