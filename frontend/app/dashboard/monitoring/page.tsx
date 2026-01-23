'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { LineChart, Line, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { AlertTriangle, TrendingUp, Zap } from 'lucide-react'

const costTimelineData = [
  { time: '12 AM', cost: 120, anomaly: false },
  { time: '1 AM', cost: 118, anomaly: false },
  { time: '2 AM', cost: 115, anomaly: false },
  { time: '3 AM', cost: 112, anomaly: false },
  { time: '4 AM', cost: 125, anomaly: true },
  { time: '5 AM', cost: 148, anomaly: true },
  { time: '6 AM', cost: 165, anomaly: true },
  { time: '7 AM', cost: 142, anomaly: false },
  { time: '8 AM', cost: 158, anomaly: true },
  { time: '9 AM', cost: 172, anomaly: false },
]

const anomalies = [
  {
    id: 1,
    severity: 'high',
    title: 'Unusual Data Egress Detected',
    description: 'S3 bucket experiencing 5x normal outbound traffic',
    cost: '$1,245',
    time: '2 hours ago',
    resource: 'prod-data-bucket'
  },
  {
    id: 2,
    severity: 'high',
    title: 'Possible Crypto-Mining Activity',
    description: 'EC2 instances running at 100% CPU for 4 hours',
    cost: '$892',
    time: '45 mins ago',
    resource: 'ec2-prod-worker-1'
  },
  {
    id: 3,
    severity: 'medium',
    title: 'Abnormal RDS Query Load',
    description: 'Database receiving 10x more queries than baseline',
    cost: '$456',
    time: '30 mins ago',
    resource: 'mysql-prod-db'
  },
]

const costByService = [
  { name: 'Compute', cost: 4520, trend: 12 },
  { name: 'Storage', cost: 2340, trend: -5 },
  { name: 'Database', cost: 1890, trend: 8 },
  { name: 'Network', cost: 980, trend: 3 },
  { name: 'ML/AI', cost: 1240, trend: 45 },
]

export default function MonitoringPage() {
  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 via-purple-600/20 to-pink-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 mb-2">Cost Monitoring & Anomalies</h1>
            <p className="text-muted-foreground">Real-time cost tracking with AI-powered anomaly detection</p>
          </div>
        </div>

        {/* Real-time Metrics */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-cyan-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Today's Spend</p>
            <p className="text-3xl font-bold text-cyan-400 mb-2">$3,842</p>
            <p className="text-xs text-emerald-400">↓ 12% vs yesterday</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-blue-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">This Hour</p>
            <p className="text-3xl font-bold text-blue-400 mb-2">$158</p>
            <p className="text-xs text-slate-400">Current rate</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Weekly Avg</p>
            <p className="text-3xl font-bold text-purple-400 mb-2">$26,540</p>
            <p className="text-xs text-slate-400">7-day average</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-pink-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Anomalies Today</p>
            <p className="text-3xl font-bold text-pink-400 mb-2">3</p>
            <p className="text-xs text-slate-400">Detected & flagged</p>
          </div>
        </div>

        {/* Cost Timeline with Anomalies */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-cyan-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">24-Hour Cost Timeline</h2>
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart data={costTimelineData}>
              <defs>
                <linearGradient id="colorCost" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#06b6d4" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,200,255,0.1)" />
              <XAxis dataKey="time" stroke="rgba(200,200,200,0.6)" />
              <YAxis stroke="rgba(200,200,200,0.6)" />
              <Tooltip contentStyle={{ backgroundColor: 'rgba(15,23,42,0.9)', border: '1px solid rgba(100,200,255,0.3)' }} />
              <Area type="monotone" dataKey="cost" stroke="#06b6d4" fillOpacity={1} fill="url(#colorCost)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Anomalies List */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-red-500/20 rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-6">
            <AlertTriangle className="w-6 h-6 text-red-400" />
            <h2 className="text-xl font-bold text-red-300">Active Anomalies</h2>
          </div>
          <div className="space-y-4">
            {anomalies.map(anomaly => (
              <div key={anomaly.id} className="backdrop-blur-sm bg-gradient-to-r from-red-950/30 to-pink-950/30 border border-red-500/30 rounded-lg p-4 hover:border-red-400/50 transition-all">
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <h3 className="font-bold text-red-300">{anomaly.title}</h3>
                    <p className="text-sm text-slate-400 mt-1">{anomaly.description}</p>
                  </div>
                  <span className={`text-xs font-bold px-3 py-1 rounded-full ${
                    anomaly.severity === 'high'
                      ? 'bg-red-500/20 text-red-300'
                      : 'bg-yellow-500/20 text-yellow-300'
                  }`}>
                    {anomaly.severity.toUpperCase()}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <div className="flex gap-4">
                    <span className="text-slate-400">Resource: <span className="text-slate-300 font-mono">{anomaly.resource}</span></span>
                    <span className="text-slate-400">{anomaly.time}</span>
                  </div>
                  <span className="font-bold text-red-400">{anomaly.cost}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Cost by Service */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">Cost Breakdown by Service</h2>
          <div className="grid md:grid-cols-2 gap-6">
            <div className="space-y-3">
              {costByService.map(service => (
                <div key={service.name} className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg border border-slate-700/50">
                  <span className="font-medium">{service.name}</span>
                  <div className="flex items-center gap-4">
                    <span className="text-cyan-400 font-bold">${service.cost.toLocaleString()}</span>
                    <span className={`text-sm font-semibold ${service.trend > 0 ? 'text-red-400' : 'text-emerald-400'}`}>
                      {service.trend > 0 ? '+' : ''}{service.trend}%
                    </span>
                  </div>
                </div>
              ))}
            </div>
            <div className="backdrop-blur-sm bg-gradient-to-br from-slate-800/40 to-slate-900/40 border border-slate-700/50 rounded-lg p-4">
              <p className="text-sm text-slate-400 mb-2">Total Daily Spend</p>
              <p className="text-3xl font-bold text-cyan-400 mb-4">$10,970</p>
              <p className="text-sm text-slate-400">ML/AI shows highest growth at 45% week-over-week</p>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
