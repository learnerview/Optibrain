"use client"

import Link from "next/link"
import { ArrowRight, Zap } from "lucide-react"
import { Button } from "@/components/ui/button"

export default function CTASection() {
  return (
    <section className="py-24 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-slate-900 via-blue-900/30 to-slate-900" />
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/10 via-slate-900/50 to-cyan-600/10" />

        {/* Subtle blue blur orbs for depth */}
        <div className="absolute top-20 right-1/4 w-96 h-96 bg-blue-500/15 rounded-full blur-3xl opacity-30"></div>
        <div className="absolute bottom-20 left-1/3 w-80 h-80 bg-cyan-500/15 rounded-full blur-3xl opacity-30"></div>
      </div>

      <div className="max-w-5xl mx-auto relative z-10">
        <div className="glass rounded-2xl p-12 md:p-20 text-center border border-primary/30 hover:border-primary/50 transition-all">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-primary/30 bg-primary/5 mb-8">
            <Zap className="w-3 h-3 text-primary" />
            <span className="text-xs font-medium text-primary">Limited Time Offer</span>
          </div>

          <h2 className="text-4xl md:text-5xl lg:text-6xl font-bold mb-4 text-balance">
            Stop Wasting Money on{" "}
            <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
              Cloud Infrastructure
            </span>
          </h2>

          <p className="text-lg md:text-xl text-muted-foreground mb-8 max-w-2xl mx-auto text-balance">
            Join 500+ enterprises saving an average of 40% on cloud costs. OptiBrain handles the optimization, you keep
            the savings.
          </p>

          {/* Stats */}
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8 py-6 border-y border-border/50">
            <div>
              <div className="text-2xl font-bold text-primary">$45M+</div>
              <p className="text-xs text-muted-foreground">Saved by customers</p>
            </div>
            <div>
              <div className="text-2xl font-bold text-accent">40%</div>
              <p className="text-xs text-muted-foreground">Average savings</p>
            </div>
            <div className="col-span-2 md:col-span-1">
              <div className="text-2xl font-bold text-primary">24/7</div>
              <p className="text-xs text-muted-foreground">AI monitoring</p>
            </div>
          </div>

          {/* Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link href="/auth/signup">
              <Button size="lg" className="w-full sm:w-auto">
                Start Free Trial <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
            <Button size="lg" variant="outline" className="w-full sm:w-auto bg-transparent">
              Schedule Demo
            </Button>
          </div>

          <p className="text-xs text-muted-foreground mt-6">
            No credit card required. 14-day free trial. Full feature access.
          </p>
        </div>
      </div>
    </section>
  )
}
