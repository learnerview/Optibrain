from pydantic_settings import BaseSettings
from typing import Optional
import os

class Settings(BaseSettings):
    # Service Configuration
    ML_SERVICE_HOST: str = "localhost"
    ML_SERVICE_PORT: int = 8000
    DEBUG: bool = True
    
    # Database Configuration
    DATABASE_URL: str = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/optibrain_ml")
    
    # Redis Configuration
    REDIS_URL: str = os.getenv("REDIS_URL", "redis://localhost:6379/0")
    
    # ML Model Configuration
    MODEL_CACHE_TTL: int = 3600  # 1 hour
    FEATURE_STORE_TTL: int = 1800  # 30 minutes
    
    # Training Configuration
    RETRAINING_INTERVAL: int = 86400  # 24 hours
    MIN_TRAINING_SAMPLES: int = 100
    
    # Anomaly Detection Configuration
    ANOMALY_SENSITIVITY: float = 0.1
    ANOMALY_WINDOW_SIZE: int = 24  # hours
    
    # Forecasting Configuration
    MAX_FORECAST_HORIZON: int = 168  # 7 days
    DEFAULT_FORECAST_HORIZON: int = 24  # 1 day
    
    # Java Backend Configuration
    JAVA_BACKEND_URL: str = "http://localhost:8080"
    
    # Logging Configuration
    LOG_LEVEL: str = "INFO"
    LOG_FILE: str = "logs/ml_service.log"
    
    # Gemini API Configuration
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    GEMINI_MODEL: str = os.getenv("GEMINI_MODEL", "gemini-pro")
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

# Global settings instance
settings = Settings()
