"use client";

import DashboardLayout from "@/components/dashboard/dashboard-layout";
import AICopilot from "@/components/copilot/ai-copilot";

export default function ChatPage() {
    return (
        <DashboardLayout>
            <div className="space-y-6">
                {/* Header */}
                <div>
                    <h1 className="text-3xl font-bold text-foreground mb-2">AI Copilot</h1>
                    <p className="text-muted-foreground">
                        Get intelligent insights and recommendations for your cloud infrastructure
                    </p>
                </div>

                {/* AI Copilot Component */}
                <AICopilot />
            </div>
        </DashboardLayout>
    );
}
