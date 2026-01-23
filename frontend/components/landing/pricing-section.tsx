"use client"

import { Check } from "lucide-react"
import { Button } from "@/components/ui/button"
import Link from "next/link"

const plans = [
  {
    name: "Starter",
    price: "Free",
    description: "Perfect for getting started",
    features: ["1 cloud provider", "Basic analytics", "7-day data retention", "Community support"],
    cta: "Start Free",
    highlighted: false,
  },
  {
    name: "Professional",
    price: "$299",
    period: "/month",
    description: "For growing teams",
    features: [
      "Unlimited providers",
      "Advanced analytics",
      "90-day data retention",
      "AI Copilot",
      "Anomaly detection",
      "Priority support",
    ],
    cta: "Start Trial",
    highlighted: true,
  },
  {
    name: "Enterprise",
    price: "Custom",
    description: "For large organizations",
    features: [
      "Everything in Pro",
      "2-year data retention",
      "Custom integrations",
      "Dedicated account manager",
      "SLA guarantee",
      "24/7 phone support",
    ],
    cta: "Contact Sales",
    highlighted: false,
  },
]

export default function PricingSection() {
  return (
    <section id="pricing" className="py-20 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-b from-slate-900 via-emerald-900/20 to-slate-900 pointer-events-none" />
      <div className="absolute inset-0 bg-gradient-to-r from-emerald-600/5 via-cyan-600/5 to-teal-600/5 pointer-events-none" />
      <div className="absolute top-1/3 right-1/4 w-[500px] h-[500px] bg-emerald-500/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/3 left-1/4 w-[500px] h-[500px] bg-teal-500/15 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-7xl mx-auto relative z-10">
        <div className="text-center mb-16">
          <h2 className="text-4xl font-bold mb-4">Simple, Transparent Pricing</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">Choose a plan that scales with your team.</p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {plans.map((plan, index) => (
            <div
              key={index}
              className={`rounded-xl p-8 relative transition-all duration-300 ${
                plan.highlighted
                  ? "glass border-primary/50 shadow-lg shadow-primary/20 scale-105"
                  : "glass border-border/50"
              }`}
            >
              {plan.highlighted && (
                <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 px-3 py-1 rounded-full bg-primary text-primary-foreground text-xs font-semibold">
                  Most Popular
                </div>
              )}
              <h3 className="text-2xl font-bold mb-2">{plan.name}</h3>
              <p className="text-sm text-muted-foreground mb-4">{plan.description}</p>
              <div className="mb-6">
                <span className="text-4xl font-bold">{plan.price}</span>
                {plan.period && <span className="text-muted-foreground text-sm">{plan.period}</span>}
              </div>
              <Link href="/auth/signup" className="w-full block mb-8">
                <Button className="w-full" variant={plan.highlighted ? "default" : "outline"}>
                  {plan.cta}
                </Button>
              </Link>
              <ul className="space-y-3">
                {plan.features.map((feature, i) => (
                  <li key={i} className="flex gap-3 text-sm">
                    <Check className="w-4 h-4 text-primary flex-shrink-0 mt-0.5" />
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
