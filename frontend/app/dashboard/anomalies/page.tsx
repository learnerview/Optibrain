'use client'

import { useEffect, useState } from 'react'
import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { getAnomalies } from '@/lib/api'
import { AlertTriangle, Clock, Activity, Zap, Info } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

export default function AnomaliesPage() {
  const [anomaly, setAnomaly] = useState<any>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchAnomaly = async () => {
      try {
        setLoading(true)
        const data = await getAnomalies()
        setAnomaly(data)
      } catch (err) {
        console.error("Failed to fetch anomaly data", err)
      } finally {
        setLoading(false)
      }
    }
    fetchAnomaly()
  }, [])

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-foreground mb-2">Cost Anomaly Detection</h1>
          <p className="text-muted-foreground">AI-powered detection of unusual spending patterns</p>
        </div>

        {loading ? (
          <div className="animate-pulse space-y-4">
            <div className="h-40 bg-slate-800 rounded-lg"></div>
            <div className="h-60 bg-slate-800 rounded-lg"></div>
          </div>
        ) : (
          <div className="grid gap-6">
            {/* Alert Banner */}
            <Card className={`border-none ${anomaly?.severity === 'HIGH' ? 'bg-red-500/10' : 'bg-amber-500/10'} p-6`}>
              <div className="flex items-start gap-4">
                <div className={`p-3 rounded-full ${anomaly?.severity === 'HIGH' ? 'bg-red-500/20 text-red-400' : 'bg-amber-500/20 text-amber-400'}`}>
                  <AlertTriangle className="w-8 h-8" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h2 className="text-xl font-bold text-foreground">
                      {anomaly?.anomalyDetected ? 'Critical Anomaly Detected' : 'No Anomalies Detected'}
                    </h2>
                    {anomaly?.anomalyDetected && (
                      <Badge variant="destructive" className="bg-red-500 text-white border-none px-3">
                        {anomaly.severity} SEVERITY
                      </Badge>
                    )}
                  </div>
                  <p className="text-lg text-foreground mb-4">
                    {anomaly?.reason || "System monitoring shows stable cost patterns across all services."}
                  </p>
                  <div className="flex items-center gap-6 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1.5">
                      <Clock className="w-4 h-4" /> Detected on {anomaly?.date || 'N/A'}
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Zap className="w-4 h-4 text-amber-500" /> Service: {anomaly?.service || 'N/A'}
                    </div>
                  </div>
                </div>
              </div>
            </Card>

            {/* Impact Details */}
            <div className="grid md:grid-cols-2 gap-6">
              <Card className="glass border-border/50 p-6">
                <h3 className="text-sm font-medium text-muted-foreground mb-4 uppercase tracking-wider">Cost Impact</h3>
                <div className="space-y-4">
                  <div className="flex justify-between items-end">
                    <div>
                      <p className="text-xs text-muted-foreground mb-1">Expected Cost</p>
                      <p className="text-2xl font-bold text-foreground">₹{anomaly?.expectedCost.toLocaleString()}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-muted-foreground mb-1">Actual Cost</p>
                      <p className="text-2xl font-bold text-red-400">₹{anomaly?.actualCost.toLocaleString()}</p>
                    </div>
                  </div>
                  <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-red-500"
                      style={{ width: `${(anomaly?.expectedCost / anomaly?.actualCost) * 100}%` }}
                    ></div>
                  </div>
                  <p className="text-sm text-red-400 font-medium">
                    Variation: +{(((anomaly?.actualCost - anomaly?.expectedCost) / anomaly?.expectedCost) * 100).toFixed(1)}% above baseline
                  </p>
                </div>
              </Card>

              <Card className="glass border-border/50 p-6">
                <h3 className="text-sm font-medium text-muted-foreground mb-4 uppercase tracking-wider">Recommended Action</h3>
                <div className="flex gap-4">
                  <div className="p-2 bg-blue-500/10 rounded-lg h-fit">
                    <Info className="w-5 h-5 text-blue-400" />
                  </div>
                  <div>
                    <p className="text-sm text-foreground leading-relaxed">
                      Investigate sudden spike in {anomaly?.service} usage. Check for runaway processes or misconfigured auto-scaling groups.
                    </p>
                    <button className="mt-4 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium rounded transition">
                      View Service Logs
                    </button>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
