"use client"

import { Brain, BarChart3, Shield, Zap } from "lucide-react"
import { Card } from "@/components/ui/card"

const features = [
  {
    icon: Brain,
    title: "AI Cloud Cost Intelligence Copilot",
    description: "Conversational AI that answers complex cost questions and provides intelligent recommendations",
    highlight: "Natural language interface powered by GPT-4",
    items: ["Multi-intent understanding", "Real-time data correlation", "Context-aware insights"],
  },
  {
    icon: BarChart3,
    title: "Real-Time Cost Intelligence",
    description: "Continuous visibility into spend patterns across all clouds with millisecond latency",
    highlight: "Get live cost data as it happens",
    items: ["Per-minute cost tracking", "Service-level granularity", "Regional cost breakdown"],
  },
  {
    icon: Zap,
    title: "Autonomous Optimization",
    description: "AI engine that identifies and implements optimizations without manual intervention",
    highlight: "40% average cost reduction automatically",
    items: ["Right-sizing automation", "Reserved Instance optimization", "Policy-driven scheduling"],
  },
  {
    icon: Shield,
    title: "Security + Cost Correlation",
    description: "Unified view of security risks and their financial impact across infrastructure",
    highlight: "Quantify security spend and risk",
    items: ["Compliance cost tracking", "Risk-adjusted spending", "Security incident correlation"],
  },
]

export default function FeaturesShowcase() {
  return (
    <section className="py-24 px-4 sm:px-6 lg:px-8 bg-gradient-to-b from-background to-secondary/10">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold mb-4">Enterprise-Grade Features</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Purpose-built for modern DevOps and Cloud Cost Intelligence teams at scale
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {features.map((feature, index) => {
            const Icon = feature.icon
            return (
              <Card key={index} className="glass p-8 border-primary/20 hover:border-primary/40 transition-all group">
                <div className="flex items-start gap-6">
                  {/* Icon */}
                  <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-primary/30 to-accent/30 flex items-center justify-center flex-shrink-0 group-hover:from-primary/50 group-hover:to-accent/50 transition">
                    <Icon className="w-7 h-7 text-primary" />
                  </div>

                  {/* Content */}
                  <div className="flex-1">
                    <h3 className="text-xl font-bold mb-2">{feature.title}</h3>
                    <p className="text-muted-foreground mb-4">{feature.description}</p>

                    {/* Highlight Box */}
                    <div className="bg-primary/10 border border-primary/30 rounded-lg px-3 py-2 mb-4">
                      <p className="text-sm font-medium text-primary">{feature.highlight}</p>
                    </div>

                    {/* Items List */}
                    <ul className="space-y-2">
                      {feature.items.map((item, i) => (
                        <li key={i} className="flex items-center gap-2 text-sm text-muted-foreground">
                          <span className="w-1.5 h-1.5 bg-primary rounded-full"></span>
                          {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              </Card>
            )
          })}
        </div>
      </div>
    </section>
  )
}
