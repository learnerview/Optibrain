"use client";

import { useState } from "react";
import { Send, BarChart3, AlertCircle, TrendingDown, Zap, Boxes } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { askCopilot } from "@/lib/api";
import ChatMessage from "@/components/copilot/chat-message";
import CopilotResponse from "@/components/copilot/copilot-response";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: Date;
  response?: {
    type: string;
    data?: unknown;
  };
}

/**
 * Queries phrased against what the backend actually answers. These are not marketing
 * prompts; each one maps to a capability in CopilotService.
 */
const suggestedQueries = [
  { icon: BarChart3, text: "What are my biggest cost drivers?" },
  { icon: TrendingDown, text: "Which resources are idle?" },
  { icon: AlertCircle, text: "How can I reduce costs?" },
  { icon: Boxes, text: "How many resources do I have?" },
];

export default function AICopilot() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const send = async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || loading) return;

    setMessages((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        role: "user",
        content: trimmed,
        timestamp: new Date(),
      },
    ]);
    setInput("");
    setLoading(true);
    setError(null);

    try {
      const reply = await askCopilot(trimmed);
      if (!reply || typeof reply.text !== "string") {
        throw new Error("The assistant returned an empty response.");
      }
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          role: "assistant",
          content: reply.text,
          timestamp: new Date(),
          response: { type: reply.type, data: reply.data },
        },
      ]);
    } catch (cause) {
      // Previously this fell through to a local `generateAIResponse` that returned
      // fabricated charts, anomalies and savings figures. A failure is reported as a
      // failure: an assistant that invents numbers when the backend is down is worse
      // than one that admits it is blind.
      setError(
        cause instanceof Error
          ? cause.message
          : "Could not reach the OptiBrain backend."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto flex h-full w-full max-w-4xl flex-col gap-4 p-4">
      <Card>
        <CardHeader>
          <CardTitle>Ask your account</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Answers come from your live inventory, CloudWatch telemetry and Cost
          Explorer. When a figure is unavailable the assistant says so instead of
          estimating.
        </CardContent>
      </Card>

      <div className="flex flex-1 flex-col gap-4 overflow-y-auto">
        {messages.length === 0 && (
          <div className="grid gap-2 sm:grid-cols-2">
            {suggestedQueries.map(({ icon: Icon, text }) => (
              <Button
                key={text}
                variant="outline"
                className="justify-start"
                onClick={() => send(text)}
              >
                <Icon className="mr-2 h-4 w-4" aria-hidden />
                {text}
              </Button>
            ))}
          </div>
        )}

        {messages.map((message) => (
          <ChatMessage key={message.id} message={message} />
        ))}

        {loading && (
          <p className="text-sm text-muted-foreground" role="status">
            Measuring your account…
          </p>
        )}

        {error && (
          <Card className="border-amber-500/30 bg-amber-500/5">
            <CardContent className="py-3 text-sm text-amber-300">
              {error}
            </CardContent>
          </Card>
        )}
      </div>

      <form
        className="flex items-center gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          void send(input);
        }}
      >
        <Input
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="Ask about cost, idle resources or savings"
          aria-label="Ask the copilot"
          disabled={loading}
        />
        <Button type="submit" disabled={loading || !input.trim()}>
          <Send className="mr-2 h-4 w-4" aria-hidden />
          Send
        </Button>
      </form>
    </div>
  );
}