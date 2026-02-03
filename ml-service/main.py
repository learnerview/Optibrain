from fastapi import FastAPI, HTTPException, BackgroundTasks, WebSocket
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Optional, Any
import asyncio
import uvicorn
from contextlib import asynccontextmanager

from services.ml_service import MLService
from services.model_manager import ModelManager
from services.feature_engineering import FeatureEngineering
from services.anomaly_detection import AnomalyDetectionService
from services.forecasting_service import ForecastingService
from services.chat_service import websocket_endpoint, chat_http_endpoint
from utils.logger import setup_logger
from config.settings import Settings

# Setup logger
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
    tenant_id: str
    metric_type: str  # 'cpu', 'memory', 'cost'
    forecast_horizon: int  # hours ahead
    historical_data: Optional[List[float]] = None

class SpotPredictionRequest(BaseModel):
    instance_type: str
    region: str

class AnomalyRequest(BaseModel):
    tenant_id: str
    metrics_data: List[MetricsData]
    sensitivity: float = 0.1

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

@app.get("/health/detailed")
async def detailed_health_check():
    """Detailed health check with component status"""
    import sys
    import psutil
    
    health = {
        "status": "healthy",
        "service": "OptiBrain ML Service",
        "version": "1.0.0",
        "timestamp": asyncio.get_event_loop().time(),
        "components": {}
    }
    
    # Check ML service initialization
    if ml_service:
        health["components"]["ml_service"] = {
            "status": "UP",
            "initialized": True
        }
    else:
        health["components"]["ml_service"] = {
            "status": "DOWN",
            "initialized": False
        }
        health["status"] = "degraded"
    
    # System metrics
    health["system"] = {
        "python_version": sys.version,
        "cpu_count": psutil.cpu_count(),
        "cpu_percent": psutil.cpu_percent(interval=0.1),
        "memory_total": psutil.virtual_memory().total,
        "memory_available": psutil.virtual_memory().available,
        "memory_percent": psutil.virtual_memory().percent
    }
    
    return health

@app.get("/health/ready")
async def readiness_check():
    """Kubernetes readiness probe"""
    if ml_service and ml_service.is_ready:
        return {"ready": True, "status": "READY"}
    return {"ready": False, "status": "NOT_READY"}

@app.get("/health/live")
async def liveness_check():
    """Kubernetes liveness probe"""
    return {"alive": True, "status": "ALIVE", "timestamp": asyncio.get_event_loop().time()}

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
        # In a real implementation, this would call an LLM service
        # For now, we return a mock intelligent response based on the context
        
        response = f"I analyzed your request about '{request.context}'. "
        if "cost" in request.message.lower():
            response += "Based on current trends, your cloud costs are stable. I recommend reviewing your RIs."
        elif "anomaly" in request.message.lower():
            response += "I detected 2 minor anomalies in the last 24 hours in the us-east-1 region."
        else:
            response += "I am ready to help you optimize your cloud infrastructure."
            
        return {"response": response}
    except Exception as e:
        logger.error(f"Chat processing failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.websocket("/ws/chat/{user_id}")
async def websocket_chat(websocket: WebSocket, user_id: str):
    """WebSocket endpoint for real-time chat"""
    await websocket_endpoint(websocket, user_id)

@app.post("/chat/message")
async def chat_message(message: str, user_id: str = "default"):
    """HTTP endpoint for chat (fallback)"""
    return await chat_http_endpoint(message, user_id)

if __name__ == "__main__":
    settings = Settings()
    uvicorn.run(
        "main:app",
        host=settings.ML_SERVICE_HOST,
        port=settings.ML_SERVICE_PORT,
        reload=settings.DEBUG,
        log_level="info"
    )
