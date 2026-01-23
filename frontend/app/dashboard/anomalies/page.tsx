'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts'
import { AlertTriangle, TrendingUp, Clock, DollarSign, Zap } from 'lucide-react'
import { useState } from 'react'

const anomalyData = [
  { date: 'Day 1', cost: 2450, baseline: 2450 },
  { date: 'Day 2', cost: 2480, baseline: 2455 },
  { date: 'Day 3', cost: 2510, baseline: 2460 },
  { date: 'Day 4', cost: 2890, baseline: 2465 },
  { date: 'Day 5', cost: 3240, baseline: 2470 },
  { date: 'Day 6', cost: 4560, baseline: 2475 },
  { date: 'Day 7', cost: 3120, baseline: 2480 },
]

const anomalies = [
  {
    id: 'ANO-001',
    title: 'Compute Cost Spike',
    description: 'Unusual spike in EC2 costs detected on Jan 16',
    severity: 'high',
    detectedAt: '2 hours ago',
    impact: '$2,085 over baseline',
    resource: 'EC2 instances (us-east-1)',
    rootCause: 'Auto-scaling triggered by traffic spike - 8 new instances launched',
    status: 'investigating',
    chart: anomalyData,
    recommendation: 'Review auto-scaling policies and set max capacity limits'
  },
  {
    id: 'ANO-002',
    title: 'Data Transfer Anomaly',
    description: 'Unusual outbound data transfer pattern detected',
    severity: 'critical',
    detectedAt: '4 hours ago',
    impact: '$1,240 over baseline',
    resource: 'NAT Gateway (us-east-1)',
    rootCause: 'Potential data exfiltration or large data backup process',
    status: 'urgent',
    chart: anomalyData,
    recommendation: 'Immediately review VPC Flow Logs and security groups. Check for unauthorized data transfers.'
  },
  {
    id: 'ANO-003',
    title: 'Database Query Spike',
    description: 'RDS read replica costs increased unexpectedly',
    severity: 'medium',
    detectedAt: '8 hours ago',
    impact: '$340 over baseline',
    resource: 'RDS Read Replicas (eu-west-1)',
    rootCause: 'Unoptimized queries causing full table scans',
    status: 'resolved',
    chart: anomalyData,
    recommendation: 'Optimize N+1 queries and add missing database indexes'
  },
  {
    id: 'ANO-004',
    title: 'Storage Growth Anomaly',
    description: 'S3 bucket storage increased rapidly without upload spike',
    severity: 'medium',
    detectedAt: '1 day ago',
    impact: '$156 over baseline',
    resource: 'S3 Buckets (us-west-2)',
    rootCause: 'Versioning enabled without cleanup policy',
    status: 'investigating',
    chart: anomalyData,
    recommendation: 'Enable S3 Lifecycle policies to delete old versions'
  },
]

