/** @type {import('next').NextConfig} */
const nextConfig = {
  // Type errors must fail the build. They were suppressed here, which is how a
  // non-existent `SpotActionDTO` import and a broken onboarding payload survived in the
  // tree unnoticed.
  typescript: {
    ignoreBuildErrors: false,
  },
  images: {
    unoptimized: true,
  },
  /**
   * Proxy API calls through the Next.js origin instead of calling the backend
   * cross-origin.
   *
   * Two reasons this matters beyond convenience:
   *  - It removes the hardcoded `http://localhost:8080` that was baked into the client
   *    bundle and would have shipped to production.
   *  - It lets the session cookie work. The backend authenticates with HTTP Basic or a
   *    session; a cross-origin request without `credentials: "include"` never sends the
   *    cookie, so auth silently fails the moment the backend enforces it.
   */
  async rewrites() {
    const target = process.env.BACKEND_URL || "http://localhost:8080"
    return [
      {
        source: "/api/:path*",
        destination: `${target}/api/:path*`,
      },
    ]
  },
}

export default nextConfig