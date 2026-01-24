"use client"

import { Database, Brain, BarChart3, Zap } from "lucide-react"

const steps = [
  {
    number: 1,
    title: "Ingest",
    description: "Multi-cloud data collection from AWS, Azure & GCP",
    icon: Database,
    color: "from-blue-500 to-cyan-500",
  },
  {
    number: 2,
    title: "Analyze",
    description: "Real-time processing with ML anomaly detection",
    icon: Brain,
    color: "from-purple-500 to-pink-500",
  },
  {
    number: 3,
    title: "Predict",
    description: "Forecast costs with 95% accuracy & confidence bands",
    icon: BarChart3,
    color: "from-orange-500 to-red-500",
  },
  {
    number: 4,
    title: "Optimize",
    description: "Intelligent rightsizing & resource recommendations",
    icon: Zap,
    color: "from-green-500 to-emerald-500",
  },
]

export default function ArchitectureFlow() {
  return (
    <section className="py-24 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Background effects */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute top-40 left-1/4 w-80 h-80 bg-primary/5 rounded-full blur-3xl opacity-30"></div>
        <div className="absolute bottom-20 right-1/3 w-96 h-96 bg-accent/5 rounded-full blur-3xl opacity-30"></div>
      </div>

      <div className="max-w-7xl mx-auto relative z-10">
        <div className="text-center mb-20">
          <h2 className="text-4xl md:text-5xl font-bold mb-4">How OptiBrain Works</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Autonomous Cloud Cost Intelligence engine that continuously learns and optimizes your cloud infrastructure
          </p>
        </div>

        {/* Architecture Flow */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 relative">
          {steps.map((step, index) => {
            const Icon = step.icon
            return (
              <div key={index} className="relative">
                {/* Arrow between steps */}
                {index < steps.length - 1 && (
                  <div className="hidden md:block absolute top-20 left-full w-8 h-1 bg-gradient-to-r from-primary/50 to-transparent"></div>
                )}

                {/* Step Card */}
                <div className="glass p-8 rounded-xl border border-primary/20 hover:border-primary/50 transition-all group">
                  {/* Icon Circle */}
                  <div
                    className={`w-16 h-16 rounded-full bg-gradient-to-r ${step.color} mb-6 flex items-center justify-center shadow-lg shadow-primary/20 group-hover:shadow-primary/40 transition`}
                  >
                    <Icon className="w-8 h-8 text-white" />
                  </div>

                  {/* Number */}
                  <div className="text-4xl font-bold text-primary/40 mb-2">{step.number}</div>

                  <h3 className="text-xl font-bold mb-2">{step.title}</h3>
                  <p className="text-sm text-muted-foreground">{step.description}</p>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
