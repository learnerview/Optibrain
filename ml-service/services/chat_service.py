"""
Enhanced WebSocket Chat Service with Gemini AI Integration
Provides real-time chat with dynamic AI-powered responses
"""

import asyncio
import json
import logging
import os
from typing import Dict, List, Any
from datetime import datetime

from fastapi import WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)

# Configure Gemini API
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "AIzaSyCESW_0GwkqGk6jLvjeKmlJ5AMYG08MllQ")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-pro")

# Try to import and configure Gemini
try:
    import google.generativeai as genai
    genai.configure(api_key=GEMINI_API_KEY)
    model = genai.GenerativeModel(GEMINI_MODEL)
    GEMINI_AVAILABLE = True
    logger.info("Gemini AI configured successfully")
except Exception as e:
    GEMINI_AVAILABLE = False
    logger.warning(f"Gemini AI not available: {str(e)}")


class ConnectionManager:
    """Manages WebSocket connections"""
    
    def __init__(self):
        self.active_connections: List[WebSocket] = []
    
    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        logger.info(f"New WebSocket connection. Total connections: {len(self.active_connections)}")
    
    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)
        logger.info(f"WebSocket disconnected. Total connections: {len(self.active_connections)}")
    
    async def send_personal_message(self, message: dict, websocket: WebSocket):
        await websocket.send_json(message)
    
    async def broadcast(self, message: dict):
        for connection in self.active_connections:
            await connection.send_json(message)


manager = ConnectionManager()


class ChatService:
    """Handles chat logic with Gemini AI-powered responses"""
    
    def __init__(self):
        self.conversation_history: Dict[str, List[Dict]] = {}
        self.system_prompt = """You are an AI assistant for OptiBrain, a cloud cost optimization platform. 
You help users understand and optimize their cloud infrastructure costs.

Current user context:
- Monthly cloud spend: ₹128,450
- Main services: EC2 (₹62,400), RDS (₹31,400), S3 (₹21,200), CloudWatch (₹13,450)
- Recent anomaly: 116% spike in EC2 costs on 2026-01-14
- Potential savings identified: ₹13,000/month

When answering:
1. Be specific and actionable
2. Reference the user's actual data when relevant
3. Provide cost-saving recommendations
4. Be concise but informative (2-3 paragraphs max)
5. Use bullet points for clarity
"""
    
    async def process_message(self, user_id: str, message: str) -> Dict[str, Any]:
        """Process incoming message and generate AI-powered response"""
        
        # Store message in history
        if user_id not in self.conversation_history:
            self.conversation_history[user_id] = []
        
        self.conversation_history[user_id].append({
            "role": "user",
            "content": message,
            "timestamp": datetime.now().isoformat()
        })
        
        # Generate response using Gemini AI or fallback
        if GEMINI_AVAILABLE:
            response = await self._generate_gemini_response(message, user_id)
        else:
            response = await self._generate_fallback_response(message)
        
        # Store response in history
        self.conversation_history[user_id].append({
            "role": "assistant",
            "content": response,
            "timestamp": datetime.now().isoformat()
        })
        
        return response
    
    async def _generate_gemini_response(self, message: str, user_id: str) -> Dict[str, Any]:
        """Generate intelligent response using Gemini AI"""
        
        try:
            # Build conversation context
            history = self.conversation_history.get(user_id, [])
            context = self.system_prompt + "\n\nUser question: " + message
            
            # Generate response with Gemini
            response = model.generate_content(context)
            ai_text = response.text
            
            logger.info(f"Gemini response generated successfully for user {user_id}")
            
            # Determine response type based on content
            response_type = self._determine_response_type(message, ai_text)
            
            return {
                "type": response_type,
                "text": ai_text,
                "data": self._extract_structured_data(message, ai_text, response_type)
            }
            
        except Exception as e:
            logger.error(f"Gemini API error: {str(e)}")
            # Fallback to rule-based response
            return await self._generate_fallback_response(message)
    
    def _determine_response_type(self, message: str, response: str) -> str:
        """Determine the type of response based on content"""
        message_lower = message.lower()
        
        if "cost" in message_lower and ("driver" in message_lower or "breakdown" in message_lower):
            return "chart"
        elif "anomal" in message_lower or "spike" in message_lower:
            return "analysis"
        elif "reduce" in message_lower or "save" in message_lower or "recommend" in message_lower:
            return "recommendation"
        elif "forecast" in message_lower or "predict" in message_lower:
            return "chart"
        else:
            return "text"
    
    def _extract_structured_data(self, message: str, response: str, response_type: str) -> Dict[str, Any]:
        """Extract structured data for visualization"""
        
        if response_type == "chart":
            return {
                "items": [
                    {"name": "EC2 Instances", "cost": 62400, "percentage": 48.6},
                    {"name": "RDS Database", "cost": 31400, "percentage": 24.4},
                    {"name": "S3 Storage", "cost": 21200, "percentage": 16.5},
                    {"name": "CloudWatch", "cost": 13450, "percentage": 10.5}
                ]
            }
        elif response_type == "analysis":
            return {
                "anomalies": [
                    {
                        "title": "EC2 Cost Spike",
                        "severity": "high",
                        "description": "116% increase detected on 2026-01-14",
                        "recommendation": "Review and optimize EC2 instances"
                    }
                ]
            }
        elif response_type == "recommendation":
            return {
                "recommendations": [
                    {
                        "title": "Right-size EC2 instances",
                        "savings": 8400,
                        "effort": "low",
                        "details": "Downsize underutilized instances"
                    },
                    {
                        "title": "Optimize RDS storage",
                        "savings": 4600,
                        "effort": "low",
                        "details": "Reduce over-provisioned storage"
                    }
                ]
            }
        else:
            return {}
    
    async def _generate_fallback_response(self, message: str) -> Dict[str, Any]:
        """Fallback response with rule-based logic"""
        
        message_lower = message.lower()
        
        # Cost optimization
        if "optim" in message_lower or "save" in message_lower or "reduce" in message_lower:
            return {
                "type": "text",
                "text": "Based on your ₹128,450/month cloud spend, here are key optimization opportunities:\n\n" +
                    "• **EC2 Right-sizing**: Your EC2 costs (₹62,400) show potential for 15-20% savings through instance optimization\n" +
                    "• **RDS Storage**: Reduce over-provisioned storage to save ₹4,600/month\n" +
                    "• **S3 Intelligent-Tiering**: Enable automatic tiering for ₹2,800/month savings\n\n" +
                    "**Total potential savings: ₹13,000/month**"
            }
        
        # Cost drivers
        elif "driver" in message_lower or "breakdown" in message_lower:
            return {
                "type": "chart",
                "text": "Your cloud costs are distributed across these services:",
                "data": {
                    "items": [
                        {"name": "EC2 Instances", "cost": 62400, "percentage": 48.6},
                        {"name": "RDS Database", "cost": 31400, "percentage": 24.4},
                        {"name": "S3 Storage", "cost": 21200, "percentage": 16.5},
                        {"name": "CloudWatch", "cost": 13450, "percentage": 10.5}
                    ]
                }
            }
        
        # Anomalies
        elif "anomal" in message_lower or "spike" in message_lower:
            return {
                "type": "analysis",
                "text": "I detected a significant cost anomaly in your infrastructure:",
                "data": {
                    "anomalies": [
                        {
                            "title": "EC2 Cost Spike",
                            "severity": "high",
                            "description": "116% increase on 2026-01-14 - likely due to auto-scaling or new instances",
                            "recommendation": "Review EC2 instance launches and terminate unused resources"
                        }
                    ]
                }
            }
        
        # Default
        else:
            return {
                "type": "text",
                "text": f"I can help you with cloud cost optimization! Here's what I know about your infrastructure:\n\n" +
                    "• **Current spend**: ₹128,450/month\n" +
                    "• **Forecast**: ₹135,200 next month\n" +
                    "• **Savings opportunity**: ₹13,000/month\n\n" +
                    "Ask me about:\n" +
                    "- Cost optimization strategies\n" +
                    "- Resource right-sizing\n" +
                    "- Anomaly investigation\n" +
                    "- Specific service recommendations"
            }


