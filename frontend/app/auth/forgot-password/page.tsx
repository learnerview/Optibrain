"use client";

import type React from "react";

import { useState } from "react";
import Link from "next/link";
import { ArrowLeft, Mail } from "lucide-react";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [attempted, setAttempted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // The backend has no password-reset endpoint. Claiming a link was sent would be
    // fabricated: the user would wait for mail that is never delivered. The honest
    // response is to say the capability is not available.
    setAttempted(true);
  };

  return (
    <div>
      <div className="glass p-8 rounded-2xl border border-border/50">
        <h1 className="text-3xl font-bold mb-2">Reset Password</h1>

        {!attempted ? (
          <>
            <p className="text-muted-foreground mb-8">
              Self-service password reset is not available in this deployment. Enter
              your username below if you need to contact support; no email will be sent.
            </p>

            <form onSubmit={handleSubmit} className="space-y-4 mb-6">
              <div>
                <label className="text-sm font-medium block mb-2">Username</label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 w-4 h-4 text-muted-foreground" />
                  <input
                    type="text"
                    placeholder="your-username"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="pl-10 w-full h-10 rounded-md border border-border bg-background px-3 text-sm outline-none focus:border-primary"
                    required
                  />
                </div>
              </div>

              <button
                type="submit"
                className="w-full h-10 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90"
              >
                Ask an Administrator
              </button>
            </form>
          </>
        ) : (
          <div className="text-center">
            <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center mx-auto mb-4">
              <Mail className="w-6 h-6 text-primary" />
            </div>
            <h2 className="text-2xl font-bold mb-2">Contact Your Administrator</h2>
            <p className="text-muted-foreground mb-6">
              Password reset is performed by an administrator in this deployment. No
              reset email is sent automatically.
            </p>
          </div>
        )}

        <Link
          href="/auth/signin"
          className="flex items-center justify-center gap-2 text-primary hover:text-primary/80 mt-6"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Sign In
        </Link>
      </div>
    </div>
  );
}