import pandas as pd
import numpy as np
from typing import List, Dict, Any, Tuple, Optional
from datetime import datetime, timedelta
import logging
from sklearn.ensemble import IsolationForest
from sklearn.svm import OneClassSVM
from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.cluster import DBSCAN
import joblib
import os

logger = logging.getLogger(__name__)

class AnomalyDetectionService:
    """Real anomaly detection for cloud metrics"""
    
    def __init__(self, model_dir: str = "models"):
        self.model_dir = model_dir
        self.models = {}
        self.scalers = {}
        self.feature_engineering = None
        
        # Create model directory if it doesn't exist
        os.makedirs(model_dir, exist_ok=True)
        
        # Initialize anomaly detection models
        self._initialize_models()
    
    def _initialize_models(self):
        """Initialize anomaly detection models"""
        self.models = {
            'isolation_forest': IsolationForest(
                n_estimators=100,
                contamination=0.1,
                random_state=42,
                n_jobs=-1
            ),
            'one_class_svm': OneClassSVM(
                kernel='rbf',
                gamma='scale',
                nu=0.1
            ),
            'dbscan': DBSCAN(
                eps=0.5,
                min_samples=5
            )
        }
        
        # Initialize scalers
        self.scalers = {
            'global': StandardScaler(),
            'cpu': StandardScaler(),
            'memory': StandardScaler(),
            'cost': StandardScaler()
        }
    
    def train_models(self, training_data: List[Dict[str, Any]]) -> Dict[str, float]:
        """Train anomaly detection models"""
        if not training_data:
            logger.warning("No training data provided for anomaly detection")
            return {}
        
        # Prepare data
        df = pd.DataFrame(training_data)
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        df = df.sort_values('timestamp')
        
        # Extract features
        from .feature_engineering import FeatureEngineering
        if self.feature_engineering is None:
            self.feature_engineering = FeatureEngineering()
        
        features_list = []
        for i in range(len(df)):
            # Use sliding window of historical data
            start_idx = max(0, i - 24)  # 2 hours of history
            window_data = df.iloc[start_idx:i+1].to_dict('records')
            features = self.feature_engineering.extract_features(window_data)
            features_list.append(features)
        
        if not features_list:
            logger.error("No features extracted for training")
            return {}
        
        X = np.array(features_list)
        
        # Remove any NaN or infinite values
        mask = np.isfinite(X).all(axis=1)
        X = X[mask]
        
        if len(X) < 10:
            logger.warning(f"Insufficient clean data for anomaly detection training: {len(X)} samples")
            return {}
        
        # Scale features
        X_scaled = self.scalers['global'].fit_transform(X)
        
        # Train models
        training_scores = {}
        
        # Train Isolation Forest
        try:
            self.models['isolation_forest'].fit(X_scaled)
            if_scores = self.models['isolation_forest'].decision_function(X_scaled)
            training_scores['isolation_forest'] = {
                'mean_score': np.mean(if_scores),
                'std_score': np.std(if_scores)
            }
            logger.info(f"Isolation Forest trained - Mean score: {np.mean(if_scores):.4f}")
        except Exception as e:
            logger.error(f"Error training Isolation Forest: {str(e)}")
        
        # Train One-Class SVM
        try:
            self.models['one_class_svm'].fit(X_scaled)
            oc_scores = self.models['one_class_svm'].decision_function(X_scaled)
            training_scores['one_class_svm'] = {
                'mean_score': np.mean(oc_scores),
                'std_score': np.std(oc_scores)
            }
            logger.info(f"One-Class SVM trained - Mean score: {np.mean(oc_scores):.4f}")
        except Exception as e:
            logger.error(f"Error training One-Class SVM: {str(e)}")
        
        # Save models
        self._save_models()
        
        return training_scores
    
    def detect_anomalies(self, 
                        metrics_data: List[Dict[str, Any]], 
                        sensitivity: float = 0.1) -> Dict[str, Any]:
        """Detect anomalies in metrics data"""
        
        if not metrics_data:
            return {
                'anomalies': [],
                'anomaly_score': 0.0,
                'confidence': 0.0,
                'affected_metrics': [],
                'recommendations': []
            }
        
        # Prepare data
        df = pd.DataFrame(metrics_data)
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        df = df.sort_values('timestamp')
        
        # Extract features for each data point
        from .feature_engineering import FeatureEngineering
        if self.feature_engineering is None:
            self.feature_engineering = FeatureEngineering()
        
        features_list = []
        for i in range(len(df)):
            start_idx = max(0, i - 24)
            window_data = df.iloc[start_idx:i+1].to_dict('records')
            features = self.feature_engineering.extract_features(window_data)
            features_list.append(features)
        
        if not features_list:
            return self._empty_anomaly_result()
        
        X = np.array(features_list)
        
        # Handle NaN values
        mask = np.isfinite(X).all(axis=1)
        if not mask.any():
            return self._empty_anomaly_result()
        
        X_clean = X[mask]
        X_scaled = self.scalers['global'].transform(X_clean)
        
        # Detect anomalies using ensemble
        anomaly_results = self._ensemble_anomaly_detection(X_scaled, sensitivity)
        
        # Map results back to original data
        anomalies = []
        affected_metrics = set()
        
        for i, (is_anomaly, score, confidence) in enumerate(zip(
            anomaly_results['is_anomaly'],
            anomaly_results['scores'],
            anomaly_results['confidence']
        )):
            if is_anomaly:
                original_idx = np.where(mask)[0][i]
                data_point = df.iloc[original_idx]
                
                # Determine which metrics are anomalous
                anomalous_metrics = self._identify_anomalous_metrics(data_point, sensitivity)
                affected_metrics.update(anomalous_metrics)
                
                anomaly = {
                    'timestamp': data_point['timestamp'].isoformat(),
                    'anomaly_score': float(score),
                    'confidence': float(confidence),
                    'anomalous_metrics': anomalous_metrics,
                    'values': {
                        'cpu_utilization': float(data_point.get('cpu_utilization', 0)),
                        'memory_utilization': float(data_point.get('memory_utilization', 0)),
                        'hourly_cost': float(data_point.get('hourly_cost', 0)),
                        'instance_count': int(data_point.get('instance_count', 0))
                    },
                    'severity': self._calculate_severity(score, confidence),
                    'type': self._classify_anomaly_type(data_point, anomalous_metrics)
                }
                anomalies.append(anomaly)
        
        # Generate recommendations
        recommendations = self._generate_recommendations(anomalies, affected_metrics)
        
        # Calculate overall metrics
        overall_score = np.mean(anomaly_results['scores']) if anomaly_results['scores'] else 0.0
        overall_confidence = np.mean(anomaly_results['confidence']) if anomaly_results['confidence'] else 0.0
        
        return {
            'anomalies': anomalies,
            'anomaly_score': float(overall_score),
            'confidence': float(overall_confidence),
            'affected_metrics': list(affected_metrics),
            'recommendations': recommendations,
            'detection_method': 'ensemble',
            'sensitivity': sensitivity
        }
    
    def _ensemble_anomaly_detection(self, X: np.ndarray, sensitivity: float) -> Dict[str, Any]:
        """Ensemble anomaly detection using multiple models"""
        results = {
            'is_anomaly': [],
            'scores': [],
            'confidence': []
        }
        
        # Isolation Forest
        try:
            if_predictions = self.models['isolation_forest'].predict(X)
            if_scores = self.models['isolation_forest'].decision_function(X)
            
            # Convert predictions (-1 for anomaly, 1 for normal) to boolean
            if_anomalies = if_predictions == -1
            if_confidence = np.abs(if_scores)
            
            results['is_anomaly'].append(if_anomalies)
            results['scores'].append(if_scores)
            results['confidence'].append(if_confidence)
        except Exception as e:
            logger.error(f"Error in Isolation Forest prediction: {str(e)}")
        
        # One-Class SVM
        try:
            oc_predictions = self.models['one_class_svm'].predict(X)
            oc_scores = self.models['one_class_svm'].decision_function(X)
            
            oc_anomalies = oc_predictions == -1
            oc_confidence = np.abs(oc_scores)
            
            results['is_anomaly'].append(oc_anomalies)
            results['scores'].append(oc_scores)
            results['confidence'].append(oc_confidence)
        except Exception as e:
            logger.error(f"Error in One-Class SVM prediction: {str(e)}")
        
        # Ensemble voting
        if results['is_anomaly']:
            # Stack predictions and take majority vote
            stacked_anomalies = np.stack(results['is_anomaly'])
            ensemble_anomalies = np.mean(stacked_anomalies, axis=0) > (1 - sensitivity)
            
            # Average scores and confidence
            ensemble_scores = np.mean(results['scores'], axis=0)
            ensemble_confidence = np.mean(results['confidence'], axis=0)
            
            return {
                'is_anomaly': ensemble_anomalies,
                'scores': ensemble_scores,
                'confidence': ensemble_confidence
            }
        
        # Fallback to empty results
        return {
            'is_anomaly': np.zeros(len(X), dtype=bool),
            'scores': np.zeros(len(X)),
            'confidence': np.zeros(len(X))
        }
    
    def _identify_anomalous_metrics(self, data_point: pd.Series, sensitivity: float) -> List[str]:
        """Identify which specific metrics are anomalous"""
        anomalous_metrics = []
        
        # Define thresholds based on sensitivity
        cpu_threshold = 90.0 - (sensitivity * 20.0)  # Higher sensitivity = lower threshold
        memory_threshold = 85.0 - (sensitivity * 15.0)
        
        # Check CPU
        if 'cpu_utilization' in data_point:
            cpu_val = data_point['cpu_utilization']
            if cpu_val > cpu_threshold or cpu_val < 5.0:  # Too high or too low
                anomalous_metrics.append('cpu_utilization')
        
        # Check Memory
        if 'memory_utilization' in data_point:
            mem_val = data_point['memory_utilization']
            if mem_val > memory_threshold or mem_val < 5.0:
                anomalous_metrics.append('memory_utilization')
        
        # Check Cost
        if 'hourly_cost' in data_point:
            cost_val = data_point['hourly_cost']
            # Simple cost anomaly detection (would use historical baseline in production)
            if cost_val > 500.0:  # Arbitrary high cost threshold
                anomalous_metrics.append('hourly_cost')
        
        # Check Instance Count
        if 'instance_count' in data_point:
            instance_val = data_point['instance_count']
            if instance_val > 100:  # Arbitrary high instance count
                anomalous_metrics.append('instance_count')
        
        return anomalous_metrics
    
    def _calculate_severity(self, score: float, confidence: float) -> str:
        """Calculate anomaly severity"""
        combined_score = abs(score) * confidence
        
        if combined_score > 0.8:
            return 'CRITICAL'
        elif combined_score > 0.6:
            return 'HIGH'
        elif combined_score > 0.4:
            return 'MEDIUM'
        else:
            return 'LOW'
    
    def _classify_anomaly_type(self, data_point: pd.Series, anomalous_metrics: List[str]) -> str:
        """Classify the type of anomaly"""
        if 'cpu_utilization' in anomalous_metrics and 'memory_utilization' in anomalous_metrics:
            return 'RESOURCE_OVERLOAD'
        elif 'hourly_cost' in anomalous_metrics:
            return 'COST_SPIKE'
        elif 'instance_count' in anomalous_metrics:
            return 'SCALING_ANOMALY'
        elif 'cpu_utilization' in anomalous_metrics:
            return 'CPU_ANOMALY'
        elif 'memory_utilization' in anomalous_metrics:
            return 'MEMORY_ANOMALY'
        else:
            return 'GENERAL_ANOMALY'
    
    def _generate_recommendations(self, anomalies: List[Dict], affected_metrics: set) -> List[str]:
        """Generate recommendations based on detected anomalies"""
        recommendations = []
        
        if not anomalies:
            return ["No anomalies detected. System operating normally."]
        
        # Analyze anomaly patterns
        critical_anomalies = [a for a in anomalies if a['severity'] == 'CRITICAL']
        cost_anomalies = [a for a in anomalies if 'hourly_cost' in a['anomalous_metrics']]
        resource_anomalies = [a for a in anomalies if 'cpu_utilization' in a['anomalous_metrics'] or 'memory_utilization' in a['anomalous_metrics']]
        
        # Critical anomalies
        if critical_anomalies:
            recommendations.append("🚨 CRITICAL: Immediate investigation required. Check system health and resource utilization.")
        
        # Cost anomalies
        if cost_anomalies:
            recommendations.append("💰 COST ANOMALY: Review recent resource changes and consider scaling down unused resources.")
        
        # Resource anomalies
        if resource_anomalies:
            recommendations.append("📊 RESOURCE ANOMALY: Consider rightsizing instances or implementing auto-scaling policies.")
        
        # General recommendations
        if len(anomalies) > 5:
            recommendations.append("🔄 MULTIPLE ANOMALIES: Consider reviewing overall system architecture and optimization strategies.")
        
        # Specific metric recommendations
        if 'cpu_utilization' in affected_metrics:
            recommendations.append("⚡ CPU: Check for CPU-intensive processes and consider load balancing.")
        
        if 'memory_utilization' in affected_metrics:
            recommendations.append("🧠 MEMORY: Investigate memory leaks and consider memory optimization.")
        
        return recommendations[:5]  # Limit to top 5 recommendations
    
    def _empty_anomaly_result(self) -> Dict[str, Any]:
        """Return empty anomaly result"""
        return {
            'anomalies': [],
            'anomaly_score': 0.0,
            'confidence': 0.0,
            'affected_metrics': [],
            'recommendations': ["No data available for anomaly detection."]
        }
    
    def _save_models(self):
        """Save trained models to disk"""
        try:
            for model_name, model in self.models.items():
                if hasattr(model, 'fit'):  # Only save trainable models
                    model_path = os.path.join(self.model_dir, f"anomaly_{model_name}.joblib")
                    joblib.dump(model, model_path)
            
            for scaler_name, scaler in self.scalers.items():
                scaler_path = os.path.join(self.model_dir, f"anomaly_scaler_{scaler_name}.joblib")
                joblib.dump(scaler, scaler_path)
            
            logger.info("Anomaly detection models saved to disk")
        except Exception as e:
            logger.error(f"Error saving anomaly detection models: {str(e)}")
    
    def load_models(self) -> bool:
        """Load trained models from disk"""
        try:
            for model_name in self.models.keys():
                model_path = os.path.join(self.model_dir, f"anomaly_{model_name}.joblib")
                if os.path.exists(model_path):
                    self.models[model_name] = joblib.load(model_path)
            
            for scaler_name in self.scalers.keys():
                scaler_path = os.path.join(self.model_dir, f"anomaly_scaler_{scaler_name}.joblib")
                if os.path.exists(scaler_path):
                    self.scalers[scaler_name] = joblib.load(scaler_path)
            
            logger.info("Anomaly detection models loaded from disk")
            return True
        except Exception as e:
            logger.error(f"Error loading anomaly detection models: {str(e)}")
            return False
