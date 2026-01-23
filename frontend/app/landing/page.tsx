"use client"

import LandingHeader from "@/components/landing/landing-header"
import HeroSection from "@/components/landing/hero-section"
import HowItWorks from "@/components/landing/how-it-works"
import Testimonials from "@/components/landing/testimonials"
import CompleteFeatures from "@/components/landing/complete-features"
import PricingSection from "@/components/landing/pricing-section"
import CTASection from "@/components/landing/cta-section"
import LandingFooter from "@/components/landing/landing-footer"
import AboutSection from "@/components/landing/about-section"

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-background">
      <LandingHeader />
      <HeroSection />
      <HowItWorks />
      <CompleteFeatures />
      <Testimonials />
      <AboutSection />
      <PricingSection />
      <CTASection />
      <LandingFooter />
    </div>
  )
}
