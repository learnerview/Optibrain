'use client'

import { useEffect, useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getResources, type Resource } from '@/lib/api'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Search, Server, AlertTriangle, CheckCircle, XCircle, TrendingDown } from 'lucide-react'

export default function ResourcesPage() {
    const [resources, setResources] = useState<Resource[]>([])
    const [loading, setLoading] = useState(true)
    const [searchTerm, setSearchTerm] = useState('')
    const [statusFilter, setStatusFilter] = useState<string>('All')

    useEffect(() => {
        const fetchResources = async () => {
            try {
                setLoading(true)
                const data = await getResources()
                setResources(data)
            } catch (err) {
                console.error('Failed to fetch resources', err)
            } finally {
                setLoading(false)
            }
        }
        fetchResources()
    }, [])

    const filteredResources = resources.filter(resource => {
        const matchesSearch = resource.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            resource.id.toLowerCase().includes(searchTerm.toLowerCase())
        const matchesStatus = statusFilter === 'All' || resource.status === statusFilter
        return matchesSearch && matchesStatus
    })

    const getStatusBadge = (status: string) => {
        switch (status) {
            case 'Running':
                return <Badge className="bg-green-500/20 text-green-400 border-green-500/30"><CheckCircle className="w-3 h-3 mr-1" />Running</Badge>
            case 'Idle':
                return <Badge className="bg-amber-500/20 text-amber-400 border-amber-500/30"><AlertTriangle className="w-3 h-3 mr-1" />Idle</Badge>
            case 'Orphaned':
                return <Badge className="bg-red-500/20 text-red-400 border-red-500/30"><XCircle className="w-3 h-3 mr-1" />Orphaned</Badge>
            default:
                return <Badge>{status}</Badge>
        }
    }

    const getTypeIcon = (type: string) => {
        return <Server className="w-4 h-4 text-cyan-400" />
    }

    const totalCost = filteredResources.reduce((sum, r) => sum + r.monthlyCost, 0)
    const optimizationCandidates = filteredResources.filter(r => r.optimizationCandidate).length

    return (
        <DashboardLayout>
            <div className="space-y-6">
                {/* Header */}
                <div>
                    <h1 className="text-3xl font-bold text-foreground mb-2">Resource Inventory</h1>
                    <p className="text-muted-foreground">Track and manage all cloud resources</p>
                </div>

                {/* Summary Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Total Resources</p>
                        <p className="text-3xl font-bold text-foreground">{filteredResources.length}</p>
                    </Card>
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Monthly Cost</p>
                        <p className="text-3xl font-bold text-foreground">₹{(totalCost / 1000).toFixed(1)}K</p>
                    </Card>
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Optimization Candidates</p>
                        <p className="text-3xl font-bold text-amber-400">{optimizationCandidates}</p>
                    </Card>
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Potential Savings</p>
                        <p className="text-3xl font-bold text-green-400">₹13K</p>
                    </Card>
                </div>

                {/* Filters */}
                <Card className="glass border-border/50 p-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {/* Search */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-muted-foreground mb-2">
                                <Search className="w-4 h-4" />
                                Search Resources
                            </label>
                            <input
                                type="text"
                                placeholder="Search by name or ID..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-cyan-500"
                            />
                        </div>

                        {/* Status Filter */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-muted-foreground mb-2">
                                <Server className="w-4 h-4" />
                                Filter by Status
                            </label>
                            <select
                                value={statusFilter}
                                onChange={(e) => setStatusFilter(e.target.value)}
                                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-cyan-500"
                            >
                                <option>All</option>
                                <option>Running</option>
                                <option>Idle</option>
                                <option>Orphaned</option>
                            </select>
                        </div>
                    </div>
                </Card>

                {/* Resource Table */}
                <Card className="glass border-border/50 overflow-hidden">
                    {loading ? (
                        <div className="animate-pulse p-6">
                            <div className="h-64 bg-slate-800 rounded"></div>
                        </div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-slate-800/50 border-b border-slate-700">
                                    <tr>
                                        <th className="px-6 py-4 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Resource</th>
                                        <th className="px-6 py-4 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Type</th>
                                        <th className="px-6 py-4 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Status</th>
                                        <th className="px-6 py-4 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Region</th>
                                        <th className="px-6 py-4 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Utilization</th>
                                        <th className="px-6 py-4 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Monthly Cost</th>
                                        <th className="px-6 py-4 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Optimization</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-700">
                                    {filteredResources.map((resource) => (
                                        <tr key={resource.id} className="hover:bg-slate-800/30 transition">
                                            <td className="px-6 py-4">
                                                <div>
                                                    <p className="text-sm font-medium text-foreground">{resource.name}</p>
                                                    <p className="text-xs text-muted-foreground">{resource.id}</p>
                                                </div>
                                            </td>
                                            <td className="px-6 py-4">
                                                <div className="flex items-center gap-2">
                                                    {getTypeIcon(resource.type)}
                                                    <span className="text-sm text-foreground">{resource.type}</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-4">
                                                {getStatusBadge(resource.status)}
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className="text-sm text-foreground">{resource.region}</span>
                                            </td>
                                            <td className="px-6 py-4">
                                                <div className="flex items-center gap-2">
                                                    <div className="w-24 h-2 bg-slate-700 rounded-full overflow-hidden">
                                                        <div
                                                            className={`h-full ${resource.utilizationPercent < 30 ? 'bg-red-500' : resource.utilizationPercent < 70 ? 'bg-amber-500' : 'bg-green-500'}`}
                                                            style={{ width: `${resource.utilizationPercent}%` }}
                                                        ></div>
                                                    </div>
                                                    <span className="text-sm text-foreground">{resource.utilizationPercent}%</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className="text-sm font-medium text-foreground">₹{resource.monthlyCost.toLocaleString()}</span>
                                            </td>
                                            <td className="px-6 py-4">
                                                {resource.optimizationCandidate ? (
                                                    <Badge className="bg-cyan-500/20 text-cyan-400 border-cyan-500/30">
                                                        <TrendingDown className="w-3 h-3 mr-1" />
                                                        Candidate
                                                    </Badge>
                                                ) : (
                                                    <span className="text-xs text-muted-foreground">—</span>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </Card>

                {filteredResources.length === 0 && !loading && (
                    <div className="text-center py-12">
                        <p className="text-muted-foreground">No resources found matching your criteria</p>
                    </div>
                )}
            </div>
        </DashboardLayout>
    )
}
