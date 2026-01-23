"use client"
import {
  Eye,
  Zap,
  Wand2,
  Download,
  AlertTriangle,
  TrendingDown,
  Shield,
  FileText,
} from "lucide-react"

interface PermissionsSetupProps {
  permissions: {
    costVisibility: boolean
    execution: boolean
    autoRemediation: boolean
    dataExport: boolean
    anomalyDetection: boolean
    billingOptimization: boolean
    securityAudit: boolean
    reportGeneration: boolean
  }
  onChange: (permission: keyof PermissionsSetupProps["permissions"]) => void
}

const permissionGroups = [
  {
    title: "Core Monitoring",
    description: "Essential read-only access to view and analyze your infrastructure",
    items: [
      {
        key: "costVisibility" as const,
        icon: Eye,
        title: "Cost Visibility",
        description: "Read-only access to view costs, usage, and analytics dashboards",
        color: "from-blue-500 to-blue-600",
      },
      {
        key: "anomalyDetection" as const,
        icon: AlertTriangle,
        title: "Anomaly Detection",
        description: "Automatic detection of unusual spending patterns and resource usage",
        color: "from-orange-500 to-orange-600",
      },
    ],
  },
  {
    title: "Optimization & Execution",
    description: "Permissions for executing cost optimization actions",
    items: [
      {
        key: "billingOptimization" as const,
        icon: TrendingDown,
        title: "Billing Optimization",
        description: "Permission to apply cost optimization recommendations",
        color: "from-green-500 to-green-600",
      },
      {
        key: "autoRemediation" as const,
        icon: Wand2,
        title: "Auto-Remediation",
        description: "Allow OptiBrain to automatically fix inefficiencies 24/7",
        color: "from-cyan-500 to-cyan-600",
      },
      {
        key: "execution" as const,
        icon: Zap,
        title: "Manual Execution",
        description: "Permission to manually execute optimization actions on demand",
        color: "from-yellow-500 to-yellow-600",
      },
    ],
  },
  {
    title: "Compliance & Reporting",
    description: "Data export and security audit capabilities",
    items: [
      {
        key: "securityAudit" as const,
        icon: Shield,
        title: "Security Audit",
        description: "Analyze security configurations and compliance posture",
        color: "from-purple-500 to-purple-600",
      },
      {
        key: "reportGeneration" as const,
        icon: FileText,
        title: "Report Generation",
        description: "Generate detailed cost and optimization reports",
        color: "from-indigo-500 to-indigo-600",
      },
      {
        key: "dataExport" as const,
        icon: Download,
        title: "Data Export",
        description: "Permission to export cost data and reports in various formats",
        color: "from-emerald-500 to-emerald-600",
      },
    ],
  },
]

export default function PermissionsSetup({ permissions, onChange }: PermissionsSetupProps) {
  return (
    <div className="space-y-8">
      <div>
        <h3 className="text-2xl font-semibold mb-2">Configure Permissions</h3>
        <p className="text-muted-foreground">
          Select which permissions OptiBrain needs to optimize your cloud infrastructure. You can change these anytime in
          settings.
        </p>
      </div>

      {permissionGroups.map((group, groupIdx) => (
        <div key={groupIdx} className="space-y-4">
          <div className="border-l-4 border-cyan-500 pl-4">
            <h4 className="text-lg font-bold">{group.title}</h4>
            <p className="text-sm text-muted-foreground">{group.description}</p>
          </div>

          <div className="grid md:grid-cols-2 gap-4">
            {group.items.map(({ key, icon: Icon, title, description, color }) => {
              const isEnabled = permissions[key]
              return (
                <button
                  key={key}
                  onClick={() => onChange(key)}
                  className={`relative p-6 rounded-xl border-2 transition-all transform hover:scale-105 overflow-hidden group ${
                    isEnabled
                      ? `border-cyan-500 shadow-lg shadow-cyan-500/30`
                      : `border-slate-700 hover:border-slate-600`
                  }`}
                >
                  {/* Gradient background for enabled */}
                  {isEnabled && (
                    <div className={`absolute inset-0 bg-gradient-to-br ${color} opacity-10`}></div>
                  )}

                  <div className="relative z-10">
                    {/* Icon */}
                    <div
                      className={`w-12 h-12 rounded-lg flex items-center justify-center mb-4 transition-all ${
                        isEnabled
                          ? `bg-gradient-to-br ${color} text-white`
                          : "bg-slate-800 text-muted-foreground"
                      }`}
                    >
                      <Icon className="w-6 h-6" />
                    </div>

                    {/* Title and description */}
                    <h5 className="font-bold text-left mb-1">{title}</h5>
                    <p className="text-sm text-muted-foreground text-left mb-4">{description}</p>

                    {/* Toggle switch */}
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold">
                        {isEnabled ? (
                          <span className="text-cyan-400">✓ Enabled</span>
                        ) : (
                          <span className="text-muted-foreground">Disabled</span>
                        )}
                      </span>
                      <div
                        className={`w-12 h-6 rounded-full relative transition-all ${
                          isEnabled
                            ? "bg-gradient-to-r from-cyan-500 to-blue-500"
                            : "bg-slate-700"
                        }`}
                      >
                        <div
                          className="absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition-transform shadow-md"
                          style={{
                            transform: isEnabled ? "translateX(24px)" : "translateX(0)",
                          }}
                        ></div>
                      </div>
                    </div>
                  </div>
                </button>
              )
            })}
          </div>
        </div>
      ))}

      {/* Summary */}
      <div className="bg-gradient-to-r from-cyan-500/10 to-blue-500/10 border border-cyan-500/30 rounded-xl p-6 mt-8">
        <p className="text-sm">
          <span className="font-semibold text-cyan-400">
            {Object.values(permissions).filter(Boolean).length} of {Object.keys(permissions).length}
          </span>
          <span className="text-muted-foreground ml-2">permissions enabled</span>
        </p>
      </div>
    </div>
  )
}
