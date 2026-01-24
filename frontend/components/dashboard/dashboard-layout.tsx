"use client"

import type React from "react"
import { useState, useRef, useEffect } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuthStore, useCloudStore } from "@/lib/store"
import {
  Menu,
  X,
  LayoutDashboard,
  Brain,
  CheckCircle2,
  Sliders,
  Network,
  TrendingDown,
  TrendingUp,
  BarChart3,
  Settings,
  LogOut,
  AlertCircle,
  Zap,
  User,
  Cloud,
  Package,
  History,
  Bell,
  FileText,
  MessageSquare,
} from "lucide-react"
import { Button } from "@/components/ui/button"

interface DashboardLayoutProps {
  children: React.ReactNode
}

const menuItems = [
  { icon: LayoutDashboard, label: "Overview", href: "/dashboard" },
  { icon: MessageSquare, label: "AI Copilot", href: "/dashboard/chat" },
  { icon: TrendingUp, label: "Cost Explorer", href: "/dashboard/cost-explorer" },
  { icon: AlertCircle, label: "Anomalies", href: "/dashboard/anomalies" },
  { icon: Package, label: "Resources", href: "/dashboard/resources" },
  { icon: CheckCircle2, label: "Recommendations", href: "/dashboard/recommendations" },
  { icon: History, label: "Optimizations", href: "/dashboard/optimizations" },
  { icon: BarChart3, label: "Savings", href: "/dashboard/savings" },
  { icon: Bell, label: "Alerts", href: "/dashboard/alerts" },
  { icon: FileText, label: "Reports", href: "/dashboard/reports" },
]

