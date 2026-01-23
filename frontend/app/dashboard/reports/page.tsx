'use client'

import { useState, useEffect } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { FileText, Download, Calendar, Filter } from 'lucide-react'
import { getFinanceReports, FinancialReportDTO } from '@/lib/api'

const reports = [
  {
    id: 1,
    title: 'Executive Monthly Summary',
    description: 'High-level cost overview and key metrics for leadership',
    generated: '2 days ago',
    format: 'PDF',
    size: '2.4 MB',
    type: 'monthly',
    tags: ['Executive', 'Summary']
  },
  {
    id: 2,
    title: 'Multi-Cloud Cost Analysis',
    description: 'Detailed comparison of costs across AWS, Azure, and GCP',
    generated: '1 day ago',
    format: 'PDF',
    size: '5.2 MB',
    type: 'analysis',
    tags: ['Multi-Cloud', 'Analysis']
  },
  {
    id: 3,
    title: 'Compliance & Billing Report',
    description: 'Finance-ready report with billing allocations and compliance data',
    generated: '4 hours ago',
    format: 'CSV',
    size: '1.8 MB',
    type: 'compliance',
    tags: ['Compliance', 'Finance']
  },
  {
    id: 4,
    title: 'Team Attribution Report',
    description: 'Cost breakdown by team and project with detailed attribution',
    generated: '1 week ago',
    format: 'Excel',
    size: '3.1 MB',
    type: 'attribution',
    tags: ['Teams', 'Attribution']
  },
  {
    id: 5,
    title: 'RI & Savings Opportunities',
    description: 'Recommendations for Reserved Instances and optimization opportunities',
    generated: '3 days ago',
    format: 'PDF',
    size: '2.8 MB',
    type: 'recommendations',
    tags: ['Savings', 'RI']
  },
]

const scheduledReports = [
  {
    id: 1,
    title: 'Weekly Cost Summary',
    schedule: 'Every Monday 9 AM',
    recipients: 'finance@company.com, devops-team@company.com',
    nextRun: '2 days',
    status: 'active'
  },
  {
    id: 2,
    title: 'Monthly Executive Report',
    schedule: 'First Friday of month',
    recipients: 'cfo@company.com, cto@company.com',
    nextRun: '5 days',
    status: 'active'
  },
  {
    id: 3,
    title: 'Team Billing Report',
    schedule: 'Monthly on 15th',
    recipients: 'team-leads@company.com',
    nextRun: '8 days',
    status: 'active'
  },
]

