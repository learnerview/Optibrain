'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { acknowledgeAlert, getAlerts } from '@/lib/api'
import { useApi } from '@/hooks/use-api'
import { EmptyState } from '@/components/empty-state'
import { formatDateTime } from '@/lib/format'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Bell, AlertTriangle, Info } from 'lucide-react'

function severityBadge(severity: string) {
    const styles: Record<string, string> = {
        HIGH: 'bg-red-500/20 text-red-400 border-red-500/30',
        MEDIUM: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
        LOW: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    }
    const Icon = severity === 'HIGH' ? AlertTriangle : Info
    return (
        <Badge className={styles[severity] ?? ''}>
            <Icon className='mr-1 h-3 w-3' aria-hidden />
            {severity.charAt(0) + severity.slice(1).toLowerCase()}
        </Badge>
    )
}

export default function AlertsPage() {
    const { data, loading, error, reload } = useApi(getAlerts, [])
    const alerts = data ?? []

    const highAlerts = alerts.filter((a) => a.severity === 'HIGH').length
    const unacknowledged = alerts.filter((a) => !a.acknowledged).length

    const handleAcknowledge = async (id: string) => {
        try {
            await acknowledgeAlert(id)
            reload()
        } catch {
            // Surface the failure by refetching; a silent no-op here would leave the
            // button looking broken with no explanation.
            reload()
        }
    }

    return (
        <DashboardLayout>
            <div className='space-y-6'>
                <div className='flex items-center justify-between'>
                    <div>
                        <h1 className='mb-2 text-3xl font-bold text-foreground'>
                            Alerts
                        </h1>
                        <p className='text-muted-foreground'>
                            Derived from measured spend and resource state
                        </p>
                    </div>
                    <div className='relative'>
                        <Bell className='h-8 w-8 text-cyan-400' aria-hidden />
                        {unacknowledged > 0 && (
                            <span className='absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs font-bold text-white'>
                                {unacknowledged}
                            </span>
                        )}
                    </div>
                </div>

                <div className='grid grid-cols-1 gap-4 md:grid-cols-3'>
                    <Card className='border-border/50 p-4'>
                        <p className='mb-1 text-sm text-muted-foreground'>Total</p>
                        <p className='text-3xl font-bold text-foreground'>
                            {alerts.length}
                        </p>
                    </Card>
                    <Card className='border-border/50 p-4'>
                        <p className='mb-1 text-sm text-muted-foreground'>
                            High severity
                        </p>
                        <p className='text-3xl font-bold text-red-400'>{highAlerts}</p>
                    </Card>
                    <Card className='border-border/50 p-4'>
                        <p className='mb-1 text-sm text-muted-foreground'>
                            Unacknowledged
                        </p>
                        <p className='text-3xl font-bold text-amber-400'>
                            {unacknowledged}
                        </p>
                    </Card>
                </div>

                {loading ? (
                    <div className='h-64 animate-pulse rounded-lg bg-muted' />
                ) : error ? (
                    <EmptyState
                        variant='error'
                        title='Could not load alerts'
                        description={error}
                        onRetry={reload}
                    />
                ) : alerts.length === 0 ? (
                    <EmptyState
                        title='No alerts'
                        description='Nothing in this account currently breaches the alerting thresholds. Alerts are raised when measured spend diverges from its trailing baseline.'
                    />
                ) : (
                    <div className='space-y-4'>
                        {alerts.map((alert) => (
                            <Card
                                key={alert.id}
                                className={`border-l-4 p-6 ${
                                    alert.severity === 'HIGH'
                                        ? 'border-l-red-500'
                                        : alert.severity === 'MEDIUM'
                                          ? 'border-l-amber-500'
                                          : 'border-l-blue-500'
                                }`}
                            >
                                <div className='flex items-start justify-between gap-4'>
                                    <div className='flex-1'>
                                        <div className='mb-2 flex flex-wrap items-center gap-3'>
                                            {severityBadge(alert.severity)}
                                            <span className='text-xs text-muted-foreground'>
                                                {alert.title}
                                            </span>
                                            <span className='text-xs text-muted-foreground'>
                                                {formatDateTime(alert.timestamp)}
                                            </span>
                                        </div>
                                        <p className='font-medium text-foreground'>
                                            {alert.message}
                                        </p>
                                    </div>
                                    {!alert.acknowledged && (
                                        <Button
                                            size='sm'
                                            variant='outline'
                                            onClick={() => handleAcknowledge(alert.id)}
                                        >
                                            Acknowledge
                                        </Button>
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