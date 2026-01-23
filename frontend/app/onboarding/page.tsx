"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuthStore, useCloudStore } from "@/lib/store"
import ProviderSelection from "@/components/onboarding/provider-selection"
import ProviderConfiguration from "@/components/onboarding/provider-configuration"
import PermissionsSetup from "@/components/onboarding/permissions-setup"
import OnboardingComplete from "@/components/onboarding/onboarding-complete"

export default function OnboardingPage() {
  const router = useRouter()
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const [currentStep, setCurrentStep] = useState(1)
  const [selectedProviders, setSelectedProviders] = useState<Array<"AWS" | "Azure" | "GCP">>([])
  const [configurations, setConfigurations] = useState<Record<string, any>>({})
  const [permissions, setPermissions] = useState({
    costVisibility: true,
    execution: false,
    autoRemediation: false,
    dataExport: true,
    anomalyDetection: false,
    billingOptimization: true,
    securityAudit: false,
    reportGeneration: true,
  })

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/signin")
    }
  }, [isAuthenticated, router])

  const handleProviderToggle = (provider: "AWS" | "Azure" | "GCP") => {
    setSelectedProviders((prev) => (prev.includes(provider) ? prev.filter((p) => p !== provider) : [...prev, provider]))
  }

  const handleConfigurationChange = (provider: string, config: any) => {
    setConfigurations((prev) => ({ ...prev, [provider]: config }))
  }

  const handlePermissionChange = (permission: keyof typeof permissions) => {
    setPermissions((prev) => ({ ...prev, [permission]: !prev[permission] }))
  }

  const handleComplete = () => {
    const addProvider = useCloudStore.getState().addProvider
    selectedProviders.forEach((provider) => {
      addProvider({
        id: `${provider}-${Date.now()}`,
        name: provider,
        accountId: configurations[provider]?.accountId || "",
        accountName: configurations[provider]?.accountName || "",
        environment: configurations[provider]?.environment || "production",
        regions: configurations[provider]?.regions || [],
        connected: true,
        lastSync: new Date().toISOString(),
      })
    })

    router.push("/dashboard")
  }

  const steps = [
    { number: 1, title: "Select Cloud Providers", description: "Choose which clouds to connect" },
    { number: 2, title: "Configure Accounts", description: "Add your account details and regions" },
    { number: 3, title: "Set Permissions", description: "Define what OptiBrain can access" },
    { number: 4, title: "Complete Setup", description: "Ready to optimize!" },
  ]

  const getStepGradient = () => {
    switch (currentStep) {
      case 1:
        return "bg-gradient-to-br from-blue-900/40 via-cyan-900/30 to-blue-900/40"
      case 2:
        return "bg-gradient-to-br from-purple-900/40 via-blue-900/30 to-purple-900/40"
      case 3:
        return "bg-gradient-to-br from-emerald-900/40 via-cyan-900/30 to-emerald-900/40"
      case 4:
        return "bg-gradient-to-br from-violet-900/40 via-purple-900/30 to-violet-900/40"
      default:
        return "bg-gradient-to-br from-blue-900/40 via-cyan-900/30 to-blue-900/40"
    }
  }

  return (
    <div className="min-h-screen bg-background overflow-hidden">
      {/* Animated background gradients */}
      <div className="fixed inset-0 -z-10">
        <div className="absolute top-0 right-0 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl"></div>
        <div className="absolute top-1/2 left-1/2 w-96 h-96 bg-blue-500/5 rounded-full blur-3xl"></div>
      </div>

      <div className="max-w-5xl mx-auto px-4 py-12 relative z-10">
        {/* Header */}
        <div className="text-center mb-16">
          <h1 className="text-4xl font-bold mb-2 bg-clip-text text-transparent bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400">
            Connect Your Cloud Infrastructure
          </h1>
          <p className="text-muted-foreground text-lg">Get started with OptiBrain in minutes</p>
        </div>

        {/* Progress indicator */}
        <div className="mb-16">
          <div className="flex justify-between items-center mb-10">
            {steps.map((step, index) => (
              <div key={step.number} className="flex items-center flex-1">
                <div className="relative flex flex-col items-center">
                  <div
                    className={`w-14 h-14 rounded-full flex items-center justify-center font-bold text-lg transition-all transform ${
                      step.number < currentStep
                        ? "bg-gradient-to-r from-green-500 to-emerald-500 text-white scale-100"
                        : step.number === currentStep
                          ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white scale-110 shadow-lg shadow-cyan-500/50"
                          : "bg-gradient-to-r from-slate-700 to-slate-800 text-muted-foreground"
                    }`}
                  >
                    {step.number < currentStep ? "✓" : step.number}
                  </div>
                  <p className="text-xs font-semibold mt-2 text-center w-24">{step.title}</p>
                </div>
                {index < steps.length - 1 && (
                  <div className={`h-1 flex-1 mx-4 rounded-full transition-all ${step.number < currentStep ? "bg-gradient-to-r from-green-500 to-emerald-500" : "bg-slate-700"}`}></div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Step content */}
        <div className={`${getStepGradient()} backdrop-blur-sm p-8 rounded-2xl border border-cyan-500/20 mb-8 transition-all duration-500 shadow-2xl shadow-cyan-900/20`}>
          <div className="mb-6">
            <h2 className="text-3xl font-bold mb-2">{steps[currentStep - 1].title}</h2>
            <p className="text-muted-foreground">{steps[currentStep - 1].description}</p>
          </div>

          <div className="min-h-96">
            {currentStep === 1 && (
              <ProviderSelection selectedProviders={selectedProviders} onToggle={handleProviderToggle} />
            )}
            {currentStep === 2 && (
              <ProviderConfiguration
                selectedProviders={selectedProviders}
                configurations={configurations}
                onChange={handleConfigurationChange}
              />
            )}
            {currentStep === 3 && <PermissionsSetup permissions={permissions} onChange={handlePermissionChange} />}
            {currentStep === 4 && <OnboardingComplete selectedProviders={selectedProviders} />}
          </div>
        </div>

        {/* Navigation buttons */}
        <div className="flex justify-between items-center gap-4">
          <button
            onClick={() => setCurrentStep(Math.max(1, currentStep - 1))}
            className="px-8 py-3 rounded-lg border border-cyan-500/30 text-foreground hover:bg-cyan-500/10 transition-all disabled:opacity-50 disabled:cursor-not-allowed font-semibold"
            disabled={currentStep === 1}
          >
            Back
          </button>

          <div className="text-center flex-1">
            <p className="text-sm text-muted-foreground">
              Step {currentStep} of {steps.length}
            </p>
          </div>

          <button
            onClick={() => {
              if (currentStep === 4) {
                handleComplete()
              } else {
                setCurrentStep(currentStep + 1)
              }
            }}
            className="px-8 py-3 rounded-lg bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:from-cyan-600 hover:to-blue-600 transition-all font-semibold shadow-lg shadow-cyan-500/50 disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={selectedProviders.length === 0 && currentStep === 1}
          >
            {currentStep === 4 ? "Go to Dashboard" : "Next"}
          </button>
        </div>

        {/* Info section */}
        <div className="mt-12 grid md:grid-cols-3 gap-6">
          <div className="bg-gradient-to-br from-blue-900/20 to-cyan-900/20 backdrop-blur p-6 rounded-xl border border-cyan-500/20">
            <div className="text-3xl mb-3">🔒</div>
            <h3 className="font-semibold mb-2">Enterprise Security</h3>
            <p className="text-sm text-muted-foreground">Military-grade encryption for all connections</p>
          </div>
          <div className="bg-gradient-to-br from-purple-900/20 to-blue-900/20 backdrop-blur p-6 rounded-xl border border-purple-500/20">
            <div className="text-3xl mb-3">⚡</div>
            <h3 className="font-semibold mb-2">Instant Setup</h3>
            <p className="text-sm text-muted-foreground">Connect multiple clouds in under 5 minutes</p>
          </div>
          <div className="bg-gradient-to-br from-emerald-900/20 to-cyan-900/20 backdrop-blur p-6 rounded-xl border border-emerald-500/20">
            <div className="text-3xl mb-3">📊</div>
            <h3 className="font-semibold mb-2">Real-time Insights</h3>
            <p className="text-sm text-muted-foreground">Start seeing savings within hours</p>
          </div>
        </div>
      </div>
    </div>
  )
}
