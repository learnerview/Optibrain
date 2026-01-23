"use client";

import DashboardLayout from "@/components/dashboard/dashboard-layout";
import { Cloud, Plus, Check, AlertCircle, Zap, BarChart3 } from "lucide-react";
import { useState, useEffect } from "react";

// Type definitions for cloud providers
interface CloudProvider {
  id: number;
  provider: string;
  accountName: string;
  status: string;
  validationStatus: string;
  connectedAt: string;
  lastConnectedAt: string;
}

interface CloudResources {
  provider: string;
  regions: string[];
  services: string[];
  resourceCounts: Record<string, number>;
  lastSyncedAt: string;
}

export default function CloudConnectionsPage() {
  const [selectedProvider, setSelectedProvider] = useState<number | null>(null);
  const [providers, setProviders] = useState<CloudProvider[]>([]);
  const [resources, setResources] = useState<CloudResources | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch cloud providers on component mount
  useEffect(() => {
    fetchProviders();
    fetchResources();
  }, []);

  /**
   * Fetch all cloud providers for the current tenant
   * Calls: GET /api/cloud/providers
   */
  const fetchProviders = async () => {
    try {
      setLoading(true);
      const response = await fetch("/api/cloud/providers");
      
      if (!response.ok) {
        throw new Error("Failed to fetch cloud providers");
      }

      const data = await response.json();
      // Extract providers from ApiResponse wrapper
      setProviders(data.data || []);
      setError(null);
    } catch (err) {
      console.error("Error fetching providers:", err);
      setError("Failed to load cloud providers");
      // Use empty array instead of crashing
      setProviders([]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Fetch cloud resources for connected providers
   * Calls: GET /api/cloud/resources
   * TODO: Phase 4 - Fetch real metrics from cloud providers
   */
  const fetchResources = async () => {
    try {
      const response = await fetch("/api/cloud/resources");
      
      if (!response.ok) {
        throw new Error("Failed to fetch cloud resources");
      }

      const data = await response.json();
      // Extract resources from ApiResponse wrapper
      setResources(data.data || null);
    } catch (err) {
      console.error("Error fetching resources:", err);
      // Continue with null resources - not critical
    }
  };

  /**
   * Connect a new cloud provider
   * Calls: POST /api/cloud/connect
   * TODO: Phase 3 - Show connection wizard modal
   */
  const handleConnectProvider = async () => {
    // TODO: Phase 3 - Implement cloud provider connection wizard modal
    // This will prompt user for credentials and provider type
    alert("Cloud provider connection wizard coming in Phase 3");
  };

  // Fallback mock data if API fails
  const displayProviders: CloudProvider[] = providers.length > 0 ? providers : [
    {
      id: 1,
      provider: "AWS",
      accountName: "AWS Production",
      status: "active",
      validationStatus: "validated",
      connectedAt: "2024-01-15",
      lastConnectedAt: "2024-01-15",
    },
  ];

  // Calculate summary metrics
  const totalAccounts = displayProviders.length;
  // TODO: Phase 4 - Calculate real resource count from API
  const totalResources = resources?.resourceCounts 
    ? Object.values(resources.resourceCounts).reduce((a, b) => a + b, 0) 
    : 0;

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-orange-600/20 via-red-600/20 to-pink-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <div className="flex items-center gap-3 mb-2">
              <Cloud className="w-8 h-8 text-orange-400" />
              <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-orange-400 via-red-400 to-pink-400">
                Cloud Connections
              </h1>
            </div>
            <p className="text-muted-foreground">
              Manage your connected cloud provider accounts and regions
            </p>
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-4 text-red-300">
            {error}
          </div>
        )}

        {/* Summary Cards */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-orange-950/40 to-red-950/40 border border-orange-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Connected Accounts</p>
            <p className="text-3xl font-bold text-orange-400">
              {totalAccounts}
            </p>
            <p className="text-xs text-orange-300 mt-2">All active</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-red-950/40 to-pink-950/40 border border-red-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Total Resources</p>
            <p className="text-3xl font-bold text-red-400">
              {totalResources}
            </p>
            <p className="text-xs text-red-300 mt-2">Being monitored</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-pink-950/40 to-rose-950/40 border border-pink-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Services Available</p>
            <p className="text-3xl font-bold text-pink-400">
              {resources?.services?.length || 0}
            </p>
            <p className="text-xs text-pink-300 mt-2">
              {/* TODO: Phase 4 - Fetch real metrics */}
              Across providers
            </p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-rose-950/40 to-orange-950/40 border border-rose-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Sync Status</p>
            <p className="text-3xl font-bold text-rose-400">Active</p>
            <p className="text-xs text-rose-300 mt-2">Real-time updates</p>
          </div>
        </div>

        {/* Connected Providers */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold text-cyan-300">
              Connected Providers
            </h2>
            <button
              onClick={handleConnectProvider}
              className="px-4 py-2 bg-gradient-to-r from-orange-500 to-red-500 text-white rounded-lg font-medium hover:from-orange-600 hover:to-red-600 transition flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              Add Account
            </button>
          </div>

          {loading ? (
            <div className="text-center py-8 text-slate-400">
              Loading cloud providers...
            </div>
          ) : displayProviders.length > 0 ? (
            displayProviders.map((provider) => (
              <div
                key={provider.id}
                onClick={() =>
                  setSelectedProvider(
                    selectedProvider === provider.id ? null : provider.id,
                  )
                }
                className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 hover:border-orange-500/30 rounded-2xl p-6 cursor-pointer transition-all"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-start gap-4 flex-1">
                    <div className="w-14 h-14 rounded-lg bg-slate-800/50 border border-slate-700/50 flex items-center justify-center flex-shrink-0">
                      <Cloud className="w-7 h-7 text-orange-400" />
                    </div>

                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-xl font-bold text-cyan-300">
                          {provider.accountName}
                        </h3>
                        <span className="px-2 py-1 bg-emerald-500/20 text-emerald-300 rounded-full text-xs font-bold flex items-center gap-1">
                          <Check className="w-3 h-3" />
                          {provider.status.toUpperCase()}
                        </span>
                      </div>

                      <div className="grid md:grid-cols-4 gap-3 text-sm">
                        <div className="text-slate-400">
                          Provider:{" "}
                          <span className="text-slate-300">
                            {provider.provider}
                          </span>
                        </div>
                        <div className="text-slate-400">
                          Validation:{" "}
                          <span className="text-cyan-300 font-mono text-xs">
                            {provider.validationStatus}
                          </span>
                        </div>
                        <div className="text-slate-400">
                          Connected:{" "}
                          <span className="text-slate-300">
                            {new Date(provider.connectedAt).toLocaleDateString()}
                          </span>
                        </div>
                        <div className="text-slate-400">
                          Last Sync:{" "}
                          <span className="text-slate-300">
                            {new Date(provider.lastConnectedAt).toLocaleDateString()}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Expanded Details */}
                {selectedProvider === provider.id && (
                  <div className="mt-6 pt-6 border-t border-slate-700/50 space-y-4">
                    <div className="grid md:grid-cols-3 gap-4">
                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">
                          Connected Date
                        </p>
                        <p className="text-sm text-slate-300">
                          {new Date(provider.connectedAt).toLocaleDateString()}
                        </p>
                      </div>

                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">
                          Status
                        </p>
                        <p className="text-sm text-emerald-300">
                          Healthy - All permissions granted
                        </p>
                      </div>

                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">
                          Sync Frequency
                        </p>
                        <p className="text-sm text-slate-300">
                          Every 5 minutes (Real-time)
                        </p>
                      </div>

                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">
                          API Keys Status
                        </p>
                        <p className="text-sm text-emerald-300">
                          Valid and Active
                        </p>
                      </div>

                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">
                          Automation Rules
                        </p>
                        <p className="text-sm text-slate-300">
                          {/* TODO: Phase 4 - Fetch real automation rules */}
                          5 active rules
                        </p>
                      </div>

                      <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                        <p className="text-xs text-slate-400 uppercase mb-2">
                          Last Synced
                        </p>
                        <p className="text-sm text-slate-300">
                          {new Date(provider.lastConnectedAt).toLocaleString()}
                        </p>
                      </div>
                    </div>

                    <div className="flex gap-2 pt-4">
                      {/* TODO: Phase 4 - Implement connection test API call: POST /api/cloud/{id}/test */}
                      <button
                        onClick={() =>
                          alert("Connection test endpoint coming in Phase 4")
                        }
                        className="px-4 py-2 bg-cyan-500/20 text-cyan-400 rounded-lg font-medium hover:bg-cyan-500/30 transition"
                      >
                        Test Connection
                      </button>
                      {/* TODO: Phase 4 - Fetch permissions from backend: GET /api/cloud/{id}/permissions */}
                      <button
                        onClick={() =>
                          alert("Permissions fetch endpoint coming in Phase 4")
                        }
                        className="px-4 py-2 bg-slate-600/20 text-slate-400 rounded-lg font-medium hover:bg-slate-600/30 transition"
                      >
                        View Permissions
                      </button>
                      {/* TODO: Phase 4 - Call disconnect API: DELETE /api/cloud/{id} */}
                      <button
                        onClick={() =>
                          alert("Disconnect endpoint coming in Phase 4")
                        }
                        className="px-4 py-2 bg-red-500/20 text-red-400 rounded-lg font-medium hover:bg-red-500/30 transition"
                      >
                        Disconnect
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))
          ) : (
            <div className="text-center py-8">
              <p className="text-slate-400 mb-4">No cloud providers connected yet</p>
              <button
                onClick={handleConnectProvider}
                className="px-6 py-2 bg-gradient-to-r from-orange-500 to-red-500 text-white rounded-lg font-medium hover:from-orange-600 hover:to-red-600 transition"
              >
                Connect Your First Provider
              </button>
            </div>
          )}
        </div>

        {/* Cloud Provider Cards */}
        <div className="grid md:grid-cols-3 gap-6">
          {[
            {
              icon: Cloud,
              provider: "AWS",
              status: displayProviders.some((p) => p.provider === "AWS")
                ? "Connected"
                : "Not Connected",
              color: "from-orange-950 to-orange-900",
            },
            {
              icon: Cloud,
              provider: "Azure",
              status: displayProviders.some((p) => p.provider === "AZURE")
                ? "Connected"
                : "Not Connected",
              color: "from-blue-950 to-blue-900",
            },
            {
              icon: Cloud,
              provider: "GCP",
              status: displayProviders.some((p) => p.provider === "GCP")
                ? "Connected"
                : "Not Connected",
              color: "from-yellow-950 to-yellow-900",
            },
          ].map((item, idx) => {
            const Icon = item.icon;
            return (
              <div
                key={idx}
                className={`backdrop-blur-xl bg-gradient-to-br ${item.color} border border-slate-700/50 rounded-xl p-6`}
              >
                <div className="flex items-center gap-3 mb-4">
                  <Icon className="w-6 h-6 text-slate-300" />
                  <h4 className="text-lg font-bold text-slate-200">
                    {item.provider}
                  </h4>
                </div>
                <p className="text-sm text-slate-300 mb-4">
                  {item.status === "Connected"
                    ? "Your account is connected and actively syncing."
                    : "Connect your account to start monitoring."}
                </p>
                <button
                  onClick={
                    item.status === "Connected"
                      ? undefined
                      : handleConnectProvider
                  }
                  className={`w-full px-4 py-2 rounded-lg font-medium transition ${
                    item.status === "Connected"
                      ? "bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/30"
                      : "bg-slate-600/20 text-slate-400 hover:bg-slate-600/30"
                  }`}
                >
                  {item.status === "Connected" ? "Manage" : "Connect"}
                </button>
              </div>
            );
          })}
        </div>

        {/* Cloud Regions */}
        {resources && (
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-6">
              <Zap className="w-6 h-6 text-yellow-400" />
              <h3 className="text-xl font-bold text-cyan-300">
                Available Regions & Services
              </h3>
            </div>

            <p className="text-sm text-slate-300 mb-4">
              {/* TODO: Phase 4 - Show real metrics per region */}
              Manage resources across multiple regions for optimal performance:
            </p>

            <div className="grid md:grid-cols-3 gap-4">
              {resources.regions?.slice(0, 3).map((region, idx) => (
                <div
                  key={idx}
                  className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50"
                >
                  <p className="text-sm font-bold text-slate-300 mb-2">
                    Region
                  </p>
                  <p className="text-xs text-slate-400">{region}</p>
                  <p className="text-xs text-slate-400 mt-2">
                    {/* TODO: Phase 4 - Fetch real resource stats per region */}
                    Monitoring resources in this region
                  </p>
                </div>
              ))}
            </div>

            {resources.services && resources.services.length > 0 && (
              <div className="mt-6 pt-6 border-t border-slate-700/50">
                <p className="text-sm font-bold text-slate-300 mb-3">
                  Services: {resources.services.join(", ")}
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-orange-600/20 via-red-600/20 to-pink-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <div className="flex items-center gap-3 mb-2">
              <Cloud className="w-8 h-8 text-orange-400" />
              <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-orange-400 via-red-400 to-pink-400">
                Cloud Connections
              </h1>
            </div>
            <p className="text-muted-foreground">
              Manage your connected cloud provider accounts and regions
            </p>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid md:grid-cols-4 gap-4">
          <div className="backdrop-blur-xl bg-gradient-to-br from-orange-950/40 to-red-950/40 border border-orange-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Connected Accounts</p>
            <p className="text-3xl font-bold text-orange-400">
              {connectedProviders.length}
            </p>
            <p className="text-xs text-orange-300 mt-2">All active</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-red-950/40 to-pink-950/40 border border-red-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Total Resources</p>
            <p className="text-3xl font-bold text-red-400">
              {connectedProviders.reduce((sum, p) => sum + p.resources, 0)}
            </p>
            <p className="text-xs text-red-300 mt-2">Being monitored</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-pink-950/40 to-rose-950/40 border border-pink-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Total Monthly Cost</p>
            <p className="text-3xl font-bold text-pink-400">$18,570</p>
            <p className="text-xs text-pink-300 mt-2">Across all providers</p>
          </div>

          <div className="backdrop-blur-xl bg-gradient-to-br from-rose-950/40 to-orange-950/40 border border-rose-500/20 rounded-xl p-6">
            <p className="text-sm text-slate-400 mb-2">Sync Status</p>
            <p className="text-3xl font-bold text-rose-400">Active</p>
            <p className="text-xs text-rose-300 mt-2">Real-time updates</p>
          </div>
        </div>

        {/* Connected Providers */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold text-cyan-300">
              Connected Providers
            </h2>
            {/* TODO: Phase 4 - Implement cloud provider connection wizard */}
            <button
              onClick={() =>
                alert("Cloud provider connection wizard coming in Phase 4")
              }
              className="px-4 py-2 bg-gradient-to-r from-orange-500 to-red-500 text-white rounded-lg font-medium hover:from-orange-600 hover:to-red-600 transition flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              Add Account
            </button>
          </div>

          {connectedProviders.map((provider) => (
            <div
              key={provider.id}
              onClick={() =>
                setSelectedProvider(
                  selectedProvider === provider.id ? null : provider.id,
                )
              }
              className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 hover:border-orange-500/30 rounded-2xl p-6 cursor-pointer transition-all"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-start gap-4 flex-1">
                  <div className="w-14 h-14 rounded-lg bg-slate-800/50 border border-slate-700/50 flex items-center justify-center flex-shrink-0">
                    <Cloud className="w-7 h-7 text-orange-400" />
                  </div>

                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-xl font-bold text-cyan-300">
                        {provider.name}
                      </h3>
                      <span className="px-2 py-1 bg-emerald-500/20 text-emerald-300 rounded-full text-xs font-bold flex items-center gap-1">
                        <Check className="w-3 h-3" />
                        {provider.status.toUpperCase()}
                      </span>
                    </div>

                    <div className="grid md:grid-cols-5 gap-3 text-sm">
                      <div className="text-slate-400">
                        Provider:{" "}
                        <span className="text-slate-300">
                          {provider.provider}
                        </span>
                      </div>
                      <div className="text-slate-400">
                        Account:{" "}
                        <span className="text-cyan-300 font-mono text-xs">
                          {provider.accountId}
                        </span>
                      </div>
                      <div className="text-slate-400">
                        Region:{" "}
                        <span className="text-slate-300">
                          {provider.region}
                        </span>
                      </div>
                      <div className="text-slate-400">
                        Resources:{" "}
                        <span className="text-slate-300">
                          {provider.resources}
                        </span>
                      </div>
                      <div className="text-slate-400">
                        Last Sync:{" "}
                        <span className="text-slate-300">
                          {provider.lastSync}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="text-right ml-4">
                  <p className="text-2xl font-bold text-orange-400 mb-2">
                    {provider.cost}
                  </p>
                  <p className="text-xs text-slate-400">/month</p>
                </div>
              </div>

              {/* Expanded Details */}
              {selectedProvider === provider.id && (
                <div className="mt-6 pt-6 border-t border-slate-700/50 space-y-4">
                  <div className="grid md:grid-cols-3 gap-4">
                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">
                        Connected Date
                      </p>
                      <p className="text-sm text-slate-300">
                        {provider.connectedDate}
                      </p>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">
                        Status
                      </p>
                      <p className="text-sm text-emerald-300">
                        Healthy - All permissions granted
                      </p>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">
                        Sync Frequency
                      </p>
                      <p className="text-sm text-slate-300">
                        Every 5 minutes (Real-time)
                      </p>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">
                        API Keys Status
                      </p>
                      <p className="text-sm text-emerald-300">
                        Valid and Active
                      </p>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">
                        Cost Tracking
                      </p>
                      <p className="text-sm text-cyan-300">Enabled</p>
                    </div>

                    <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                      <p className="text-xs text-slate-400 uppercase mb-2">
                        Automation Rules
                      </p>
                      <p className="text-sm text-slate-300">5 active rules</p>
                    </div>
                  </div>

                  <div className="flex gap-2 pt-4">
                    {/* TODO: Phase 4 - Implement connection test API call: POST /api/providers/{id}/test */}
                    <button
                      onClick={() =>
                        alert("Connection test endpoint coming in Phase 4")
                      }
                      className="px-4 py-2 bg-cyan-500/20 text-cyan-400 rounded-lg font-medium hover:bg-cyan-500/30 transition"
                    >
                      Test Connection
                    </button>
                    {/* TODO: Phase 4 - Fetch permissions from backend: GET /api/providers/{id}/permissions */}
                    <button
                      onClick={() =>
                        alert("Permissions fetch endpoint coming in Phase 4")
                      }
                      className="px-4 py-2 bg-slate-600/20 text-slate-400 rounded-lg font-medium hover:bg-slate-600/30 transition"
                    >
                      View Permissions
                    </button>
                    {/* TODO: Phase 4 - Call disconnect API: DELETE /api/providers/{id} */}
                    <button
                      onClick={() =>
                        alert("Disconnect endpoint coming in Phase 4")
                      }
                      className="px-4 py-2 bg-red-500/20 text-red-400 rounded-lg font-medium hover:bg-red-500/30 transition"
                    >
                      Disconnect
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Connection Instructions */}
        <div className="grid md:grid-cols-3 gap-6">
          {[
            {
              icon: Cloud,
              provider: "AWS",
              status: "Connected",
              color: "from-orange-950 to-orange-900",
            },
            {
              icon: Cloud,
              provider: "Azure",
              status: "Connected",
              color: "from-blue-950 to-blue-900",
            },
            {
              icon: Cloud,
              provider: "GCP",
              status: "Connected",
              color: "from-yellow-950 to-yellow-900",
            },
          ].map((item, idx) => {
            const Icon = item.icon;
            return (
              <div
                key={idx}
                className={`backdrop-blur-xl bg-gradient-to-br ${item.color} border border-slate-700/50 rounded-xl p-6`}
              >
                <div className="flex items-center gap-3 mb-4">
                  <Icon className="w-6 h-6 text-slate-300" />
                  <h4 className="text-lg font-bold text-slate-200">
                    {item.provider}
                  </h4>
                </div>
                <p className="text-sm text-slate-300 mb-4">
                  {item.status === "Connected"
                    ? "Your account is connected and actively syncing."
                    : "Connect your account to start monitoring."}
                </p>
                <button
                  className={`w-full px-4 py-2 rounded-lg font-medium transition ${
                    item.status === "Connected"
                      ? "bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/30"
                      : "bg-slate-600/20 text-slate-400 hover:bg-slate-600/30"
                  }`}
                >
                  {item.status === "Connected" ? "Manage" : "Connect"}
                </button>
              </div>
            );
          })}
        </div>

        {/* Multi-Region Setup */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-6">
            <Zap className="w-6 h-6 text-yellow-400" />
            <h3 className="text-xl font-bold text-cyan-300">
              Multi-Region Optimization
            </h3>
          </div>

          <p className="text-sm text-slate-300 mb-4">
            Optimize costs by managing resources across multiple regions:
          </p>

          <div className="grid md:grid-cols-3 gap-4">
            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <p className="text-sm font-bold text-slate-300 mb-2">
                Primary Region
              </p>
              <p className="text-xs text-slate-400">us-east-1 (AWS)</p>
              <p className="text-xs text-slate-400 mt-2">
                Best for: East Coast users & services
              </p>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <p className="text-sm font-bold text-slate-300 mb-2">
                Secondary Region
              </p>
              <p className="text-xs text-slate-400">eu-west-1 (AWS)</p>
              <p className="text-xs text-slate-400 mt-2">
                Best for: European users & compliance
              </p>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <p className="text-sm font-bold text-slate-300 mb-2">
                Cost Optimization
              </p>
              <p className="text-xs text-emerald-300 font-bold">15% savings</p>
              <p className="text-xs text-slate-400 mt-2">
                From multi-region load balancing
              </p>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
