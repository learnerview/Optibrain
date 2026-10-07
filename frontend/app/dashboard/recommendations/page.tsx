'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import {
    approveRecommendation,
    executeRecommendation,
    getRecommendations,
    rejectRecommendation,
} from '@/lib/api'
import { useApi } from '@/hooks/use-api'
import { EmptyState } from '@/components/empty-state'
import { formatCurrency, formatDateTime, formatPercent } from '@/lib/format'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useState } from 'react'
import {
    Brain,
    TrendingDown,
    AlertCircle,
    CheckCircle2,
    Play,
    X,
    Clock3,
} from 'lucide-react'

export default function RecommendationsPage() {
    const { data, loading, error, reload } = useApi(getRecommendations, [])
    const recommendations = data ?? []
    const [busyId, setBusyId] = useState<string | null>(null)
    const [feedback, setFeedback] = useState<string | null>(null)

    async function act(
        id: string | undefined,
        fn: (id: string) => Promise<boolean>,
        okMessage: string
    ) {
        if (!id) return
        setBusyId(id)
        setFeedback(null)
        try {
            const applied = await fn(id)
            setFeedback(applied ? okMessage : 'Action was not applied (blocked or not in the required state).')
        } catch (cause) {
            setFeedback(cause instanceof Error ? cause.message : 'Request failed')
        } finally {
            setBusyId(null)
            reload()
        }
    }

    const totalMonthlySavings = recommendations.reduce(
        (sum, rec) => sum + (rec.monthlySavings ?? 0),
        0
    )

    return (
        <DashboardLayout>
            <div className='space-y-6'>
                <div className='flex flex-col justify-between gap-4 md:flex-row md:items-end'>
                    <div>
                        <h1 className='mb-2 text-3xl font-bold text-foreground'>
                            Optimisation recommendations
                        </h1>
                        <p className='text-muted-foreground'>
                            Derived from live inventory and CloudWatch telemetry
                        </p>
                    </div>
                    {recommendations.length > 0 && (
                        <div className='text-right'>
                            <p className='mb-1 text-xs uppercase tracking-wider text-muted-foreground'>
                                Potential monthly savings
                            </p>
                            <p className='text-3xl font-bold text-emerald-400'>
                                {formatCurrency(totalMonthlySavings)}
                            </p>
                        </div>
                    )}
                </div>

                {feedback && (
                    <div className='flex items-start gap-2 rounded-lg border border-blue-500/20 bg-blue-500/5 p-3 text-sm'>
                        <Brain className='mt-0.5 h-4 w-4 shrink-0 text-blue-400' aria-hidden />
                        <span className='text-blue-300'>{feedback}</span>
                    </div>
                )}

                {loading ? (
                    <div className='grid animate-pulse gap-6'>
                        <div className='h-40 rounded-xl bg-muted' />
                        <div className='h-40 rounded-xl bg-muted' />
                    </div>
                ) : error ? (
                    <EmptyState
                        variant='error'
                        title='Could not load recommendations'
                        description={error}
                        onRetry={reload}
                    />
                ) : recommendations.length === 0 ? (
                    <EmptyState
                        title='No recommendations'
                        description='Nothing in this account currently qualifies. Recommendations require measured signals - unattached storage, or instances with sustained low CPU. In the sandbox there is usually no CloudWatch history yet.'
                    />
                ) : (
                    <div className='grid gap-6'>
                        {recommendations.map((rec, index) => (
                            <Card
                                key={rec.id ?? `${rec.resourceId}-${index}`}
                                className='border-emerald-500/20 p-6 transition-all hover:border-emerald-500/40'
                            >
                                <div className='flex flex-col gap-6 md:flex-row'>
                                    <div className='flex items-center justify-between rounded-xl border border-emerald-500/10 bg-emerald-500/5 p-4 md:min-w-[160px] md:flex-col'>
                                        <div className='flex h-12 w-12 items-center justify-center rounded-full bg-emerald-500/20'>
                                            <TrendingDown
                                                className='h-6 w-6 text-emerald-400'
                                                aria-hidden
                                            />
                                        </div>
                                        <div className='text-right md:text-center'>
                                            <p className='text-xl font-bold text-emerald-400'>
                                                {formatCurrency(rec.monthlySavings)}
                                            </p>
                                            <p className='text-[10px] font-bold uppercase tracking-widest text-emerald-400/60'>
                                                Savings / mo
                                            </p>
                                        </div>
                                    </div>

                                    <div className='flex-1 space-y-3'>
                                        <div className='flex flex-wrap items-center gap-2'>
                                            <h3 className='text-lg font-bold text-foreground'>
                                                {rec.recommendedType ?? rec.action ?? 'Resource'}
                                            </h3>
                                            {rec.action && (
                                                <Badge variant='secondary'>
                                                    {rec.action}
                                                </Badge>
                                            )}
                                            {typeof rec.confidence === 'number' && (
                                                <Badge variant='outline'>
                                                    {formatPercent(
                                                        rec.confidence * 100
                                                    )}{' '}
                                                    confidence
                                                </Badge>
                                            )}
                                            {rec.status ? (
                                                <Badge
                                                    variant={
                                                        rec.status === 'PENDING' ||
                                                        rec.status === 'APPROVED'
                                                            ? 'outline'
                                                            : rec.status === 'EXECUTED'
                                                                ? 'default'
                                                                : 'destructive'
                                                    }
                                                >
                                                    {rec.status}
                                                </Badge>
                                            ) : null}
                                        </div>

                                        {rec.resourceId && (
                                            <p className='font-mono text-xs text-muted-foreground'>
                                                {rec.resourceId}
                                            </p>
                                        )}

                                        {rec.reason && (
                                            <div className='flex items-start gap-2 text-sm'>
                                                <AlertCircle
                                                    className='mt-0.5 h-4 w-4 shrink-0 text-amber-400'
                                                    aria-hidden
                                                />
                                                <span className='text-muted-foreground'>
                                                    {rec.reason}
                                                </span>
                                            </div>
                                        )}

                                        {rec.createdAt && (
                                            <div className='flex items-start gap-2 text-sm'>
                                                <CheckCircle2
                                                    className='mt-0.5 h-4 w-4 shrink-0 text-emerald-400'
                                                    aria-hidden
                                                />
                                                <span className='text-xs text-muted-foreground'>
                                                    Raised {formatDateTime(rec.createdAt)}
                                                </span>
                                            </div>
                                        )}

                                        <div className='flex flex-wrap gap-2 pt-1'>
                                            {rec.status === 'PENDING' && (
                                                <>
                                                    <Button
                                                        size='sm'
                                                        variant='outline'
                                                        disabled={busyId === rec.id}
                                                        onClick={() =>
                                                            act(
                                                                rec.id,
                                                                approveRecommendation,
                                                                'Approved — ready to execute'
                                                            )
                                                        }
                                                    >
                                                        {busyId === rec.id ? (
                                                            <Clock3 className='mr-1 h-3 w-3 animate-spin' aria-hidden />
                                                        ) : (
                                                            <CheckCircle2 className='mr-1 h-3 w-3' aria-hidden />
                                                        )}
                                                        Approve
                                                    </Button>
                                                    <Button
                                                        size='sm'
                                                        variant='outline'
                                                        disabled={busyId === rec.id}
                                                        onClick={() =>
                                                            act(
                                                                rec.id,
                                                                rejectRecommendation,
                                                                'Rejected'
                                                            )
                                                        }
                                                    >
                                                        <X className='mr-1 h-3 w-3' aria-hidden />
                                                        Reject
                                                    </Button>
                                                </>
                                            )}
                                            {rec.status === 'APPROVED' && (
                                                <Button
                                                    size='sm'
                                                    variant='default'
                                                    disabled={busyId === rec.id}
                                                    onClick={() =>
                                                        act(
                                                            rec.id,
                                                            executeRecommendation,
                                                            'Executed (or simulated under dry-run)'
                                                        )
                                                    }
                                                >
                                                    <Play className='mr-1 h-3 w-3' aria-hidden />
                                                    Execute
                                                </Button>
                                            )}
                                            {rec.status === 'FAILED' && (
                                                <p className='text-xs text-red-500'>
                                                    Execution was blocked or failed — review the
                                                    remediation history.
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </Card>
                        ))}
                    </div>
                )}

                <div className='rounded-lg border border-blue-500/20 bg-blue-500/5 p-4'>
                    <div className='flex gap-3'>
                        <Brain className='h-5 w-5 shrink-0 text-blue-400' aria-hidden />
                        <p className='text-xs italic leading-relaxed text-blue-300'>
                            A recommendation is raised only where a measurement supports
                            it. Resources with no telemetry are skipped rather than
                            estimated, so an empty list means &ldquo;nothing
                            measured&rdquo;, not &ldquo;nothing wrong&rdquo;.
                        </p>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    )
}