"use client"

import { mockCloudData } from "@/lib/mock-data"
import { Card } from "@/components/ui/card"
import { Zap, Timer, Database, HardDrive, Activity } from "lucide-react"

interface TodayImpactProps {
  data?: any
}

export default function TodayImpact({ data }: TodayImpactProps) {
  const topServices = data?.topServices || [
    { name: "EC2", cost: 62400 },
    { name: "RDS", cost: 31400 },
    { name: "S3", cost: 21200 },
    { name: "CloudWatch", cost: 13450 }
  ];

  const getIcon = (name: string) => {
    switch (name) {
      case "EC2": return Zap;
      case "RDS": return Database;
      case "S3": return HardDrive;
      case "CloudWatch": return Activity;
      default: return Activity;
    }
  };

  return (
    <Card className="glass border-border/50 p-6">
      <h2 className="text-lg font-bold text-foreground mb-4">Top Services by Cost</h2>
      <div className="space-y-4">
        {topServices.map((service: any, idx: number) => {
          const Icon = getIcon(service.name);
          return (
            <div key={idx} className="flex items-center gap-3 p-3 rounded-lg bg-secondary/20 border border-border/30">
              <div
                className={`w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500 to-cyan-600 p-2 flex items-center justify-center flex-shrink-0`}
              >
                <Icon className="w-5 h-5 text-white" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-muted-foreground">{service.name}</p>
                <p className="text-lg font-bold text-foreground">₹{service.cost.toLocaleString()}</p>
              </div>
            </div>
          );
        })}
      </div>
    </Card>
  )
}