export default function ReportsPage() {
  const [filter, setFilter] = useState('all')
  const [reportsList, setReportsList] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function fetchReports() {
      try {
        const data = await getFinanceReports()
        const mapped = (data || []).map((r: FinancialReportDTO, idx: number) => ({
          id: idx,
          title: `${r.period} Financial Summary`,
          description: `Automatic cost analysis report generated for ${r.period} period.`,
          generated: new Date(r.createdAt).toLocaleDateString(),
          format: 'PDF',
          size: '1.2 MB',
          type: r.period.toLowerCase(),
          tags: [r.source || 'ML']
        }))
        setReportsList(mapped)
      } catch (error) {
        console.error('Failed to fetch reports:', error)
      } finally {
        setLoading(false)
      }
    }
    fetchReports()
  }, [])

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 via-indigo-600/20 to-purple-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-400 mb-2">Reports Hub</h1>
            <p className="text-muted-foreground">Generate, download, and manage FinOps reports</p>
          </div>
        </div>

        {/* Recent Reports */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold text-cyan-300">Recent Reports</h2>
            <button className="px-4 py-2 bg-gradient-to-r from-blue-500 to-indigo-500 text-white rounded-lg font-medium hover:from-blue-600 hover:to-indigo-600 transition">
              Generate New Report
            </button>
          </div>

          <div className="grid gap-4">
            {reportsList.map((report: any) => (
              <div key={report.id} className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-blue-500/20 rounded-xl p-6 hover:border-blue-400/50 transition-all">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <FileText className="w-5 h-5 text-blue-400" />
                      <h3 className="text-lg font-bold text-cyan-300">{report.title}</h3>
                    </div>
                    <p className="text-sm text-slate-400">{report.description}</p>
                  </div>
                  <span className="px-3 py-1 bg-blue-500/20 text-blue-300 rounded-full text-xs font-bold whitespace-nowrap ml-4">{report.format}</span>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-6 text-sm">
                    <span className="text-slate-400">Generated: <span className="text-slate-300">{report.generated}</span></span>
                    <span className="text-slate-400">Size: <span className="text-slate-300">{report.size}</span></span>
                  </div>
                  <button className="px-4 py-2 bg-gradient-to-r from-emerald-500 to-teal-500 text-white rounded-lg font-medium hover:from-emerald-600 hover:to-teal-600 transition flex items-center gap-2">
                    <Download className="w-4 h-4" />
                    Download
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Scheduled Reports */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold text-cyan-300">Scheduled Reports</h2>
            <button className="px-4 py-2 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-lg font-medium hover:from-purple-600 hover:to-pink-600 transition">
              Create Schedule
            </button>
          </div>

          <div className="grid gap-4">
            {scheduledReports.map(report => (
              <div key={report.id} className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-xl p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <h3 className="text-lg font-bold text-cyan-300 mb-2">{report.title}</h3>
                    <div className="flex items-center gap-4 text-sm">
                      <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4 text-slate-400" />
                        <span className="text-slate-400">{report.schedule}</span>
                      </div>
                      <span className="px-2 py-1 bg-emerald-500/20 text-emerald-300 rounded text-xs font-bold">{report.status.toUpperCase()}</span>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-slate-400">Next run in</p>
                    <p className="text-xl font-bold text-purple-400">{report.nextRun}</p>
                  </div>
                </div>

                <div className="mb-4 p-3 bg-slate-800/30 rounded-lg border border-slate-700/50">
                  <p className="text-sm text-slate-400">Recipients: <span className="text-slate-300 font-mono text-xs">{report.recipients}</span></p>
                </div>

                <div className="flex gap-2">
                  <button className="px-3 py-2 bg-slate-700/30 text-slate-300 rounded-lg text-sm font-medium hover:bg-slate-600/30 transition">Edit</button>
                  <button className="px-3 py-2 bg-slate-700/30 text-slate-300 rounded-lg text-sm font-medium hover:bg-slate-600/30 transition">Send Now</button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Report Builder */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-indigo-500/20 rounded-2xl p-6">
          <h2 className="text-2xl font-bold text-cyan-300 mb-6">Custom Report Builder</h2>
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-semibold text-cyan-300 mb-3">Report Type</label>
              <select className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-white">
                <option>Cost Summary</option>
                <option>Savings Analysis</option>
                <option>Team Attribution</option>
                <option>Compliance Report</option>
                <option>Custom Report</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-semibold text-cyan-300 mb-3">Date Range</label>
              <select className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-white">
                <option>Last 30 Days</option>
                <option>Last 90 Days</option>
                <option>Last 6 Months</option>
                <option>Last Year</option>
                <option>Custom Range</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-semibold text-cyan-300 mb-3">Cloud Providers</label>
              <div className="flex gap-2">
                {['AWS', 'Azure', 'GCP'].map(cloud => (
                  <label key={cloud} className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" defaultChecked className="rounded" />
                    <span className="text-slate-300 text-sm">{cloud}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-cyan-300 mb-3">Format</label>
              <select className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-white">
                <option>PDF</option>
                <option>Excel</option>
                <option>CSV</option>
                <option>JSON</option>
              </select>
            </div>
          </div>

          <button className="mt-6 px-6 py-3 bg-gradient-to-r from-indigo-500 to-purple-500 text-white rounded-lg font-bold hover:from-indigo-600 hover:to-purple-600 transition">
            Generate Report
          </button>
        </div>
      </div>
    </DashboardLayout>
  )
}
