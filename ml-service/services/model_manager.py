import asyncio
import json
import os
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
import logging
import aiofiles

logger = logging.getLogger(__name__)

class ModelManager:
    """Manages ML model lifecycle, versioning, and monitoring"""
    
    def __init__(self, settings):
        self.settings = settings
        self.model_dir = "models"
        self.log_dir = "logs"
        
        # Create directories
        os.makedirs(self.model_dir, exist_ok=True)
        os.makedirs(self.log_dir, exist_ok=True)
        
        # Model registry
        self.model_registry = {}
        self.prediction_logs = []
        self.anomaly_logs = []
        self.feedback_data = []
        
    async def load_all_models(self):
        """Load all available models"""
        try:
            # Load model registry
            registry_path = os.path.join(self.model_dir, "model_registry.json")
            if os.path.exists(registry_path):
                async with aiofiles.open(registry_path, 'r') as f:
                    content = await f.read()
                    self.model_registry = json.loads(content)
            
            # Load logs
            await self._load_logs()
            
            logger.info("Model manager loaded successfully")
            
        except Exception as e:
            logger.error(f"Error loading models: {str(e)}")
    
    async def save_model_metadata(self, model_name: str, metadata: Dict[str, Any]):
        """Save model metadata"""
        try:
            self.model_registry[model_name] = {
                **metadata,
                'last_updated': datetime.now().isoformat(),
                'version': metadata.get('version', '1.0.0')
            }
            
            # Save to file
            registry_path = os.path.join(self.model_dir, "model_registry.json")
            async with aiofiles.open(registry_path, 'w') as f:
                await f.write(json.dumps(self.model_registry, indent=2))
            
            logger.info(f"Model metadata saved for {model_name}")
            
        except Exception as e:
            logger.error(f"Error saving model metadata: {str(e)}")
    
    async def check_models_exist(self) -> bool:
        """Check if trained models exist"""
        try:
            required_files = [
                "cpu_rf.joblib", "cpu_gb.joblib", "cpu_lr.joblib",
                "memory_rf.joblib", "memory_gb.joblib", "memory_lr.joblib", 
                "cost_rf.joblib", "cost_gb.joblib", "cost_lr.joblib",
                "anomaly_isolation_forest.joblib", "anomaly_one_class_svm.joblib"
            ]
            
            for file_name in required_files:
                file_path = os.path.join(self.model_dir, file_name)
                if not os.path.exists(file_path):
                    logger.info(f"Model file missing: {file_name}")
                    return False
            
            return True
            
        except Exception as e:
            logger.error(f"Error checking model existence: {str(e)}")
            return False
    
    async def get_model_status(self) -> Dict[str, Any]:
        """Get status of all models"""
        try:
            status = {
                'models': {},
                'last_training': None,
                'model_count': 0,
                'prediction_count': len(self.prediction_logs),
                'anomaly_count': len(self.anomaly_logs),
                'feedback_count': len(self.feedback_data)
            }
            
            # Check each model type
            model_types = ['cpu', 'memory', 'cost', 'anomaly']
            
            for model_type in model_types:
                status['models'][model_type] = {
                    'exists': await self._check_model_exists(model_type),
                    'last_trained': self.model_registry.get(f"{model_type}_model", {}).get('last_updated'),
                    'version': self.model_registry.get(f"{model_type}_model", {}).get('version', '1.0.0'),
                    'performance': self.model_registry.get(f"{model_type}_model", {}).get('performance', {})
                }
                if status['models'][model_type]['exists']:
                    status['model_count'] += 1
            
            # Get last training time
            if self.model_registry:
                last_training = max(
                    (meta.get('last_updated', '') for meta in self.model_registry.values()),
                    default=''
                )
                status['last_training'] = last_training
            
            return status
            
        except Exception as e:
            logger.error(f"Error getting model status: {str(e)}")
            return {'error': str(e)}
    
    async def save_training_results(self, training_results: Dict[str, Any]):
        """Save training results"""
        try:
            training_log = {
                'timestamp': datetime.now().isoformat(),
                'results': training_results
            }
            
            # Save to file
            log_path = os.path.join(self.log_dir, "training_logs.json")
            
            # Load existing logs
            existing_logs = []
            if os.path.exists(log_path):
                async with aiofiles.open(log_path, 'r') as f:
                    content = await f.read()
                    if content:
                        existing_logs = json.loads(content)
            
            # Add new log
            existing_logs.append(training_log)
            
            # Keep only last 100 training logs
            if len(existing_logs) > 100:
                existing_logs = existing_logs[-100:]
            
            # Save back
            async with aiofiles.open(log_path, 'w') as f:
                await f.write(json.dumps(existing_logs, indent=2))
            
            logger.info(f"Training results saved for {len(training_results)} tenants")
            
        except Exception as e:
            logger.error(f"Error saving training results: {str(e)}")
    
    async def save_prediction_log(self, log_entry: Dict[str, Any]):
        """Save prediction log"""
        try:
            self.prediction_logs.append(log_entry)
            
            # Keep only last 1000 predictions in memory
            if len(self.prediction_logs) > 1000:
                self.prediction_logs = self.prediction_logs[-1000:]
            
            # Save to file periodically
            if len(self.prediction_logs) % 10 == 0:
                await self._save_prediction_logs()
            
        except Exception as e:
            logger.error(f"Error saving prediction log: {str(e)}")
    
    async def save_anomaly_log(self, log_entry: Dict[str, Any]):
        """Save anomaly log"""
        try:
            self.anomaly_logs.append(log_entry)
            
            # Keep only last 1000 anomalies in memory
            if len(self.anomaly_logs) > 1000:
                self.anomaly_logs = self.anomaly_logs[-1000:]
            
            # Save to file periodically
            if len(self.anomaly_logs) % 10 == 0:
                await self._save_anomaly_logs()
            
        except Exception as e:
            logger.error(f"Error saving anomaly log: {str(e)}")
    
    async def save_feedback(self, feedback: Dict[str, Any]):
        """Save model feedback"""
        try:
            self.feedback_data.append(feedback)
            
            # Keep only last 1000 feedback entries in memory
            if len(self.feedback_data) > 1000:
                self.feedback_data = self.feedback_data[-1000:]
            
            # Save to file periodically
            if len(self.feedback_data) % 10 == 0:
                await self._save_feedback_data()
            
        except Exception as e:
            logger.error(f"Error saving feedback: {str(e)}")
    
    async def get_model_performance(self, model_name: str, days: int = 7) -> Dict[str, Any]:
        """Get model performance metrics"""
        try:
            cutoff_time = datetime.now() - timedelta(days=days)
            
            # Filter feedback for the model and time period
            relevant_feedback = [
                fb for fb in self.feedback_data
                if fb.get('timestamp') and datetime.fromisoformat(fb['timestamp']) > cutoff_time
            ]
            
            if not relevant_feedback:
                return {'error': 'No feedback data available'}
            
            # Calculate metrics
            errors = [fb['error'] for fb in relevant_feedback]
            mae = sum(errors) / len(errors)
            mse = sum(e**2 for e in errors) / len(errors)
            rmse = mse ** 0.5
            
            # Calculate accuracy (within 10% threshold)
            accurate_predictions = sum(
                1 for fb in relevant_feedback
                if abs(fb['error']) / abs(fb['actual_value']) < 0.1
            )
            accuracy = accurate_predictions / len(relevant_feedback)
            
            return {
                'model_name': model_name,
                'period_days': days,
                'sample_count': len(relevant_feedback),
                'mae': mae,
                'mse': mse,
                'rmse': rmse,
                'accuracy': accuracy,
                'last_updated': datetime.now().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error getting model performance: {str(e)}")
            return {'error': str(e)}
    
    async def schedule_retraining(self, model_name: str, interval_hours: int = 24):
        """Schedule periodic model retraining"""
        try:
            # Check last training time
            last_trained = self.model_registry.get(model_name, {}).get('last_updated')
            
            if last_trained:
                last_training_time = datetime.fromisoformat(last_trained)
                time_since_training = datetime.now() - last_training_time
                
                if time_since_training.total_seconds() < interval_hours * 3600:
                    logger.info(f"Model {model_name} was trained recently, skipping retraining")
                    return False
            
            logger.info(f"Scheduling retraining for model {model_name}")
            # In a real implementation, this would trigger the training pipeline
            return True
            
        except Exception as e:
            logger.error(f"Error scheduling retraining: {str(e)}")
            return False
    
    # Private helper methods
    
    async def _check_model_exists(self, model_type: str) -> bool:
        """Check if a model type exists"""
        try:
            if model_type == 'anomaly':
                required_files = ['anomaly_isolation_forest.joblib', 'anomaly_one_class_svm.joblib']
            else:
                required_files = [f"{model_type}_rf.joblib", f"{model_type}_gb.joblib", f"{model_type}_lr.joblib"]
            
            for file_name in required_files:
                file_path = os.path.join(self.model_dir, file_name)
                if not os.path.exists(file_path):
                    return False
            
            return True
            
        except Exception as e:
            logger.error(f"Error checking model existence: {str(e)}")
            return False
    
    async def _load_logs(self):
        """Load existing logs from files"""
        try:
            # Load prediction logs
            prediction_log_path = os.path.join(self.log_dir, "prediction_logs.json")
            if os.path.exists(prediction_log_path):
                async with aiofiles.open(prediction_log_path, 'r') as f:
                    content = await f.read()
                    if content:
                        self.prediction_logs = json.loads(content)
            
            # Load anomaly logs
            anomaly_log_path = os.path.join(self.log_dir, "anomaly_logs.json")
            if os.path.exists(anomaly_log_path):
                async with aiofiles.open(anomaly_log_path, 'r') as f:
                    content = await f.read()
                    if content:
                        self.anomaly_logs = json.loads(content)
            
            # Load feedback data
            feedback_path = os.path.join(self.log_dir, "feedback_data.json")
            if os.path.exists(feedback_path):
                async with aiofiles.open(feedback_path, 'r') as f:
                    content = await f.read()
                    if content:
                        self.feedback_data = json.loads(content)
            
            logger.info(f"Loaded {len(self.prediction_logs)} prediction logs, {len(self.anomaly_logs)} anomaly logs, {len(self.feedback_data)} feedback entries")
            
        except Exception as e:
            logger.error(f"Error loading logs: {str(e)}")
    
    async def _save_prediction_logs(self):
        """Save prediction logs to file"""
        try:
            log_path = os.path.join(self.log_dir, "prediction_logs.json")
            async with aiofiles.open(log_path, 'w') as f:
                await f.write(json.dumps(self.prediction_logs, indent=2))
        except Exception as e:
            logger.error(f"Error saving prediction logs: {str(e)}")
    
    async def _save_anomaly_logs(self):
        """Save anomaly logs to file"""
        try:
            log_path = os.path.join(self.log_dir, "anomaly_logs.json")
            async with aiofiles.open(log_path, 'w') as f:
                await f.write(json.dumps(self.anomaly_logs, indent=2))
        except Exception as e:
            logger.error(f"Error saving anomaly logs: {str(e)}")
    
    async def _save_feedback_data(self):
        """Save feedback data to file"""
        try:
            feedback_path = os.path.join(self.log_dir, "feedback_data.json")
            async with aiofiles.open(feedback_path, 'w') as f:
                await f.write(json.dumps(self.feedback_data, indent=2))
        except Exception as e:
            logger.error(f"Error saving feedback data: {str(e)}")
