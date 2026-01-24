"use client"

import Link from "next/link"
import { ArrowRight, Play } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useState } from "react"

export default function HeroSection() {
  const [showDemo, setShowDemo] = useState(false)

  return (
    <section className="pt-32 pb-20 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      <div className="absolute inset-0 overflow-hidden">
        {/* Gradient background - blue to cyan to purple */}
        <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-blue-900/20 to-slate-900"></div>

        {/* Animated glow orbs */}
        <div className="absolute top-20 right-1/4 w-96 h-96 bg-cyan-500/20 rounded-full blur-3xl opacity-30 animate-pulse"></div>
        <div className="absolute bottom-10 left-1/3 w-72 h-72 bg-purple-500/20 rounded-full blur-3xl opacity-30 animate-pulse"></div>
        <div className="absolute top-1/2 right-1/6 w-80 h-80 bg-blue-500/15 rounded-full blur-3xl opacity-20 animate-pulse"></div>

        {/* Grid pattern overlay */}
        <div className="absolute inset-0 opacity-10">
          <svg className="w-full h-full" preserveAspectRatio="none">
            <defs>
              <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
                <path d="M 40 0 L 0 0 0 40" fill="none" stroke="currentColor" strokeWidth="0.5" />
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid)" />
          </svg>
        </div>
      </div>

      <div className="max-w-6xl mx-auto relative z-10">
        <div className="text-center max-w-4xl mx-auto mb-16">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full border border-cyan-500/30 bg-cyan-500/5 mb-8">
            <span className="w-2 h-2 bg-cyan-500 rounded-full animate-pulse"></span>
            <span className="text-xs font-medium bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">
              AI-Powered Cloud Cost Intelligence Platform
            </span>
          </div>

          <h1 className="text-6xl sm:text-7xl lg:text-8xl font-bold mb-8 leading-tight tracking-tight">
            Autonomous AI for{" "}
            <span className="bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400 bg-clip-text text-transparent block">
              Multi-Cloud Intelligence
            </span>
          </h1>

          <p className="text-lg sm:text-xl text-muted-foreground mb-10 leading-relaxed max-w-3xl mx-auto">
            Control your cloud economics with intelligent automation. OptiBrain understands, predicts, and autonomously
            optimizes costs across AWS, Azure, GCP, Kubernetes, and beyond.
          </p>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
            <Link href="/auth/signup">
              <Button
                size="lg"
                className="w-full sm:w-auto bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
              >
                Start Free Trial <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
            <Button
              variant="outline"
              size="lg"
              className="w-full sm:w-auto border-cyan-500/30 hover:bg-cyan-500/10 bg-transparent"
              onClick={() => setShowDemo(true)}
            >
              <Play className="w-4 h-4 mr-2 fill-current" /> Watch Demo
            </Button>
          </div>

          {showDemo && (
            <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
              <div className="relative w-full max-w-4xl">
                <button
                  onClick={() => setShowDemo(false)}
                  className="absolute -top-10 right-0 text-white/60 hover:text-white transition"
                >
                  ✕
                </button>
                <div className="rounded-lg overflow-hidden border border-cyan-500/30 glass">
                  <div className="aspect-video bg-slate-900 flex items-center justify-center">
                    <div className="text-center">
                      <div className="w-20 h-20 rounded-full bg-cyan-500/20 flex items-center justify-center mx-auto mb-4">
                        <Play className="w-8 h-8 text-cyan-400 fill-cyan-400" />
                      </div>
                      <p className="text-muted-foreground">OptiBrain Demo Video</p>
                      <p className="text-sm text-muted-foreground mt-2">Autonomous Cloud Cost Optimization</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-20 pt-12 border-t border-border/50">
          <div className="text-center group">
            <div className="text-3xl sm:text-4xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent mb-2">
              40%
            </div>
            <p className="text-xs sm:text-sm text-muted-foreground group-hover:text-foreground transition">
              Avg Cost Savings
            </p>
          </div>
          <div className="text-center group">
            <div className="text-3xl sm:text-4xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent mb-2">
              24/7
            </div>
            <p className="text-xs sm:text-sm text-muted-foreground group-hover:text-foreground transition">
              AI Monitoring
            </p>
          </div>
          <div className="text-center group">
            <div className="text-3xl sm:text-4xl font-bold bg-gradient-to-r from-blue-400 to-cyan-400 bg-clip-text text-transparent mb-2">
              500+
            </div>
            <p className="text-xs sm:text-sm text-muted-foreground group-hover:text-foreground transition">
              Active Customers
            </p>
          </div>
          <div className="text-center group">
            <div className="text-3xl sm:text-4xl font-bold bg-gradient-to-r from-emerald-400 to-teal-400 bg-clip-text text-transparent mb-2">
              $2.3M+
            </div>
            <p className="text-xs sm:text-sm text-muted-foreground group-hover:text-foreground transition">
              Total Saved
            </p>
          </div>
        </div>
      </div>
    </section>
  )
}
