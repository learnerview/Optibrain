"use client"

import { BarChart3, Zap, AlertCircle, Sparkles, Lock, Cpu } from "lucide-react"

const features = [
  {
    icon: BarChart3,
    title: "Real-Time Analytics",
    description:
      "Get instant visibility into your cloud spending across all providers with detailed breakdowns by service and region.",
    color: "from-cyan-500 to-blue-500",
  },
  {
    icon: Zap,
    title: "Smart Optimization",
    description:
      "AI-powered recommendations that automatically identify and fix inefficiencies in your cloud infrastructure.",
    color: "from-purple-500 to-pink-500",
  },
  {
    icon: AlertCircle,
    title: "Anomaly Detection",
    description: "Instantly catch unusual spending patterns with ML-based anomaly detection and real-time alerts.",
    color: "from-orange-500 to-red-500",
  },
  {
    icon: Sparkles,
    title: "Predictive Forecasting",
    description: "Forecast future costs with 95% accuracy and simulate what-if scenarios for better planning.",
    color: "from-blue-500 to-cyan-500",
  },
  {
    icon: Lock,
    title: "Enterprise Security",
    description: "SOC 2 certified, end-to-end encryption, and granular RBAC for complete data protection.",
    color: "from-emerald-500 to-teal-500",
  },
  {
    icon: Cpu,
    title: "Auto-Remediation",
    description: "Automated scaling policies and resource cleanup that runs 24/7 without manual intervention.",
    color: "from-indigo-500 to-purple-500",
  },
]

export default function FeaturesGrid() {
  return (
    <section
      id="features"
      className="py-24 px-4 sm:px-6 lg:px-8 bg-gradient-to-b from-transparent via-blue-500/5 to-transparent"
    >
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-4xl sm:text-5xl font-bold mb-4">
            Enterprise-Grade{" "}
            <span className="bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">
              FinOps Platform
            </span>
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Everything you need to optimize, monitor, and automate your multi-cloud financial operations.
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          {features.map((feature, index) => {
            const Icon = feature.icon
            return (
              <div
                key={index}
                className="group relative glass p-8 rounded-xl border border-border/50 hover:border-cyan-500/50 transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/20 overflow-hidden"
              >
                <div
                  className={`absolute inset-0 bg-gradient-to-br ${feature.color} opacity-0 group-hover:opacity-5 transition-opacity duration-300`}
                ></div>

                <div className="relative z-10">
                  <div
                    className={`w-14 h-14 rounded-lg bg-gradient-to-br ${feature.color} p-0.5 mb-4 group-hover:shadow-lg group-hover:shadow-current/20 transition-all duration-300`}
                  >
                    <div className="w-full h-full bg-slate-900 rounded-lg flex items-center justify-center">
                      <Icon className="w-6 h-6 text-white" />
                    </div>
                  </div>
                  <h3 className="text-lg font-semibold mb-2 group-hover:text-white transition">{feature.title}</h3>
                  <p className="text-sm text-muted-foreground group-hover:text-muted-foreground transition">
                    {feature.description}
                  </p>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
