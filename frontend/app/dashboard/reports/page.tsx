'use client'

import { useEffect, useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getMonthlyReport, type MonthlyReport } from '@/lib/api'
import { Card } from '@/components/ui/card'
import { FileText, Download, TrendingUp, TrendingDown, AlertCircle, CheckCircle } from 'lucide-react'

export default function ReportsPage() {
    const [report, setReport] = useState<MonthlyReport | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const fetchReport = async () => {
            try {
                setLoading(true)
                const data = await getMonthlyReport()
                setReport(data)
            } catch (err) {
                console.error('Failed to fetch monthly report', err)
            } finally {
                setLoading(false)
            }
        }
        fetchReport()
    }, [])

    return (
        <DashboardLayout>
            <div className="space-y-6">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-foreground mb-2">Reports & Exports</h1>
                        <p className="text-muted-foreground">Executive-ready cost intelligence reports</p>
                    </div>
                    <div className="flex gap-3">
                        <button className="px-4 py-2 bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 rounded-lg hover:bg-cyan-500/30 transition flex items-center gap-2">
                            <Download className="w-4 h-4" />
                            Export CSV
                        </button>
                        <button className="px-4 py-2 bg-purple-500/20 text-purple-400 border border-purple-500/30 rounded-lg hover:bg-purple-500/30 transition flex items-center gap-2">
                            <Download className="w-4 h-4" />
                            Download PDF
                        </button>
                    </div>
                </div>

                {loading ? (
                    <div className="animate-pulse space-y-4">
                        <div className="h-64 bg-slate-800 rounded-lg"></div>
                    </div>
                ) : report && (
                    <>
                        {/* Report Header */}
                        <Card className="glass border-border/50 p-6">
                            <div className="flex items-center gap-3 mb-4">
                                <FileText className="w-8 h-8 text-cyan-400" />
                                <div>
                                    <h2 className="text-2xl font-bold text-foreground">{report.month} Report</h2>
                                    <p className="text-sm text-muted-foreground">Generated on {new Date(report.generatedAt).toLocaleDateString()}</p>
                                </div>
                            </div>
                        </Card>

                        {/* Key Metrics */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                            <Card className="glass border-border/50 p-6">
                                <p className="text-sm text-muted-foreground mb-2">Total Cost</p>
                                <p className="text-3xl font-bold text-foreground mb-1">₹{report.totalCost.toLocaleString()}</p>
                                <div className="flex items-center gap-1 text-sm">
                                    {report.costChange > 0 ? (
                                        <>
                                            <TrendingUp className="w-4 h-4 text-red-400" />
                                            <span className="text-red-400">+{report.costChange}%</span>
                                        </>
                                    ) : (
                                        <>
                                            <TrendingDown className="w-4 h-4 text-green-400" />
                                            <span className="text-green-400">{report.costChange}%</span>
                                        </>
                                    )}
                                    <span className="text-muted-foreground ml-1">vs last month</span>
                                </div>
                            </Card>

                            <Card className="glass border-border/50 p-6">
                                <p className="text-sm text-muted-foreground mb-2">Potential Savings</p>
                                <p className="text-3xl font-bold text-green-400 mb-1">₹{report.totalSavings.toLocaleString()}</p>
                                <p className="text-sm text-muted-foreground">{report.savingsOpportunities} opportunities identified</p>
                            </Card>

                            <Card className="glass border-border/50 p-6">
                                <p className="text-sm text-muted-foreground mb-2">Top Service</p>
                                <p className="text-3xl font-bold text-foreground mb-1">{report.topService}</p>
                                <p className="text-sm text-muted-foreground">₹{report.topServiceCost.toLocaleString()}/month</p>
                            </Card>

                            <Card className="glass border-border/50 p-6">
                                <p className="text-sm text-muted-foreground mb-2">Anomalies Detected</p>
                                <p className="text-3xl font-bold text-red-400 mb-1">{report.anomaliesDetected}</p>
                                <p className="text-sm text-muted-foreground">{report.optimizationsApplied} optimizations applied</p>
                            </Card>
                        </div>

                        {/* Summary Section */}
                        <Card className="glass border-border/50 p-6">
                            <h3 className="text-xl font-semibold text-foreground mb-4">Executive Summary</h3>
                            <div className="space-y-4">
                                <div className="flex items-start gap-3">
                                    <AlertCircle className="w-5 h-5 text-amber-400 mt-0.5" />
                                    <div>
                                        <p className="font-medium text-foreground">Cost Trend</p>
                                        <p className="text-sm text-muted-foreground">
                                            Total cloud spending increased by {report.costChange}% compared to the previous month,
                                            primarily driven by {report.topService} usage.
                                        </p>
                                    </div>
                                </div>

                                <div className="flex items-start gap-3">
                                    <TrendingDown className="w-5 h-5 text-green-400 mt-0.5" />
                                    <div>
                                        <p className="font-medium text-foreground">Optimization Opportunities</p>
                                        <p className="text-sm text-muted-foreground">
                                            {report.savingsOpportunities} optimization recommendations identified with potential monthly savings
                                            of ₹{report.totalSavings.toLocaleString()}.
                                        </p>
                                    </div>
                                </div>

                                <div className="flex items-start gap-3">
                                    <CheckCircle className="w-5 h-5 text-cyan-400 mt-0.5" />
                                    <div>
                                        <p className="font-medium text-foreground">Anomaly Detection</p>
                                        <p className="text-sm text-muted-foreground">
                                            {report.anomaliesDetected} cost anomaly detected and flagged for investigation.
                                            AI-powered monitoring continues to track spending patterns.
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </Card>
                    </>
                )}
            </div>
        </DashboardLayout>
    )
}
