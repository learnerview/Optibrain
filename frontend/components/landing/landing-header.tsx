"use client"

import type React from "react"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import OptiBrainLogo from "./optibrain-logo"

const navItems = [
  { label: "Features", href: "#features" },
  { label: "Solutions", href: "#complete-features" },
  { label: "How It Works", href: "#how-it-works" },
  { label: "Analytics", href: "#testimonials" },
  { label: "Pricing", href: "#pricing" },
  { label: "About", href: "#about" },
]

export default function LandingHeader() {
  const handleSmoothScroll = (e: React.MouseEvent<HTMLAnchorElement>, href: string) => {
    e.preventDefault()
    const element = document.querySelector(href)
    if (element) {
      element.scrollIntoView({ behavior: "smooth" })
    }
  }

  return (
    <header className="fixed top-0 left-0 right-0 z-50 glass border-b border-border">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <Link href="/" className="hover:opacity-80 transition">
            <OptiBrainLogo />
          </Link>

          <nav className="hidden lg:flex gap-1 text-sm">
            {navItems.map((item) => (
              <Link
                key={item.label}
                href={item.href}
                onClick={(e) => handleSmoothScroll(e, item.href)}
                className="px-3 py-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-white/5 transition"
              >
                {item.label}
              </Link>
            ))}
          </nav>

          <div className="flex gap-3">
            <Link href="/auth/signin">
              <Button variant="outline" size="sm">
                Sign In
              </Button>
            </Link>
            <Link href="/auth/signup">
              <Button size="sm">Get Started</Button>
            </Link>
          </div>
        </div>
      </div>
    </header>
  )
}
