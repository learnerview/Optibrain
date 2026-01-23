"use client"

import { Input } from "@/components/ui/input"
import { ChevronDown } from "lucide-react"
import { useState } from "react"

interface ProviderConfigurationProps {
  selectedProviders: Array<"AWS" | "Azure" | "GCP">
  configurations: Record<string, any>
  onChange: (provider: string, config: any) => void
}

const AWS_REGIONS = [
  "us-east-1",
  "us-east-2",
  "us-west-1",
  "us-west-2",
  "eu-west-1",
  "eu-west-2",
  "eu-central-1",
  "ap-southeast-1",
  "ap-southeast-2",
  "ap-northeast-1",
  "ap-south-1",
  "ca-central-1",
]

const AZURE_REGIONS = [
  "East US",
  "West US",
  "West US 2",
  "North Europe",
  "West Europe",
  "East Asia",
  "Southeast Asia",
  "Central India",
  "Japan East",
  "Korea Central",
  "Canada Central",
  "Brazil South",
]

const GCP_REGIONS = [
  "us-central1",
  "us-east1",
  "us-east4",
  "us-west1",
  "us-west2",
  "us-west3",
  "us-west4",
  "europe-west1",
  "europe-west2",
  "europe-north1",
  "asia-east1",
  "asia-northeast1",
  "asia-southeast1",
]

export default function ProviderConfiguration({
  selectedProviders,
  configurations,
  onChange,
}: ProviderConfigurationProps) {
  const regionMap = { AWS: AWS_REGIONS, Azure: AZURE_REGIONS, GCP: GCP_REGIONS }
  const [expandedRegions, setExpandedRegions] = useState<Record<string, boolean>>({})

  const toggleRegions = (provider: string) => {
    setExpandedRegions((prev) => ({ ...prev, [provider]: !prev[provider] }))
  }

  const getProviderColor = (provider: string) => {
    switch (provider) {
      case "AWS":
        return "orange"
      case "Azure":
        return "blue"
      case "GCP":
        return "red"
      default:
        return "cyan"
    }
  }

  return (
    <div className="space-y-8">
      <h3 className="text-2xl font-semibold">Configure Your Cloud Accounts</h3>

      {selectedProviders.map((provider, idx) => {
        const color = getProviderColor(provider)
        const colorClass =
          color === "orange"
            ? "from-orange-500/20 to-orange-600/10 border-orange-500/30"
            : color === "blue"
              ? "from-blue-500/20 to-blue-600/10 border-blue-500/30"
              : "from-red-500/20 to-red-600/10 border-red-500/30"

        return (
          <div key={provider} className={`bg-gradient-to-br ${colorClass} backdrop-blur p-8 rounded-2xl border-2 space-y-6`}>
            {/* Provider header */}
            <div className="flex items-center gap-3 pb-4 border-b border-slate-700">
              <div className="text-4xl">
                {provider === "AWS" && "☁️"}
                {provider === "Azure" && "#️⃣"}
                {provider === "GCP" && "🔴"}
              </div>
              <div>
                <h4 className="text-xl font-bold">{provider}</h4>
                <p className="text-sm text-muted-foreground">
                  {idx + 1} of {selectedProviders.length}
                </p>
              </div>
            </div>

            {/* Account ID */}
            <div>
              <label className="text-sm font-semibold block mb-3">
                Account ID / Subscription ID <span className="text-cyan-400">*</span>
              </label>
              <Input
                type="text"
                placeholder={
                  provider === "AWS"
                    ? "e.g., 123456789012"
                    : provider === "Azure"
                      ? "e.g., xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                      : "e.g., my-gcp-project-12345"
                }
                value={configurations[provider]?.accountId || ""}
                onChange={(e) =>
                  onChange(provider, {
                    ...configurations[provider],
                    accountId: e.target.value,
                  })
                }
                className="bg-slate-900/50 border-cyan-500/30 focus:border-cyan-400 text-foreground placeholder:text-muted-foreground"
              />
            </div>

            {/* Account Name */}
            <div>
              <label className="text-sm font-semibold block mb-3">
                Account Name <span className="text-cyan-400">*</span>
              </label>
              <Input
                type="text"
                placeholder={`e.g., ${provider} Production Account`}
                value={configurations[provider]?.accountName || ""}
                onChange={(e) =>
                  onChange(provider, {
                    ...configurations[provider],
                    accountName: e.target.value,
                  })
                }
                className="bg-slate-900/50 border-cyan-500/30 focus:border-cyan-400 text-foreground placeholder:text-muted-foreground"
              />
            </div>

            {/* Environment */}
            <div>
              <label className="text-sm font-semibold block mb-3">
                Environment <span className="text-cyan-400">*</span>
              </label>
              <select
                value={configurations[provider]?.environment || "production"}
                onChange={(e) =>
                  onChange(provider, {
                    ...configurations[provider],
                    environment: e.target.value,
                  })
                }
                className="w-full px-4 py-2 bg-slate-900/50 border border-cyan-500/30 rounded-lg text-foreground focus:border-cyan-400 focus:outline-none"
              >
                <option value="production">Production</option>
                <option value="staging">Staging</option>
                <option value="development">Development</option>
                <option value="testing">Testing</option>
              </select>
            </div>

            {/* Regions */}
            <div>
              <button
                type="button"
                onClick={() => toggleRegions(provider)}
                className="w-full flex items-center justify-between p-4 bg-slate-900/50 border border-cyan-500/30 rounded-lg hover:bg-slate-800/50 transition"
              >
                <div className="flex items-center gap-2">
                  <span className="font-semibold">Select Regions to Monitor</span>
                  <span className="text-xs bg-cyan-500/30 text-cyan-300 px-2 py-1 rounded">
                    {configurations[provider]?.regions?.length || 0} selected
                  </span>
                </div>
                <ChevronDown
                  className={`w-5 h-5 transition-transform ${expandedRegions[provider] ? "rotate-180" : ""}`}
                />
              </button>

              {expandedRegions[provider] && (
                <div className="mt-4 p-4 bg-slate-900/30 border border-cyan-500/20 rounded-lg">
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                    {regionMap[provider as keyof typeof regionMap].map((region) => (
                      <label key={region} className="flex items-center gap-3 p-2 rounded hover:bg-slate-800/50 cursor-pointer transition">
                        <input
                          type="checkbox"
                          checked={configurations[provider]?.regions?.includes(region) || false}
                          onChange={(e) => {
                            const regions = configurations[provider]?.regions || []
                            if (e.target.checked) {
                              onChange(provider, {
                                ...configurations[provider],
                                regions: [...regions, region],
                              })
                            } else {
                              onChange(provider, {
                                ...configurations[provider],
                                regions: regions.filter((r: string) => r !== region),
                              })
                            }
                          }}
                          className="w-4 h-4 rounded border-cyan-500/50 accent-cyan-500"
                        />
                        <span className="text-sm">{region}</span>
                      </label>
                    ))}
                  </div>

                  {configurations[provider]?.regions && configurations[provider].regions.length > 0 && (
                    <div className="mt-4 p-3 bg-cyan-500/10 border border-cyan-500/30 rounded text-sm text-cyan-300">
                      Selected regions: {configurations[provider].regions.join(", ")}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}
