import asyncio
import aiohttp
import pandas as pd
import numpy as np
from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta
import logging
import json

from .forecasting_service import ForecastingService
from .anomaly_detection import AnomalyDetectionService
from .feature_engineering import FeatureEngineering
from .model_manager import ModelManager
from .spot_predictor import SpotPredictor
from .deep_learning_forecaster import DeepLearningForecaster
from config.settings import Settings

logger = logging.getLogger(__name__)

class MLService:
    """Main ML Service coordinating all ML capabilities"""
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.forecasting_service = ForecastingService()
        self.anomaly_detection = AnomalyDetectionService()
        self.feature_engineering = FeatureEngineering()
        self.model_manager = ModelManager(settings)
        self.spot_predictor = SpotPredictor(logger)
        self.dl_forecaster = DeepLearningForecaster()
        
        # Cache for tenant data
        self.tenant_cache = {}
        self.model_cache = {}
        
        # HTTP session for Java backend communication
        self.session = None
    
    async def initialize(self):
        """Initialize the ML service"""
        logger.info("Initializing ML Service...")
        
        # Create HTTP session
        self.session = aiohttp.ClientSession()
        
        # Load existing models
        await self.model_manager.load_all_models()
        
        # Initialize spot predictor
        await self.spot_predictor.initialize()
        
        # Initialize DL forecaster
        await self.dl_forecaster.initialize()
        
        # Initialize models if not exists
        await self._initialize_default_models()
        
        logger.info("ML Service initialized successfully")
    
    async def cleanup(self):
        """Cleanup resources"""
        if self.session:
            await self.session.close()
        await self.spot_predictor.cleanup()
        await self.dl_forecaster.cleanup()
        logger.info("ML Service cleanup completed")
    
    async def generate_forecast(self, 
                             tenant_id: str, 
                             metric_type: str, 
                             forecast_horizon: int,
                             historical_data: Optional[List[float]] = None) -> Dict[str, Any]:
        """Generate forecast for a tenant metric"""
        try:
            logger.info(f"Generating {metric_type} forecast for tenant {tenant_id}")
            
            # Get historical data if not provided
            if historical_data is None:
                historical_data = await self._get_historical_data(tenant_id, metric_type)
            
            # Get tenant metadata
            tenant_metadata = await self._get_tenant_metadata(tenant_id)
            
            # Generate forecast
            forecast_result = self.forecasting_service.forecast(
                metric_type=metric_type,
                historical_data=historical_data,
                forecast_horizon=forecast_horizon,
                tenant_metadata=tenant_metadata,
                dl_forecaster=self.dl_forecaster
            )
            
            # Log prediction for monitoring
            await self._log_prediction(tenant_id, metric_type, forecast_result)
            
            return forecast_result
            
        except Exception as e:
            logger.error(f"Error generating forecast for tenant {tenant_id}: {str(e)}")
            raise

    async def generate_spot_prediction(self, instance_type: str, region: str) -> Dict[str, Any]:
        """Generate spot interruption prediction"""
        try:
            logger.info(f"Generating spot prediction for {instance_type} in {region}")
            return await self.spot_predictor.predict_interruption(instance_type, region)
        except Exception as e:
            logger.error(f"Error generating spot prediction: {str(e)}")
            raise
    
    async def detect_anomalies(self, 
                             tenant_id: str, 
                             metrics_data: List[Dict[str, Any]],
                             sensitivity: float = 0.1) -> Dict[str, Any]:
        """Detect anomalies in tenant metrics"""
        try:
            logger.info(f"Detecting anomalies for tenant {tenant_id}")
            
            # Detect anomalies
            anomaly_result = self.anomaly_detection.detect_anomalies(
                metrics_data=metrics_data,
                sensitivity=sensitivity
            )
            
            # Add tenant context
            anomaly_result['tenant_id'] = tenant_id
            anomaly_result['detection_time'] = datetime.now().isoformat()
            
            # Log anomalies for monitoring
            if anomaly_result['anomalies']:
                await self._log_anomalies(tenant_id, anomaly_result)
            
            return anomaly_result
            
        except Exception as e:
            logger.error(f"Error detecting anomalies for tenant {tenant_id}: {str(e)}")
            raise
    
    async def optimize_costs(self, 
                          tenant_id: str, 
                          current_metrics: Optional[Dict[str, Any]] = None,
                          budget_constraints: Optional[Dict[str, float]] = None,
                          aws_data: Optional[Dict[str, Any]] = None,
                          generate_defaults: bool = False) -> Dict[str, Any]:
        """Generate cost optimization recommendations using ML"""
        try:
            logger.info(f"Generating cost optimization for tenant {tenant_id}")
            
            # If generating defaults for dynamic config
            if generate_defaults and aws_data:
                return await self._generate_dynamic_configuration(tenant_id, aws_data)

            # Standard cost optimization flow
            current_metrics_dict = current_metrics.dict() if hasattr(current_metrics, 'dict') else current_metrics
            
            # Get historical data for context
            historical_data = await self._get_historical_metrics(tenant_id)
            
            # Generate optimization recommendations
            if current_metrics_dict:
                recommendations = await self._generate_optimization_recommendations(
                    tenant_id, current_metrics_dict, historical_data, budget_constraints
                )
                return recommendations
            else:
                return {
                    'recommended_actions': [],
                    'expected_savings': 0.0,
                    'confidence': 0.0,
                    'risk_assessment': 'LOW',
                    'implementation_priority': 'LOW'
                }
            
        except Exception as e:
            logger.error(f"Error generating cost optimization for tenant {tenant_id}: {str(e)}")
            raise
    
    async def train_all_models(self):
        """Train all ML models with latest data"""
        try:
            logger.info("Starting model training for all tenants")
            
            # Get all tenants
            tenants = await self._get_all_tenants()
            
            training_results = {}
            
            for tenant in tenants:
                tenant_id = tenant.get('id')
                try:
                    # Get training data
                    training_data = await self._get_training_data(tenant_id)
                    
                    if training_data:
                        # Train forecasting models
                        forecast_scores = self.forecasting_service.train_models(training_data)
                        
                        # Train anomaly detection
                        anomaly_scores = self.anomaly_detection.train_models(
                            training_data.get('metrics', [])
                        )
                        
                        training_results[tenant_id] = {
                            'forecast_scores': forecast_scores,
                            'anomaly_scores': anomaly_scores,
                            'training_samples': len(training_data.get('metrics', [])),
                            'timestamp': datetime.now().isoformat()
                        }
                        
                        logger.info(f"Training completed for tenant {tenant_id}")
                    else:
                        logger.warning(f"No training data available for tenant {tenant_id}")
                        
                except Exception as e:
                    logger.error(f"Error training models for tenant {tenant_id}: {str(e)}")
                    training_results[tenant_id] = {'error': str(e)}
            
            # Save training results
            await self.model_manager.save_training_results(training_results)
            
            logger.info("Model training completed for all tenants")
            return training_results
            
        except Exception as e:
            logger.error(f"Error in model training: {str(e)}")
            raise
    
    async def get_model_status(self) -> Dict[str, Any]:
        """Get status of all ML models"""
        try:
            model_status = await self.model_manager.get_model_status()
            
            # Add service health
            model_status['service_health'] = {
                'status': 'healthy',
                'timestamp': datetime.now().isoformat(),
                'models_loaded': len(self.forecasting_service.models),
                'anomaly_models_loaded': len(self.anomaly_detection.models)
            }
            
            return model_status
            
        except Exception as e:
            logger.error(f"Error getting model status: {str(e)}")
            raise
    
    async def get_tenant_features(self, tenant_id: str) -> Dict[str, Any]:
        """Get engineered features for a tenant"""
        try:
            # Get historical data
            historical_data = await self._get_historical_metrics(tenant_id)
            
            if not historical_data:
                return {'error': 'No historical data available'}
            
            # Extract features
            features = self.feature_engineering.extract_features(historical_data)
            feature_names = self.feature_engineering.get_feature_names()
            
            # Create feature dictionary
            feature_dict = dict(zip(feature_names, features))
            
            return {
                'tenant_id': tenant_id,
                'features': feature_dict,
                'feature_count': len(features),
                'timestamp': datetime.now().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error getting features for tenant {tenant_id}: {str(e)}")
            raise
    
    async def record_feedback(self, tenant_id: str, prediction_id: str, actual_value: float, predicted_value: float):
        """Record feedback for model improvement"""
        try:
            feedback = {
                'tenant_id': tenant_id,
                'prediction_id': prediction_id,
                'actual_value': actual_value,
                'predicted_value': predicted_value,
                'error': abs(actual_value - predicted_value),
                'timestamp': datetime.now().isoformat()
            }
            
            await self.model_manager.save_feedback(feedback)
            logger.info(f"Feedback recorded for tenant {tenant_id}, prediction {prediction_id}")
            
        except Exception as e:
            logger.error(f"Error recording feedback: {str(e)}")
            raise
    
    # Private helper methods
    
    async def _initialize_default_models(self):
        """Initialize default models if none exist"""
        try:
            # Check if models exist
            models_exist = await self.model_manager.check_models_exist()
            
            if not models_exist:
                logger.info("No existing models found, training with synthetic data")
                
                # Generate synthetic training data
                synthetic_data = self._generate_synthetic_training_data()
                
                # Train models with synthetic data
                self.forecasting_service.train_models(synthetic_data)
                self.anomaly_detection.train_models(synthetic_data.get('metrics', []))
                
                logger.info("Default models trained with synthetic data")
            else:
                logger.info("Existing models found and loaded")
                
        except Exception as e:
            logger.error(f"Error initializing default models: {str(e)}")
    
    async def _get_historical_data(self, tenant_id: str, metric_type: str) -> List[float]:
        """Get historical data for a specific metric"""
        try:
            # Call Java backend API
            url = f"{self.settings.JAVA_BACKEND_URL}/metrics/historical/{tenant_id}/{metric_type}"
            
            async with self.session.get(url) as response:
                if response.status == 200:
                    data = await response.json()
                    return data.get('values', [])
                else:
                    logger.warning(f"Failed to get historical data: {response.status}")
                    return self._generate_synthetic_data(metric_type, 168)  # 7 days of data
                    
        except Exception as e:
            logger.error(f"Error getting historical data: {str(e)}")
            return self._generate_synthetic_data(metric_type, 168)
    
    async def _get_historical_metrics(self, tenant_id: str) -> List[Dict[str, Any]]:
        """Get comprehensive historical metrics for a tenant"""
        try:
            url = f"{self.settings.JAVA_BACKEND_URL}/metrics/tenant/{tenant_id}/history"
            
            async with self.session.get(url) as response:
                if response.status == 200:
                    data = await response.json()
                    return data.get('metrics', [])
                else:
                    return self._generate_synthetic_metrics_data(168)
                    
        except Exception as e:
            logger.error(f"Error getting historical metrics: {str(e)}")
            return self._generate_synthetic_metrics_data(168)
    
    async def _get_tenant_metadata(self, tenant_id: str) -> Dict[str, Any]:
        """Get tenant metadata"""
        try:
            url = f"{self.settings.JAVA_BACKEND_URL}/api/tenants/{tenant_id}"
            
            async with self.session.get(url) as response:
                if response.status == 200:
                    tenant = await response.json()
                    return {
                        'plan': tenant.get('plan', 'FREE'),
                        'settings': tenant.get('settings', {})
                    }
                else:
                    return {'plan': 'FREE', 'settings': {}}
                    
        except Exception as e:
            logger.error(f"Error getting tenant metadata: {str(e)}")
            return {'plan': 'FREE', 'settings': {}}
    
    async def _get_all_tenants(self) -> List[Dict[str, Any]]:
        """Get all tenants"""
        try:
            url = f"{self.settings.JAVA_BACKEND_URL}/api/tenants"
            
            async with self.session.get(url) as response:
                if response.status == 200:
                    data = await response.json()
                    return data.get('data', [])
                else:
                    return []
                    
        except Exception as e:
            logger.error(f"Error getting tenants: {str(e)}")
            return []
    
    async def _get_training_data(self, tenant_id: str) -> Optional[Dict[str, pd.DataFrame]]:
        """Get training data for a tenant"""
        try:
            historical_metrics = await self._get_historical_metrics(tenant_id)
            
            if not historical_metrics:
                return None
            
            df = pd.DataFrame(historical_metrics)
            
            # Prepare data for different models
            training_data = {
                'metrics': historical_metrics,
                'cpu': df[['timestamp', 'cpu_utilization']].rename(columns={'cpu_utilization': 'cpu'}),
                'memory': df[['timestamp', 'memory_utilization']].rename(columns={'memory_utilization': 'memory'}),
                'cost': df[['timestamp', 'hourly_cost']].rename(columns={'hourly_cost': 'cost'})
            }
            
            return training_data
            
        except Exception as e:
            logger.error(f"Error getting training data: {str(e)}")
            return None
    
    async def _generate_optimization_recommendations(self, 
                                                 tenant_id: str,
                                                 current_metrics: Dict[str, Any],
                                                 historical_data: List[Dict[str, Any]],
                                                 budget_constraints: Optional[Dict[str, float]]) -> Dict[str, Any]:
        """Generate ML-based cost optimization recommendations"""
        
        # Extract features from current metrics
        current_features = self.feature_engineering.extract_features([current_metrics])
        
        # Generate recommendations based on ML analysis
        recommendations = []
        
        # Rightsizing recommendations
        if current_metrics.get('cpu_utilization', 0) < 30:
            recommendations.append({
                'type': 'RIGHTSIZE_DOWN',
                'description': 'CPU utilization is low, consider downsizing instances',
                'expected_savings': current_metrics.get('hourly_cost', 0) * 0.3,
                'confidence': 0.8,
                'priority': 'HIGH'
            })
        elif current_metrics.get('cpu_utilization', 0) > 80:
            recommendations.append({
                'type': 'RIGHTSIZE_UP',
                'description': 'CPU utilization is high, consider upsizing instances',
                'expected_savings': -current_metrics.get('hourly_cost', 0) * 0.2,  # Negative means cost increase
                'confidence': 0.9,
                'priority': 'CRITICAL'
            })
        
        # Schedule optimization
        if current_metrics.get('hourly_cost', 0) > 100:
            recommendations.append({
                'type': 'SCHEDULE_OPTIMIZATION',
                'description': 'High costs detected, consider implementing start/stop schedules',
                'expected_savings': current_metrics.get('hourly_cost', 0) * 0.4,
                'confidence': 0.7,
                'priority': 'MEDIUM'
            })
        
        # Reserved instances
        if len(historical_data) > 720:  # More than 2.5 days of data
            avg_cost = np.mean([m.get('hourly_cost', 0) for m in historical_data])
            if avg_cost > 50:
                recommendations.append({
                    'type': 'RESERVED_INSTANCES',
                    'description': 'Stable usage pattern detected, consider reserved instances',
                    'expected_savings': avg_cost * 0.3 * 730,  # Monthly savings
                    'confidence': 0.8,
                    'priority': 'MEDIUM'
                })
        
        # Calculate totals
        total_savings = sum(r.get('expected_savings', 0) for r in recommendations if r.get('expected_savings', 0) > 0)
        avg_confidence = np.mean([r.get('confidence', 0) for r in recommendations])
        
        # Risk assessment
        risk_score = self._calculate_risk_score(recommendations)
        risk_level = 'LOW' if risk_score < 0.3 else 'MEDIUM' if risk_score < 0.7 else 'HIGH'
        
        return {
            'recommended_actions': recommendations,
            'expected_savings': total_savings,
            'confidence': avg_confidence,
            'risk_assessment': risk_level,
            'implementation_priority': 'HIGH' if total_savings > 100 else 'MEDIUM' if total_savings > 50 else 'LOW',
            'tenant_id': tenant_id,
            'generated_at': datetime.now().isoformat()
        }
    
    def _calculate_risk_score(self, recommendations: List[Dict[str, Any]]) -> float:
        """Calculate risk score for recommendations"""
        if not recommendations:
            return 0.0
        
        # Higher risk for actions that increase cost or have low confidence
        risk_scores = []
        for rec in recommendations:
            base_risk = 0.5
            if rec.get('expected_savings', 0) < 0:  # Cost increase
                base_risk += 0.3
            if rec.get('confidence', 0) < 0.7:
                base_risk += 0.2
            if rec.get('priority') == 'CRITICAL':
                base_risk += 0.1
            
            risk_scores.append(min(1.0, base_risk))
        
        return np.mean(risk_scores)
    
    def _generate_synthetic_training_data(self) -> Dict[str, pd.DataFrame]:
        """Generate synthetic training data for model initialization"""
        np.random.seed(42)
        
        # Generate 30 days of synthetic data
        timestamps = pd.date_range(end=datetime.now(), periods=720, freq='1H')  # 30 days
        
        # Generate realistic patterns
        cpu_usage = 50 + 20 * np.sin(np.arange(720) * 2 * np.pi / 24) + np.random.normal(0, 5, 720)
        memory_usage = 60 + 15 * np.sin(np.arange(720) * 2 * np.pi / 24 + np.pi/4) + np.random.normal(0, 3, 720)
        hourly_cost = 25 + 10 * np.sin(np.arange(720) * 2 * np.pi / 24) + np.random.normal(0, 2, 720)
        
        # Ensure positive values
        cpu_usage = np.clip(cpu_usage, 0, 100)
        memory_usage = np.clip(memory_usage, 0, 100)
        hourly_cost = np.clip(hourly_cost, 0, 200)
        
        df = pd.DataFrame({
            'timestamp': timestamps,
            'cpu_utilization': cpu_usage,
            'memory_utilization': memory_usage,
            'hourly_cost': hourly_cost,
            'instance_count': np.random.randint(2, 8, 720)
        })
        
        return {
            'metrics': df.to_dict('records'),
            'cpu': df[['timestamp', 'cpu_utilization']].rename(columns={'cpu_utilization': 'cpu'}),
            'memory': df[['timestamp', 'memory_utilization']].rename(columns={'memory_utilization': 'memory'}),
            'cost': df[['timestamp', 'hourly_cost']].rename(columns={'hourly_cost': 'cost'})
        }
    
    def _generate_synthetic_data(self, metric_type: str, count: int) -> List[float]:
        """Generate synthetic data for a metric"""
        np.random.seed(42)
        
        if metric_type == 'cpu':
            return np.clip(50 + 20 * np.sin(np.arange(count) * 2 * np.pi / 24) + np.random.normal(0, 5, count), 0, 100).tolist()
        elif metric_type == 'memory':
            return np.clip(60 + 15 * np.sin(np.arange(count) * 2 * np.pi / 24 + np.pi/4) + np.random.normal(0, 3, count), 0, 100).tolist()
        elif metric_type == 'cost':
            return np.clip(25 + 10 * np.sin(np.arange(count) * 2 * np.pi / 24) + np.random.normal(0, 2, count), 0, 200).tolist()
        else:
            return [50.0] * count
    
    def _generate_synthetic_metrics_data(self, count: int) -> List[Dict[str, Any]]:
        """Generate synthetic metrics data"""
        np.random.seed(42)
        
        timestamps = pd.date_range(end=datetime.now(), periods=count, freq='1H')
        cpu_usage = self._generate_synthetic_data('cpu', count)
        memory_usage = self._generate_synthetic_data('memory', count)
        hourly_cost = self._generate_synthetic_data('cost', count)
        instance_count = np.random.randint(2, 8, count)
        
        return [
            {
                'timestamp': ts.isoformat(),
                'cpu_utilization': float(cpu),
                'memory_utilization': float(mem),
                'hourly_cost': float(cost),
                'instance_count': int(instances)
            }
            for ts, cpu, mem, cost, instances in zip(timestamps, cpu_usage, memory_usage, hourly_cost, instance_count)
        ]
    
    async def _log_prediction(self, tenant_id: str, metric_type: str, forecast_result: Dict[str, Any]):
        """Log prediction for monitoring"""
        try:
            log_entry = {
                'tenant_id': tenant_id,
                'metric_type': metric_type,
                'prediction_id': f"{tenant_id}_{metric_type}_{datetime.now().timestamp()}",
                'forecast_result': forecast_result,
                'timestamp': datetime.now().isoformat()
            }
            
            await self.model_manager.save_prediction_log(log_entry)
            
        except Exception as e:
            logger.error(f"Error logging prediction: {str(e)}")
    
    async def _generate_dynamic_configuration(self, tenant_id: str, aws_data: Dict[str, Any]) -> Dict[str, Any]:
        """Generate complete dynamic configuration based on AWS environment analysis"""
        
        # Analyze environment complexity and risk profile
        resource_count = len(aws_data.get('resources', [])) if isinstance(aws_data.get('resources'), list) else 0
        monthly_cost = aws_data.get('costData', {}).get('monthlyCost', 0)
        has_production_tags = any('prod' in str(tag).lower() for tag in aws_data.get('tags', {}).get('commonTags', []))
        
        # 1. Generate Optimization Bounds
        optimization_bounds = {
            "min_hourly_cost": 0.01,
            "max_hourly_cost": max(monthly_cost / 730 * 2, 10.0), # 2x current avg hourly cost
            "recommended_budget": monthly_cost * 1.2, # +20% buffer
            "min_instances": 1,
            "max_instances": max(resource_count * 2, 10),
            "scale_up_threshold": 75.0 if has_production_tags else 85.0, # More aggressive on non-prod
            "scale_down_threshold": 25.0 if has_production_tags else 15.0,
            "bounds_confidence": 0.85
        }
        
        # 2. Generate Decision Engine Settings
        is_high_value = monthly_cost > 1000
        decision_engine = {
            "enable_ml_recommendations": True,
            "require_human_approval": is_high_value, # Require approval for high-value environments
            "ml_weight": 0.7 if not is_high_value else 0.5, # Trust ML more for smaller envs
            "rule_weight": 0.3 if not is_high_value else 0.5,
            "approval_threshold": 0.8 if is_high_value else 0.6,
            "risk_threshold": 0.7,
            "auto_execute_low_risk": True,
            "decision_confidence": 0.82
        }
        
        # 3. Autonomous Settings
        autonomous_settings = {
            "enable_autonomous_optimization": True,
            "enable_autonomous_scaling": True,
            "optimization_interval": 300 if is_high_value else 900, # More frequent for high value
            "autonomous_confidence": 0.88
        }
        
        # 4. User Permissions
        user_permissions = {
            "allow_autonomous_mode": True,
            "max_autonomous_savings": monthly_cost * 0.5, # Cap at 50% of monthly cost
            "require_budget_approval": is_high_value,
            "permissions_confidence": 0.90
        }
        
        # 5. Alert Thresholds
        alert_thresholds = {
            "cost_spike_threshold": 1.5 if is_high_value else 2.0, # Stricter for high value
            "utilization_alert_threshold": 90.0 if has_production_tags else 95.0,
            "anomaly_alert_threshold": 0.8,
            "alerts_confidence": 0.85
        }
        
        return {
            'recommended_actions': [], # Required by response model
            'expected_savings': 0.0,
            'confidence': 0.85,
            'risk_assessment': 'LOW',
            'implementation_priority': 'MEDIUM',
            'optimization_bounds': optimization_bounds,
            'decision_engine': decision_engine,
            'autonomous_settings': autonomous_settings,
            'user_permissions': user_permissions,
            'alert_thresholds': alert_thresholds
        }
        
    async def _log_anomalies(self, tenant_id: str, anomaly_result: Dict[str, Any]):
        """Log anomalies for monitoring"""
        try:
            log_entry = {
                'tenant_id': tenant_id,
                'anomaly_result': anomaly_result,
                'timestamp': datetime.now().isoformat()
            }
            
            await self.model_manager.save_anomaly_log(log_entry)
            
        except Exception as e:
            logger.error(f"Error logging anomalies: {str(e)}")
