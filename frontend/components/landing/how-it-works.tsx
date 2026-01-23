"use client"

import { Cloud, Brain, Zap, BarChart3 } from "lucide-react"

const steps = [
  {
    number: "01",
    icon: Cloud,
    title: "Connect Your Cloud",
    description:
      "One-click integration with AWS, Azure, GCP, or Kubernetes. Read-only access to start—no risky permissions needed.",
    points: ["SSO authentication", "Real-only initial scan", "Multi-account support"],
    color: "from-cyan-500 to-blue-500",
  },
  {
    number: "02",
    icon: Brain,
    title: "AI Learns Your Patterns",
    description:
      "Our ML models analyze 6+ months of usage patterns, spending trends, and performance baselines in under 24 hours.",
    points: ["Traffic pattern analysis", "Cost anomaly detection", "Workload classification"],
    color: "from-purple-500 to-pink-500",
  },
  {
    number: "03",
    icon: Zap,
    title: "Autonomous Optimization",
    description:
      "Start in simulation mode, then enable autonomous actions. OptiBrain executes 1000s of optimizations per day automatically.",
    points: ["Right-sizing instances", "Spot instance arbitrage", "Reserved capacity planning"],
    color: "from-emerald-500 to-teal-500",
  },
  {
    number: "04",
    icon: BarChart3,
    title: "Watch Savings Grow",
    description:
      "Real-time dashboards show every dollar saved. Typical results: 30-60% cost reduction in the first month.",
    points: ["Live savings counter", "ROI attribution", "Executive reports"],
    color: "from-orange-500 to-red-500",
  },
]

export default function HowItWorks() {
  return (
    <section id="how-it-works" className="py-24 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-b from-slate-900 via-purple-900/30 to-slate-900 pointer-events-none" />
      <div className="absolute inset-0 bg-gradient-to-r from-blue-600/5 via-transparent to-cyan-600/5 pointer-events-none" />
      <div className="absolute top-0 left-1/4 w-[500px] h-[500px] bg-purple-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-1/4 w-[500px] h-[500px] bg-cyan-600/10 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-7xl mx-auto relative z-10">
        <div className="text-center mb-16">
          <h2 className="text-4xl sm:text-5xl font-bold mb-4">
            How{" "}
            <span className="bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">
              OptiBrain
            </span>{" "}
            Works
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            From connection to savings in under 24 hours. No consultants, no complex setup, no waiting months for
            results.
          </p>
        </div>

        <div className="grid md:grid-cols-2 gap-8 lg:gap-12">
          {steps.map((step, index) => {
            const Icon = step.icon
            return (
              <div
                key={index}
                className="group relative glass p-8 rounded-xl border border-border/50 hover:border-cyan-500/50 transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/10 overflow-hidden"
              >
                {/* Gradient background on hover */}
                <div
                  className={`absolute inset-0 bg-gradient-to-br ${step.color} opacity-0 group-hover:opacity-5 transition-opacity duration-300`}
                />

                <div className="relative z-10">
                  {/* Step number and icon */}
                  <div className="flex items-start gap-4 mb-4">
                    <div
                      className={`w-12 h-12 rounded-lg bg-gradient-to-br ${step.color} p-0.5 flex-shrink-0 group-hover:shadow-lg group-hover:shadow-current/20 transition-all duration-300`}
                    >
                      <div className="w-full h-full bg-slate-900 rounded-lg flex items-center justify-center">
                        <Icon className="w-6 h-6 text-white" />
                      </div>
                    </div>
                    <div className={`text-2xl font-bold bg-gradient-to-r ${step.color} bg-clip-text text-transparent`}>
                      {step.number}
                    </div>
                  </div>

                  <h3 className="text-xl font-semibold mb-3 group-hover:text-white transition">{step.title}</h3>
                  <p className="text-muted-foreground mb-4 text-sm">{step.description}</p>

                  {/* Bullet points */}
                  <ul className="space-y-2">
                    {step.points.map((point, i) => (
                      <li key={i} className="flex items-center gap-2 text-sm text-muted-foreground">
                        <div className="w-1.5 h-1.5 rounded-full bg-gradient-to-r from-cyan-400 to-purple-400" />
                        {point}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
