from pydantic_settings import BaseSettings
from typing import Optional
import os

class DynamicSettings(BaseSettings):
    # Service Configuration
    ML_SERVICE_HOST: str = os.getenv("ML_SERVICE_HOST", "localhost")
    ML_SERVICE_PORT: int = int(os.getenv("ML_SERVICE_PORT", "8000"))
    DEBUG: bool = os.getenv("DEBUG", "true").lower() == "true"
    
    # Database Configuration
    DATABASE_URL: str = os.getenv("DATABASE_URL", "postgresql://postgres:password@localhost:5432/optibrain_ml")
    
    # Redis Configuration
    REDIS_URL: str = os.getenv("REDIS_URL", "redis://localhost:6379/0")
    
    # Java Backend Configuration
    JAVA_BACKEND_URL: str = os.getenv("JAVA_BACKEND_URL", "http://localhost:8080")
    
    # Logging Configuration
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    LOG_FILE: str = os.getenv("LOG_FILE", "logs/ml_service.log")
    
    # ML Model Configuration
    MODEL_CACHE_TTL: int = int(os.getenv("MODEL_CACHE_TTL", "3600"))  # 1 hour
    FEATURE_STORE_TTL: int = int(os.getenv("FEATURE_STORE_TTL", "1800"))  # 30 minutes
    
    # Training Configuration
    RETRAINING_INTERVAL: int = int(os.getenv("RETRAINING_INTERVAL", "86400"))  # 24 hours
    MIN_TRAINING_SAMPLES: int = int(os.getenv("MIN_TRAINING_SAMPLES", "100"))
    
    # Anomaly Detection Configuration (Dynamic)
    ANOMALY_SENSITIVITY: float = float(os.getenv("ANOMALY_SENSITIVITY", "0.1"))
    ANOMALY_WINDOW_SIZE: int = int(os.getenv("ANOMALY_WINDOW_SIZE", "24"))  # hours
    
    # Forecasting Configuration (Dynamic)
    MAX_FORECAST_HORIZON: int = int(os.getenv("MAX_FORECAST_HORIZON", "168"))  # 7 days
    DEFAULT_FORECAST_HORIZON: int = int(os.getenv("DEFAULT_FORECAST_HORIZON", "24"))  # 1 day
    
    # Optimization Bounds (Dynamic)
    MIN_HOURLY_COST: float = float(os.getenv("MIN_HOURLY_COST", "0.01"))
    MAX_HOURLY_COST: float = float(os.getenv("MAX_HOURLY_COST", "10000.0"))
    MIN_CPU_UTILIZATION: float = float(os.getenv("MIN_CPU_UTILIZATION", "0.0"))
    MAX_CPU_UTILIZATION: float = float(os.getenv("MAX_CPU_UTILIZATION", "100.0"))
    MIN_MEMORY_UTILIZATION: float = float(os.getenv("MIN_MEMORY_UTILIZATION", "0.0"))
    MAX_MEMORY_UTILIZATION: float = float(os.getenv("MAX_MEMORY_UTILIZATION", "100.0"))
    SCALE_UP_THRESHOLD: float = float(os.getenv("SCALE_UP_THRESHOLD", "80.0"))
    SCALE_DOWN_THRESHOLD: float = float(os.getenv("SCALE_DOWN_THRESHOLD", "20.0"))
    
    # Decision Engine Configuration (Dynamic)
    ML_WEIGHT: float = float(os.getenv("ML_WEIGHT", "0.7"))
    RULE_WEIGHT: float = float(os.getenv("RULE_WEIGHT", "0.3"))
    APPROVAL_THRESHOLD: float = float(os.getenv("APPROVAL_THRESHOLD", "0.6"))
    RISK_THRESHOLD: float = float(os.getenv("RISK_THRESHOLD", "0.8"))
    SAVINGS_THRESHOLD: float = float(os.getenv("SAVINGS_THRESHOLD", "10.0"))
    
    # Model Configuration
    ENABLE_ML_RECOMMENDATIONS: bool = os.getenv("ENABLE_ML_RECOMMENDATIONS", "true").lower() == "true"
    REQUIRE_HUMAN_APPROVAL: bool = os.getenv("REQUIRE_HUMAN_APPROVAL", "false").lower() == "true"
    AUTO_EXECUTE_LOW_RISK: bool = os.getenv("AUTO_EXECUTE_LOW_RISK", "true").lower() == "true"
    AUTO_EXECUTE_MEDIUM_RISK: bool = os.getenv("AUTO_EXECUTE_MEDIUM_RISK", "false").lower() == "true"
    REQUIRE_APPROVAL_FOR_HIGH_RISK: bool = os.getenv("REQUIRE_APPROVAL_FOR_HIGH_RISK", "true").lower() == "true"
    
    # Feature Engineering Configuration
    ENABLE_FEATURE_CACHING: bool = os.getenv("ENABLE_FEATURE_CACHING", "true").lower() == "true"
    FEATURE_CACHE_SIZE: int = int(os.getenv("FEATURE_CACHE_SIZE", "1000"))
    
    # Performance Configuration
    ENABLE_PERFORMANCE_MONITORING: bool = os.getenv("ENABLE_PERFORMANCE_MONITORING", "true").lower() == "true"
    PREDICTION_TIMEOUT: int = int(os.getenv("PREDICTION_TIMEOUT", "30"))  # seconds
    
    # API Configuration
    RATE_LIMIT_REQUESTS: int = int(os.getenv("RATE_LIMIT_REQUESTS", "1000"))
    RATE_LIMIT_WINDOW: int = int(os.getenv("RATE_LIMIT_WINDOW", "3600"))
    ENABLE_CACHING: bool = os.getenv("ENABLE_CACHING", "true").lower() == "true"
    CACHE_TTL: int = int(os.getenv("CACHE_TTL", "300"))
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

# Global settings instance
dynamic_settings = DynamicSettings()
