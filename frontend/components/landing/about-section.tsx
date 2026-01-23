"use client"

import { Mail, Linkedin, Twitter, Github } from "lucide-react"

export default function AboutSection() {
  return (
    <section id="about" className="py-24 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-r from-emerald-600/10 via-teal-600/5 to-cyan-600/10 pointer-events-none" />

      <div className="max-w-6xl mx-auto relative z-10">
        <div className="grid md:grid-cols-2 gap-12 lg:gap-16 items-center">
          {/* Left side - Logo and mission */}
          <div className="flex flex-col items-start justify-center">
            <div className="mb-8">
              {/* OptiBrain Logo with neon effect */}
              <div className="relative w-24 h-24 mb-6">
                <svg className="w-full h-full" viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
                  {/* Brain outline with neon glow */}
                  <defs>
                    <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
                      <feGaussianBlur stdDeviation="3" result="coloredBlur" />
                      <feMerge>
                        <feMergeNode in="coloredBlur" />
                        <feMergeNode in="SourceGraphic" />
                      </feMerge>
                    </filter>
                  </defs>
                  <circle cx="100" cy="100" r="85" fill="none" stroke="#06b6d4" strokeWidth="2" opacity="0.3" />
                  <circle cx="100" cy="100" r="70" fill="none" stroke="#8b5cf6" strokeWidth="1.5" opacity="0.2" />
                  <circle cx="100" cy="100" r="50" fill="none" stroke="#06b6d4" strokeWidth="2" filter="url(#glow)" />
                  <circle cx="100" cy="100" r="15" fill="#06b6d4" opacity="0.6" filter="url(#glow)" />
                </svg>
              </div>

              <h2 className="text-4xl sm:text-5xl font-bold mb-4">
                About{" "}
                <span className="bg-gradient-to-r from-cyan-400 to-emerald-400 bg-clip-text text-transparent">
                  OptiBrain
                </span>
              </h2>

              <p className="text-lg text-muted-foreground mb-6 leading-relaxed">
                OptiBrain is an autonomous AI platform designed to revolutionize how enterprises manage multi-cloud
                financial operations. We combine cutting-edge machine learning with deep FinOps expertise to deliver
                intelligent, real-time cost optimization across AWS, Azure, GCP, and beyond.
              </p>

              <div className="space-y-4 mb-8">
                <div className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full bg-gradient-to-br from-cyan-500 to-purple-500 flex items-center justify-center flex-shrink-0 mt-1">
                    <span className="text-white text-sm font-bold">✓</span>
                  </div>
                  <div>
                    <p className="font-semibold text-white">Mission</p>
                    <p className="text-sm text-muted-foreground">
                      Empower engineering teams with autonomous intelligence to optimize cloud spending intelligently
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full bg-gradient-to-br from-emerald-500 to-teal-500 flex items-center justify-center flex-shrink-0 mt-1">
                    <span className="text-white text-sm font-bold">✓</span>
                  </div>
                  <div>
                    <p className="font-semibold text-white">Vision</p>
                    <p className="text-sm text-muted-foreground">
                      Create a world where cloud costs are automatically optimized, AI agents handle complexity, and
                      teams focus on innovation
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center flex-shrink-0 mt-1">
                    <span className="text-white text-sm font-bold">✓</span>
                  </div>
                  <div>
                    <p className="font-semibold text-white">Values</p>
                    <p className="text-sm text-muted-foreground">
                      Autonomous intelligence, continuous learning, enterprise trust, and measurable impact
                    </p>
                  </div>
                </div>
              </div>

              {/* Social Links */}
              <div className="flex items-center gap-4">
                <span className="text-sm text-muted-foreground">Connect with us:</span>
                <div className="flex gap-3">
                  <a
                    href="#"
                    className="w-10 h-10 rounded-lg bg-gradient-to-br from-cyan-500/20 to-blue-500/20 border border-cyan-500/30 flex items-center justify-center hover:bg-cyan-500/30 transition-all hover:shadow-lg hover:shadow-cyan-500/20"
                  >
                    <Mail className="w-5 h-5 text-cyan-400" />
                  </a>
                  <a
                    href="#"
                    className="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500/20 to-purple-500/20 border border-blue-500/30 flex items-center justify-center hover:bg-blue-500/30 transition-all hover:shadow-lg hover:shadow-blue-500/20"
                  >
                    <Linkedin className="w-5 h-5 text-blue-400" />
                  </a>
                  <a
                    href="#"
                    className="w-10 h-10 rounded-lg bg-gradient-to-br from-purple-500/20 to-pink-500/20 border border-purple-500/30 flex items-center justify-center hover:bg-purple-500/30 transition-all hover:shadow-lg hover:shadow-purple-500/20"
                  >
                    <Twitter className="w-5 h-5 text-purple-400" />
                  </a>
                  <a
                    href="#"
                    className="w-10 h-10 rounded-lg bg-gradient-to-br from-slate-500/20 to-slate-600/20 border border-slate-500/30 flex items-center justify-center hover:bg-slate-500/30 transition-all hover:shadow-lg hover:shadow-slate-500/20"
                  >
                    <Github className="w-5 h-5 text-slate-400" />
                  </a>
                </div>
              </div>
            </div>
          </div>

          {/* Right side - Stats and Trust */}
          <div className="grid grid-cols-2 gap-6">
            {[
              { number: "$2.3B+", label: "Cloud Costs Analyzed" },
              { number: "1000+", label: "Enterprise Customers" },
              { number: "50%", label: "Avg. Cost Reduction" },
              { number: "24/7", label: "Autonomous Operation" },
            ].map((stat, i) => (
              <div
                key={i}
                className="glass p-6 rounded-xl border border-border/50 hover:border-emerald-500/50 transition-all hover:shadow-lg hover:shadow-emerald-500/10 text-center group"
              >
                <div className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-emerald-400 bg-clip-text text-transparent mb-2 group-hover:scale-110 transition-transform">
                  {stat.number}
                </div>
                <p className="text-sm text-muted-foreground">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
