'use client'

import { useEffect, useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getCostExplorerData, type CostExplorerData } from '@/lib/api'
import { Card } from '@/components/ui/card'
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { TrendingUp, Filter, Calendar, MapPin, Server } from 'lucide-react'

export default function CostExplorerPage() {
    const [data, setData] = useState<CostExplorerData | null>(null)
    const [loading, setLoading] = useState(true)
    const [selectedService, setSelectedService] = useState<string>('All')
    const [selectedRegion, setSelectedRegion] = useState<string>('ap-south-1')

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true)
                const explorerData = await getCostExplorerData()
                setData(explorerData)
            } catch (err) {
                console.error('Failed to fetch cost explorer data', err)
            } finally {
                setLoading(false)
            }
        }
        fetchData()
    }, [selectedService, selectedRegion])

    const services = ['All', 'EC2', 'RDS', 'S3', 'CloudWatch']
    const regions = ['ap-south-1', 'us-east-1', 'eu-west-1']

    return (
        <DashboardLayout>
            <div className="space-y-6">
                {/* Header */}
                <div>
                    <h1 className="text-3xl font-bold text-foreground mb-2">Cost Explorer</h1>
                    <p className="text-muted-foreground">Deep dive into your cloud spending patterns</p>
                </div>

                {/* Filters */}
                <Card className="glass border-border/50 p-6">
                    <div className="flex items-center gap-2 mb-4">
                        <Filter className="w-5 h-5 text-cyan-400" />
                        <h2 className="text-lg font-semibold text-foreground">Filters</h2>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        {/* Date Range */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-muted-foreground mb-2">
                                <Calendar className="w-4 h-4" />
                                Date Range
                            </label>
                            <select className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-cyan-500">
                                <option>Last 7 days</option>
                                <option selected>Last 30 days</option>
                                <option>Last 90 days</option>
                            </select>
                        </div>

                        {/* Service Filter */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-muted-foreground mb-2">
                                <Server className="w-4 h-4" />
                                Service
                            </label>
                            <select
                                value={selectedService}
                                onChange={(e) => setSelectedService(e.target.value)}
                                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-cyan-500"
                            >
                                {services.map(service => (
                                    <option key={service} value={service}>{service}</option>
                                ))}
                            </select>
                        </div>

                        {/* Region Filter */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-muted-foreground mb-2">
                                <MapPin className="w-4 h-4" />
                                Region
                            </label>
                            <select
                                value={selectedRegion}
                                onChange={(e) => setSelectedRegion(e.target.value)}
                                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-cyan-500"
                            >
                                {regions.map(region => (
                                    <option key={region} value={region}>{region}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                </Card>

                {loading ? (
                    <div className="animate-pulse space-y-4">
                        <div className="h-96 bg-slate-800 rounded-lg"></div>
                        <div className="h-80 bg-slate-800 rounded-lg"></div>
                    </div>
                ) : (
                    <>
                        {/* Daily Cost Trend */}
                        <Card className="glass border-border/50 p-6">
                            <div className="flex items-center justify-between mb-6">
                                <div>
                                    <h2 className="text-xl font-semibold text-foreground mb-1">Daily Cost Trend</h2>
                                    <p className="text-sm text-muted-foreground">Showing cost patterns over time</p>
                                </div>
                                <div className="flex items-center gap-2 px-4 py-2 bg-red-500/10 border border-red-500/20 rounded-lg">
                                    <TrendingUp className="w-4 h-4 text-red-400" />
                                    <span className="text-sm font-medium text-red-400">Spike Detected: Jan 14</span>
                                </div>
                            </div>

                            <ResponsiveContainer width="100%" height={350}>
                                <LineChart data={data?.dailyCosts || []}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                                    <XAxis
                                        dataKey="date"
                                        stroke="#94a3b8"
                                        tick={{ fill: '#94a3b8' }}
                                    />
                                    <YAxis
                                        stroke="#94a3b8"
                                        tick={{ fill: '#94a3b8' }}
                                        tickFormatter={(value) => `₹${(value / 1000).toFixed(1)}K`}
                                    />
                                    <Tooltip
                                        contentStyle={{
                                            backgroundColor: '#1e293b',
                                            border: '1px solid #334155',
                                            borderRadius: '8px'
                                        }}
                                        labelStyle={{ color: '#e2e8f0' }}
                                        formatter={(value: number) => [`₹${value.toLocaleString()}`, 'Cost']}
                                    />
                                    <Legend />
                                    <Line
                                        type="monotone"
                                        dataKey="cost"
                                        stroke="#06b6d4"
                                        strokeWidth={3}
                                        dot={{ fill: '#06b6d4', r: 4 }}
                                        activeDot={{ r: 6, fill: '#22d3ee' }}
                                        name="Daily Cost (₹)"
                                    />
                                </LineChart>
                            </ResponsiveContainer>

                            {/* Spike Callout */}
                            <div className="mt-4 p-4 bg-red-500/5 border border-red-500/20 rounded-lg">
                                <p className="text-sm text-red-400">
                                    <strong>Anomaly Alert:</strong> Cost spike of <strong>₹9,100</strong> detected on <strong>2026-01-14</strong>
                                    — 316% above baseline. Primary driver: EC2 on-demand usage surge.
                                </p>
                            </div>
                        </Card>

                        {/* Service Breakdown */}
                        <Card className="glass border-border/50 p-6">
                            <div className="mb-6">
                                <h2 className="text-xl font-semibold text-foreground mb-1">Service Cost Breakdown</h2>
                                <p className="text-sm text-muted-foreground">Monthly cost distribution by service</p>
                            </div>

                            <ResponsiveContainer width="100%" height={300}>
                                <BarChart data={data?.breakdown || []}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                                    <XAxis
                                        dataKey="service"
                                        stroke="#94a3b8"
                                        tick={{ fill: '#94a3b8' }}
                                    />
                                    <YAxis
                                        stroke="#94a3b8"
                                        tick={{ fill: '#94a3b8' }}
                                        tickFormatter={(value) => `₹${(value / 1000).toFixed(0)}K`}
                                    />
                                    <Tooltip
                                        contentStyle={{
                                            backgroundColor: '#1e293b',
                                            border: '1px solid #334155',
                                            borderRadius: '8px'
                                        }}
                                        labelStyle={{ color: '#e2e8f0' }}
                                        formatter={(value: number) => [`₹${value.toLocaleString()}`, 'Monthly Cost']}
                                    />
                                    <Legend />
                                    <Bar
                                        dataKey="cost"
                                        fill="#06b6d4"
                                        radius={[8, 8, 0, 0]}
                                        name="Monthly Cost (₹)"
                                    />
                                </BarChart>
                            </ResponsiveContainer>

                            {/* Service Summary */}
                            <div className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-4">
                                {data?.breakdown.map((service) => (
                                    <div key={service.service} className="p-4 bg-slate-800/50 rounded-lg border border-slate-700">
                                        <p className="text-xs text-muted-foreground mb-1">{service.service}</p>
                                        <p className="text-2xl font-bold text-foreground">₹{(service.cost / 1000).toFixed(1)}K</p>
                                        <p className="text-xs text-cyan-400 mt-1">
                                            {((service.cost / 128450) * 100).toFixed(1)}% of total
                                        </p>
                                    </div>
                                ))}
                            </div>
                        </Card>
                    </>
                )}
            </div>
        </DashboardLayout>
    )
}
