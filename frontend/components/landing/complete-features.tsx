"use client"

import { MessageSquare, Box, TrendingUp, Leaf, Zap, Shield, CloudLightning as Lightning, Globe } from "lucide-react"

const features = [
  {
    icon: Zap,
    title: "Autonomous Decision Engine",
    description:
      "Self-healing AI that makes 1000s of optimization decisions daily without human intervention. Real-time scaling, scheduling, and rightsizing.",
    color: "from-cyan-500 to-blue-500",
  },
  {
    icon: MessageSquare,
    title: "AI Copilot Chat",
    description:
      "Natural language interface to query your cloud costs. Ask 'What's my top 5 services by cost?' and get instant answers with actionable insights.",
    color: "from-purple-500 to-pink-500",
  },
  {
    icon: Box,
    title: "3D Infrastructure Topology",
    description:
      "Visualize your entire cloud as an interactive 3D city. Buildings represent services, roads show traffic flow, colors indicate health.",
    color: "from-purple-500 to-indigo-500",
  },
  {
    icon: TrendingUp,
    title: "Prediction Markets",
    description:
      "Gamified forecasting for engineering teams. Bet on feature delivery, capacity needs, and budget outcomes with OptiPoints rewards.",
    color: "from-orange-500 to-red-500",
  },
  {
    icon: Leaf,
    title: "GreenOps Carbon Dashboard",
    description:
      "Track carbon footprint in real-time. Follow-the-sun scheduling automatically routes workloads to regions with lowest carbon intensity.",
    color: "from-emerald-500 to-teal-500",
  },
  {
    icon: Lightning,
    title: "DePIN Arbitrage Engine",
    description:
      "Automatically find 40-70% cheaper compute on decentralized networks like Akash and Render. One-click migration with TEE security.",
    color: "from-indigo-500 to-purple-500",
  },
  {
    icon: Shield,
    title: "Military-Grade Safety",
    description:
      "Human-in-the-loop for high-risk decisions. Bias radius containment, automatic rollback, multi-layer approval workflows.",
    color: "from-blue-500 to-cyan-500",
  },
  {
    icon: Lightning,
    title: "50ms Decision Latency",
    description:
      "Ultra-fast execution with global edge deployment. Real-time bidding on spot instances, instant anomaly remediation.",
    color: "from-pink-500 to-purple-500",
  },
  {
    icon: Globe,
    title: "Multi-Cloud Intelligence",
    description:
      "Unified optimization across AWS, Azure, GCP, and Kubernetes. Single pane of glass for all your cloud economics.",
    color: "from-cyan-500 to-emerald-500",
  },
]

export default function CompleteFeatures() {
  return (
    <section id="complete-features" className="py-24 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-b from-slate-900 via-blue-900/20 to-slate-900 pointer-events-none" />
      <div className="absolute inset-0 bg-gradient-to-r from-cyan-600/5 via-purple-600/5 to-blue-600/5 pointer-events-none" />
      <div className="absolute top-20 right-0 w-[600px] h-[600px] bg-cyan-500/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-20 left-0 w-[600px] h-[600px] bg-purple-500/15 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-7xl mx-auto relative z-10">
        <div className="text-center mb-16">
          <h2 className="text-4xl sm:text-5xl font-bold mb-4">
            Complete Cloud Cost Intelligence{" "}
            <span className="bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">Solution</span>
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Enterprise-grade features purpose-built for autonomous cloud cost optimization and intelligence.
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => {
            const Icon = feature.icon
            return (
              <div
                key={index}
                className="group relative glass p-6 rounded-xl border border-border/50 hover:border-cyan-500/50 transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/10 overflow-hidden"
              >
                {/* Gradient background on hover */}
                <div
                  className={`absolute inset-0 bg-gradient-to-br ${feature.color} opacity-0 group-hover:opacity-5 transition-opacity duration-300`}
                />

                <div className="relative z-10">
                  <div
                    className={`w-12 h-12 rounded-lg bg-gradient-to-br ${feature.color} p-0.5 mb-4 group-hover:shadow-lg group-hover:shadow-current/20 transition-all duration-300`}
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
