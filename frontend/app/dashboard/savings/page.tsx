'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { BarChart, Bar, LineChart, Line, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { TrendingUp, Award, Target } from 'lucide-react'

const monthlySavings = [
  { month: 'Jan', amount: 12450, target: 10000 },
  { month: 'Feb', amount: 15230, target: 10000 },
  { month: 'Mar', amount: 18900, target: 15000 },
  { month: 'Apr', amount: 21340, target: 15000 },
  { month: 'May', amount: 24560, target: 20000 },
  { month: 'Jun', amount: 27890, target: 20000 },
]

const savingsByCategory = [
  { name: 'Compute', value: 45000, color: '#10b981' },
  { name: 'Storage', value: 28000, color: '#3b82f6' },
  { name: 'Database', value: 18500, color: '#a855f7' },
  { name: 'Network', value: 12300, color: '#f59e0b' },
  { name: 'Other', value: 6200, color: '#ec4899' },
]

const savingsByCloud = [
  { cloud: 'AWS', savings: 89000, actions: 1245 },
  { cloud: 'Azure', savings: 45600, actions: 687 },
  { cloud: 'GCP', savings: 23400, actions: 432 },
]

const topTeams = [
  { team: 'Platform Engineering', savings: 45000, badge: 'Gold' },
  { team: 'Data Science', savings: 32500, badge: 'Silver' },
  { team: 'DevOps', savings: 28900, badge: 'Silver' },
  { team: 'Backend Services', savings: 18200, badge: 'Bronze' },
  { team: 'ML Infrastructure', savings: 12340, badge: 'Bronze' },
]

export default function SavingsPage() {
  const totalSavings = monthlySavings[monthlySavings.length - 1]?.amount || 0
  const ytdSavings = monthlySavings.reduce((sum, m) => sum + m.amount, 0)

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-emerald-600/20 via-teal-600/20 to-cyan-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-teal-400 to-cyan-400 mb-2">Savings Analytics</h1>
            <p className="text-muted-foreground">Track and optimize your FinOps achievements</p>
          </div>
        </div>

        {/* Key Metrics */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-emerald-950/40 to-teal-950/40 border border-emerald-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">This Month</p>
            <p className="text-3xl font-bold text-emerald-400">${(totalSavings / 1000).toFixed(1)}K</p>
            <p className="text-xs text-emerald-300 mt-2">↑ 14% vs last month</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-teal-950/40 to-cyan-950/40 border border-teal-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">YTD Savings</p>
            <p className="text-3xl font-bold text-teal-400">${(ytdSavings / 1000).toFixed(1)}K</p>
            <p className="text-xs text-teal-300 mt-2">6-month total</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-cyan-950/40 to-blue-950/40 border border-cyan-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Annual Projection</p>
            <p className="text-3xl font-bold text-cyan-400">${(ytdSavings * 2 / 1000).toFixed(1)}K</p>
            <p className="text-xs text-cyan-300 mt-2">Extrapolated</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-blue-950/40 to-indigo-950/40 border border-blue-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">YTD Target</p>
            <p className="text-3xl font-bold text-blue-400">110%</p>
            <p className="text-xs text-blue-300 mt-2">Achievement</p>
          </div>
        </div>

        {/* Monthly Trend */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-emerald-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">Monthly Savings Trend vs Target</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={monthlySavings}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,200,255,0.1)" />
              <XAxis dataKey="month" stroke="rgba(200,200,200,0.6)" />
              <YAxis stroke="rgba(200,200,200,0.6)" />
              <Tooltip contentStyle={{ backgroundColor: 'rgba(15,23,42,0.9)', border: '1px solid rgba(100,200,255,0.3)' }} />
              <Legend />
              <Bar dataKey="amount" fill="#10b981" name="Actual Savings" />
              <Bar dataKey="target" fill="#6b7280" name="Target" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Savings Breakdown */}
        <div className="grid md:grid-cols-2 gap-6">
          {/* By Category */}
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl p-6">
            <h2 className="text-xl font-bold text-cyan-300 mb-4">Savings by Category</h2>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={savingsByCategory} cx="50%" cy="50%" labelLine={false} label={({ name, value }) => `${name}: $${(value/1000).toFixed(0)}K`} outerRadius={100} fill="#8884d8" dataKey="value">
                  {savingsByCategory.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => `$${value.toLocaleString()}`} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          {/* By Cloud Provider */}
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-blue-500/20 rounded-2xl p-6">
            <h2 className="text-xl font-bold text-cyan-300 mb-4">Savings by Cloud Provider</h2>
            <div className="space-y-4">
              {savingsByCloud.map(cloud => (
                <div key={cloud.cloud} className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                  <div className="flex items-center justify-between mb-2">
                    <h3 className="font-bold text-cyan-300">{cloud.cloud}</h3>
                    <span className="text-2xl font-bold text-emerald-400">${(cloud.savings / 1000).toFixed(1)}K</span>
                  </div>
                  <div className="w-full h-2 bg-slate-700/50 rounded-full overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-emerald-500 to-cyan-500" style={{ width: `${(cloud.savings / 89000) * 100}%` }}></div>
                  </div>
                  <p className="text-xs text-slate-400 mt-2">{cloud.actions} optimization actions</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Team Leaderboard */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-yellow-500/20 rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-6">
            <Award className="w-6 h-6 text-yellow-400" />
            <h2 className="text-xl font-bold text-cyan-300">Team Leaderboard</h2>
          </div>
          <div className="space-y-3">
            {topTeams.map((team, idx) => (
              <div key={team.team} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50 hover:border-yellow-500/30 transition">
                <div className="flex items-center gap-4">
                  <div className="w-8 h-8 bg-gradient-to-br from-yellow-500 to-orange-500 rounded-full flex items-center justify-center font-bold text-white">
                    {idx + 1}
                  </div>
                  <div>
                    <p className="font-bold text-cyan-300">{team.team}</p>
                    <p className="text-xs text-slate-400">Total contributions</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-2xl font-bold text-emerald-400">${(team.savings / 1000).toFixed(1)}K</span>
                  <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                    team.badge === 'Gold' ? 'bg-yellow-500/20 text-yellow-300'
                      : team.badge === 'Silver' ? 'bg-slate-400/20 text-slate-300'
                      : 'bg-orange-500/20 text-orange-300'
                  }`}>
                    {team.badge}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Achievements */}
        <div className="grid md:grid-cols-3 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-yellow-950/40 to-orange-950/40 border border-yellow-500/20 rounded-xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <Target className="w-6 h-6 text-yellow-400" />
              <h3 className="font-bold text-yellow-300">Monthly Goal Achieved</h3>
            </div>
            <p className="text-3xl font-bold text-yellow-400 mb-2">12/12</p>
            <p className="text-sm text-slate-400">Consecutive months hitting targets</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-purple-950/40 to-pink-950/40 border border-purple-500/20 rounded-xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <TrendingUp className="w-6 h-6 text-purple-400" />
              <h3 className="font-bold text-purple-300">Best Month</h3>
            </div>
            <p className="text-3xl font-bold text-purple-400 mb-2">June</p>
            <p className="text-sm text-slate-400">$27,890 in optimizations</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-blue-950/40 to-cyan-950/40 border border-blue-500/20 rounded-xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <Award className="w-6 h-6 text-blue-400" />
              <h3 className="font-bold text-blue-300">Impact Score</h3>
            </div>
            <p className="text-3xl font-bold text-blue-400 mb-2">98/100</p>
            <p className="text-sm text-slate-400">Based on consistency & growth</p>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
