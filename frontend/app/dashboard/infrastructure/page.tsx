'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { HardDrive, Database, Cpu, Globe, Zap, AlertCircle } from 'lucide-react'
import { useState } from 'react'

const resources = [
  {
    id: 'ec2-prod-web-01',
    type: 'compute',
    service: 'EC2',
    region: 'us-east-1',
    cost: '$245',
    status: 'optimized',
    utilization: 42,
    recommendation: 'Right-sized'
  },
  {
    id: 'ec2-prod-web-02',
    type: 'compute',
    service: 'EC2',
    region: 'us-east-1',
    cost: '$156',
    status: 'underutilized',
    utilization: 8,
    recommendation: 'Downsize recommended'
  },
  {
    id: 'rds-prod-mysql',
    type: 'database',
    service: 'RDS (MySQL)',
    region: 'us-east-1',
    cost: '$890',
    status: 'optimized',
    utilization: 68,
    recommendation: 'Monitor connections'
  },
  {
    id: 's3-app-data',
    type: 'storage',
    service: 'S3',
    region: 'us-east-1',
    cost: '$340',
    status: 'warning',
    utilization: 45,
    recommendation: 'Archive cold data'
  },
  {
    id: 'ebs-data-volume',
    type: 'storage',
    service: 'EBS',
    region: 'us-east-1',
    cost: '$120',
    status: 'optimized',
    utilization: 72,
    recommendation: 'Well-configured'
  },
  {
    id: 'lambda-api',
    type: 'compute',
    service: 'Lambda',
    region: 'us-east-1',
    cost: '$45',
    status: 'optimized',
    utilization: 34,
    recommendation: 'Efficient'
  },
  {
    id: 'ec2-prod-api-01',
    type: 'compute',
    service: 'EC2',
    region: 'eu-west-1',
    cost: '$220',
    status: 'optimized',
    utilization: 55,
    recommendation: 'Right-sized'
  },
  {
    id: 'dynamodb-sessions',
    type: 'database',
    service: 'DynamoDB',
    region: 'eu-west-1',
    cost: '$280',
    status: 'optimized',
    utilization: 61,
    recommendation: 'Auto-scaling active'
  },
  {
    id: 'ec2-prod-batch',
    type: 'compute',
    service: 'EC2 Spot',
    region: 'us-west-2',
    cost: '$78',
    status: 'optimized',
    utilization: 89,
    recommendation: 'Spot utilization optimal'
  },
  {
    id: 's3-backups',
    type: 'storage',
    service: 'S3 Glacier',
    region: 'us-west-2',
    cost: '$52',
    status: 'optimized',
    utilization: 100,
    recommendation: 'Archive tier optimal'
  },
]

const groupedByRegion = resources.reduce((acc, resource) => {
  if (!acc[resource.region]) {
    acc[resource.region] = []
  }
  acc[resource.region].push(resource)
  return acc
}, {} as Record<string, typeof resources>)

const getIcon = (type: string) => {
  switch (type) {
    case 'compute':
      return Cpu
    case 'database':
      return Database
    case 'storage':
      return HardDrive
    default:
      return Zap
  }
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'optimized':
      return 'text-emerald-400 bg-emerald-500/20'
    case 'warning':
      return 'text-yellow-400 bg-yellow-500/20'
    case 'underutilized':
      return 'text-orange-400 bg-orange-500/20'
    case 'critical':
      return 'text-red-400 bg-red-500/20'
    default:
      return 'text-slate-400 bg-slate-500/20'
  }
}

