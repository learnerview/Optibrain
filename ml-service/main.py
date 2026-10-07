from fastapi import FastAPI, HTTPException, BackgroundTasks, WebSocket
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Any
import asyncio
import uvicorn
from contextlib import asynccontextmanager

from services.ml_service import MLService
from utils.logger import setup_logger
from config.settings import Settings
import os

logger = setup_logger(__name__)

# Global ML service instance
ml_service = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    global ml_service
    logger.info("Starting ML Service...")
    
    settings = Settings()
    ml_service = MLService(settings)
    await ml_service.initialize()
    
    logger.info("ML Service started successfully")
    yield
    
    # Shutdown
    logger.info("Shutting down ML Service...")
    if ml_service:
        await ml_service.cleanup()
    logger.info("ML Service shutdown complete")

# Create FastAPI app
app = FastAPI(
    title="OptiBrain ML Service",
    description="Real Machine Learning for Autonomous Cloud Cost Intelligence",
    version="1.0.0",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:8080",
        "http://optibrain-backend:8080",
        "http://localhost:3000" # For frontend development
    ],
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

ML_SERVICE_API_KEY = os.getenv("ML_SERVICE_API_KEY", "dev-local-ml-api-key")

@app.middleware("http")
async def require_ml_api_key(request, call_next):
    if request.url.path in ("/health", "/docs", "/openapi.json", "/redoc"):
        return await call_next(request)
    provided = request.headers.get("X-Api-Key")
    if provided != ML_SERVICE_API_KEY:
        raise HTTPException(status_code=401, detail="Missing or invalid X-Api-Key")
    return await call_next(request)

# Pydantic models for API
class MetricsData(BaseModel):
    tenant_id: str
    timestamp: str
    cpu_utilization: float
    memory_utilization: float
    hourly_cost: float
    instance_count: int
    region: str
    service_breakdown: Dict[str, float]

class ForecastRequest(BaseModel):
    tenant_id: str = Field(min_length=1, max_length=128)
    metric_type: str = Field(min_length=1, max_length=64)  # 'cpu', 'memory', 'cost'
    forecast_horizon: int = Field(ge=1, le=720)  # hours ahead
    historical_data: Optional[List[float]] = None

class SpotPredictionRequest(BaseModel):
    instance_type: str = Field(min_length=1, max_length=64)
    region: str = Field(min_length=1, max_length=64)

class AnomalyRequest(BaseModel):
    tenant_id: str = Field(min_length=1, max_length=128)
    metrics_data: List[MetricsData]
    sensitivity: float = Field(default=0.1, ge=0.001, le=1.0)

class OptimizationRequest(BaseModel):
    tenant_id: str
    current_metrics: Optional[MetricsData] = None
    budget_constraints: Optional[Dict[str, float]] = None
    aws_data: Optional[Dict[str, Any]] = None
    analysis_type: Optional[str] = "cost_optimization"
    generate_defaults: Optional[bool] = False

class OptimizationResponse(BaseModel):
    recommended_actions: List[Dict[str, Any]]
    expected_savings: float  # Maps to predictedSavings in backend
    confidence: float
    risk_assessment: str
    implementation_priority: str
    # Dynamic Configuration Fields
    optimization_bounds: Optional[Dict[str, Any]] = None
    decision_engine: Optional[Dict[str, Any]] = None
    user_permissions: Optional[Dict[str, Any]] = None
    autonomous_settings: Optional[Dict[str, Any]] = None
    alert_thresholds: Optional[Dict[str, Any]] = None

class PredictionResponse(BaseModel):
    predictions: List[float]
    confidence_intervals: List[List[float]]
    model_metadata: Dict[str, Any]
    forecast_horizon: int
    metric_type: str

class AnomalyResponse(BaseModel):
    anomalies: List[Dict[str, Any]]
    anomaly_score: float
    sensitivity: float
    total_data_points: int
    anomaly_count: int

# API Endpoints
@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "service": "optibrain-ml", "version": "1.0.0"}

@app.post("/predict/forecast", response_model=PredictionResponse)
async def generate_forecast(request: ForecastRequest):
    """Generate time series forecast for tenant metrics"""
    try:
        result = await ml_service.generate_forecast(
            tenant_id=request.tenant_id,
            metric_type=request.metric_type,
            forecast_horizon=request.forecast_horizon,
            historical_data=request.historical_data
        )
        return result
    except Exception as e:
        logger.error(f"Forecast generation failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/predict/spot")
