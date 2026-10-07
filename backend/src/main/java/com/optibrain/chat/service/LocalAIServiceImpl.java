package com.optibrain.chat.service;

import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class LocalAIServiceImpl implements AIService {

    @Override
    public String generateResponse(String userMessage) {
        String msg = userMessage.toLowerCase(Locale.ROOT);

        if (msg.contains("cost") || msg.contains("price") || msg.contains("billing")) {
            return "Based on your current usage, your projected cost for this month is $5,240. I recommend looking at the Rightsizing metrics to save approx $400.";
        } else if (msg.contains("policy") || msg.contains("rule")) {
            return "Your current policy is set to 'Balanced' mode. You can adjust cpu thresholds and budget limits in the Policy Settings.";
        } else if (msg.contains("anomaly") || msg.contains("alert") || msg.contains("issue")) {
            return "I detected a small memory spike in 'prod-db-01' at 08:00 AM today. It has since stabilized. No critical anomalies currently.";
        } else if (msg.contains("hello") || msg.contains("hi")) {
            return "Hello! I am OptiBrain, your autonomous Cloud Cost Intelligence assistant. How can I help you optimize your cloud today?";
        } else {
            return "I am a local AI assistant. I can help with Cost Analysis, Policy Configuration, and Anomaly Detection. Try asking 'What are my costs?' or 'Check my policies'.";
        }
    }
}
