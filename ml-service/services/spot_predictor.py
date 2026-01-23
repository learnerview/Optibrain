import numpy as np
import pandas as pd
from datetime import datetime
import asyncio
from typing import Dict, Any, List
import random

class SpotPredictor:
    def __init__(self, logger):
        self.logger = logger
        self.model_loaded = False
        
    async def initialize(self):
        """Initialize the spot interruption predictor"""
        self.logger.info("Initializing SpotPredictor...")
        # In a real implementation, we would load a pre-trained XGBoost or LightGBM model
        await asyncio.sleep(0.5)
        self.model_loaded = True
        self.logger.info("SpotPredictor initialized")

    async def predict_interruption(self, instance_type: str, region: str) -> Dict[str, Any]:
        """
        Predict probability of spot interruption based on market signals
        In production, this would use real-time spot market data and historical patterns
        """
        if not self.model_loaded:
            raise Exception("SpotPredictor model not loaded")
            
        # Simulated prediction logic based on instance family and region demand features
        base_risk = self._get_base_risk(instance_type)
        region_factor = self._get_region_volatility(region)
        
        # Add some random market noise
        market_noise = random.uniform(-0.05, 0.05)
        
        risk_score = min(max(base_risk * region_factor + market_noise, 0.0), 1.0)
        
        # Determine recommendation based on risk
        if risk_score < 0.3:
            recommendation = "SUSTAIN"
            est_minutes = None
        elif risk_score < 0.7:
            recommendation = "COOLDOWN"
            est_minutes = random.randint(30, 120)
        else:
            recommendation = "EVACUATE"
            est_minutes = random.randint(2, 15)
            
        return {
            "instance_type": instance_type,
            "region": region,
            "risk_score": round(risk_score, 3),
            "estimated_minutes_until_interruption": est_minutes,
            "prediction_timestamp": datetime.utcnow().isoformat(),
            "recommendation": recommendation
        }

    def _get_base_risk(self, instance_type: str) -> float:
        # High demand instances (like g4dn, p3) have higher base risk
        if any(prefix in instance_type for prefix in ["g", "p", "f", "x"]):
            return 0.6
        # Balanced instances
        if any(prefix in instance_type for prefix in ["c", "r", "m"]):
            return 0.3
        # Micro/Nano usually very stable
        return 0.1

    def _get_region_volatility(self, region: str) -> float:
        # Major regions have higher volatility
        volatility_map = {
            "us-east-1": 1.2,
            "us-west-2": 1.1,
            "eu-west-1": 1.0,
            "ap-southeast-1": 0.9,
            "us-east-2": 0.8
        }
        return volatility_map.get(region, 1.0)

    async def cleanup(self):
        """Cleanup resources"""
        self.logger.info("SpotPredictor cleanup complete")
