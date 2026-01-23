"use client";

import DashboardLayout from "@/components/dashboard/dashboard-layout";
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import { Sliders, TrendingDown, DollarSign, Zap } from "lucide-react";
import { useState } from "react";

const projectionData = [
  { month: "Jan", current: 10240, optimized: 10240, aggressive: 10240 },
  { month: "Feb", current: 11560, optimized: 10890, aggressive: 9540 },
  { month: "Mar", current: 12890, optimized: 11240, aggressive: 8920 },
  { month: "Apr", current: 13450, optimized: 11680, aggressive: 8340 },
  { month: "May", current: 14120, optimized: 12030, aggressive: 7980 },
  { month: "Jun", current: 14890, optimized: 12450, aggressive: 7520 },
  { month: "Jul", current: 15340, optimized: 12780, aggressive: 7210 },
  { month: "Aug", current: 15890, optimized: 13120, aggressive: 6890 },
  { month: "Sep", current: 16450, optimized: 13520, aggressive: 6540 },
  { month: "Oct", current: 17120, optimized: 13890, aggressive: 6230 },
  { month: "Nov", current: 17680, optimized: 14210, aggressive: 5890 },
  { month: "Dec", current: 18340, optimized: 14560, aggressive: 5450 },
];

export default function SimulatorPage() {
  const [riPercentage, setRiPercentage] = useState(30);
  const [spotPercentage, setSpotPercentage] = useState(20);
  const [storageArchival, setStorageArchival] = useState(40);
  const [rightsizing, setRightsizing] = useState(25);

  const currentAnnual = projectionData.reduce((sum, m) => sum + m.current, 0);
  const optimizedAnnual = projectionData.reduce(
    (sum, m) => sum + m.optimized,
    0,
  );
  const aggressiveAnnual = projectionData.reduce(
    (sum, m) => sum + m.aggressive,
    0,
  );

  const savingsOptimized = currentAnnual - optimizedAnnual;
  const savingsAggressive = currentAnnual - aggressiveAnnual;

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-purple-600/20 via-pink-600/20 to-red-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <div className="flex items-center gap-3 mb-2">
              <Sliders className="w-8 h-8 text-purple-400" />
              <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-purple-400 via-pink-400 to-red-400">
                What-If Simulator
              </h1>
            </div>
            <p className="text-muted-foreground">
              Model cost changes with interactive optimization scenarios
            </p>
          </div>
        </div>

        {/* Scenario Sliders */}
        <div className="grid md:grid-cols-2 gap-6">
          {/* Left Column */}
          <div className="space-y-4">
            {/* Reserved Instances */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-cyan-300">Reserved Instances</h3>
                <span className="text-2xl font-bold text-purple-400">
                  {riPercentage}%
                </span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                value={riPercentage}
                onChange={(e) => setRiPercentage(Number(e.target.value))}
                className="w-full"
              />
              <p className="text-xs text-slate-400 mt-3">
                Predicted savings:{" "}
                <span className="text-emerald-400">$2,340/month</span>
              </p>
            </div>

            {/* Spot Instances */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-pink-500/20 rounded-2xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-cyan-300">Spot Instances</h3>
                <span className="text-2xl font-bold text-pink-400">
                  {spotPercentage}%
                </span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                value={spotPercentage}
                onChange={(e) => setSpotPercentage(Number(e.target.value))}
                className="w-full"
              />
              <p className="text-xs text-slate-400 mt-3">
                Predicted savings:{" "}
                <span className="text-emerald-400">$1,890/month</span>
              </p>
            </div>

            {/* Storage Archival */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-red-500/20 rounded-2xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-cyan-300">Storage Archival</h3>
                <span className="text-2xl font-bold text-red-400">
                  {storageArchival}%
                </span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                value={storageArchival}
                onChange={(e) => setStorageArchival(Number(e.target.value))}
                className="w-full"
              />
              <p className="text-xs text-slate-400 mt-3">
                Predicted savings:{" "}
                <span className="text-emerald-400">$1,240/month</span>
              </p>
            </div>

            {/* Right-Sizing */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-orange-500/20 rounded-2xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-cyan-300">Right-Sizing</h3>
                <span className="text-2xl font-bold text-orange-400">
                  {rightsizing}%
                </span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                value={rightsizing}
                onChange={(e) => setRightsizing(Number(e.target.value))}
                className="w-full"
              />
              <p className="text-xs text-slate-400 mt-3">
                Predicted savings:{" "}
                <span className="text-emerald-400">$890/month</span>
              </p>
            </div>
          </div>

          {/* Right Column - Summary Cards */}
          <div className="space-y-4">
            {/* Current Spend */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-6">
              <p className="text-sm text-slate-400 mb-2">
                Current Annual Spend
              </p>
              <p className="text-4xl font-bold text-slate-300">
                ${(currentAnnual / 1000).toFixed(1)}K
              </p>
              <p className="text-xs text-slate-400 mt-2">
                No optimizations applied
              </p>
            </div>

            {/* Optimized Scenario */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-emerald-950/40 to-teal-950/40 border border-emerald-500/20 rounded-2xl p-6">
              <p className="text-sm text-slate-400 mb-2">Optimized Scenario</p>
              <p className="text-4xl font-bold text-emerald-400">
                ${(optimizedAnnual / 1000).toFixed(1)}K
              </p>
              <div className="mt-3 pt-3 border-t border-emerald-500/20">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-emerald-300">
                    Annual Savings
                  </span>
                  <span className="text-2xl font-bold text-emerald-400">
                    -${(savingsOptimized / 1000).toFixed(1)}K
                  </span>
                </div>
                <p className="text-xs text-emerald-300 mt-1">
                  {((savingsOptimized / currentAnnual) * 100).toFixed(1)}%
                  reduction
                </p>
              </div>
            </div>

            {/* Aggressive Scenario */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-purple-950/40 to-pink-950/40 border border-purple-500/20 rounded-2xl p-6">
              <p className="text-sm text-slate-400 mb-2">Aggressive Scenario</p>
              <p className="text-4xl font-bold text-purple-400">
                ${(aggressiveAnnual / 1000).toFixed(1)}K
              </p>
              <div className="mt-3 pt-3 border-t border-purple-500/20">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-purple-300">
                    Annual Savings
                  </span>
                  <span className="text-2xl font-bold text-purple-400">
                    -${(savingsAggressive / 1000).toFixed(1)}K
                  </span>
                </div>
                <p className="text-xs text-purple-300 mt-1">
                  {((savingsAggressive / currentAnnual) * 100).toFixed(1)}%
                  reduction
                </p>
              </div>
            </div>

            {/* Risk Assessment */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-yellow-500/20 rounded-2xl p-6">
              <h4 className="font-bold text-cyan-300 mb-3">Risk Assessment</h4>
              <div className="space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-400">Workload Impact</span>
                  <span className="text-yellow-300">Low</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-400">Implementation</span>
                  <span className="text-emerald-300">Easy</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-400">Reversibility</span>
                  <span className="text-emerald-300">High</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 12-Month Projection */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-cyan-500/20 rounded-2xl p-6">
          <h2 className="text-xl font-bold text-cyan-300 mb-4">
            12-Month Cost Projection
          </h2>
          <ResponsiveContainer width="100%" height={350}>
            <LineChart data={projectionData}>
              <CartesianGrid
                strokeDasharray="3 3"
                stroke="rgba(100,200,255,0.1)"
              />
              <XAxis dataKey="month" stroke="rgba(200,200,200,0.6)" />
              <YAxis stroke="rgba(200,200,200,0.6)" />
              <Tooltip
                contentStyle={{
                  backgroundColor: "rgba(15,23,42,0.9)",
                  border: "1px solid rgba(100,200,255,0.3)",
                }}
              />
              <Legend />
              <Line
                type="monotone"
                dataKey="current"
                stroke="#ef4444"
                strokeWidth={2}
                name="Current Trajectory"
              />
              <Line
                type="monotone"
                dataKey="optimized"
                stroke="#10b981"
                strokeWidth={2}
                name="Optimized Path"
              />
              <Line
                type="monotone"
                dataKey="aggressive"
                stroke="#8b5cf6"
                strokeWidth={2}
                name="Aggressive Path"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Comparison Table */}
        <div className="grid md:grid-cols-3 gap-4">
          {[
            {
              label: "Current",
              value: `$${(currentAnnual / 1000).toFixed(1)}K`,
              color: "from-slate-950 to-slate-900",
            },
            {
              label: "Optimized",
              value: `$${(optimizedAnnual / 1000).toFixed(1)}K`,
              color: "from-emerald-950 to-teal-950",
            },
            {
              label: "Aggressive",
              value: `$${(aggressiveAnnual / 1000).toFixed(1)}K`,
              color: "from-purple-950 to-pink-950",
            },
          ].map((scenario, idx) => (
            <div
              key={idx}
              className={`backdrop-blur-xl bg-gradient-to-br ${scenario.color} border rounded-xl p-6`}
            >
              <p className="text-sm text-slate-400 mb-2">
                {scenario.label} Scenario
              </p>
              <p className="text-3xl font-bold text-cyan-300 mb-4">
                {scenario.value}
              </p>
              {/* TODO: Phase 4 - Implement scenario application: POST /api/simulator/apply-scenario */}
              <button
                onClick={() =>
                  alert("Scenario application endpoint coming in Phase 4")
                }
                className="w-full px-4 py-2 bg-cyan-500/20 text-cyan-400 rounded-lg font-medium hover:bg-cyan-500/30 transition"
              >
                {idx === 0 ? "Current" : "Apply Scenario"}
              </button>
            </div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  );
}
