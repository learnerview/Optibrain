"use client"

import { Zap, TrendingDown, Shield, Brain, BarChart3, AlertCircle } from "lucide-react"

const services = [
  {
    icon: BarChart3,
    title: "Cost Analytics",
    description: "Comprehensive cost tracking across AWS, Azure, and GCP with drill-down capabilities.",
    features: ["Provider comparison", "Service-level breakdown", "Historical trends", "Custom reports"],
  },
  {
    icon: Zap,
    title: "Optimization Engine",
    description: "Automated recommendations for right-sizing, reserved instances, and resource consolidation.",
    features: [
      "Right-sizing suggestions",
      "Spot instance recommendations",
      "Reserved capacity planning",
      "Savings estimation",
    ],
  },
  {
    icon: Brain,
    title: "AI Copilot",
    description: "Conversational AI that answers your Cloud Cost Intelligence questions and provides actionable insights.",
    features: ["Natural language queries", "Cost analysis", "Trend explanation", "Custom investigations"],
  },
  {
    icon: AlertCircle,
    title: "Anomaly Detection",
    description: "ML-powered detection of unusual spending patterns with intelligent root cause analysis.",
    features: ["Real-time alerts", "Severity classification", "Root cause analysis", "Historical context"],
  },
  {
    icon: TrendingDown,
    title: "Predictive Forecasting",
    description: "Forecast future costs with ML models trained on your historical data.",
    features: ["Monthly forecasts", "What-if scenarios", "Trend analysis", "Budget planning"],
  },
  {
    icon: Shield,
    title: "Automation Center",
    description: "Set up automated policies for resource cleanup, scaling, and cost management.",
    features: ["Auto-scaling policies", "Resource lifecycle", "Cost tagging automation", "Compliance rules"],
  },
]

export default function ServicesShowcase() {
  return (
    <section id="services" className="py-20 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-4xl font-bold mb-4">Complete Cloud Cost Intelligence Solution</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Six core modules designed to give you complete control over your multi-cloud spending.
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {services.map((service, index) => {
            const Icon = service.icon
            return (
              <div
                key={index}
                className="glass p-8 rounded-xl border border-border/50 hover:border-accent/50 transition-all group"
              >
                <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-primary to-accent flex items-center justify-center mb-4">
                  <Icon className="w-6 h-6 text-background" />
                </div>
                <h3 className="text-xl font-semibold mb-3">{service.title}</h3>
                <p className="text-sm text-muted-foreground mb-6">{service.description}</p>
                <ul className="space-y-2">
                  {service.features.map((feature, i) => (
                    <li key={i} className="flex gap-2 text-xs text-muted-foreground">
                      <span className="w-1.5 h-1.5 rounded-full bg-primary/60 mt-1.5 flex-shrink-0"></span>
                      {feature}
                    </li>
                  ))}
                </ul>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
