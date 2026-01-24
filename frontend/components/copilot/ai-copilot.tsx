"use client";

import { useState, useRef, useEffect } from "react";
import { Send, Zap, BarChart3, AlertCircle, TrendingDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import ChatMessage from "@/components/copilot/chat-message";
import CopilotResponse from "@/components/copilot/copilot-response";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: Date;
  response?: {
    type: "text" | "chart" | "recommendation" | "analysis";
    data?: any;
  };
}

const suggestedQueries = [
  { icon: BarChart3, text: "What are my biggest cost drivers?" },
  { icon: AlertCircle, text: "Any anomalies in my spending?" },
  { icon: TrendingDown, text: "How can I reduce costs by 30%?" },
  { icon: Zap, text: "Which resources are underutilized?" },
];

export default function AICopilot() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const generateAIResponse = (query: string) => {
    const lowerQuery = query.toLowerCase();

    if (
      lowerQuery.includes("biggest cost") ||
      lowerQuery.includes("cost driver")
    ) {
      return {
        type: "chart" as const,
        data: {
          text: "Your biggest cost drivers are:",
          items: [
            { name: "EC2 Instances", cost: 2400, percentage: 38.6 },
            { name: "RDS Database", cost: 1800, percentage: 29 },
            { name: "CloudFront", cost: 980, percentage: 15.8 },
            { name: "S3 Storage", cost: 650, percentage: 10.5 },
            { name: "Lambda", cost: 320, percentage: 5.1 },
          ],
        },
      };
    }

    if (lowerQuery.includes("anomal")) {
      return {
        type: "analysis" as const,
        data: {
          text: "I detected 3 anomalies in your infrastructure:",
          anomalies: [
            {
              title: "Spike in EC2 Instances",
              severity: "high",
              description:
                "Unusual 40% increase in compute costs over the last 2 days",
              recommendation:
                "Review running instances and terminate unused resources",
            },
            {
              title: "Idle RDS Instance",
              severity: "medium",
              description: "Database running but receiving minimal queries",
              recommendation: "Consider down-sizing or consolidating databases",
            },
            {
              title: "Data Transfer Surge",
              severity: "medium",
              description: "Network bandwidth usage up 25% week-over-week",
              recommendation:
                "Review data transfer patterns and optimize CDN settings",
            },
          ],
        },
      };
    }

    if (lowerQuery.includes("reduce cost") || lowerQuery.includes("save")) {
      return {
        type: "recommendation" as const,
        data: {
          text: "Here are my top recommendations to save money:",
          recommendations: [
            {
              title: "Right-size EC2 instances",
              savings: 450,
              effort: "low",
              details:
                "Your t3.xlarge instances are over-provisioned. Downsize to t3.large.",
            },
            {
              title: "Use Reserved Instances",
              savings: 1200,
              effort: "medium",
              details:
                "3-year Reserved Instances on your baseline workload save significantly",
            },
            {
              title: "Enable S3 Intelligent-Tiering",
              savings: 280,
              effort: "low",
              details:
                "Automatically move objects to lower-cost storage classes",
            },
          ],
        },
      };
    }

    if (lowerQuery.includes("underutil") || lowerQuery.includes("unused")) {
      return {
        type: "text" as const,
        data: {
          text: "I found 5 underutilized resources that could be optimized:\n\n• Lambda functions: 12 functions with <1% monthly invocations\n• RDS instances: 3 databases with <10% CPU usage\n• EC2 instances: 8 instances in standby with minimal traffic\n• NAT Gateways: 2 NAT gateways with very low data processing\n• Load Balancers: 4 load balancers serving single instances\n\nYou could save ~$850/month by consolidating these.",
        },
      };
    }

    return {
      type: "text" as const,
      data: {
        text: `I'm analyzing your cloud infrastructure based on your query: "${query}".\n\nBased on the current data:\n• Total monthly spend: $8,230.75\n• Forecasted next month: $9,180.25\n• Available savings: $1,850.30\n\nWould you like me to dive deeper into any specific area?`,
      },
    };
  };

  const handleSendMessage = async (text?: string) => {
    const messageText = text || input;
    if (!messageText.trim()) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: "user",
      content: messageText,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setLoading(true);

    try {
      // Call backend API
      const response = await fetch("http://localhost:8080/api/copilot/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: messageText }),
      });

      if (response.ok) {
        const data = await response.json();
        const responseData = data.data;

        const assistantMessage: Message = {
          id: (Date.now() + 1).toString(),
          role: "assistant",
          content: responseData.type === "text" ? responseData.text : `[${responseData.type}]`,
          timestamp: new Date(),
          response: {
            type: responseData.type,
            data: responseData.data || responseData,
          },
        };

        setMessages((prev) => [...prev, assistantMessage]);
      } else {
        // Fallback to local response if backend fails
        const fallbackResponse = generateAIResponse(messageText);
        const assistantMessage: Message = {
          id: (Date.now() + 1).toString(),
          role: "assistant",
          content: fallbackResponse.type === "text" ? fallbackResponse.data.text : `[${fallbackResponse.type}]`,
          timestamp: new Date(),
          response: fallbackResponse,
        };
        setMessages((prev) => [...prev, assistantMessage]);
      }
    } catch (error) {
      console.error("Copilot API error:", error);
      // Fallback to local response
      const fallbackResponse = generateAIResponse(messageText);
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: fallbackResponse.type === "text" ? fallbackResponse.data.text : `[${fallbackResponse.type}]`,
        timestamp: new Date(),
        response: fallbackResponse,
      };
      setMessages((prev) => [...prev, assistantMessage]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid lg:grid-cols-4 gap-6 h-screen max-h-[80vh]">
      {/* Chat area */}
      <div className="lg:col-span-2 glass rounded-xl border border-border/50 flex flex-col overflow-hidden">
        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {messages.length === 0 ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-center">
                <div className="w-16 h-16 rounded-xl bg-primary/20 flex items-center justify-center mx-auto mb-4">
                  <Zap className="w-8 h-8 text-primary" />
                </div>
                <h3 className="text-lg font-semibold mb-2">
                  Welcome to OptiBrain Copilot
                </h3>
                <p className="text-muted-foreground max-w-xs">
                  Ask me about your cloud costs, get recommendations, or analyze
                  your spending patterns.
                </p>
              </div>
            </div>
          ) : (
            messages.map((msg) => (
              <ChatMessage key={msg.id} message={msg}>
                {msg.response && <CopilotResponse response={msg.response} />}
              </ChatMessage>
            ))
          )}
          {loading && (
            <div className="flex justify-start">
              <div className="bg-secondary/30 rounded-lg p-4 rounded-bl-none">
                <div className="flex gap-2">
                  <div className="w-2 h-2 bg-primary rounded-full animate-bounce"></div>
                  <div className="w-2 h-2 bg-primary rounded-full animate-bounce delay-100"></div>
                  <div className="w-2 h-2 bg-primary rounded-full animate-bounce delay-200"></div>
                </div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input */}
        <div className="border-t border-border/50 p-6">
          <div className="flex gap-3">
            <Input
              placeholder="Ask OptiBrain about your cloud costs..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyPress={(e) => e.key === "Enter" && handleSendMessage()}
              disabled={loading}
            />
            <Button
              size="icon"
              onClick={() => handleSendMessage()}
              disabled={!input.trim() || loading}
            >
              <Send className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </div>

      {/* Sidebar - Suggested queries */}
      <div className="lg:col-span-2 space-y-4">
        <div className="glass p-6 rounded-xl border border-border/50">
          <h3 className="font-semibold mb-4">Suggested Queries</h3>
          <div className="space-y-3">
            {suggestedQueries.map((query, index) => {
              const Icon = query.icon;
              return (
                <button
                  key={index}
                  onClick={() => handleSendMessage(query.text)}
                  className="w-full flex items-start gap-3 p-3 rounded-lg bg-secondary/30 hover:bg-secondary/50 border border-border/50 hover:border-primary/50 transition text-left group"
                >
                  <Icon className="w-5 h-5 text-primary flex-shrink-0 mt-0.5" />
                  <span className="text-sm group-hover:text-foreground text-muted-foreground">
                    {query.text}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Quick stats */}
        <div className="glass p-6 rounded-xl border border-border/50">
          <h3 className="font-semibold mb-4">Quick Stats</h3>
          <div className="space-y-3">
            <div className="flex justify-between items-center p-3 bg-secondary/30 rounded-lg">
              <span className="text-sm text-muted-foreground">Total Spend</span>
              <span className="font-semibold">$8,230</span>
            </div>
            <div className="flex justify-between items-center p-3 bg-secondary/30 rounded-lg">
              <span className="text-sm text-muted-foreground">Forecasted</span>
              <span className="font-semibold">$9,180</span>
            </div>
            <div className="flex justify-between items-center p-3 bg-secondary/30 rounded-lg">
              <span className="text-sm text-muted-foreground">Savings</span>
              <span className="font-semibold text-green-500">$1,850</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
