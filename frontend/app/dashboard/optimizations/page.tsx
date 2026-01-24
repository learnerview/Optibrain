'use client'

import { useEffect, useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getOptimizationHistory, type Optimization } from '@/lib/api'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Clock, CheckCircle, AlertCircle, XCircle, TrendingDown, Info } from 'lucide-react'

export default function OptimizationsPage() {
    const [optimizations, setOptimizations] = useState<Optimization[]>([])
    const [loading, setLoading] = useState(true)
    const [statusFilter, setStatusFilter] = useState<string>('All')

    useEffect(() => {
        const fetchOptimizations = async () => {
            try {
                setLoading(true)
                const data = await getOptimizationHistory()
                setOptimizations(data)
            } catch (err) {
                console.error('Failed to fetch optimization history', err)
            } finally {
                setLoading(false)
            }
        }
        fetchOptimizations()
    }, [])

    const filteredOptimizations = optimizations.filter(opt => {
        return statusFilter === 'All' || opt.status === statusFilter
    })

    const getStatusBadge = (status: string) => {
        switch (status) {
            case 'Applied':
                return <Badge className="bg-green-500/20 text-green-400 border-green-500/30"><CheckCircle className="w-3 h-3 mr-1" />Applied</Badge>
            case 'Pending':
                return <Badge className="bg-blue-500/20 text-blue-400 border-blue-500/30"><Clock className="w-3 h-3 mr-1" />Pending</Badge>
            case 'Simulated':
                return <Badge className="bg-amber-500/20 text-amber-400 border-amber-500/30"><Info className="w-3 h-3 mr-1" />Simulated</Badge>
            case 'Rejected':
                return <Badge className="bg-red-500/20 text-red-400 border-red-500/30"><XCircle className="w-3 h-3 mr-1" />Rejected</Badge>
            default:
                return <Badge>{status}</Badge>
        }
    }

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'Applied':
                return <CheckCircle className="w-6 h-6 text-green-400" />
            case 'Pending':
                return <Clock className="w-6 h-6 text-blue-400" />
            case 'Simulated':
                return <Info className="w-6 h-6 text-amber-400" />
            case 'Rejected':
                return <XCircle className="w-6 h-6 text-red-400" />
            default:
                return <AlertCircle className="w-6 h-6 text-gray-400" />
        }
    }

    const totalSavings = filteredOptimizations
        .filter(o => o.status === 'Applied' || o.status === 'Simulated' || o.status === 'Pending')
        .reduce((sum, o) => sum + o.monthlySavings, 0)

    const appliedCount = optimizations.filter(o => o.status === 'Applied').length
    const pendingCount = optimizations.filter(o => o.status === 'Pending').length

    return (
        <DashboardLayout>
            <div className="space-y-6">
                {/* Header */}
                <div>
                    <h1 className="text-3xl font-bold text-foreground mb-2">Optimization History</h1>
                    <p className="text-muted-foreground">Track the lifecycle of all optimization recommendations</p>
                </div>

                {/* Summary Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Total Optimizations</p>
                        <p className="text-3xl font-bold text-foreground">{optimizations.length}</p>
                    </Card>
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Applied</p>
                        <p className="text-3xl font-bold text-green-400">{appliedCount}</p>
                    </Card>
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Pending</p>
                        <p className="text-3xl font-bold text-blue-400">{pendingCount}</p>
                    </Card>
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Potential Savings</p>
                        <p className="text-3xl font-bold text-cyan-400">₹{(totalSavings / 1000).toFixed(1)}K</p>
                    </Card>
                </div>

                {/* Status Filter */}
                <Card className="glass border-border/50 p-6">
                    <label className="text-sm font-medium text-muted-foreground mb-2 block">Filter by Status</label>
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="w-full md:w-64 bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-cyan-500"
                    >
                        <option>All</option>
                        <option>Applied</option>
                        <option>Pending</option>
                        <option>Simulated</option>
                        <option>Rejected</option>
                    </select>
                </Card>

                {/* Timeline */}
                {loading ? (
                    <div className="animate-pulse space-y-4">
                        <div className="h-40 bg-slate-800 rounded-lg"></div>
                        <div className="h-40 bg-slate-800 rounded-lg"></div>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {filteredOptimizations.map((optimization, index) => (
                            <Card key={optimization.id} className="glass border-border/50 p-6 hover:border-cyan-500/30 transition">
                                <div className="flex gap-6">
                                    {/* Timeline Icon */}
                                    <div className="flex flex-col items-center">
                                        <div className="p-3 rounded-full bg-slate-800 border-2 border-slate-700">
                                            {getStatusIcon(optimization.status)}
                                        </div>
                                        {index < filteredOptimizations.length - 1 && (
                                            <div className="w-0.5 h-full bg-slate-700 mt-2"></div>
                                        )}
                                    </div>

                                    {/* Content */}
                                    <div className="flex-1">
                                        <div className="flex items-start justify-between mb-3">
                                            <div>
                                                <h3 className="text-lg font-semibold text-foreground mb-1">{optimization.action}</h3>
                                                <p className="text-sm text-muted-foreground">{optimization.resourceName}</p>
                                            </div>
                                            {getStatusBadge(optimization.status)}
                                        </div>

                                        <p className="text-sm text-foreground mb-4">{optimization.description}</p>

                                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                            <div className="flex items-center gap-2">
                                                <TrendingDown className="w-4 h-4 text-cyan-400" />
                                                <div>
                                                    <p className="text-xs text-muted-foreground">Monthly Savings</p>
                                                    <p className="text-sm font-semibold text-foreground">₹{optimization.monthlySavings.toLocaleString()}</p>
                                                </div>
                                            </div>

                                            <div className="flex items-center gap-2">
                                                <CheckCircle className="w-4 h-4 text-green-400" />
                                                <div>
                                                    <p className="text-xs text-muted-foreground">Confidence</p>
                                                    <p className="text-sm font-semibold text-foreground">{optimization.confidence}</p>
                                                </div>
                                            </div>

                                            <div className="flex items-center gap-2">
                                                <Clock className="w-4 h-4 text-blue-400" />
                                                <div>
                                                    <p className="text-xs text-muted-foreground">
                                                        {optimization.appliedAt ? 'Applied' : 'Created'}
                                                    </p>
                                                    <p className="text-sm font-semibold text-foreground">
                                                        {new Date(optimization.appliedAt || optimization.createdAt).toLocaleDateString()}
                                                    </p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </Card>
                        ))}
                    </div>
                )}

                {filteredOptimizations.length === 0 && !loading && (
                    <div className="text-center py-12">
                        <p className="text-muted-foreground">No optimizations found matching your criteria</p>
                    </div>
                )}
            </div>
        </DashboardLayout>
    )
}
