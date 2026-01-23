'use client'

import { useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { TrendingUp, DollarSign, Zap, AlertTriangle } from 'lucide-react'

const pricingData = [
  { instance: 't3.medium', aws: 0.0416, azure: 0.039, gcp: 0.035, region: 'us-east-1' },
  { instance: 't3.large', aws: 0.0832, azure: 0.078, gcp: 0.070, region: 'us-east-1' },
  { instance: 't3.xlarge', aws: 0.1664, azure: 0.156, gcp: 0.140, region: 'us-east-1' },
  { instance: 'm5.large', aws: 0.096, azure: 0.091, gcp: 0.082, region: 'us-east-1' },
  { instance: 'm5.xlarge', aws: 0.192, azure: 0.182, gcp: 0.164, region: 'us-east-1' },
  { instance: 'm5.2xlarge', aws: 0.384, azure: 0.364, gcp: 0.328, region: 'us-east-1' },
]

const regionData = [
  { name: 'us-east-1', pricing: 1.0, latency: 0, availability: 99.99 },
  { name: 'us-west-2', pricing: 1.05, latency: 15, availability: 99.99 },
  { name: 'eu-west-1', pricing: 1.15, latency: 80, availability: 99.99 },
  { name: 'ap-southeast-1', pricing: 1.2, latency: 150, availability: 99.99 },
]

export default function PricingPage() {
  const [selectedCloud, setSelectedCloud] = useState('aws')
  const [selectedRegion, setSelectedRegion] = useState('us-east-1')

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 via-purple-600/20 to-cyan-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-purple-400 to-cyan-400 mb-2">Pricing Intelligence</h1>
            <p className="text-muted-foreground">Multi-cloud instance pricing comparison and cost optimization</p>
          </div>
        </div>

        {/* Cloud & Region Selector */}
        <div className="grid md:grid-cols-2 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-blue-950/40 to-purple-950/40 border border-blue-500/20 rounded-2xl p-6">
            <label className="block text-sm font-semibold mb-3 text-cyan-300">Cloud Provider</label>
            <div className="flex gap-2">
              {['aws', 'azure', 'gcp'].map(cloud => (
                <button
                  key={cloud}
                  onClick={() => setSelectedCloud(cloud)}
                  className={`px-4 py-2 rounded-lg font-medium transition-all ${
                    selectedCloud === cloud
                      ? 'bg-gradient-to-r from-blue-500 to-cyan-500 text-white'
                      : 'bg-slate-700/30 text-slate-300 hover:bg-slate-600/30'
                  }`}
                >
                  {cloud.toUpperCase()}
                </button>
              ))}
            </div>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-purple-950/40 to-pink-950/40 border border-purple-500/20 rounded-2xl p-6">
            <label className="block text-sm font-semibold mb-3 text-cyan-300">Region</label>
            <select
              value={selectedRegion}
              onChange={(e) => setSelectedRegion(e.target.value)}
              className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-white"
            >
              {regionData.map(region => (
                <option key={region.name} value={region.name}>{region.name}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Instance Pricing Cards */}
        <div className="grid md:grid-cols-3 gap-4">
          {pricingData.slice(0, 6).map(item => (
            <div key={item.instance} className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-emerald-500/20 rounded-xl p-4 hover:border-emerald-400/50 transition-all">
              <h3 className="font-bold text-lg text-cyan-300 mb-3">{item.instance}</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate-400">AWS:</span>
                  <span className="text-emerald-400 font-semibold">${item.aws}/hr</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Azure:</span>
                  <span className="text-blue-400 font-semibold">${item.azure}/hr</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">GCP:</span>
                  <span className="text-purple-400 font-semibold">${item.gcp}/hr</span>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Pricing Comparison Chart */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-cyan-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">Price Comparison by Instance Type</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={pricingData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,200,255,0.1)" />
              <XAxis dataKey="instance" stroke="rgba(200,200,200,0.6)" />
              <YAxis stroke="rgba(200,200,200,0.6)" />
              <Tooltip contentStyle={{ backgroundColor: 'rgba(15,23,42,0.9)', border: '1px solid rgba(100,200,255,0.3)' }} />
              <Legend />
              <Bar dataKey="aws" fill="#10b981" />
              <Bar dataKey="azure" fill="#3b82f6" />
              <Bar dataKey="gcp" fill="#a855f7" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Region Analysis */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">Regional Pricing & Latency Analysis</h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={regionData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,200,255,0.1)" />
              <XAxis dataKey="name" stroke="rgba(200,200,200,0.6)" />
              <YAxis stroke="rgba(200,200,200,0.6)" />
              <Tooltip contentStyle={{ backgroundColor: 'rgba(15,23,42,0.9)', border: '1px solid rgba(100,200,255,0.3)' }} />
              <Legend />
              <Line type="monotone" dataKey="pricing" stroke="#3b82f6" strokeWidth={2} />
              <Line type="monotone" dataKey="latency" stroke="#ef4444" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Cost Optimization Insights */}
        <div className="grid md:grid-cols-2 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-emerald-950/40 to-teal-950/40 border border-emerald-500/20 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-emerald-500 to-teal-500 rounded-lg flex items-center justify-center">
                <DollarSign className="w-6 h-6 text-white" />
              </div>
              <div>
                <p className="text-sm text-slate-400">Potential Monthly Savings</p>
                <p className="text-2xl font-bold text-emerald-400">$8,432</p>
              </div>
            </div>
            <p className="text-sm text-slate-400">By switching to GCP for compute workloads in ap-southeast-1</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-blue-950/40 to-cyan-950/40 border border-blue-500/20 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-lg flex items-center justify-center">
                <TrendingUp className="w-6 h-6 text-white" />
              </div>
              <div>
                <p className="text-sm text-slate-400">Best Current Price</p>
                <p className="text-2xl font-bold text-cyan-400">GCP us-east-1</p>
              </div>
            </div>
            <p className="text-sm text-slate-400">$0.035/hr - 16% cheaper than AWS equivalent</p>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
