"use client"

import { Star } from "lucide-react"

const testimonials = [
  {
    quote:
      "OptiBrain cut our AWS bill by 47% in the first month. The autonomous decision engine is like having a team of expert Cloud Cost Intelligence engineers working 24/7.",
    savings: "$2.3M saved annually",
    author: "Sarah Chen",
    title: "VP of Engineering",
    company: "TechScale Inc.",
    initials: "SC",
    bgColor: "from-cyan-500 to-blue-500",
  },
  {
    quote:
      "The 3D infrastructure visualization completely changed how we understand our cloud architecture. It's like having X-ray vision into our costs.",
    savings: "$890K saved annually",
    author: "Michael Rodriguez",
    title: "Cloud Architect",
    company: "DataFlow Systems",
    initials: "MR",
    bgColor: "from-purple-500 to-pink-500",
  },
  {
    quote:
      "The AI Copilot is incredible. I just ask questions in plain English and get instant answers with actionable recommendations. Game changer.",
    savings: "$1.5M saved annually",
    author: "Emily Watson",
    title: "Finance Lead",
    company: "CloudFirst Corp",
    initials: "EW",
    bgColor: "from-cyan-500 to-teal-500",
  },
  {
    quote:
      "The prediction markets feature got our whole engineering team engaged in cost optimization. It's actually fun now. Plus the accuracy is mind-blowing.",
    savings: "$3.1M saved annually",
    author: "James Park",
    title: "CTO",
    company: "Innovate Labs",
    initials: "JP",
    bgColor: "from-blue-500 to-purple-500",
  },
  {
    quote:
      "GreenOps helped us hit our sustainability targets while reducing costs. The carbon-aware scheduling is brilliant engineering.",
    savings: "40% carbon reduction",
    author: "Lisa Thompson",
    title: "Sustainability Director",
    company: "EcoTech Global",
    initials: "LT",
    bgColor: "from-emerald-500 to-green-500",
  },
  {
    quote:
      "Migrating spot workloads to Akash through the DePIN Arbitrage Engine saved 60% on compute costs. This is the future of cloud.",
    savings: "$720K saved annually",
    author: "David Kim",
    title: "Platform Lead",
    company: "Web3 Ventures",
    initials: "DK",
    bgColor: "from-purple-500 to-indigo-500",
  },
]

const companies = [
  { name: "AWS", color: "text-orange-400" },
  { name: "Google Cloud", color: "text-blue-400" },
  { name: "Microsoft Azure", color: "text-cyan-400" },
  { name: "Datadog", color: "text-purple-400" },
  { name: "Snowflake", color: "text-blue-300" },
  { name: "Stripe", color: "text-blue-600" },
]

export default function Testimonials() {
  return (
    <section id="testimonials" className="py-24 px-4 sm:px-6 lg:px-8 relative">
      <div className="absolute inset-0 bg-gradient-to-b from-cyan-500/5 via-transparent to-purple-500/5 pointer-events-none" />

      <div className="max-w-7xl mx-auto relative z-10">
        <div className="text-center mb-16">
          <div className="flex justify-center gap-1 mb-4">
            {[...Array(5)].map((_, i) => (
              <Star key={i} className="w-5 h-5 fill-cyan-400 text-cyan-400" />
            ))}
          </div>
          <h2 className="text-4xl sm:text-5xl font-bold mb-4">
            Trusted by{" "}
            <span className="bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
              Industry Leaders
            </span>
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            See what our customers are saying about their experience with OptiBrain.
          </p>
        </div>

        {/* Testimonial Cards Grid */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6 mb-16">
          {testimonials.map((testimonial, index) => (
            <div
              key={index}
              className="group relative glass p-6 rounded-xl border border-border/50 hover:border-cyan-500/50 transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/10 overflow-hidden"
            >
              {/* Gradient background on hover */}
              <div
                className={`absolute inset-0 bg-gradient-to-br ${testimonial.bgColor} opacity-0 group-hover:opacity-5 transition-opacity duration-300`}
              />

              <div className="relative z-10">
                {/* Quote marks and testimonial text */}
                <div className="mb-4">
                  <p className="text-sm text-cyan-400 text-2xl leading-none mb-2">''</p>
                  <p className="text-muted-foreground text-sm leading-relaxed">{testimonial.quote}</p>
                </div>

                {/* Savings amount */}
                <div className="mb-4 pt-4 border-t border-border/30">
                  <p
                    className={`text-sm font-semibold bg-gradient-to-r ${testimonial.bgColor} bg-clip-text text-transparent`}
                  >
                    {testimonial.savings}
                  </p>
                </div>

                {/* Author info */}
                <div className="flex items-center gap-3">
                  <div
                    className={`w-10 h-10 rounded-full bg-gradient-to-br ${testimonial.bgColor} flex items-center justify-center text-white font-semibold text-sm`}
                  >
                    {testimonial.initials}
                  </div>
                  <div>
                    <p className="text-sm font-semibold">{testimonial.author}</p>
                    <p className="text-xs text-muted-foreground">
                      {testimonial.title} at {testimonial.company}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Trusted by section */}
        <div className="text-center py-12 border-t border-border/50">
          <p className="text-sm text-muted-foreground mb-8">Trusted by teams at industry-leading companies</p>
          <div className="flex flex-wrap justify-center gap-6 sm:gap-8">
            {companies.map((company, index) => (
              <div
                key={index}
                className={`text-lg font-semibold ${company.color} opacity-60 hover:opacity-100 transition`}
              >
                {company.name}
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
