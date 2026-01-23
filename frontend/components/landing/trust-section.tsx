"use client"

import { Shield, Lock, CheckCircle, Award } from "lucide-react"
import { Card } from "@/components/ui/card"

const trustItems = [
  {
    icon: Shield,
    title: "Enterprise Security",
    description: "SOC 2 Type II certified with end-to-end encryption and zero-knowledge architecture",
  },
  {
    icon: Lock,
    title: "Data Privacy",
    description: "GDPR, HIPAA, and SOC 2 compliant. Your data never leaves your VPC.",
  },
  {
    icon: CheckCircle,
    title: "99.99% Uptime SLA",
    description: "Multi-region redundancy and automatic failover across availability zones",
  },
  {
    icon: Award,
    title: "Industry Recognition",
    description: "Trusted by Fortune 500 companies and recognized leader in FinOps",
  },
]

export default function TrustSection() {
  return (
    <section className="py-24 px-4 sm:px-6 lg:px-8 bg-secondary/30">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold mb-4">Enterprise Trust</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            The security and compliance foundation trusted by enterprises worldwide
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {trustItems.map((item, index) => {
            const Icon = item.icon
            return (
              <Card
                key={index}
                className="glass p-6 border-primary/20 hover:border-primary/40 transition-all text-center"
              >
                <div className="w-12 h-12 rounded-xl bg-primary/20 flex items-center justify-center mx-auto mb-4">
                  <Icon className="w-6 h-6 text-primary" />
                </div>
                <h3 className="font-bold mb-2">{item.title}</h3>
                <p className="text-sm text-muted-foreground">{item.description}</p>
              </Card>
            )
          })}
        </div>

        {/* Trust Badges */}
        <div className="mt-16 grid grid-cols-2 md:grid-cols-4 gap-4 pt-12 border-t border-border/50">
          <div className="flex flex-col items-center gap-2 py-4">
            <div className="text-2xl font-bold text-primary">SOC 2</div>
            <p className="text-xs text-muted-foreground text-center">Type II Certified</p>
          </div>
          <div className="flex flex-col items-center gap-2 py-4">
            <div className="text-2xl font-bold text-primary">GDPR</div>
            <p className="text-xs text-muted-foreground text-center">Compliant</p>
          </div>
          <div className="flex flex-col items-center gap-2 py-4">
            <div className="text-2xl font-bold text-primary">HIPAA</div>
            <p className="text-xs text-muted-foreground text-center">Eligible</p>
          </div>
          <div className="flex flex-col items-center gap-2 py-4">
            <div className="text-2xl font-bold text-primary">ISO 27001</div>
            <p className="text-xs text-muted-foreground text-center">Certified</p>
          </div>
        </div>
      </div>
    </section>
  )
}
