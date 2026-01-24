'use client'

import { useEffect, useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getAlerts, type Alert } from '@/lib/api'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Bell, AlertTriangle, Info, XCircle } from 'lucide-react'

export default function AlertsPage() {
    const [alerts, setAlerts] = useState<Alert[]>([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const fetchAlerts = async () => {
            try {
                setLoading(true)
                const data = await getAlerts()
                setAlerts(data)
            } catch (err) {
                console.error('Failed to fetch alerts', err)
            } finally {
                setLoading(false)
            }
        }
        fetchAlerts()
    }, [])

    const handleAcknowledge = async (id: string) => {
        try {
            const response = await fetch(`http://localhost:8080/api/alerts/${id}/acknowledge`, {
                method: 'PATCH',
            });

            if (response.ok) {
                // Update local state
                setAlerts(alerts.map(alert =>
                    alert.id === id ? { ...alert, acknowledged: true } : alert
                ));
            }
        } catch (error) {
            console.error('Failed to acknowledge alert:', error);
        }
    };

    const getSeverityBadge = (severity: string) => {
        switch (severity) {
            case 'HIGH':
                return <Badge className="bg-red-500/20 text-red-400 border-red-500/30"><AlertTriangle className="w-3 h-3 mr-1" />High</Badge>
            case 'MEDIUM':
                return <Badge className="bg-amber-500/20 text-amber-400 border-amber-500/30"><Info className="w-3 h-3 mr-1" />Medium</Badge>
            case 'LOW':
                return <Badge className="bg-blue-500/20 text-blue-400 border-blue-500/30"><Info className="w-3 h-3 mr-1" />Low</Badge>
            default:
                return <Badge>{severity}</Badge>
        }
    }

    const highAlerts = alerts.filter(a => a.severity === 'HIGH').length
    const unacknowledged = alerts.filter(a => !a.acknowledged).length

    return (
        <DashboardLayout>
            <div className="space-y-6">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-foreground mb-2">Alerts & Notifications</h1>
                        <p className="text-muted-foreground">Stay informed about cost anomalies and optimization opportunities</p>
                    </div>
                    <div className="relative">
                        <Bell className="w-8 h-8 text-cyan-400" />
                        {unacknowledged > 0 && (
                            <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                                {unacknowledged}
                            </span>
                        )}
                    </div>
                </div>

                {/* Summary Cards */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Total Alerts</p>
                        <p className="text-3xl font-bold text-foreground">{alerts.length}</p>
                    </Card>
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">High Severity</p>
                        <p className="text-3xl font-bold text-red-400">{highAlerts}</p>
                    </Card>
                    <Card className="glass border-border/50 p-4">
                        <p className="text-sm text-muted-foreground mb-1">Unacknowledged</p>
                        <p className="text-3xl font-bold text-amber-400">{unacknowledged}</p>
                    </Card>
                </div>

                {/* Alerts List */}
                {loading ? (
                    <div className="animate-pulse space-y-4">
                        <div className="h-24 bg-slate-800 rounded-lg"></div>
                        <div className="h-24 bg-slate-800 rounded-lg"></div>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {alerts.map((alert) => (
                            <Card key={alert.id} className={`glass p-6 border-l-4 ${alert.severity === 'HIGH' ? 'border-l-red-500' :
                                alert.severity === 'MEDIUM' ? 'border-l-amber-500' :
                                    'border-l-blue-500'
                                }`}>
                                <div className="flex items-start justify-between">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-3 mb-2">
                                            {getSeverityBadge(alert.severity)}
                                            <span className="text-xs text-muted-foreground">{alert.service}</span>
                                            <span className="text-xs text-muted-foreground">
                                                {new Date(alert.timestamp).toLocaleString()}
                                            </span>
                                        </div>
                                        <p className="text-foreground font-medium">{alert.message}</p>
                                    </div>
                                    {!alert.acknowledged && (
                                        <button
                                            className="px-3 py-1 text-xs bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 rounded hover:bg-cyan-500/30 transition"
                                            onClick={() => handleAcknowledge(alert.id)}
                                        >
                                            Acknowledge
                                        </button>
                                    )}
                                </div>
                            </Card>
                        ))}
                    </div>
                )}
            </div>
        </DashboardLayout>
    )
}