export default function DashboardLayout({ children }: DashboardLayoutProps) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(true)
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const router = useRouter()
  const { user, logout } = useAuthStore()
  const { providers, selectedProvider, selectProvider } = useCloudStore()

  const handleLogout = () => {
    logout()
    router.push("/auth/signin")
  }

  const selected = providers.find((p) => p.id === selectedProvider)

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false)
      }
    }

    document.addEventListener("mousedown", handleClickOutside)
    return () => document.removeEventListener("mousedown", handleClickOutside)
  }, [])

  return (
    <div className="min-h-screen bg-background">
      {/* Sidebar */}
      <aside
        className={`fixed left-0 top-0 h-screen bg-sidebar border-r border-sidebar-border glass z-40 transition-all duration-300 overflow-y-auto ${sidebarCollapsed ? "w-20" : "w-64"
          }`}
        onMouseEnter={() => setSidebarCollapsed(false)}
        onMouseLeave={() => setSidebarCollapsed(true)}
      >
        <div className={`flex items-center justify-center ${sidebarCollapsed ? "p-4" : "p-6"} mb-8 border-b border-sidebar-border`}>
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-cyan-400 to-purple-500 rounded-lg flex items-center justify-center flex-shrink-0">
              <span className="text-white font-bold text-sm">◆</span>
            </div>
            {!sidebarCollapsed && <span className="font-bold text-lg text-sidebar-foreground">OptiBrain</span>}
          </Link>
        </div>

        {/* Navigation */}
        <div className={`mb-6 ${sidebarCollapsed ? "px-2" : "px-4"}`}>
          {!sidebarCollapsed && <p className="text-xs font-semibold text-sidebar-foreground/60 uppercase mb-3">Cloud Cost Intelligence</p>}
          <nav className="space-y-1">
            {menuItems.map((item: any) => {
              const Icon = item.icon
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  title={sidebarCollapsed ? item.label : ""}
                  className="flex items-center gap-3 px-3 py-2 rounded-lg text-sidebar-foreground hover:bg-sidebar-accent/20 transition-colors group"
                >
                  <Icon className="w-5 h-5 text-sidebar-primary group-hover:text-sidebar-accent flex-shrink-0" />
                  {!sidebarCollapsed && <span className="text-sm font-medium">{item.label}</span>}
                </Link>
              )
            })}
          </nav>
        </div>

        {/* User menu */}
        <div className={`border-t border-sidebar-border pt-4 ${sidebarCollapsed ? "px-2" : "px-4"}`}>
          <button
            onClick={handleLogout}
            title={sidebarCollapsed ? "Sign Out" : ""}
            className="flex items-center gap-2 w-full px-3 py-2 rounded-lg text-sidebar-foreground hover:bg-destructive/20 text-destructive transition group"
          >
            <LogOut className="w-5 h-5 flex-shrink-0" />
            {!sidebarCollapsed && <span className="text-sm font-medium">Sign Out</span>}
          </button>
        </div>
      </aside>

      {/* Main content */}
      <div className={`transition-all duration-300 ${sidebarCollapsed ? "md:ml-20" : "md:ml-64"}`}>
        {/* Header - 52px flat design */}
        <header className="h-[52px] bg-slate-900 border-b border-slate-800 sticky top-0 z-30">
          <div className="px-4 flex items-center justify-between h-full gap-4">
            {/* Left: Collapse button + Provider Selector + Status */}
            <div className="flex items-center gap-3">
              <button
                onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
                className="p-1 hover:bg-slate-800 rounded transition hidden md:block"
                title="Toggle sidebar"
              >
                <Menu className="w-5 h-5 text-slate-400" />
              </button>

              {/* Provider Selector */}
              {selected && (
                <div className="hidden md:flex items-center gap-2 px-3 py-1.5 text-xs">
                  <span className="font-semibold text-slate-300">{selected.name}</span>
                  <span className="text-slate-600">•</span>
                  <div className="flex items-center gap-1.5">
                    <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></div>
                    <span className="text-slate-400">Live</span>
                  </div>
                </div>
              )}
            </div>

            {/* Center: Empty (optional breadcrumb later) */}
            <div className="flex-1"></div>

            {/* Right: Auto-sync + Avatar */}
            <div className="flex items-center gap-4">
              {/* Auto-sync indicator */}
              <div className="hidden sm:flex items-center gap-1.5 px-2 py-1 text-xs">
                <div className="w-1.5 h-1.5 rounded-full bg-emerald-500"></div>
                <span className="text-slate-400">Auto-sync</span>
              </div>

              {/* User Avatar Dropdown - Icon Only */}
              <div className="relative" ref={dropdownRef}>
                <button
                  onClick={() => setDropdownOpen(!dropdownOpen)}
                  className="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-500 to-purple-500 flex items-center justify-center hover:opacity-90 transition font-bold text-white text-sm flex-shrink-0"
                  title="Account menu"
                >
                  {user?.email?.[0]?.toUpperCase() || "U"}
                </button>

                {/* Dropdown Menu - Only shows when open */}
                {dropdownOpen && (
                  <div className="absolute right-0 mt-2 w-56 bg-slate-950 border border-slate-800 rounded-lg shadow-lg z-50 overflow-hidden">
                    {/* User Info Section */}
                    <div className="px-4 py-3 border-b border-slate-800">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-cyan-500 to-purple-500 flex items-center justify-center font-bold text-white text-sm flex-shrink-0">
                          {user?.email?.[0]?.toUpperCase() || "U"}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-semibold text-slate-200 truncate">John Doe</p>
                          <p className="text-xs text-cyan-400 truncate">{user?.email || "user@example.com"}</p>
                        </div>
                      </div>
                    </div>

                    {/* Sign Out */}
                    <button
                      onClick={() => {
                        setDropdownOpen(false)
                        handleLogout()
                      }}
                      className="w-full flex items-center gap-2 px-4 py-3 text-sm text-red-400 hover:bg-red-500/10 transition text-left"
                    >
                      <LogOut className="w-4 h-4" />
                      Sign Out
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="p-6 lg:p-8">{children}</main>
      </div>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 bg-black/50 z-30 md:hidden" onClick={() => setSidebarOpen(false)}></div>
      )}
    </div>
  )
}
