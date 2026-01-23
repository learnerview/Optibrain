"use client"

import { Cloud, AlertCircle } from "lucide-react"

interface ProviderSelectionProps {
  selectedProviders: Array<"AWS" | "Azure" | "GCP">
  onToggle: (provider: "AWS" | "Azure" | "GCP") => void
}

const providers = [
  {
    name: "AWS",
    icon: "☁️",
    color: "from-orange-500 to-orange-600",
    borderColor: "border-orange-500/50",
    hoverColor: "hover:shadow-orange-500/50",
    bgColor: "bg-orange-500/10",
    description: "Amazon Web Services",
    features: ["EC2, S3, RDS", "Lambda, DynamoDB", "CloudFront, ELB"],
  },
  {
    name: "Azure",
    icon: "#️⃣",
    color: "from-blue-500 to-blue-600",
    borderColor: "border-blue-500/50",
    hoverColor: "hover:shadow-blue-500/50",
    bgColor: "bg-blue-500/10",
    description: "Microsoft Azure",
    features: ["Virtual Machines", "App Services", "Azure SQL Database"],
  },
  {
    name: "GCP",
    icon: "🔴",
    color: "from-red-500 to-red-600",
    borderColor: "border-red-500/50",
    hoverColor: "hover:shadow-red-500/50",
    bgColor: "bg-red-500/10",
    description: "Google Cloud Platform",
    features: ["Compute Engine", "Cloud Storage", "BigQuery"],
  },
]

export default function ProviderSelection({ selectedProviders, onToggle }: ProviderSelectionProps) {
  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-2xl font-semibold mb-3">Select Your Cloud Providers</h3>
        <p className="text-muted-foreground">
          Choose all cloud platforms you want to monitor. You can add or remove them later from settings.
        </p>
      </div>

      <div className="grid md:grid-cols-3 gap-6 mt-8">
        {providers.map((provider) => {
          const isSelected = selectedProviders.includes(provider.name as "AWS" | "Azure" | "GCP")
          return (
            <button
              key={provider.name}
              onClick={() => onToggle(provider.name as "AWS" | "Azure" | "GCP")}
              className={`relative p-8 rounded-2xl border-2 transition-all transform hover:scale-105 group ${
                isSelected
                  ? `border-${provider.name === "AWS" ? "orange" : provider.name === "Azure" ? "blue" : "red"}-500 ${provider.bgColor} shadow-xl shadow-${provider.name === "AWS" ? "orange" : provider.name === "Azure" ? "blue" : "red"}-500/30`
                  : `border-slate-700 bg-slate-800/30 hover:border-slate-600 ${provider.hoverColor}`
              }`}
            >
              {/* Gradient background for selected */}
              {isSelected && (
                <div className={`absolute inset-0 bg-gradient-to-br ${provider.color} opacity-5 rounded-2xl`}></div>
              )}

              {/* Icon */}
              <div className="relative z-10">
                <div className={`text-6xl mb-6 transition-transform group-hover:scale-110 ${isSelected ? "scale-110" : ""}`}>
                  {provider.name === "AWS" && "☁️"}
                  {provider.name === "Azure" && "#️⃣"}
                  {provider.name === "GCP" && "🔴"}
                </div>

                {/* Provider name and description */}
                <h4 className="font-bold text-xl mb-2">{provider.name}</h4>
                <p className="text-sm text-muted-foreground mb-6">{provider.description}</p>

                {/* Features */}
                <ul className="space-y-2 text-left mb-6">
                  {provider.features.map((feature, i) => (
                    <li key={i} className="text-xs text-muted-foreground flex gap-2">
                      <span className="text-cyan-400">•</span> {feature}
                    </li>
                  ))}
                </ul>

                {/* Checkmark */}
                {isSelected && (
                  <div className={`inline-flex items-center justify-center w-8 h-8 rounded-full bg-gradient-to-r ${provider.color} text-white font-bold`}>
                    ✓
                  </div>
                )}
              </div>
            </button>
          )
        })}
      </div>

      {selectedProviders.length > 0 && (
        <div className="mt-8 p-4 bg-cyan-500/10 border border-cyan-500/30 rounded-lg flex gap-3">
          <AlertCircle className="w-5 h-5 text-cyan-400 flex-shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-semibold text-cyan-300 mb-1">Connected Providers</p>
            <p className="text-muted-foreground">{selectedProviders.join(", ")} will be configured in the next step</p>
          </div>
        </div>
      )}
    </div>
  )
}