async def generate_spot_prediction(request: SpotPredictionRequest):
    """
    Analyzes historical spot market data and regional trends to predict
    the probability of instance interruption.
    """
    try:
        result = await ml_service.generate_spot_prediction(
            instance_type=request.instance_type,
            region=request.region
        )
        return result
    except Exception as e:
        logger.error(f"Spot prediction failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/detect/anomalies", response_model=AnomalyResponse)
async def detect_anomalies(request: AnomalyRequest):
    """
    Identifies statistical outliers in telemetry data streams using
    Isolation Forest and ensemble detection techniques.
    """
    try:
        result = await ml_service.detect_anomalies(
            tenant_id=request.tenant_id,
            metrics_data=request.metrics_data,
            sensitivity=request.sensitivity
        )
        return result
    except Exception as e:
        logger.error(f"Anomaly detection failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/optimize/cost", response_model=OptimizationResponse)
async def optimize_costs(request: OptimizationRequest):
    """
    Generates high-impact cost optimization recommendations based on
    resource rightsizing and commitment management logic.
    """
    try:
        result = await ml_service.optimize_costs(
            tenant_id=request.tenant_id,
            current_metrics=request.current_metrics,
            budget_constraints=request.budget_constraints,
            aws_data=request.aws_data,
            generate_defaults=request.generate_defaults
        )
        return result
    except Exception as e:
        logger.error(f"Cost optimization failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/train/models")
async def train_models(background_tasks: BackgroundTasks):
    """Trigger model training for all tenants"""
    try:
        background_tasks.add_task(ml_service.train_all_models)
        return {"message": "Model training started", "status": "in_progress"}
    except Exception as e:
        logger.error(f"Model training failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/models/status")
async def get_model_status():
    """Get status of all ML models"""
    try:
        status = await ml_service.get_model_status()
        return status
    except Exception as e:
        logger.error(f"Model status check failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/features/{tenant_id}")
async def get_features(tenant_id: str):
    """Get engineered features for a tenant"""
    try:
        features = await ml_service.get_tenant_features(tenant_id)
        return {"tenant_id": tenant_id, "features": features}
    except Exception as e:
        logger.error(f"Feature retrieval failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/feedback")
async def record_feedback(tenant_id: str, prediction_id: str, actual_value: float, predicted_value: float):
    """Record feedback for model improvement"""
    try:
        await ml_service.record_feedback(tenant_id, prediction_id, actual_value, predicted_value)
        return {"message": "Feedback recorded successfully"}
    except Exception as e:
        logger.error(f"Feedback recording failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

class ChatRequest(BaseModel):
    message: str
    context: str

@app.post("/chat/intelligent")
async def intelligent_chat(request: ChatRequest):
    """
    Process natural language queries about Cloud Cost Intelligence data using LLM capability
    """
    try:
        # This service does not host a chat LLM. The honest answer is that the OptiBrain
        # Copilot lives behind the backend; answering differently would be fabrication.
        return {
            "response": (
                "Chat/copilot is served by the backend, not by the ML service. "
                "Call the backend's /api/copilot/chat endpoint for cost answers."
            )
        }
    except Exception as e:
        logger.error(f"Chat processing failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.websocket("/ws/chat/{user_id}")
async def websocket_chat(websocket: WebSocket, user_id: str):
    """WebSocket endpoint for real-time chat"""
    # The ML service does not host a chat LLM. Take the connection, state why no
    # conversation is available, and close. Sending a fabricated answer is not honest mode.
    await websocket.accept()
    await websocket.send_text("{\"error\":\"chat/copilot is served by the backend /api/copilot/chat\"}")
    await websocket.close(code=1000)

@app.post("/chat/message")
async def chat_message(message: str, user_id: str = "default"):
    """HTTP endpoint for chat (fallback)"""
    return {
        "success": False,
        "data": {
            "message": (
                "Chat/copilot is served by the backend, not by the ML service. "
                "Call the backend's /api/copilot/chat endpoint for cost answers."
            )
        },
    }

if __name__ == "__main__":
    settings = Settings()
    uvicorn.run(
        "main:app",
        host=settings.ML_SERVICE_HOST,
        port=settings.ML_SERVICE_PORT,
        reload=settings.DEBUG,
        log_level="info"
    )
