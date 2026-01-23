"use client";

import type React from "react";
import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuthStore } from "@/lib/store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  ArrowRight,
  Mail,
  Lock,
  Building2,
  AlertCircle,
  CheckCircle,
  Shield,
} from "lucide-react";

export default function SignUpPage() {
  const router = useRouter();
  const login = useAuthStore((state) => state.login);
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    company: "",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const getPasswordStrength = (password: string) => {
    if (!password) return 0;
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    return strength;
  };

  const passwordStrength = getPasswordStrength(formData.password);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError("");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    // Validation
    if (
      !formData.email ||
      !formData.password ||
      !formData.confirmPassword ||
      !formData.company
    ) {
      setError("All fields are required");
      setLoading(false);
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match");
      setLoading(false);
      return;
    }

    if (formData.password.length < 8) {
      setError("Password must be at least 8 characters");
      setLoading(false);
      return;
    }

    // Simulate signup
    setTimeout(() => {
      login(formData.email, formData.password);
      setSuccess(true);
      setTimeout(() => {
        setLoading(false);
        router.push("/onboarding");
      }, 1000);
    }, 1000);
  };

  return (
    <div className="glass p-8 rounded-2xl border border-primary/20 hover:border-primary/40 transition-all">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">Create Account</h1>
        <p className="text-muted-foreground">
          Join OptiBrain and start optimizing your cloud costs
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4 mb-6">
        {/* Email */}
        <div>
          <label className="text-sm font-medium block mb-2">
            Email Address
          </label>
          <div className="relative">
            <Mail className="absolute left-3 top-3.5 w-4 h-4 text-muted-foreground" />
            <Input
              type="email"
              name="email"
              placeholder="you@company.com"
              value={formData.email}
              onChange={handleChange}
              className="pl-10"
            />
          </div>
        </div>

        {/* Company */}
        <div>
          <label className="text-sm font-medium block mb-2">Company Name</label>
          <div className="relative">
            <Building2 className="absolute left-3 top-3.5 w-4 h-4 text-muted-foreground" />
            <Input
              type="text"
              name="company"
              placeholder="Acme Corp"
              value={formData.company}
              onChange={handleChange}
              className="pl-10"
            />
          </div>
        </div>

        {/* Password */}
        <div>
          <label className="text-sm font-medium block mb-2">Password</label>
          <div className="relative">
            <Lock className="absolute left-3 top-3.5 w-4 h-4 text-muted-foreground" />
            <Input
              type="password"
              name="password"
              placeholder="Min 8 characters"
              value={formData.password}
              onChange={handleChange}
              className="pl-10"
            />
          </div>
          {/* Password Strength Indicator */}
          {formData.password && (
            <div className="mt-2 space-y-2">
              <div className="flex gap-1">
                {[0, 1, 2, 3].map((index) => (
                  <div
                    key={index}
                    className={`flex-1 h-1 rounded-full transition-all ${
                      index < passwordStrength
                        ? "bg-primary"
                        : "bg-secondary/50"
                    }`}
                  ></div>
                ))}
              </div>
              <p className="text-xs text-muted-foreground">
                {passwordStrength === 0 && "Weak password"}
                {passwordStrength === 1 && "Weak"}
                {passwordStrength === 2 && "Fair"}
                {passwordStrength === 3 && "Good"}
                {passwordStrength === 4 && "Strong"}
              </p>
            </div>
          )}
        </div>

        {/* Confirm Password */}
        <div>
          <label className="text-sm font-medium block mb-2">
            Confirm Password
          </label>
          <div className="relative">
            <Lock className="absolute left-3 top-3.5 w-4 h-4 text-muted-foreground" />
            <Input
              type="password"
              name="confirmPassword"
              placeholder="Confirm password"
              value={formData.confirmPassword}
              onChange={handleChange}
              className="pl-10"
            />
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20 flex items-center gap-2 text-sm text-destructive">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            {error}
          </div>
        )}

        {/* Submit Button */}
        <Button type="submit" className="w-full" disabled={loading || success}>
          {success ? (
            <>
              <CheckCircle className="mr-2 w-4 h-4" />
              Account Created
            </>
          ) : loading ? (
            "Creating Account..."
          ) : (
            <>
              Create Account <ArrowRight className="ml-2 w-4 h-4" />
            </>
          )}
        </Button>
      </form>

      {/* Divider */}
      <div className="relative mb-6">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-border/30"></div>
        </div>
        <div className="relative flex justify-center text-xs">
          <span className="px-2 bg-card text-muted-foreground">
            Or continue with
          </span>
        </div>
      </div>

      {/* OAuth Buttons */}
      <div className="grid grid-cols-3 gap-3 mb-6">
        {/* TODO: Phase 4 - Implement Google OAuth integration with backend */}
        <Button
          variant="outline"
          className="bg-transparent hover:bg-slate-800/50 transition"
          onClick={() => alert("Google OAuth integration coming in Phase 4")}
          title="Google Sign-In - Phase 4"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
          </svg>
        </Button>
        {/* TODO: Phase 4 - Implement GitHub OAuth integration with backend */}
        <Button
          variant="outline"
          className="bg-transparent hover:bg-slate-800/50 transition"
          onClick={() => alert("GitHub OAuth integration coming in Phase 4")}
          title="GitHub Sign-In - Phase 4"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v 3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
          </svg>
        </Button>
        {/* TODO: Phase 4 - Implement LinkedIn OAuth integration with backend */}
        <Button
          variant="outline"
          className="bg-transparent hover:bg-slate-800/50 transition"
          onClick={() => alert("LinkedIn OAuth integration coming in Phase 4")}
          title="LinkedIn Sign-In - Phase 4"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.225 0z" />
          </svg>
        </Button>
      </div>

      {/* Sign In Link */}
      <p className="text-center text-sm text-muted-foreground">
        Already have an account?{" "}
        <Link
          href="/auth/signin"
          className="text-primary hover:text-primary/80 font-medium transition"
        >
          Sign in
        </Link>
      </p>

      {/* Security Badge */}
      <div className="mt-6 pt-6 border-t border-border/50 text-center">
        <p className="text-xs text-muted-foreground flex items-center justify-center gap-2">
          <Shield className="w-3 h-3" />
          Enterprise-grade security with SOC 2 compliance
        </p>
      </div>
    </div>
  );
}
