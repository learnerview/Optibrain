"use client"

import type { ReactNode } from "react"
import { Brain, Zap, BarChart3, Shield } from "lucide-react"

const features = [
  { icon: Brain, text: "AI-Powered Optimization" },
  { icon: Zap, text: "Real-Time Cost Intelligence" },
  { icon: BarChart3, text: "Predictive Forecasting" },
  { icon: Shield, text: "Enterprise Security" },
]

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex relative overflow-hidden">
      {/* Left Side - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-background via-primary/10 to-accent/10 flex-col justify-between p-12 relative overflow-hidden">
        {/* Background effects */}
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute top-1/4 -left-1/3 w-96 h-96 bg-primary/20 rounded-full blur-3xl opacity-40"></div>
          <div className="absolute bottom-1/4 -right-1/3 w-96 h-96 bg-accent/20 rounded-full blur-3xl opacity-40"></div>
        </div>

        <div className="relative z-10">
          {/* Logo */}
          <div className="flex items-center gap-3 mb-16">
            <div className="w-10 h-10 bg-gradient-to-br from-primary to-accent rounded-xl flex items-center justify-center shadow-lg">
              <span className="text-primary-foreground font-bold text-xl">O</span>
            </div>
            <div>
              <span className="text-2xl font-bold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                OptiBrain
              </span>
              <div className="text-xs text-muted-foreground">Autonomous Cloud Cost Intelligence OS</div>
            </div>
          </div>

          {/* Tagline */}
          <div className="max-w-md mb-16">
            <h1 className="text-4xl font-bold mb-4 leading-tight">
              Control Your{" "}
              <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
                Multi-Cloud Costs
              </span>
            </h1>
            <p className="text-lg text-muted-foreground">
              Join enterprise teams saving an average of 40% on cloud infrastructure.
            </p>
          </div>

          {/* Features List */}
          <div className="space-y-4">
            {features.map((feature, index) => {
              const Icon = feature.icon
              return (
                <div key={index} className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center flex-shrink-0">
                    <Icon className="w-5 h-5 text-primary" />
                  </div>
                  <span className="text-sm font-medium">{feature.text}</span>
                </div>
              )
            })}
          </div>
        </div>

        {/* Bottom Stats */}
        <div className="relative z-10 grid grid-cols-3 gap-4 pt-12 border-t border-primary/20">
          <div>
            <div className="text-2xl font-bold text-primary mb-1">$45M+</div>
            <p className="text-xs text-muted-foreground">Saved by customers</p>
          </div>
          <div>
            <div className="text-2xl font-bold text-accent mb-1">500+</div>
            <p className="text-xs text-muted-foreground">Enterprise customers</p>
          </div>
          <div>
            <div className="text-2xl font-bold text-primary mb-1">40%</div>
            <p className="text-xs text-muted-foreground">Avg savings</p>
          </div>
        </div>
      </div>

      {/* Right Side - Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-4 relative overflow-hidden">
        {/* Background effects for mobile */}
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute top-1/4 -left-1/3 w-96 h-96 bg-primary/10 rounded-full blur-3xl opacity-30"></div>
          <div className="absolute bottom-1/4 -right-1/3 w-96 h-96 bg-accent/10 rounded-full blur-3xl opacity-30"></div>
        </div>

        <div className="relative z-10 w-full max-w-md">{children}</div>
      </div>
    </div>
  )
}
