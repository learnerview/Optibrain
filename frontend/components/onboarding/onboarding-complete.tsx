"use client"

import {
  CheckCircle2,
  Zap,
  BarChart3,
  Brain,
  Gauge,
  Shield,
  TrendingDown,
  Clock,
} from "lucide-react"

interface OnboardingCompleteProps {
  selectedProviders: Array<string>
}

export default function OnboardingComplete({ selectedProviders }: OnboardingCompleteProps) {
  const features = [
    {
      icon: Zap,
      title: "Real-time Optimization",
      description: "AI is now analyzing your infrastructure for cost savings",
      color: "from-yellow-500 to-orange-500",
    },
    {
      icon: BarChart3,
      title: "Cost Analytics",
      description: "Dashboard loading your historical and projected costs",
      color: "from-blue-500 to-cyan-500",
    },
    {
      icon: Brain,
      title: "AI Copilot Ready",
      description: "Chat with OptiBrain for instant recommendations",
      color: "from-purple-500 to-pink-500",
    },
    {
      icon: Gauge,
      title: "Performance Monitoring",
      description: "Tracking resource utilization across all regions",
      color: "from-green-500 to-emerald-500",
    },
    {
      icon: TrendingDown,
      title: "Optimization Recommendations",
      description: "ML models generating personalized cost-saving actions",
      color: "from-indigo-500 to-purple-500",
    },
    {
      icon: Shield,
      title: "Security Scanning",
      description: "Analyzing cloud configuration for security risks",
      color: "from-red-500 to-pink-500",
    },
  ]

  return (
    <div className="space-y-12">
      {/* Success header */}
      <div className="text-center">
        <div className="relative w-20 h-20 mx-auto mb-6">
          <div className="absolute inset-0 bg-gradient-to-r from-green-500 to-emerald-500 rounded-full blur-lg opacity-50"></div>
          <div className="relative w-20 h-20 rounded-full bg-gradient-to-r from-green-500 to-emerald-500 flex items-center justify-center">
            <CheckCircle2 className="w-10 h-10 text-white" />
          </div>
        </div>

        <h3 className="text-4xl font-bold mb-2 bg-clip-text text-transparent bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400">
          Setup Complete!
        </h3>
        <p className="text-xl text-muted-foreground mb-2">
          OptiBrain is now connected to your {selectedProviders.join(", ")} account
          {selectedProviders.length > 1 ? "s" : ""}
        </p>
        <p className="text-sm text-cyan-400">
          Welcome to intelligent cloud cost optimization
        </p>
      </div>

      {/* Status cards */}
      <div className="bg-gradient-to-r from-emerald-500/10 to-cyan-500/10 border border-emerald-500/30 rounded-xl p-6">
        <div className="flex items-center gap-3 mb-4">
          <Clock className="w-5 h-5 text-emerald-400" />
          <h4 className="font-semibold">What's Happening Right Now</h4>
        </div>
        <ul className="space-y-2 text-sm">
          <li className="flex items-center gap-2 text-muted-foreground">
            <div className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse"></div>
            Syncing cloud infrastructure metadata
          </li>
          <li className="flex items-center gap-2 text-muted-foreground">
            <div className="w-2 h-2 rounded-full bg-blue-400 animate-pulse"></div>
            Analyzing historical cost patterns
          </li>
          <li className="flex items-center gap-2 text-muted-foreground">
            <div className="w-2 h-2 rounded-full bg-purple-400 animate-pulse"></div>
            Running ML models for optimization opportunities
          </li>
        </ul>
      </div>

      {/* Features grid */}
      <div className="space-y-3">
        <h4 className="text-lg font-semibold">Activated Features</h4>
        <div className="grid md:grid-cols-2 gap-4">
          {features.map((feature, idx) => {
            const Icon = feature.icon
            return (
              <div
                key={idx}
                className={`bg-gradient-to-br ${feature.color} opacity-10 backdrop-blur p-5 rounded-xl border border-${feature.color.split(" ")[1]}-500/30 group hover:border-opacity-100 transition`}
              >
                <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${feature.color} flex items-center justify-center mb-3`}>
                  <Icon className="w-5 h-5 text-white" />
                </div>
                <h5 className="font-semibold mb-1">{feature.title}</h5>
                <p className="text-sm text-muted-foreground">{feature.description}</p>
              </div>
            )
          })}
        </div>
      </div>

      {/* Connected providers summary */}
      <div className="bg-gradient-to-r from-blue-500/10 to-purple-500/10 border border-blue-500/30 rounded-xl p-6">
        <h4 className="font-semibold mb-4">Connected Infrastructure</h4>
        <div className="grid md:grid-cols-3 gap-4">
          {selectedProviders.map((provider) => (
            <div
              key={provider}
              className="p-4 bg-slate-900/50 rounded-lg border border-cyan-500/20"
            >
              <div className="text-2xl mb-2">
                {provider === "AWS" && "☁️"}
                {provider === "Azure" && "#️⃣"}
                {provider === "GCP" && "🔴"}
              </div>
              <p className="font-semibold text-sm">{provider}</p>
              <p className="text-xs text-emerald-400 mt-1">✓ Connected</p>
            </div>
          ))}
        </div>
      </div>

      {/* Next steps */}
      <div className="space-y-3">
        <h4 className="text-lg font-semibold">Next Steps</h4>
        <div className="space-y-2">
          <div className="flex gap-3 p-4 bg-slate-800/50 rounded-lg border border-slate-700 hover:border-cyan-500/50 transition cursor-pointer">
            <div className="w-8 h-8 rounded-full bg-cyan-500/20 flex items-center justify-center flex-shrink-0 text-cyan-400 font-bold">
              1
            </div>
            <div>
              <p className="font-semibold text-sm">Review Dashboard</p>
              <p className="text-xs text-muted-foreground">See real-time cost analytics and insights</p>
            </div>
          </div>
          <div className="flex gap-3 p-4 bg-slate-800/50 rounded-lg border border-slate-700 hover:border-purple-500/50 transition cursor-pointer">
            <div className="w-8 h-8 rounded-full bg-purple-500/20 flex items-center justify-center flex-shrink-0 text-purple-400 font-bold">
              2
            </div>
            <div>
              <p className="font-semibold text-sm">Chat with AI Copilot</p>
              <p className="text-xs text-muted-foreground">Ask questions about your cloud costs</p>
            </div>
          </div>
          <div className="flex gap-3 p-4 bg-slate-800/50 rounded-lg border border-slate-700 hover:border-green-500/50 transition cursor-pointer">
            <div className="w-8 h-8 rounded-full bg-green-500/20 flex items-center justify-center flex-shrink-0 text-green-400 font-bold">
              3
            </div>
            <div>
              <p className="font-semibold text-sm">Review Recommendations</p>
              <p className="text-xs text-muted-foreground">
                Explore optimization opportunities and estimated savings
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Timeline */}
      <div className="bg-gradient-to-r from-cyan-500/5 to-blue-500/5 border border-cyan-500/20 rounded-xl p-6">
        <p className="text-sm text-muted-foreground">
          First cost insights will appear in your dashboard within <span className="font-semibold text-cyan-400">5-10 minutes</span>. Full analytics
          and recommendations typically available within <span className="font-semibold text-cyan-400">1-2 hours</span>.
        </p>
      </div>
    </div>
  )
}
