import pandas as pd
import numpy as np
from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
import logging

logger = logging.getLogger(__name__)

class FeatureEngineering:
    """Real feature engineering for FinOps ML models"""
    
    def __init__(self):
        self.feature_names = [
            # Time-based features
            'hour_of_day', 'day_of_week', 'day_of_month', 'week_of_year',
            'is_weekend', 'is_business_hours', 'is_month_end', 'is_quarter_end',
            
            # Usage patterns
            'cpu_avg_1h', 'cpu_avg_6h', 'cpu_avg_24h', 'cpu_avg_7d',
            'memory_avg_1h', 'memory_avg_6h', 'memory_avg_24h', 'memory_avg_7d',
            'cpu_trend_1h', 'cpu_trend_6h', 'cpu_trend_24h',
            'memory_trend_1h', 'memory_trend_6h', 'memory_trend_24h',
            'cpu_volatility_24h', 'memory_volatility_24h',
            
            # Cost features
            'cost_avg_1h', 'cost_avg_6h', 'cost_avg_24h', 'cost_avg_7d',
            'cost_growth_rate_24h', 'cost_growth_rate_7d',
            'cost_vs_budget_ratio', 'cost_efficiency_score',
            
            # Resource features
            'instance_count', 'instance_count_trend_24h',
            'resource_utilization_ratio', 'resource_efficiency_score',
            
            # Business features
            'tenant_size_score', 'usage_pattern_score', 'seasonality_score'
        ]
    
    def extract_features(self, data: List[Dict[str, Any]], tenant_metadata: Optional[Dict] = None) -> np.ndarray:
        """Extract comprehensive features from metrics data"""
        if not data:
            return np.zeros(len(self.feature_names))
        
        df = pd.DataFrame(data)
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        df = df.sort_values('timestamp')
        
        features = {}
        
        # Time-based features
        latest_timestamp = df['timestamp'].iloc[-1]
        features.update(self._extract_time_features(latest_timestamp))
        
        # Usage pattern features
        features.update(self._extract_usage_features(df))
        
        # Cost features
        features.update(self._extract_cost_features(df))
        
        # Resource features
        features.update(self._extract_resource_features(df))
        
        # Business features
        if tenant_metadata:
            features.update(self._extract_business_features(tenant_metadata))
        
        # Ensure all features are present
        feature_vector = []
        for feature_name in self.feature_names:
            feature_vector.append(features.get(feature_name, 0.0))
        
        return np.array(feature_vector)
    
    def _extract_time_features(self, timestamp: datetime) -> Dict[str, float]:
        """Extract time-based features"""
        return {
            'hour_of_day': timestamp.hour,
            'day_of_week': timestamp.weekday(),
            'day_of_month': timestamp.day,
            'week_of_year': timestamp.isocalendar().week,
            'is_weekend': 1.0 if timestamp.weekday() >= 5 else 0.0,
            'is_business_hours': 1.0 if 9 <= timestamp.hour <= 17 else 0.0,
            'is_month_end': 1.0 if timestamp.day >= 28 else 0.0,
            'is_quarter_end': 1.0 if timestamp.month in [3, 6, 9, 12] and timestamp.day >= 28 else 0.0
        }
    
    def _extract_usage_features(self, df: pd.DataFrame) -> Dict[str, float]:
        """Extract usage pattern features"""
        features = {}
        
        # CPU features
        if 'cpu_utilization' in df.columns:
            cpu_series = df['cpu_utilization']
            features['cpu_avg_1h'] = cpu_series.tail(12).mean()  # Assuming 5-min intervals
            features['cpu_avg_6h'] = cpu_series.tail(72).mean()
            features['cpu_avg_24h'] = cpu_series.tail(288).mean()
            features['cpu_avg_7d'] = cpu_series.mean()
            
            # Trends
            features['cpu_trend_1h'] = self._calculate_trend(cpu_series.tail(12))
            features['cpu_trend_6h'] = self._calculate_trend(cpu_series.tail(72))
            features['cpu_trend_24h'] = self._calculate_trend(cpu_series.tail(288))
            
            # Volatility
            features['cpu_volatility_24h'] = cpu_series.tail(288).std()
        
        # Memory features
        if 'memory_utilization' in df.columns:
            memory_series = df['memory_utilization']
            features['memory_avg_1h'] = memory_series.tail(12).mean()
            features['memory_avg_6h'] = memory_series.tail(72).mean()
            features['memory_avg_24h'] = memory_series.tail(288).mean()
            features['memory_avg_7d'] = memory_series.mean()
            
            # Trends
            features['memory_trend_1h'] = self._calculate_trend(memory_series.tail(12))
            features['memory_trend_6h'] = self._calculate_trend(memory_series.tail(72))
            features['memory_trend_24h'] = self._calculate_trend(memory_series.tail(288))
            
            # Volatility
            features['memory_volatility_24h'] = memory_series.tail(288).std()
        
        return features
    
    def _extract_cost_features(self, df: pd.DataFrame) -> Dict[str, float]:
        """Extract cost-related features"""
        features = {}
        
        if 'hourly_cost' in df.columns:
            cost_series = df['hourly_cost']
            features['cost_avg_1h'] = cost_series.tail(12).mean()
            features['cost_avg_6h'] = cost_series.tail(72).mean()
            features['cost_avg_24h'] = cost_series.tail(288).mean()
            features['cost_avg_7d'] = cost_series.mean()
            
            # Growth rates
            if len(cost_series) >= 2:
                features['cost_growth_rate_24h'] = (cost_series.iloc[-1] - cost_series.iloc[-288]) / cost_series.iloc[-288]
                features['cost_growth_rate_7d'] = (cost_series.iloc[-1] - cost_series.iloc[0]) / cost_series.iloc[0]
            
            # Budget ratio (assuming monthly budget of $1000 for now)
            monthly_cost_estimate = features['cost_avg_24h'] * 730
            features['cost_vs_budget_ratio'] = monthly_cost_estimate / 1000.0
            
            # Cost efficiency (cost per unit of compute)
            if 'cpu_utilization' in df.columns and 'instance_count' in df.columns:
                avg_cpu = df['cpu_utilization'].mean()
                avg_instances = df['instance_count'].mean()
                if avg_cpu > 0 and avg_instances > 0:
                    features['cost_efficiency_score'] = (avg_cpu * avg_instances) / features['cost_avg_24h']
                else:
                    features['cost_efficiency_score'] = 0.0
        
        return features
    
    def _extract_resource_features(self, df: pd.DataFrame) -> Dict[str, float]:
        """Extract resource-related features"""
        features = {}
        
        if 'instance_count' in df.columns:
            instance_series = df['instance_count']
            features['instance_count'] = instance_series.iloc[-1]
            features['instance_count_trend_24h'] = self._calculate_trend(instance_series.tail(288))
        
        # Resource utilization ratio
        if 'cpu_utilization' in df.columns and 'memory_utilization' in df.columns:
            avg_cpu = df['cpu_utilization'].mean()
            avg_memory = df['memory_utilization'].mean()
            features['resource_utilization_ratio'] = (avg_cpu + avg_memory) / 2.0
            
            # Resource efficiency (how well resources are utilized)
            if avg_cpu > 0 and avg_memory > 0:
                features['resource_efficiency_score'] = min(avg_cpu, avg_memory) / max(avg_cpu, avg_memory)
            else:
                features['resource_efficiency_score'] = 0.0
        
        return features
    
    def _extract_business_features(self, tenant_metadata: Dict) -> Dict[str, float]:
        """Extract business-level features"""
        features = {}
        
        # Tenant size score based on plan and resources
        plan = tenant_metadata.get('plan', 'FREE')
        plan_scores = {'FREE': 1.0, 'PRO': 2.0, 'ENTERPRISE': 3.0}
        features['tenant_size_score'] = plan_scores.get(plan, 1.0)
        
        # Usage pattern score (simplified)
        features['usage_pattern_score'] = 1.0  # Would be calculated from historical patterns
        
        # Seasonality score (simplified)
        features['seasonality_score'] = 0.5  # Would be calculated from seasonal analysis
        
        return features
    
    def _calculate_trend(self, series: pd.Series) -> float:
        """Calculate linear trend of a time series"""
        if len(series) < 2:
            return 0.0
        
        x = np.arange(len(series))
        y = series.values
        
        # Remove NaN values
        mask = ~np.isnan(y)
        if mask.sum() < 2:
            return 0.0
        
        x_clean = x[mask]
        y_clean = y[mask]
        
        # Calculate slope (trend)
        slope = np.polyfit(x_clean, y_clean, 1)[0]
        return slope
    
    def get_feature_names(self) -> List[str]:
        """Get list of all feature names"""
        return self.feature_names.copy()
    
    def get_feature_importance(self, model: Any, feature_names: Optional[List[str]] = None) -> Dict[str, float]:
        """Get feature importance from trained model"""
        if feature_names is None:
            feature_names = self.feature_names
        
        if hasattr(model, 'feature_importances_'):
            importances = model.feature_importances_
            return dict(zip(feature_names, importances))
        elif hasattr(model, 'coef_'):
            coef = model.coef_
            if len(coef.shape) == 1:
                return dict(zip(feature_names, np.abs(coef)))
            else:
                # For multi-class, take average importance
                avg_importance = np.mean(np.abs(coef), axis=0)
                return dict(zip(feature_names, avg_importance))
        
        return {}
