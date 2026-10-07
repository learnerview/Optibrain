"use client"

import type { ReactNode } from "react"

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4 py-8">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-lg bg-primary text-lg font-semibold text-primary-foreground">
            O
          </div>
          <h1 className="text-xl font-semibold">OptiBrain</h1>
          <p className="text-sm text-muted-foreground">
            AWS-first cloud cost intelligence, with sandbox and dry-run safety by default.
          </p>
        </div>
        {children}
      </div>
    </div>
  )
}
