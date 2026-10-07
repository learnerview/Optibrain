'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getAnomalies } from '@/lib/api'
import { useApi } from '@/hooks/use-api'
import { EmptyState } from '@/components/empty-state'
import { formatCurrency, formatDateTime } from '@/lib/format'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { AlertTriangle, CheckCircle2, Activity } from 'lucide-react'

export default function AnomaliesPage() {
    const { data, loading, error, reload } = useApi(getAnomalies, [])

    const status = data?.status ?? 'UNKNOWN'
    const anomalies = data?.anomalies ?? []

    return (
        <DashboardLayout>
            <div className='space-y-6'>
                <div>
                    <h1 className='mb-2 text-3xl font-bold text-foreground'>
                        Cost anomaly detection
                    </h1>
                    <p className='text-muted-foreground'>
                        Daily spend compared against its own rolling baseline
                        {data?.method ? ` (${data.method})` : ''}
                    </p>
                </div>

                {loading ? (
                    <div className='h-72 animate-pulse rounded-lg bg-muted' />
                ) : error ? (
                    <EmptyState
                        variant='error'
                        title='Could not load anomaly data'
                        description={error}
                        onRetry={reload}
                    />
                ) : status === 'INSUFFICIENT_DATA' || status === 'UNAVAILABLE' ? (
                    <EmptyState
                        title='Not enough data to judge'
                        description={
                            data?.reason ??
                            'More days of cost history are required before a baseline can be established.'
                        }
                    />
                ) : anomalies.length === 0 ? (
                    <EmptyState
                        title='No anomalies detected'
                        description={`Spend has stayed within its expected range across ${data?.daysAnalysed ?? 0} days of history.`}
                    />
                ) : (
                    <div className='space-y-4'>
                        <div className='flex items-center gap-3 rounded-lg border border-red-500/30 bg-red-500/10 p-4'>
                            <AlertTriangle
                                className='h-6 w-6 text-red-400'
                                aria-hidden
                            />
                            <div>
                                <p className='font-semibold text-red-400'>
                                    {anomalies.length} anomalous day(s) detected
                                </p>
                                <p className='text-sm text-muted-foreground'>
                                    Measured against {data?.daysAnalysed} days of history
                                </p>
                            </div>
                        </div>

                        {anomalies.map((anomaly, index) => (
                            <Card
                                key={`${anomaly.timestamp}-${index}`}
                                className={`border-l-4 p-6 ${
                                    anomaly.severity === 'HIGH'
                                        ? 'border-l-red-500'
                                        : 'border-l-amber-500'
                                }`}
                            >
                                <div className='mb-3 flex flex-wrap items-center justify-between gap-2'>
                                    <div className='flex items-center gap-3'>
                                        <Badge
                                            variant={
                                                anomaly.severity === 'HIGH'
                                                    ? 'destructive'
                                                    : 'secondary'
                                            }
                                        >
                                            {anomaly.severity}
                                        </Badge>
                                        <span className='font-medium'>
                                            {anomaly.title}
                                        </span>
                                    </div>
                                    <span className='text-xs text-muted-foreground'>
                                        {formatDateTime(anomaly.timestamp)}
                                    </span>
                                </div>
                                <p className='text-sm text-muted-foreground'>
                                    {anomaly.description}
                                </p>
                            </Card>
                        ))}
                    </div>
                )}

                {status === 'NOMINAL' && (
                    <Card className='flex items-center gap-3 p-4'>
                        <CheckCircle2
                            className='h-5 w-5 text-emerald-400'
                            aria-hidden
                        />
                        <p className='text-sm text-muted-foreground'>
                            <Activity className='mr-1 inline h-3.5 w-3.5' aria-hidden />
                            All measured days are within the expected range.
                        </p>
                    </Card>
                )}
            </div>
        </DashboardLayout>
    )
}