"""
Prediction Accuracy Tracker for ML Model Improvement
Tracks predicted vs actual values to measure accuracy and trigger retraining.
"""

import logging
import json
import os
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
import numpy as np
from collections import defaultdict

logger = logging.getLogger(__name__)


class PredictionAccuracyTracker:
    """
    Tracks prediction accuracy for continuous model improvement.
    Stores predicted vs actual values, calculates accuracy metrics,
    and triggers retraining when accuracy drops below threshold.
    """
    
    def __init__(self, storage_dir: str = "data/predictions"):
        self.storage_dir = storage_dir
        self.predictions: Dict[str, List[Dict]] = defaultdict(list)
        self.accuracy_metrics: Dict[str, Dict] = {}
        self.retrain_threshold = 0.7  # Trigger retrain if accuracy drops below 70%
        
        # Create storage directory
        os.makedirs(storage_dir, exist_ok=True)
        
        # Load existing predictions
        self._load_predictions()
    
    def record_prediction(self, tenant_id: str, metric_type: str, 
                         prediction_id: str, predicted_value: float,
                         confidence: float, horizon_hours: int) -> None:
        """Record a new prediction for later validation"""
        prediction_record = {
            'prediction_id': prediction_id,
            'tenant_id': tenant_id,
            'metric_type': metric_type,
            'predicted_value': float(predicted_value),
            'confidence': float(confidence),
            'horizon_hours': horizon_hours,
            'predicted_at': datetime.now().isoformat(),
            'expected_validation_time': (datetime.now() + timedelta(hours=horizon_hours)).isoformat(),
            'actual_value': None,
            'validated': False
        }
        
        key = f"{tenant_id}_{metric_type}"
        self.predictions[key].append(prediction_record)
        
        # Keep only last 1000 predictions per tenant-metric pair
        if len(self.predictions[key]) > 1000:
            self.predictions[key] = self.predictions[key][-1000:]
        
        logger.info(f"Recorded prediction {prediction_id} for {tenant_id}/{metric_type}")
        self._save_predictions()
    
    def validate_prediction(self, prediction_id: str, actual_value: float) -> Dict[str, Any]:
        """Validate a prediction with the actual value"""
        for key, preds in self.predictions.items():
            for pred in preds:
                if pred['prediction_id'] == prediction_id and not pred['validated']:
                    pred['actual_value'] = float(actual_value)
                    pred['validated'] = True
                    pred['validated_at'] = datetime.now().isoformat()
                    
                    # Calculate error
                    error = abs(pred['predicted_value'] - actual_value)
                    pred['absolute_error'] = error
                    
                    if actual_value != 0:
                        pred['percentage_error'] = (error / abs(actual_value)) * 100
                    else:
                        pred['percentage_error'] = error * 100 if pred['predicted_value'] != 0 else 0
                    
                    self._save_predictions()
                    self._update_accuracy_metrics(key)
                    
                    logger.info(f"Validated prediction {prediction_id}: predicted={pred['predicted_value']:.2f}, actual={actual_value:.2f}, error={error:.2f}")
                    
                    return {
                        'prediction_id': prediction_id,
                        'predicted_value': pred['predicted_value'],
                        'actual_value': actual_value,
                        'absolute_error': error,
                        'percentage_error': pred['percentage_error'],
                        'accuracy_score': max(0, 1 - (error / (abs(actual_value) + 1e-6)))
                    }
        
        return {'error': f'Prediction {prediction_id} not found or already validated'}
    
    def get_accuracy_metrics(self, tenant_id: str, metric_type: str) -> Dict[str, Any]:
        """Get accuracy metrics for a tenant-metric pair"""
        key = f"{tenant_id}_{metric_type}"
        
        if key not in self.accuracy_metrics:
            self._update_accuracy_metrics(key)
        
        metrics = self.accuracy_metrics.get(key, {})
        
        # Check if retraining is needed
        metrics['needs_retrain'] = metrics.get('accuracy_score', 1.0) < self.retrain_threshold
        
        return metrics
    
    def _update_accuracy_metrics(self, key: str) -> None:
        """Update accuracy metrics for a tenant-metric pair"""
        predictions = self.predictions.get(key, [])
        validated = [p for p in predictions if p.get('validated', False)]
        
        if not validated:
            self.accuracy_metrics[key] = {
                'total_predictions': len(predictions),
                'validated_predictions': 0,
                'accuracy_score': None,
                'mae': None,
                'rmse': None,
                'mape': None,
                'last_updated': datetime.now().isoformat()
            }
            return
        
        # Calculate metrics
        actual_values = np.array([p['actual_value'] for p in validated])
        predicted_values = np.array([p['predicted_value'] for p in validated])
        
        # Mean Absolute Error
        mae = float(np.mean(np.abs(actual_values - predicted_values)))
        
        # Root Mean Squared Error
        rmse = float(np.sqrt(np.mean((actual_values - predicted_values) ** 2)))
        
        # Mean Absolute Percentage Error (avoid division by zero)
        non_zero_mask = actual_values != 0
        if np.any(non_zero_mask):
            mape = float(np.mean(np.abs((actual_values[non_zero_mask] - predicted_values[non_zero_mask]) / actual_values[non_zero_mask])) * 100)
        else:
            mape = 0.0
        
        # Accuracy score (1 - normalized error)
        mean_actual = np.mean(np.abs(actual_values))
        accuracy_score = float(max(0, 1 - (mae / (mean_actual + 1e-6))))
        
        self.accuracy_metrics[key] = {
            'total_predictions': len(predictions),
            'validated_predictions': len(validated),
            'accuracy_score': accuracy_score,
            'mae': mae,
            'rmse': rmse,
            'mape': mape,
            'last_updated': datetime.now().isoformat(),
            'recent_trend': self._calculate_trend(validated[-20:] if len(validated) >= 20 else validated)
        }
    
    def _calculate_trend(self, recent_predictions: List[Dict]) -> str:
        """Calculate trend in prediction accuracy"""
        if len(recent_predictions) < 5:
            return 'insufficient_data'
        
        errors = [p.get('percentage_error', 0) for p in recent_predictions]
        
        # Simple trend: compare first half to second half
        mid = len(errors) // 2
        first_half_avg = np.mean(errors[:mid])
        second_half_avg = np.mean(errors[mid:])
        
        if second_half_avg < first_half_avg * 0.9:
            return 'improving'
        elif second_half_avg > first_half_avg * 1.1:
            return 'degrading'
        else:
            return 'stable'
    
    def get_predictions_needing_validation(self, tenant_id: Optional[str] = None) -> List[Dict]:
        """Get predictions that are past their validation time and need actual values"""
        now = datetime.now()
        needing_validation = []
        
        for key, preds in self.predictions.items():
            if tenant_id and not key.startswith(f"{tenant_id}_"):
                continue
                
            for pred in preds:
                if pred.get('validated', False):
                    continue
                    
                expected_time = datetime.fromisoformat(pred['expected_validation_time'])
                if expected_time <= now:
                    needing_validation.append({
                        'prediction_id': pred['prediction_id'],
                        'tenant_id': pred['tenant_id'],
                        'metric_type': pred['metric_type'],
                        'predicted_value': pred['predicted_value'],
                        'expected_validation_time': pred['expected_validation_time']
                    })
        
        return needing_validation
    
    def should_retrain(self, tenant_id: str, metric_type: str) -> bool:
        """Check if model should be retrained based on accuracy"""
        metrics = self.get_accuracy_metrics(tenant_id, metric_type)
        return metrics.get('needs_retrain', False)
    
    def _load_predictions(self) -> None:
        """Load predictions from storage"""
        try:
            filepath = os.path.join(self.storage_dir, 'predictions.json')
            if os.path.exists(filepath):
                with open(filepath, 'r') as f:
                    data = json.load(f)
                    self.predictions = defaultdict(list, data.get('predictions', {}))
                    self.accuracy_metrics = data.get('accuracy_metrics', {})
                logger.info(f"Loaded {sum(len(v) for v in self.predictions.values())} predictions from storage")
        except Exception as e:
            logger.error(f"Error loading predictions: {str(e)}")
    
    def _save_predictions(self) -> None:
        """Save predictions to storage"""
        try:
            filepath = os.path.join(self.storage_dir, 'predictions.json')
            with open(filepath, 'w') as f:
                json.dump({
                    'predictions': dict(self.predictions),
                    'accuracy_metrics': self.accuracy_metrics
                }, f, indent=2)
        except Exception as e:
            logger.error(f"Error saving predictions: {str(e)}")
    
    def get_summary(self) -> Dict[str, Any]:
        """Get overall summary of prediction tracking"""
        total_predictions = sum(len(v) for v in self.predictions.values())
        total_validated = sum(
            len([p for p in v if p.get('validated', False)]) 
            for v in self.predictions.values()
        )
        
        # Calculate overall accuracy
        all_accuracy_scores = [
            m.get('accuracy_score') 
            for m in self.accuracy_metrics.values() 
            if m.get('accuracy_score') is not None
        ]
        
        return {
            'total_predictions': total_predictions,
            'total_validated': total_validated,
            'pending_validation': total_predictions - total_validated,
            'metrics_tracked': len(self.predictions),
            'average_accuracy': float(np.mean(all_accuracy_scores)) if all_accuracy_scores else None,
            'needs_retrain_count': sum(
                1 for m in self.accuracy_metrics.values() 
                if m.get('accuracy_score', 1.0) < self.retrain_threshold
            )
        }
