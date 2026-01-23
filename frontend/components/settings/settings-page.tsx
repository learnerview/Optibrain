"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Bell,
  Lock,
  Users,
  Zap,
  Download,
  Database,
  Shield,
  Sliders,
} from "lucide-react";

export default function SettingsPage() {
  const [settings, setSettings] = useState({
    emailAlerts: true,
    slackAlerts: false,
    anomalyThreshold: "50",
    dataRetention: "90",
    autoApproveDecisions: false,
    notificationFrequency: "daily",
  });

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="relative">
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/20 via-indigo-600/20 to-blue-600/20 blur-3xl rounded-3xl"></div>
        <div className="relative">
          <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-purple-400 via-indigo-400 to-blue-400 mb-2">
            Settings
          </h1>
          <p className="text-muted-foreground">
            Configure your OptiBrain preferences and notifications
          </p>
        </div>
      </div>

      <div className="space-y-6">
        {/* Notifications */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl p-6">
          <h3 className="text-xl font-bold flex items-center gap-3 mb-6 text-cyan-300">
            <Bell className="w-6 h-6 text-purple-400" />
            Notifications
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">Email Alerts</p>
                <p className="text-sm text-slate-400">
                  Receive alerts about anomalies and recommendations
                </p>
              </div>
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  defaultChecked={settings.emailAlerts}
                  className="w-5 h-5 rounded"
                />
              </label>
            </div>

            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">
                  Slack Notifications
                </p>
                <p className="text-sm text-slate-400">
                  Send alerts to your Slack workspace
                </p>
              </div>
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  defaultChecked={settings.slackAlerts}
                  className="w-5 h-5 rounded"
                />
              </label>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-3">
                Notification Frequency
              </label>
              <select
                value={settings.notificationFrequency}
                onChange={(e) =>
                  setSettings({
                    ...settings,
                    notificationFrequency: e.target.value,
                  })
                }
                className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-slate-300"
              >
                <option value="immediate">Immediate (Critical only)</option>
                <option value="hourly">Hourly Summary</option>
                <option value="daily">Daily Digest</option>
                <option value="weekly">Weekly Report</option>
              </select>
            </div>

            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">
                  Cost Alert Threshold
                </p>
                <p className="text-sm text-slate-400">
                  Alert me if daily costs exceed this amount
                </p>
              </div>
              <Input type="number" placeholder="$500" className="w-24" />
            </div>
          </div>
        </div>

        {/* Automation */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-indigo-500/20 rounded-2xl p-6">
          <h3 className="text-xl font-bold flex items-center gap-3 mb-6 text-cyan-300">
            <Sliders className="w-6 h-6 text-indigo-400" />
            Automation
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">
                  Auto-Approve Low-Risk Decisions
                </p>
                <p className="text-sm text-slate-400">
                  Automatically approve decisions with confidence score {">"}{" "}
                  95%
                </p>
              </div>
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  defaultChecked={settings.autoApproveDecisions}
                  className="w-5 h-5 rounded"
                />
              </label>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-3">
                Anomaly Detection Sensitivity
              </label>
              <input
                type="range"
                min="10"
                max="100"
                value={settings.anomalyThreshold}
                onChange={(e) =>
                  setSettings({ ...settings, anomalyThreshold: e.target.value })
                }
                className="w-full"
              />
              <div className="flex items-center justify-between mt-2">
                <span className="text-xs text-slate-400">More Sensitive</span>
                <span className="text-sm font-bold text-indigo-400">
                  {settings.anomalyThreshold}%
                </span>
                <span className="text-xs text-slate-400">Less Sensitive</span>
              </div>
              <p className="text-xs text-slate-400 mt-2">
                Lower = more alerts, Higher = fewer alerts
              </p>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-3">
                Automation Execution Window
              </label>
              <select className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-slate-300">
                <option>Anytime (24/7)</option>
                <option>Business Hours (9 AM - 5 PM)</option>
                <option>Off-Peak Hours (6 PM - 6 AM)</option>
                <option>Weekends Only</option>
              </select>
              <p className="text-xs text-slate-400 mt-2">
                Control when automated optimizations can be executed
              </p>
            </div>
          </div>
        </div>

        {/* Anomaly Detection */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-yellow-500/20 rounded-2xl p-6">
          <h3 className="text-xl font-bold flex items-center gap-3 mb-6 text-cyan-300">
            <Zap className="w-6 h-6 text-yellow-400" />
            Anomaly Detection
          </h3>
          <div className="space-y-4">
            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-3">
                Anomaly Detection Model
              </label>
              <select className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-slate-300">
                <option>Standard (Machine Learning)</option>
                <option>Advanced (Deep Learning)</option>
                <option>Custom Baseline</option>
              </select>
            </div>

            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">
                  Historical Baseline
                </p>
                <p className="text-sm text-slate-400">
                  Use 30-day historical data for comparison
                </p>
              </div>
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  defaultChecked
                  className="w-5 h-5 rounded"
                />
              </label>
            </div>

            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">
                  Weekday/Weekend Normalization
                </p>
                <p className="text-sm text-slate-400">
                  Account for different spending patterns
                </p>
              </div>
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  defaultChecked
                  className="w-5 h-5 rounded"
                />
              </label>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-3">
                Minimum Anomaly Impact
              </label>
              <Input type="number" placeholder="$50" defaultValue="50" />
              <p className="text-xs text-slate-400 mt-2">
                Only alert on anomalies exceeding this cost impact
              </p>
            </div>
          </div>
        </div>

        {/* Data & Privacy */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-red-500/20 rounded-2xl p-6">
          <h3 className="text-xl font-bold flex items-center gap-3 mb-6 text-cyan-300">
            <Lock className="w-6 h-6 text-red-400" />
            Data & Privacy
          </h3>
          <div className="space-y-4">
            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-3">
                Data Retention Period
              </label>
              <select
                value={settings.dataRetention}
                onChange={(e) =>
                  setSettings({ ...settings, dataRetention: e.target.value })
                }
                className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-slate-300"
              >
                <option value="30">30 days (Free)</option>
                <option value="90">90 days (Standard)</option>
                <option value="365">1 year (Professional)</option>
                <option value="730">2 years (Enterprise)</option>
              </select>
              <p className="text-xs text-slate-400 mt-2">
                Data older than this will be automatically deleted
              </p>
            </div>

            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">
                  Share Usage Analytics
                </p>
                <p className="text-sm text-slate-400">
                  Help improve OptiBrain with anonymous data
                </p>
              </div>
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  defaultChecked
                  className="w-5 h-5 rounded"
                />
              </label>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <p className="text-sm font-medium text-slate-300 mb-3">
                Data Export
              </p>
              {/* TODO: Phase 4 - Implement data export: POST /api/data/export with format parameter */}
              <Button
                onClick={() => alert("Data export endpoint coming in Phase 4")}
                className="gap-2 bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:from-cyan-600 hover:to-blue-600"
              >
                <Download className="w-4 h-4" />
                Export All Data (CSV)
              </Button>
            </div>

            <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
              <p className="text-sm text-red-300">
                <strong>Note:</strong> Your cloud credentials are encrypted and
                never stored in plain text. We only store read-only access keys.
              </p>
            </div>
          </div>
        </div>

        {/* Team Management */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-green-500/20 rounded-2xl p-6">
          <h3 className="text-xl font-bold flex items-center gap-3 mb-6 text-cyan-300">
            <Users className="w-6 h-6 text-green-400" />
            Team Management
          </h3>
          <div className="space-y-4">
            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50 flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-300">you@company.com</p>
                <p className="text-sm text-slate-400">Owner • Full Access</p>
              </div>
              <span className="px-3 py-1 bg-emerald-500/20 text-emerald-300 rounded text-sm font-bold">
                Owner
              </span>
            </div>

            <Button className="w-full gap-2 bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:from-cyan-600 hover:to-blue-600">
              <Users className="w-4 h-4" />
              Invite Team Members
            </Button>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <p className="text-sm font-medium text-slate-300 mb-3">
                Role-Based Access Control (RBAC)
              </p>
              <div className="space-y-2 text-sm text-slate-400">
                <p>• Owner: Full access to all features</p>
                <p>• Admin: Can manage users and settings</p>
                <p>• Analyst: View-only access to dashboards</p>
                <p>• Viewer: Limited read-only access</p>
              </div>
            </div>
          </div>
        </div>

        {/* Integrations */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-blue-500/20 rounded-2xl p-6">
          <h3 className="text-xl font-bold flex items-center gap-3 mb-6 text-cyan-300">
            <Database className="w-6 h-6 text-blue-400" />
            Integrations
          </h3>
          <div className="space-y-4">
            {["Slack", "Webhook", "PagerDuty", "ServiceNow"].map(
              (integration) => (
                <div
                  key={integration}
                  className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50"
                >
                  <div>
                    <p className="font-medium text-slate-300">{integration}</p>
                    <p className="text-sm text-slate-400">
                      Send notifications and alerts
                    </p>
                  </div>
                  {/* TODO: Phase 4 - Implement integration setup: POST /api/integrations/{integration-type} */}
                  <Button
                    onClick={() =>
                      alert(
                        `${integration} integration setup coming in Phase 4`,
                      )
                    }
                    className="bg-cyan-500/20 text-cyan-400 hover:bg-cyan-500/30 text-sm"
                  >
                    Connect
                  </Button>
                </div>
              ),
            )}
          </div>
        </div>

        {/* Security Preferences */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl p-6">
          <h3 className="text-xl font-bold flex items-center gap-3 mb-6 text-cyan-300">
            <Shield className="w-6 h-6 text-purple-400" />
            Security Preferences
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">
                  Two-Factor Authentication
                </p>
                <p className="text-sm text-slate-400">
                  Add an extra layer of security
                </p>
              </div>
              <Button className="bg-emerald-500/20 text-emerald-400 text-sm">
                Enabled
              </Button>
            </div>

            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <div>
                <p className="font-medium text-slate-300">Session Timeout</p>
                <p className="text-sm text-slate-400">
                  Auto-logout after inactivity
                </p>
              </div>
              <select className="bg-slate-700/30 border border-slate-600/50 rounded px-3 py-1 text-sm text-slate-300">
                <option>30 minutes</option>
                <option>1 hour</option>
                <option>2 hours</option>
              </select>
            </div>

            <div className="p-4 bg-purple-500/10 border border-purple-500/30 rounded-lg">
              <Button variant="outline" className="bg-transparent text-sm">
                View Security Audit Log
              </Button>
            </div>
          </div>
        </div>

        {/* Danger Zone */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-red-950/40 to-orange-950/40 border border-red-500/20 rounded-2xl p-6">
          <h3 className="text-xl font-bold text-red-300 mb-4">Danger Zone</h3>
          <p className="text-sm text-slate-300 mb-4">
            These actions are permanent and cannot be undone.
          </p>
          <div className="flex gap-3">
            <Button className="bg-red-500/20 text-red-400 hover:bg-red-500/30">
              Deactivate Account
            </Button>
            <Button className="bg-red-500/20 text-red-400 hover:bg-red-500/30">
              Delete Account
            </Button>
          </div>
        </div>

        {/* Save Button */}
        <div className="flex gap-3">
          <Button className="bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:from-cyan-600 hover:to-blue-600">
            Save All Settings
          </Button>
          <Button variant="outline" className="bg-transparent">
            Cancel
          </Button>
        </div>
      </div>
    </div>
  );
}
