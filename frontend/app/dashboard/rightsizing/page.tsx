"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuthStore } from "@/lib/store"
import DashboardLayout from "@/components/dashboard/dashboard-layout"
import RightsizingEngine from "@/components/analytics/rightsizing-engine"

export default function RightsizingPage() {
  const router = useRouter()
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/signin")
    }
  }, [isAuthenticated, router])

  return (
    <DashboardLayout>
      <RightsizingEngine />
    </DashboardLayout>
  )
}
