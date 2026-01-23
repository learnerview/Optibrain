'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { User, Mail, Shield, Calendar, CreditCard, CheckCircle2, AlertCircle, Cloud } from 'lucide-react'
import { useState } from 'react'

const AccountPage = () => {
  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 via-cyan-600/20 to-teal-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-cyan-400 to-teal-400 mb-2">Profile & Organization</h1>
            <p className="text-muted-foreground">Manage your account, organization, plan, and security settings</p>
          </div>
        </div>

        {/* User Profile */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-cyan-500/20 rounded-2xl p-8">
          <div className="flex items-start justify-between mb-8">
            <div className="flex items-center gap-6">
              <div className="w-24 h-24 rounded-xl bg-gradient-to-br from-cyan-500 to-purple-500 flex items-center justify-center text-4xl font-bold text-white">
                J
              </div>
              <div>
                <h2 className="text-2xl font-bold text-slate-100 mb-1">John Doe</h2>
                <p className="text-slate-400 mb-3">john.doe@company.com</p>
                <div className="flex items-center gap-3">
                  <span className="px-3 py-1 bg-emerald-500/20 text-emerald-300 rounded-full text-sm font-medium flex items-center gap-1">
                    <CheckCircle2 className="w-4 h-4" />
                    Verified
                  </span>
                  <span className="px-3 py-1 bg-purple-500/20 text-purple-300 rounded-full text-sm font-medium">
                    Premium Plan
                  </span>
                </div>
              </div>
            </div>
            <button className="px-4 py-2 bg-cyan-500/20 text-cyan-400 rounded-lg font-medium hover:bg-cyan-500/30 transition">
              Edit Profile
            </button>
          </div>

          <div className="grid md:grid-cols-3 gap-4">
            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <p className="text-xs text-slate-400 uppercase mb-2">Account Status</p>
              <p className="text-lg font-bold text-emerald-400">Active</p>
            </div>
            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <p className="text-xs text-slate-400 uppercase mb-2">Member Since</p>
              <p className="text-lg font-bold text-slate-300">January 15, 2024</p>
            </div>
            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <p className="text-xs text-slate-400 uppercase mb-2">Last Login</p>
              <p className="text-lg font-bold text-slate-300">Today, 2:45 PM</p>
            </div>
          </div>
        </div>

        {/* Organization */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-8">
          <h3 className="text-2xl font-bold text-cyan-300 mb-6">Organization</h3>

          <div className="space-y-4">
            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-2">Organization Name</label>
              <input type="text" placeholder="Acme Corporation" className="w-full bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-slate-300" />
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-2">Organization ID</label>
              <div className="flex items-center gap-2">
                <code className="text-sm bg-slate-900/50 px-3 py-2 rounded text-slate-400 font-mono flex-1">org_5d8e9c2b1f4a7d6k</code>
                <button className="text-sm text-cyan-400 hover:text-cyan-300 px-3 py-2">Copy</button>
              </div>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-2">Organization Tier</label>
              <p className="text-slate-300">Enterprise</p>
            </div>

            <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
              <label className="text-sm font-semibold text-slate-300 block mb-2">Team Members</label>
              <p className="text-slate-300">12 members (1 owner, 3 admins, 8 analysts)</p>
            </div>
          </div>
        </div>

        {/* Subscription & Billing */}
        <div className="grid md:grid-cols-2 gap-6">
          {/* Current Plan */}
          <div className="backdrop-blur-xl bg-gradient-to-br from-purple-950/40 to-pink-950/40 border border-purple-500/20 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-6">
              <CreditCard className="w-6 h-6 text-purple-400" />
              <h3 className="text-xl font-bold text-cyan-300">Current Plan</h3>
            </div>

            <div className="space-y-4">
              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <p className="text-sm text-slate-400 mb-2">Plan Type</p>
                <p className="text-xl font-bold text-purple-400">Premium Annual</p>
              </div>

              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <p className="text-sm text-slate-400 mb-2">Monthly Cost</p>
                <p className="text-xl font-bold text-slate-300">$99/month</p>
              </div>

              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <p className="text-sm text-slate-400 mb-2">Renewal Date</p>
                <p className="text-xl font-bold text-slate-300">March 15, 2025</p>
              </div>

              <div className="p-4 bg-purple-500/10 border border-purple-500/30 rounded-lg">
                <p className="text-sm text-purple-300">
                  You have <span className="font-bold">84 days</span> remaining on your current plan. You'll be billed on the renewal date.
                </p>
              </div>

              <button className="w-full px-4 py-2 bg-slate-600/20 text-slate-300 rounded-lg font-medium hover:bg-slate-600/30 transition">
                Manage Subscription
              </button>
            </div>
          </div>

          {/* Billing History */}
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-6">
              <Calendar className="w-6 h-6 text-cyan-400" />
              <h3 className="text-xl font-bold text-cyan-300">Billing History</h3>
            </div>

            <div className="space-y-3">
              {[
                { date: 'Mar 15, 2024', amount: '$99.00', status: 'Paid' },
                { date: 'Feb 15, 2024', amount: '$99.00', status: 'Paid' },
                { date: 'Jan 15, 2024', amount: '$99.00', status: 'Paid' },
              ].map((invoice, idx) => (
                <div key={idx} className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg border border-slate-700/50 hover:border-slate-600/50 transition">
                  <div>
                    <p className="text-sm font-medium text-slate-300">{invoice.date}</p>
                    <p className="text-xs text-slate-400">Premium Plan</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-bold text-emerald-400">{invoice.amount}</p>
                    <p className="text-xs text-emerald-300">{invoice.status}</p>
                  </div>
                </div>
              ))}
            </div>

            <button className="w-full mt-4 px-4 py-2 bg-slate-600/20 text-slate-300 rounded-lg font-medium hover:bg-slate-600/30 transition">
              View All Invoices
            </button>
          </div>
        </div>

        {/* Security & Integrations */}
        <div className="grid md:grid-cols-2 gap-6">
          {/* Security */}
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-6">
              <Shield className="w-6 h-6 text-emerald-400" />
              <h3 className="text-xl font-bold text-cyan-300">Security</h3>
            </div>

            <div className="space-y-4">
              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-sm font-medium text-slate-300">Two-Factor Authentication</p>
                  <span className="px-2 py-1 bg-emerald-500/20 text-emerald-300 rounded text-xs font-bold">Enabled</span>
                </div>
                <button className="text-xs text-cyan-400 hover:text-cyan-300">Manage 2FA</button>
              </div>

              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <p className="text-sm font-medium text-slate-300 mb-2">Password</p>
                <button className="text-xs text-cyan-400 hover:text-cyan-300">Change Password</button>
              </div>

              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <p className="text-sm font-medium text-slate-300 mb-2">Active Sessions</p>
                <p className="text-xs text-slate-400 mb-3">1 active session</p>
                <button className="text-xs text-red-400 hover:text-red-300">Sign Out All Other Sessions</button>
              </div>

              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <p className="text-sm font-medium text-slate-300 mb-2">Session Timeout</p>
                <select className="w-full bg-slate-700/30 border border-slate-600/50 rounded px-3 py-2 text-sm text-slate-300">
                  <option>30 minutes</option>
                  <option>1 hour</option>
                  <option>2 hours</option>
                </select>
              </div>
            </div>
          </div>

          {/* API Keys */}
          <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-6">
            <div className="flex items-center gap-3 mb-6">
              <User className="w-6 h-6 text-blue-400" />
              <h3 className="text-xl font-bold text-cyan-300">API Access</h3>
            </div>

            <div className="space-y-4">
              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <p className="text-sm font-medium text-slate-300 mb-2">API Key</p>
                <div className="flex items-center gap-2 mb-2">
                  <code className="text-xs bg-slate-900/50 px-2 py-1 rounded text-slate-400">opt_****...****</code>
                  <button className="text-xs text-cyan-400 hover:text-cyan-300">Copy</button>
                </div>
                <p className="text-xs text-slate-400">Created: Jan 15, 2024</p>
              </div>

              <div className="p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <p className="text-sm font-medium text-slate-300 mb-3">API Usage</p>
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-400">This month</span>
                    <span className="text-slate-300">12,450 / 50,000 calls</span>
                  </div>
                  <div className="w-full h-2 bg-slate-700/50 rounded-full overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-cyan-500 to-blue-500" style={{ width: '24.9%' }}></div>
                  </div>
                </div>
              </div>

              <button className="w-full px-4 py-2 bg-slate-600/20 text-slate-300 rounded-lg font-medium hover:bg-slate-600/30 transition">
                View API Documentation
              </button>
            </div>
          </div>
        </div>

        {/* Connected Cloud Accounts */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-slate-700/50 rounded-2xl p-6">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-slate-800/50 border border-slate-700/50 flex items-center justify-center">
                <Cloud className="w-6 h-6 text-cyan-400" />
              </div>
              <h3 className="text-xl font-bold text-cyan-300">Cloud Accounts</h3>
            </div>
            <a href="/dashboard/cloud-connections" className="text-sm text-cyan-400 hover:text-cyan-300 underline">
              Manage →
            </a>
          </div>

          <div className="space-y-3">
            {[
              { name: "AWS Production", status: "Connected", id: "123456789012" },
              { name: "Azure Enterprise", status: "Connected", id: "azure-prod-001" },
              { name: "GCP Development", status: "Connected", id: "gcp-dev-001" },
            ].map((account, idx) => (
              <div key={idx} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-lg border border-slate-700/50">
                <div className="flex-1">
                  <p className="font-medium text-slate-300">{account.name}</p>
                  <p className="text-xs text-slate-400 font-mono">{account.id}</p>
                </div>
                <span className="px-2 py-1 bg-emerald-500/20 text-emerald-300 rounded text-xs font-bold">{account.status}</span>
              </div>
            ))}
          </div>

          <p className="text-xs text-slate-400 mt-4">For full cloud account management, go to Cloud Connections page.</p>
        </div>

        {/* Danger Zone */}
        <div className="backdrop-blur-xl bg-gradient-to-br from-red-950/40 to-orange-950/40 border border-red-500/20 rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-4">
            <AlertCircle className="w-6 h-6 text-red-400" />
            <h3 className="text-xl font-bold text-red-300">Danger Zone</h3>
          </div>
          <p className="text-sm text-slate-300 mb-4">These actions are permanent and cannot be undone.</p>
          <div className="flex gap-3">
            <button className="px-4 py-2 bg-red-500/20 text-red-400 rounded-lg font-medium hover:bg-red-500/30 transition">
              Deactivate Account
            </button>
            <button className="px-4 py-2 bg-red-500/20 text-red-400 rounded-lg font-medium hover:bg-red-500/30 transition">
              Delete Account
            </button>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}

export default AccountPage
