"use client"

import Link from "next/link"
import { Github, Linkedin, Twitter, Mail } from "lucide-react"

const footerSections = [
  {
    title: "Product",
    links: [
      { label: "Features", href: "#features" },
      { label: "Pricing", href: "#pricing" },
      { label: "Security", href: "#security" },
      { label: "Roadmap", href: "#" },
      { label: "Status Page", href: "#" },
    ],
  },
  {
    title: "Company",
    links: [
      { label: "About", href: "#" },
      { label: "Blog", href: "#" },
      { label: "Careers", href: "#" },
      { label: "Press Kit", href: "#" },
      { label: "Partners", href: "#" },
    ],
  },
  {
    title: "Resources",
    links: [
      { label: "Documentation", href: "#" },
      { label: "API Reference", href: "#" },
      { label: "Community", href: "#" },
      { label: "Support", href: "#" },
      { label: "Contact", href: "#" },
    ],
  },
  {
    title: "Legal",
    links: [
      { label: "Privacy Policy", href: "#" },
      { label: "Terms of Service", href: "#" },
      { label: "Cookie Policy", href: "#" },
      { label: "DPA", href: "#" },
      { label: "SOC 2", href: "#" },
    ],
  },
]

const socialLinks = [
  { icon: Twitter, href: "#", label: "Twitter" },
  { icon: Linkedin, href: "#", label: "LinkedIn" },
  { icon: Github, href: "#", label: "GitHub" },
  { icon: Mail, href: "#", label: "Email" },
]

export default function LandingFooter() {
  return (
    <footer className="relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-b from-slate-900 via-slate-900/90 to-slate-950 pointer-events-none" />
      <div className="absolute inset-0 bg-gradient-to-r from-cyan-600/5 via-purple-600/5 to-blue-600/5 pointer-events-none" />
      <div className="absolute top-0 left-1/4 w-[400px] h-[400px] bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-1/4 w-[400px] h-[400px] bg-purple-500/10 rounded-full blur-3xl pointer-events-none" />

      {/* Top border with gradient */}
      <div className="h-px bg-gradient-to-r from-transparent via-cyan-500/50 to-transparent" />

      <div className="relative z-10 py-16 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          {/* Newsletter signup */}
          <div className="mb-16 p-8 rounded-xl glass border border-cyan-500/30 bg-gradient-to-r from-cyan-500/10 to-purple-500/10">
            <h3 className="text-2xl font-bold mb-2">Stay Updated</h3>
            <p className="text-muted-foreground mb-4">
              Get the latest Cloud Cost Intelligence insights and OptiBrain updates delivered to your inbox.
            </p>
            <div className="flex gap-2 max-w-md">
              <input
                type="email"
                placeholder="your@email.com"
                className="flex-1 px-4 py-2 rounded-lg bg-slate-800/50 border border-border/50 text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-cyan-500/50"
              />
              <button className="px-6 py-2 rounded-lg bg-gradient-to-r from-cyan-500 to-purple-500 text-white font-medium hover:from-cyan-600 hover:to-purple-600 transition">
                Subscribe
              </button>
            </div>
          </div>

          {/* Main footer grid */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-8 mb-12">
            {/* Brand section */}
            <div className="col-span-2 md:col-span-1">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-10 h-10 bg-gradient-to-br from-cyan-400 to-purple-500 rounded-lg flex items-center justify-center shadow-lg shadow-cyan-500/20">
                  <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z" />
                  </svg>
                </div>
                <span className="font-bold text-xl bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">
                  OptiBrain
                </span>
              </div>
              <p className="text-sm text-muted-foreground mb-6">
                Autonomous Cloud Cost Intelligence operating system for multi-cloud enterprises. Making cloud economics intelligent and
                autonomous.
              </p>
              {/* Social links */}
              <div className="flex gap-3">
                {socialLinks.map((social) => {
                  const Icon = social.icon
                  return (
                    <Link
                      key={social.label}
                      href={social.href}
                      className="w-10 h-10 rounded-lg bg-gradient-to-br from-cyan-500/20 to-purple-500/20 hover:from-cyan-500/40 hover:to-purple-500/40 flex items-center justify-center text-cyan-400 hover:text-cyan-300 transition border border-cyan-500/30 hover:border-cyan-500/60"
                      aria-label={social.label}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <Icon className="w-4 h-4" />
                    </Link>
                  )
                })}
              </div>
            </div>

            {/* Footer sections */}
            {footerSections.map((section) => (
              <div key={section.title}>
                <h4 className="font-semibold mb-4 text-sm uppercase tracking-wide text-cyan-400">{section.title}</h4>
                <ul className="space-y-3">
                  {section.links.map((link) => (
                    <li key={link.label}>
                      <Link href={link.href} className="text-sm text-muted-foreground hover:text-cyan-400 transition">
                        {link.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>

          {/* Divider */}
          <div className="h-px bg-gradient-to-r from-transparent via-border/50 to-transparent my-8" />

          {/* Bottom section with certifications */}
          <div className="mb-8 p-6 rounded-lg bg-slate-800/30 border border-border/30">
            <h4 className="text-sm font-semibold text-cyan-400 mb-4">Trust & Security</h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-sm">
                <p className="font-medium">SOC 2 Type II</p>
                <p className="text-xs text-muted-foreground">Certified</p>
              </div>
              <div className="text-sm">
                <p className="font-medium">ISO 27001</p>
                <p className="text-xs text-muted-foreground">Compliant</p>
              </div>
              <div className="text-sm">
                <p className="font-medium">GDPR</p>
                <p className="text-xs text-muted-foreground">Compliant</p>
              </div>
              <div className="text-sm">
                <p className="font-medium">99.99% Uptime</p>
                <p className="text-xs text-muted-foreground">SLA</p>
              </div>
            </div>
          </div>

          {/* Copyright and links */}
          <div className="border-t border-border/30 pt-8 flex flex-col md:flex-row justify-between items-center gap-6">
            <p className="text-sm text-muted-foreground">
              © 2025 OptiBrain Inc. All rights reserved. | Made with ❤️ for the cloud.
            </p>
            <div className="flex gap-6 text-sm text-muted-foreground">
              <Link href="#" className="hover:text-cyan-400 transition">
                Status
              </Link>
              <Link href="#" className="hover:text-cyan-400 transition">
                Changelog
              </Link>
              <Link href="#" className="hover:text-cyan-400 transition">
                Credits
              </Link>
              <Link href="#" className="hover:text-cyan-400 transition">
                Sitemap
              </Link>
            </div>
          </div>
        </div>
      </div>
    </footer>
  )
}