export default function AnomaliesPage() {
  const [selectedAnomaly, setSelectedAnomaly] = useState(anomalies[0])

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'critical':
        return 'from-red-950 to-red-900 border-red-500/30 text-red-400'
      case 'high':
        return 'from-orange-950 to-orange-900 border-orange-500/30 text-orange-400'
      case 'medium':
        return 'from-yellow-950 to-yellow-900 border-yellow-500/30 text-yellow-400'
      default:
        return 'from-slate-950 to-slate-900 border-slate-500/30 text-slate-400'
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'urgent':
        return 'bg-red-500/20 text-red-300'
      case 'investigating':
        return 'bg-yellow-500/20 text-yellow-300'
      case 'resolved':
        return 'bg-emerald-500/20 text-emerald-300'
      default:
        return 'bg-slate-500/20 text-slate-300'
    }
  }

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-red-600/20 via-orange-600/20 to-yellow-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <div className="flex items-center gap-3 mb-2">
              <AlertTriangle className="w-8 h-8 text-red-400" />
              <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-red-400 via-orange-400 to-yellow-400">Anomaly Detection</h1>
            </div>
            <p className="text-muted-foreground">AI-powered detection of unusual spending patterns and cost anomalies</p>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-red-950/40 to-orange-950/40 border border-red-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Critical Alerts</p>
            <p className="text-3xl font-bold text-red-400">{anomalies.filter(a => a.severity === 'critical').length}</p>
            <p className="text-xs text-red-300 mt-2">Immediate action required</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-orange-950/40 to-yellow-950/40 border border-orange-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">High Priority</p>
            <p className="text-3xl font-bold text-orange-400">{anomalies.filter(a => a.severity === 'high').length}</p>
            <p className="text-xs text-orange-300 mt-2">Review recommended</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-yellow-950/40 to-amber-950/40 border border-yellow-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Total Impact</p>
            <p className="text-3xl font-bold text-yellow-400">$3,821</p>
            <p className="text-xs text-yellow-300 mt-2">Over baseline</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-amber-950/40 to-slate-950/40 border border-amber-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Anomalies Detected</p>
            <p className="text-3xl font-bold text-amber-400">{anomalies.length}</p>
            <p className="text-xs text-amber-300 mt-2">Last 7 days</p>
          </div>
        </div>

        {/* Main Content */}
        <div className="grid lg:grid-cols-3 gap-6">
          {/* Chart and Details */}
          <div className="lg:col-span-2 space-y-6">
            {/* Selected Anomaly Chart */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-red-500/20 rounded-2xl p-6">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-xl font-bold text-cyan-300 mb-1">{selectedAnomaly.title}</h3>
                  <p className="text-sm text-slate-400">{selectedAnomaly.description}</p>
                </div>
                <span className={`px-3 py-1 rounded-full text-xs font-bold ${getStatusColor(selectedAnomaly.status)}`}>
                  {selectedAnomaly.status.toUpperCase()}
                </span>
              </div>

              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={selectedAnomaly.chart}>
                  <defs>
                    <linearGradient id="colorCost" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="colorBaseline" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.2} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,200,255,0.1)" />
                  <XAxis dataKey="date" stroke="rgba(200,200,200,0.6)" />
                  <YAxis stroke="rgba(200,200,200,0.6)" />
                  <Tooltip contentStyle={{ backgroundColor: 'rgba(15,23,42,0.9)', border: '1px solid rgba(100,200,255,0.3)' }} />
                  <Area type="monotone" dataKey="baseline" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#colorBaseline)" name="Baseline" />
                  <Area type="monotone" dataKey="cost" stroke="#ef4444" strokeWidth={2} fillOpacity={1} fill="url(#colorCost)" name="Actual Cost" />
                </AreaChart>
              </ResponsiveContainer>

              <div className="mt-4 p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
                <p className="text-sm text-red-300 font-medium">Anomaly Impact: <span className="font-bold">{selectedAnomaly.impact}</span></p>
              </div>
            </div>

            {/* Detailed Analysis */}
            <div className="grid md:grid-cols-2 gap-4">
              <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-xl p-6">
                <div className="flex items-center gap-3 mb-3">
                  <Zap className="w-5 h-5 text-yellow-400" />
                  <h4 className="font-bold text-cyan-300">Root Cause Analysis</h4>
                </div>
                <p className="text-sm text-slate-300">{selectedAnomaly.rootCause}</p>
              </div>

              <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-xl p-6">
                <div className="flex items-center gap-3 mb-3">
                  <TrendingUp className="w-5 h-5 text-emerald-400" />
                  <h4 className="font-bold text-cyan-300">Recommendation</h4>
                </div>
                <p className="text-sm text-slate-300">{selectedAnomaly.recommendation}</p>
              </div>

              <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-xl p-6">
                <div className="flex items-center gap-3 mb-3">
                  <DollarSign className="w-5 h-5 text-emerald-400" />
                  <h4 className="font-bold text-cyan-300">Affected Resource</h4>
                </div>
                <p className="text-sm text-slate-300">{selectedAnomaly.resource}</p>
              </div>

              <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-xl p-6">
                <div className="flex items-center gap-3 mb-3">
                  <Clock className="w-5 h-5 text-blue-400" />
                  <h4 className="font-bold text-cyan-300">Detected</h4>
                </div>
                <p className="text-sm text-slate-300">{selectedAnomaly.detectedAt}</p>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-2">
              <button className="px-6 py-3 bg-emerald-500/20 text-emerald-400 rounded-lg font-medium hover:bg-emerald-500/30 transition">
                Acknowledge
              </button>
              <button className="px-6 py-3 bg-cyan-500/20 text-cyan-400 rounded-lg font-medium hover:bg-cyan-500/30 transition">
                View Details
              </button>
              <button className="px-6 py-3 bg-slate-600/20 text-slate-400 rounded-lg font-medium hover:bg-slate-600/30 transition">
                Dismiss
              </button>
            </div>
          </div>

          {/* Anomalies List */}
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-6 h-fit">
            <h3 className="text-lg font-bold text-cyan-300 mb-4">All Anomalies</h3>
            <div className="space-y-3">
              {anomalies.map(anomaly => (
                <button
                  key={anomaly.id}
                  onClick={() => setSelectedAnomaly(anomaly)}
                  className={`w-full text-left p-4 rounded-lg border transition ${
                    selectedAnomaly.id === anomaly.id
                      ? `backdrop-blur-xl bg-gradient-to-br ${getSeverityColor(anomaly.severity)} border-current`
                      : 'bg-slate-800/30 border-slate-700/50 hover:border-slate-600/50'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <AlertTriangle className={`w-4 h-4 flex-shrink-0 mt-0.5 ${
                      anomaly.severity === 'critical' ? 'text-red-400'
                        : anomaly.severity === 'high' ? 'text-orange-400'
                        : 'text-yellow-400'
                    }`} />
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-sm text-slate-300">{anomaly.title}</p>
                      <p className="text-xs text-slate-400 mt-1">{anomaly.detectedAt}</p>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
