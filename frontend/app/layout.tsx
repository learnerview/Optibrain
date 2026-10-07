import type React from "react"
import type { Metadata, Viewport } from "next"
import "./globals.css"

export const metadata: Metadata = {
  title: "OptiBrain - AWS Cost Intelligence",
  description:
    "AWS cost optimisation: inventory, cost attribution, recommendations and governed remediation.",
}

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  // Zoom is left enabled. The previous viewport meta set maximumScale: 1 and
  // userScalable: false, which blocks pinch-zoom and fails WCAG 1.4.4.
  themeColor: "#0a0a0a",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" className="dark">
      <body className="min-h-screen bg-background font-sans antialiased text-foreground">
        {children}
      </body>
    </html>
  )
}