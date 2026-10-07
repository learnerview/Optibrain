'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getSavingsProjection } from '@/lib/api'
import { useApi } from '@/hooks/use-api'
import { EmptyState } from '@/components/empty-state'
import { formatCurrency } from '@/lib/format'
import { Card } from '@/components/ui/card'
import { ArrowUpRight, ListChecks, Info } from 'lucide-react'

export default function SavingsPage() {
    const { data, loading, error, reload } = useApi(getSavingsProjection, [])
    const savings = data

    return (
        <DashboardLayout>
            <div className='space-y-6'>
                <div>
                    <h1 className='mb-2 text-3xl font-bold text-foreground'>
                        Savings projection
                    </h1>
                    <p className='text-muted-foreground'>
                        Derived from pending recommendations and measured spend
                    </p>
                </div>

                {loading ? (
                    <div className='grid animate-pulse gap-6 md:grid-cols-2'>
                        <div className='h-40 rounded-xl bg-muted' />
                        <div className='h-40 rounded-xl bg-muted' />
                    </div>
                ) : error ? (
                    <EmptyState
                        variant='error'
                        title='Could not load the savings projection'
                        description={error}
                        onRetry={reload}
                    />
                ) : !savings || savings.monthlySavings === 0 ? (
                    <EmptyState
                        title='No projected savings'
                        description='The projection sums pending recommendations against measured spend. With no open recommendations or no Cost Explorer data, there is nothing to project yet.'
                    />
                ) : (
                    <div className='grid gap-6'>
                        <div className='grid gap-6 md:grid-cols-2'>
                            <Card className='relative overflow-hidden border-emerald-500/20 p-8'>
                                <div className='absolute right-0 top-0 p-4 opacity-10'>
                                    <ArrowUpRight
                                        className='h-24 w-24 text-emerald-400'
                                        aria-hidden
                                    />
                                </div>
                                <div className='relative z-10'>
                                    <p className='mb-2 text-sm font-medium uppercase tracking-widest text-emerald-400/80'>
                                        Estimated monthly savings
                                    </p>
                                    <h2 className='mb-4 text-5xl font-extrabold text-foreground'>
                                        {formatCurrency(savings.monthlySavings)}
                                    </h2>
                                    <p className='text-xs text-muted-foreground'>
                                        Sum of pending recommendations
                                    </p>
                                </div>
                            </Card>

                            <Card className='relative overflow-hidden border-blue-500/20 p-8'>
                                <div className='absolute right-0 top-0 p-4 opacity-10'>
                                    <ArrowUpRight
                                        className='h-24 w-24 text-blue-400'
                                        aria-hidden
                                    />
                                </div>
                                <div className='relative z-10'>
                                    <p className='mb-2 text-sm font-medium uppercase tracking-widest text-blue-400/80'>
                                        Projected annual savings
                                    </p>
                                    <h2 className='mb-4 text-5xl font-extrabold text-foreground'>
                                        {formatCurrency(savings.annualSavings)}
                                    </h2>
                                    <p className='text-xs text-muted-foreground'>
                                        Monthly figure annualised
                                    </p>
                                </div>
                            </Card>
                        </div>

                        <Card className='border-border/50 p-6'>
                            <div className='mb-6 flex items-center gap-2'>
                                <ListChecks
                                    className='h-5 w-5 text-blue-400'
                                    aria-hidden
                                />
                                <h3 className='text-sm font-medium uppercase tracking-wider text-muted-foreground'>
                                    How this is calculated
                                </h3>
                            </div>
                            <div className='grid gap-4'>
                                <div className='flex items-center gap-4 rounded-xl border border-border/50 p-4'>
                                    <div className='flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-blue-500/20 text-xs font-bold text-blue-400'>
                                        1
                                    </div>
                                    <p className='text-sm font-medium text-foreground'>
                                        {savings.confidence}
                                    </p>
                                </div>
                                {savings.assumptions.map((assumption, index) => (
                                    <div
                                        key={index}
                                        className='flex items-center gap-4 rounded-xl border border-border/50 p-4'
                                    >
                                        <div className='flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-blue-500/20 text-xs font-bold text-blue-400'>
                                            {index + 2}
                                        </div>
                                        <p className='text-sm font-medium text-foreground'>
                                            {assumption}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        </Card>

                        <div className='rounded-xl border border-amber-500/20 bg-amber-500/5 p-4'>
                            <p className='flex items-start gap-2 text-sm text-amber-300'>
                                <Info
                                    className='mt-0.5 h-4 w-4 shrink-0'
                                    aria-hidden
                                />
                                A projection is only as good as the recommendations
                                behind it. Nothing here is applied unless remediation
                                is explicitly executed, and dry-run is the default.
                            </p>
                        </div>
                    </div>
                )}
            </div>
        </DashboardLayout>
    )
}