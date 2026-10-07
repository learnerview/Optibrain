"use client"

import type React from "react"

interface ChatMessageProps {
  message: {
    role: "user" | "assistant"
    content: string
    timestamp: Date
  }
  children?: React.ReactNode
}

export default function ChatMessage({ message, children }: ChatMessageProps) {
  const isUser = message.role === "user"

  return (
    <div className={`flex ${isUser ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-sm ${
          isUser
            ? "bg-primary text-primary-foreground rounded-lg rounded-tr-none"
            : "bg-secondary/30 text-foreground rounded-lg rounded-bl-none border border-border/50"
        } p-4`}
      >
        {!isUser && !children && <p className="text-sm whitespace-pre-wrap">{message.content}</p>}
        {children}
      </div>
    </div>
  )
}
