"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Line, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts"
import { TrendingUp, RefreshCw } from "lucide-react"

const forecastData = [
  { month: "Jan", actual: 8230, forecast: 8230, lower: 7800, upper: 8600 },
  { month: "Feb", actual: 8950, forecast: 8950, lower: 8400, upper: 9500 },
  { month: "Mar", actual: 9450, forecast: 9450, lower: 8900, upper: 10100 },
  { month: "Apr", actual: null, forecast: 9850, lower: 9200, upper: 10500 },
  { month: "May", actual: null, forecast: 10200, lower: 9400, upper: 11000 },
  { month: "Jun", actual: null, forecast: 10650, lower: 9700, upper: 11600 },
]

const whatIfScenarios = [
  { name: "Current Trajectory", multiplier: 1, color: "#22D3EE" },
  { name: "30% Optimization", multiplier: 0.7, color: "#22C55E" },
  { name: "50% Growth", multiplier: 1.5, color: "#EF4444" },
]

export default function PredictiveForecasting() {
  const [selectedScenario, setSelectedScenario] = useState(0)
  const [reservationMonths, setReservationMonths] = useState("36")

  const getScenarioData = () => {
    const scenario = whatIfScenarios[selectedScenario]
    return forecastData.map((item) => ({
      ...item,
      scenario: item.forecast * scenario.multiplier,
    }))
  }

  const calculateSavings = () => {
    const months = Number.parseInt(reservationMonths)
    const baselineCost = forecastData.slice(3).reduce((acc, item) => acc + item.forecast, 0)
    const withReservation = baselineCost * 0.65 // 35% discount with reservation
    return (baselineCost - withReservation).toFixed(0)
  }

  return (
    <div className="space-y-6">
      {/* Forecast chart */}
      <div className="glass p-6 rounded-xl border border-border/50">
        <h3 className="text-lg font-semibold mb-6">12-Month Cost Forecast</h3>

        <div className="h-80 mb-8">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={getScenarioData()}>
              <defs>
                <linearGradient id="colorForecast" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#22D3EE" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#22D3EE" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
              <XAxis dataKey="month" stroke="rgba(255,255,255,0.5)" />
              <YAxis stroke="rgba(255,255,255,0.5)" />
              <Tooltip contentStyle={{ backgroundColor: "rgba(15, 23, 42, 0.9)" }} formatter={(value) => `$${value}`} />
              <Area
                type="monotone"
                dataKey="forecast"
                stroke="#22D3EE"
                fillOpacity={1}
                fill="url(#colorForecast)"
                isAnimationActive={true}
              />
              {selectedScenario > 0 && (
                <Line
                  type="monotone"
                  dataKey="scenario"
                  stroke={whatIfScenarios[selectedScenario].color}
                  strokeDasharray="5 5"
                  dot={false}
                />
              )}
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="grid md:grid-cols-4 gap-4">
          {forecastData.slice(3).map((item, index) => (
            <div key={index} className="p-3 rounded-lg bg-secondary/30 border border-border/50">
              <p className="text-xs text-muted-foreground mb-1">{item.month}</p>
              <p className="font-semibold">${item.forecast}</p>
              <p className="text-xs text-muted-foreground mt-1">
                +{(((item.forecast - 8230) / 8230) * 100).toFixed(0)}%
              </p>
            </div>
          ))}
        </div>
      </div>

      {/* What-if scenarios */}
      <div className="grid lg:grid-cols-2 gap-6">
        <div className="glass p-6 rounded-xl border border-border/50">
          <h3 className="text-lg font-semibold mb-4">What-If Scenarios</h3>

          <div className="space-y-2 mb-6">
            {whatIfScenarios.map((scenario, index) => (
              <button
                key={index}
                onClick={() => setSelectedScenario(index)}
                className={`w-full text-left p-4 rounded-lg transition border-2 ${
                  selectedScenario === index
                    ? "border-accent bg-accent/10"
                    : "border-border/50 bg-secondary/30 hover:border-accent/50"
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: scenario.color }}></div>
                    <span className="font-medium">{scenario.name}</span>
                  </div>
                  <span className="text-sm text-muted-foreground">
                    {scenario.multiplier === 1 ? "Baseline" : `${Math.round(scenario.multiplier * 100)}%`}
                  </span>
                </div>
              </button>
            ))}
          </div>

          <div className="p-4 rounded-lg bg-secondary/30 border border-border/50">
            <p className="text-sm text-muted-foreground mb-1">6-Month Projection</p>
            <p className="text-2xl font-bold">
              $
              {getScenarioData()
                .slice(3, 9)
                .reduce((acc, item) => acc + (selectedScenario === 0 ? item.forecast : item.scenario), 0)
                .toFixed(0)}
            </p>
          </div>
        </div>

        {/* Reserved Instances calculator */}
        <div className="glass p-6 rounded-xl border border-border/50">
          <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <RefreshCw className="w-5 h-5 text-accent" />
            Reserved Instances Calculator
          </h3>

          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium block mb-2">Reservation Term</label>
              <div className="flex gap-2">
                {["12", "24", "36"].map((months) => (
                  <button
                    key={months}
                    onClick={() => setReservationMonths(months)}
                    className={`px-4 py-2 rounded-lg border transition ${
                      reservationMonths === months
                        ? "bg-primary border-primary text-primary-foreground"
                        : "border-border/50 bg-secondary/30 hover:border-primary/50"
                    }`}
                  >
                    {months} months
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-3 p-4 rounded-lg bg-secondary/30 border border-border/50">
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">On-Demand Cost</span>
                <span className="font-semibold">$58,500</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">With {reservationMonths}m RI</span>
                <span className="font-semibold text-green-500">$38,025</span>
              </div>
              <div className="border-t border-border/50 pt-3 flex justify-between items-center">
                <span className="font-semibold">Total Savings</span>
                <span className="text-lg font-bold text-green-500">${calculateSavings()}</span>
              </div>
              <p className="text-xs text-muted-foreground pt-2">35% discount with upfront payment</p>
            </div>

            <Button className="w-full">
              <TrendingUp className="w-4 h-4 mr-2" />
              Apply Recommendation
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
