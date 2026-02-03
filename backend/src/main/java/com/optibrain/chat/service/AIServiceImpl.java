package com.optibrain.chat.service;

import org.springframework.stereotype.Service;

/**
 * Implementation of AIService
 */
@Service
public class AIServiceImpl implements AIService {
    
    @Override
    public String generateResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "I'm here to help with your cloud cost optimization questions. What would you like to know?";
        }
        
        String lowerMessage = userMessage.toLowerCase();
        
        if (lowerMessage.contains("cost") || lowerMessage.contains("spending")) {
            return "Your current monthly cloud costs are $8,920.30. I've identified several optimization opportunities that could save you approximately $2,340 per month. Would you like to see the detailed recommendations?";
        } else if (lowerMessage.contains("saving") || lowerMessage.contains("optimize")) {
            return "I've found several optimization opportunities: 1) Resize underutilized EC2 instances (save $450/mo), 2) Convert to Reserved Instances (save $1,200/mo), and 3) Delete unused EBS volumes (save $120/mo). Should I provide more details on any of these?";
        } else if (lowerMessage.contains("resource") || lowerMessage.contains("instance")) {
            return "You currently have 145 active resources across your cloud infrastructure. 12 of these are identified as idle or underutilized. Would you like me to show you the list of idle resources?";
        } else if (lowerMessage.contains("alert") || lowerMessage.contains("warning")) {
            return "You have 5 active alerts: 1 high-severity cost spike alert, 2 medium-severity idle resource alerts, and 2 low-severity recommendation notifications. Would you like to review them?";
        } else {
            return "I'm OptiBrain AI, your cloud cost optimization assistant. I can help you with cost analysis, resource optimization, savings projections, and answering questions about your cloud infrastructure. What specific information would you like to know?";
        }
    }
}