chat_service = ChatService()


async def websocket_endpoint(websocket: WebSocket, user_id: str = "default"):
    """WebSocket endpoint for real-time chat"""
    await manager.connect(websocket)
    
    try:
        # Send welcome message
        await manager.send_personal_message({
            "type": "system",
            "message": f"Connected to OptiBrain AI Copilot {'(Powered by Gemini)' if GEMINI_AVAILABLE else '(Demo Mode)'}",
            "timestamp": datetime.now().isoformat()
        }, websocket)
        
        while True:
            # Receive message from client
            data = await websocket.receive_text()
            message_data = json.loads(data)
            
            message = message_data.get("message", "")
            logger.info(f"Received message from {user_id}: {message}")
            
            # Send typing indicator
            await manager.send_personal_message({
                "type": "typing",
                "timestamp": datetime.now().isoformat()
            }, websocket)
            
            # Process message and get AI response
            response = await chat_service.process_message(user_id, message)
            
            # Send response
            await manager.send_personal_message({
                "type": "message",
                "data": response,
                "timestamp": datetime.now().isoformat()
            }, websocket)
            
    except WebSocketDisconnect:
        manager.disconnect(websocket)
        logger.info(f"Client {user_id} disconnected")
    except Exception as e:
        logger.error(f"Error in WebSocket connection: {str(e)}")
        manager.disconnect(websocket)


async def chat_http_endpoint(message: str, user_id: str = "default") -> JSONResponse:
    """HTTP fallback endpoint for chat"""
    try:
        response = await chat_service.process_message(user_id, message)
        return JSONResponse({
            "success": True,
            "data": response,
            "timestamp": datetime.now().isoformat()
        })
    except Exception as e:
        logger.error(f"Error processing chat message: {str(e)}")
        return JSONResponse({
            "success": False,
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }, status_code=500)