export default function InfrastructurePage() {
  const [selectedRegion, setSelectedRegion] = useState<string | null>(null)
  const [expandedResource, setExpandedResource] = useState<string | null>(null)

  const regions = Object.keys(groupedByRegion).sort()
  const displayRegion = selectedRegion || regions[0]

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-slate-600/20 via-slate-600/20 to-gray-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <div className="flex items-center gap-3 mb-2">
              <Globe className="w-8 h-8 text-cyan-400" />
              <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-slate-300 via-cyan-300 to-blue-300">Infrastructure Inventory</h1>
            </div>
            <p className="text-muted-foreground">View all cloud resources, costs, and optimization status by region</p>
          </div>
        </div>

        {/* Summary Stats */}
        <div className="grid md:grid-cols-5 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-cyan-950/40 to-blue-950/40 border border-cyan-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Total Resources</p>
            <p className="text-3xl font-bold text-cyan-400">{resources.length}</p>
            <p className="text-xs text-cyan-300 mt-2">Across all regions</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-emerald-950/40 to-teal-950/40 border border-emerald-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Optimized</p>
            <p className="text-3xl font-bold text-emerald-400">{resources.filter(r => r.status === 'optimized').length}</p>
            <p className="text-xs text-emerald-300 mt-2">Well configured</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-yellow-950/40 to-orange-950/40 border border-yellow-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Warnings</p>
            <p className="text-3xl font-bold text-yellow-400">{resources.filter(r => r.status === 'warning').length}</p>
            <p className="text-xs text-yellow-300 mt-2">Need attention</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-orange-950/40 to-red-950/40 border border-orange-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Underutilized</p>
            <p className="text-3xl font-bold text-orange-400">{resources.filter(r => r.status === 'underutilized').length}</p>
            <p className="text-xs text-orange-300 mt-2">Can optimize</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-purple-950/40 to-pink-950/40 border border-purple-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Monthly Cost</p>
            <p className="text-3xl font-bold text-purple-400">$3,320</p>
            <p className="text-xs text-purple-300 mt-2">All infrastructure</p>
          </div>
        </div>

        {/* Region Selector */}
        <div className="flex gap-2 flex-wrap">
          {regions.map(region => (
            <button
              key={region}
              onClick={() => setSelectedRegion(selectedRegion === region ? null : region)}
              className={`px-4 py-2 rounded-lg font-medium transition ${
                (selectedRegion || regions[0]) === region
                  ? 'bg-gradient-to-r from-cyan-500 to-blue-500 text-white'
                  : 'bg-slate-700/30 text-slate-300 hover:bg-slate-600/30'
              }`}
            >
              {region}
            </button>
          ))}
        </div>

        {/* Resources by Region */}
        <div className="space-y-4">
          {groupedByRegion[displayRegion]?.map(resource => {
            const Icon = getIcon(resource.type)
            return (
              <div
                key={resource.id}
                onClick={() => setExpandedResource(expandedResource === resource.id ? null : resource.id)}
                className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 hover:border-cyan-500/30 rounded-xl p-6 cursor-pointer transition-all"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1 flex items-start gap-4">
                    <div className="w-12 h-12 rounded-lg bg-slate-800/50 border border-slate-700/50 flex items-center justify-center flex-shrink-0">
                      <Icon className="w-6 h-6 text-cyan-400" />
                    </div>

                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-bold text-cyan-300 font-mono">{resource.id}</h3>
                        <span className={`px-2 py-1 rounded-full text-xs font-bold ${getStatusColor(resource.status)}`}>
                          {resource.status.toUpperCase()}
                        </span>
                      </div>

                      <div className="grid md:grid-cols-4 gap-3 text-sm">
                        <div className="text-slate-400">
                          Service: <span className="text-slate-300">{resource.service}</span>
                        </div>
                        <div className="text-slate-400">
                          Region: <span className="text-slate-300">{resource.region}</span>
                        </div>
                        <div className="text-slate-400">
                          Utilization: <span className="text-cyan-300">{resource.utilization}%</span>
                        </div>
                        <div className="text-slate-400">
                          Recommendation: <span className="text-yellow-300">{resource.recommendation}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="text-right ml-4">
                    <p className="text-2xl font-bold text-emerald-400 mb-2">{resource.cost}</p>
                    <p className="text-xs text-slate-400">/month</p>
                  </div>
                </div>

                {/* Utilization Bar */}
                <div className="mb-4">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs text-slate-400">CPU/Memory Utilization</span>
                    <span className="text-xs font-mono text-cyan-300">{resource.utilization}%</span>
                  </div>
                  <div className="w-full h-2 bg-slate-700/50 rounded-full overflow-hidden">
                    <div
                      className={`h-full transition-all ${
                        resource.utilization > 80
                          ? 'bg-gradient-to-r from-emerald-500 to-cyan-500'
                          : resource.utilization > 50
                          ? 'bg-gradient-to-r from-yellow-500 to-orange-500'
                          : 'bg-gradient-to-r from-blue-500 to-cyan-500'
                      }`}
                      style={{ width: `${resource.utilization}%` }}
                    ></div>
                  </div>
                </div>

                {/* Expanded Details */}
                {expandedResource === resource.id && (
                  <div className="mt-6 pt-6 border-t border-slate-700/50 space-y-4">
                    <div className="grid md:grid-cols-3 gap-4">
                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">Resource ID</p>
                        <p className="text-sm font-mono text-cyan-300">{resource.id}</p>
                      </div>
                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">Type</p>
                        <p className="text-sm text-slate-300">{resource.type.toUpperCase()}</p>
                      </div>
                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">Last Updated</p>
                        <p className="text-sm text-slate-300">2 minutes ago</p>
                      </div>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">Optimization Opportunity</p>
                      <p className="text-sm text-yellow-300">{resource.recommendation}</p>
                    </div>

                    <div className="flex gap-2">
                      {resource.status !== 'optimized' && (
                        <button className="px-4 py-2 bg-cyan-500/20 text-cyan-400 rounded-lg font-medium hover:bg-cyan-500/30 transition">
                          Optimize Now
                        </button>
                      )}
                      <button className="px-4 py-2 bg-slate-600/20 text-slate-400 rounded-lg font-medium hover:bg-slate-600/30 transition">
                        View Details
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>
    </DashboardLayout>
  )
}
