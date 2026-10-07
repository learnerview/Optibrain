import pandas as pd
import numpy as np
from typing import List, Dict, Any, Tuple, Optional
from datetime import datetime, timedelta
import logging
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error
import joblib
import os

# PROPHET: Core time-series forecasting library for trend and seasonality analysis.
try:
    from prophet import Prophet
    PROPHET_AVAILABLE = True
except ImportError:
    PROPHET_AVAILABLE = False
    
logger = logging.getLogger(__name__)

class ForecastingService:
    """Real time series forecasting for cloud metrics with Prophet and seasonality detection"""
    
    def __init__(self, model_dir: str = "models"):
        self.model_dir = model_dir
        self.models = {}
        self.scalers = {}
        self.prophet_models = {}  # Store Prophet models per metric
        self.feature_engineering = None
        self.seasonality_patterns = {}  # Store detected seasonality
        
        # Create model directory if it doesn't exist
        os.makedirs(model_dir, exist_ok=True)
        
        # Initialize models
        self._initialize_models()
    
    def _initialize_models(self):
        """Initialize forecasting models"""
        self.models = {
            'cpu': {
                'rf': RandomForestRegressor(n_estimators=100, random_state=42),
                'gb': GradientBoostingRegressor(n_estimators=100, random_state=42),
                'lr': LinearRegression()
            },
            'memory': {
                'rf': RandomForestRegressor(n_estimators=100, random_state=42),
                'gb': GradientBoostingRegressor(n_estimators=100, random_state=42),
                'lr': LinearRegression()
            },
            'cost': {
                'rf': RandomForestRegressor(n_estimators=100, random_state=42),
                'gb': GradientBoostingRegressor(n_estimators=100, random_state=42),
                'lr': LinearRegression()
            }
        }
        
        # Initialize scalers
        for metric in ['cpu', 'memory', 'cost']:
            self.scalers[metric] = StandardScaler()
    
    def train_models(self, training_data: Dict[str, pd.DataFrame]) -> Dict[str, float]:
        """Train forecasting models for all metrics"""
        training_scores = {}
        
        for metric, df in training_data.items():
            if metric not in self.models:
                continue
            
            logger.info(f"Training models for {metric}")
            
            # Prepare features and target
            X, y = self._prepare_training_data(df, metric)
            
            if len(X) < 10:  # Minimum samples for training
                logger.warning(f"Insufficient data for {metric} training: {len(X)} samples")
                continue
            
            # Split data
            split_idx = int(len(X) * 0.8)
            X_train, X_test = X[:split_idx], X[split_idx:]
            y_train, y_test = y[:split_idx], y[split_idx:]
            
            # Scale features
            X_train_scaled = self.scalers[metric].fit_transform(X_train)
            X_test_scaled = self.scalers[metric].transform(X_test)
            
            # Train ensemble models
            model_scores = {}
            for model_name, model in self.models[metric].items():
                try:
                    model.fit(X_train_scaled, y_train)
                    y_pred = model.predict(X_test_scaled)
                    
                    # Calculate metrics
                    mae = mean_absolute_error(y_test, y_pred)
                    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
                    
                    model_scores[model_name] = {'mae': mae, 'rmse': rmse}
                    logger.info(f"{metric} {model_name} - MAE: {mae:.4f}, RMSE: {rmse:.4f}")
                    
                except Exception as e:
                    logger.error(f"Error training {metric} {model_name}: {str(e)}")
            
            training_scores[metric] = model_scores
            
            # Save models
            self._save_models(metric)
        
        return training_scores
    
    def forecast(self, 
                metric_type: str, 
                historical_data: List[float], 
                forecast_horizon: int = 24,
                tenant_metadata: Optional[Dict] = None,
                dl_forecaster: Optional[Any] = None) -> Dict[str, Any]:
        """
        Generate forecast for a specific metric using Prophet or ensemble ML.
        Includes seasonality detection and confidence intervals.
        """
        
        if metric_type not in self.models:
            raise ValueError(f"Unsupported metric type: {metric_type}")
        
        # Prepare data
        df = self._prepare_forecast_data(historical_data, metric_type)
        
        if len(df) < 10:
            logger.warning(f"Insufficient data for {metric_type} forecast: {len(df)} samples")
            return self._generate_fallback_forecast(metric_type, forecast_horizon)
        
        # Detect seasonality patterns
        seasonality = self._detect_seasonality(historical_data, metric_type)
        self.seasonality_patterns[metric_type] = seasonality
        
        # Try Prophet first for better time series forecasting
        if PROPHET_AVAILABLE and len(historical_data) >= 48:  # Need at least 2 days for Prophet
            try:
                result = self._forecast_with_prophet(historical_data, metric_type, forecast_horizon, seasonality)
                if result:
                    result['seasonality'] = seasonality
                    return result
            except Exception as e:
                logger.warning(f"Prophet forecast failed, falling back to ensemble: {str(e)}")
        
        # Use DL forecaster if available
        dl_result = None
        if dl_forecaster:
            try:
                dl_result = dl_forecaster.predict(historical_data, forecast_horizon)
            except Exception as e:
                logger.error(f"DL forecasting error: {str(e)}")
        
        # Fallback to ensemble ML forecasting
        from .feature_engineering import FeatureEngineering
        if self.feature_engineering is None:
            self.feature_engineering = FeatureEngineering()
        
        # Prepare training data
        X, y = self._prepare_training_data(df, metric_type)
        
        if len(X) < 10:
            return self._generate_fallback_forecast(metric_type, forecast_horizon)
        
        # Scale features
        X_scaled = self.scalers[metric_type].fit_transform(X)
        
        # LATENCY OPTIMIZATION: Retrain ensemble models only if strictly necessary
        # based on a defined TTL (Time-To-Live). This minimizes resource overhead
        # and ensures stability across varied compute environments.
        current_time = datetime.now()
        last_train = self.models.get(f'{metric_type}_last_train')
        
        if last_train is None or (current_time - last_train) > timedelta(hours=1):
            logger.info(f"Retraining ensemble models for {metric_type}")
            for model_name, model in self.models[metric_type].items():
                try:
                    model.fit(X_scaled, y)
                except Exception as e:
                    logger.error(f"Error training {metric_type} {model_name} for forecast: {str(e)}")
            self.models[f'{metric_type}_last_train'] = current_time
        
        # Generate forecast
        forecast_values = []
        confidence_scores = []
        confidence_lower = []
        confidence_upper = []
        timestamps = []
        
        # Create future timestamps
        last_timestamp = datetime.now()
        for i in range(forecast_horizon):
            future_time = last_timestamp + timedelta(hours=i+1)
            timestamps.append(future_time.isoformat())
        
        # Ensemble forecasting with seasonality adjustment
        for i in range(forecast_horizon):
            predictions = []
            
            for model_name, model in self.models[metric_type].items():
                try:
                    # Create features for future prediction
                    future_features = self._create_future_features(df, i+1, metric_type)
                    if future_features is not None:
                        future_features_scaled = self.scalers[metric_type].transform(future_features.reshape(1, -1))
                        pred = model.predict(future_features_scaled)[0]
                        predictions.append(pred)
                except Exception as e:
                    logger.error(f"Error in {model_name} prediction: {str(e)}")
            
            if predictions:
                # Ensemble prediction (weighted average)
                ensemble_pred = np.mean(predictions)
                std_pred = np.std(predictions) if len(predictions) > 1 else ensemble_pred * 0.1
                
                # Apply seasonality adjustment
                hour_of_day = (last_timestamp.hour + i + 1) % 24
                if seasonality.get('hourly_pattern'):
                    hourly_factor = seasonality['hourly_pattern'].get(hour_of_day, 1.0)
                    ensemble_pred *= hourly_factor
                
                # Further adjust with DL prediction if available
                if dl_result and i < len(dl_result['predictions']):
                    dl_weight = 0.4
                    ensemble_pred = (ensemble_pred * (1 - dl_weight)) + (dl_result['predictions'][i] * dl_weight)
                
                confidence = 1.0 - (std_pred / (np.mean(predictions) + 1e-6))
                confidence = max(0.5, min(0.95, confidence))
                
                forecast_values.append(float(ensemble_pred))
                confidence_scores.append(float(confidence))
                confidence_lower.append(float(ensemble_pred - 1.96 * std_pred))
                confidence_upper.append(float(ensemble_pred + 1.96 * std_pred))
            else:
                # Fallback prediction
                last_value = historical_data[-1] if historical_data else 50.0
                forecast_values.append(float(last_value))
                confidence_scores.append(0.5)
                confidence_lower.append(float(last_value * 0.8))
                confidence_upper.append(float(last_value * 1.2))
        
        # Apply post-processing
        forecast_values = self._post_process_forecast(metric_type, forecast_values)
        
        # Get feature importance
        feature_importance = {}
        if hasattr(self.models[metric_type]['rf'], 'feature_importances_'):
            feature_names = self.feature_engineering.get_feature_names()
            importance = self.models[metric_type]['rf'].feature_importances_
            if len(feature_names) == len(importance):
                feature_importance = dict(zip(feature_names, [float(x) for x in importance]))
        
        return {
            'predictions': forecast_values,
            'confidence_scores': confidence_scores,
            'confidence_lower': confidence_lower,
            'confidence_upper': confidence_upper,
            'timestamps': timestamps,
            'model_version': f"ensemble_{metric_type}_v2.0",
            'feature_importance': feature_importance,
            'forecast_horizon': forecast_horizon,
            'metric_type': metric_type,
            'seasonality': seasonality,
            'is_dl_enhanced': dl_result is not None
        }
    
    def _forecast_with_prophet(self, historical_data: List[float], metric_type: str, 
                                forecast_horizon: int, seasonality: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Use Facebook Prophet for time series forecasting with seasonality"""
        if not PROPHET_AVAILABLE:
            return None
            
        try:
            # Prepare data for Prophet (requires 'ds' and 'y' columns)
            timestamps = [datetime.now() - timedelta(hours=len(historical_data)-i-1) 
                         for i in range(len(historical_data))]
            prophet_df = pd.DataFrame({
                'ds': timestamps,
                'y': historical_data
            })
            
            # Configure Prophet with detected seasonality
            model = Prophet(
                yearly_seasonality=False,
                weekly_seasonality=seasonality.get('has_weekly', True),
                daily_seasonality=seasonality.get('has_daily', True),
                changepoint_prior_scale=0.05,
                interval_width=0.95
            )
            
            # Add hourly seasonality if detected
            if seasonality.get('has_hourly', False):
                model.add_seasonality(name='hourly', period=1, fourier_order=5)
            
            # Fit model
            model.fit(prophet_df)
            
            # Create future dataframe
            future = model.make_future_dataframe(periods=forecast_horizon, freq='H')
            
            # Predict
            forecast = model.predict(future)
            
            # Extract predictions for future periods only
            future_forecast = forecast.tail(forecast_horizon)
            
            predictions = future_forecast['yhat'].tolist()
            predictions = self._post_process_forecast(metric_type, predictions)
            
            return {
                'predictions': predictions,
                'confidence_scores': [0.85] * forecast_horizon,  # Prophet provides confidence intervals
                'confidence_lower': future_forecast['yhat_lower'].tolist(),
                'confidence_upper': future_forecast['yhat_upper'].tolist(),
                'timestamps': [ts.isoformat() for ts in future_forecast['ds'].tolist()],
                'model_version': f"prophet_{metric_type}_v2.0",
                'feature_importance': {},
                'forecast_horizon': forecast_horizon,
                'metric_type': metric_type
            }
            
        except Exception as e:
            logger.error(f"Prophet forecasting error: {str(e)}")
            return None
    
    def _detect_seasonality(self, historical_data: List[float], metric_type: str) -> Dict[str, Any]:
        """Detect hourly, daily, and weekly seasonality patterns"""
        seasonality = {
            'has_hourly': False,
            'has_daily': False,
            'has_weekly': False,
            'hourly_pattern': {},
            'daily_pattern': {},
            'weekly_pattern': {}
        }
        
        if len(historical_data) < 24:
            return seasonality
        
        data = np.array(historical_data)
        
        # Detect hourly patterns (need at least 48 hours)
        if len(data) >= 48:
            try:
                hourly_means = {}
                for i in range(24):
                    indices = list(range(i, len(data), 24))
                    if indices:
                        hourly_means[i] = float(np.mean([data[j] for j in indices if j < len(data)]))
                
                # Calculate coefficient of variation for hourly pattern
                mean_val = np.mean(list(hourly_means.values()))
                if mean_val > 0:
                    cv = np.std(list(hourly_means.values())) / mean_val
                    seasonality['has_hourly'] = cv > 0.1  # >10% variation suggests seasonality
                    
                    # Normalize hourly pattern (factor relative to mean)
                    if seasonality['has_hourly']:
                        seasonality['hourly_pattern'] = {
                            h: v / mean_val for h, v in hourly_means.items()
                        }
            except Exception as e:
                logger.warning(f"Error detecting hourly seasonality: {str(e)}")
        
        # Detect daily patterns (need at least 7 days)
        if len(data) >= 168:  # 7 days * 24 hours
            try:
                daily_means = []
                for i in range(7):
                    start = i * 24
                    end = start + 24
                    daily_means.append(float(np.mean(data[start:end])))
                
                mean_val = np.mean(daily_means)
                if mean_val > 0:
                    cv = np.std(daily_means) / mean_val
                    seasonality['has_daily'] = cv > 0.05
                    seasonality['daily_pattern'] = {i: v / mean_val for i, v in enumerate(daily_means)}
            except Exception as e:
                logger.warning(f"Error detecting daily seasonality: {str(e)}")
        
        # Check for weekly patterns
        if len(data) >= 336:  # 2 weeks
            seasonality['has_weekly'] = True
        
        return seasonality
    
    def _prepare_training_data(self, df: pd.DataFrame, metric_type: str) -> Tuple[np.ndarray, np.ndarray]:
        """Prepare training data for forecasting"""
        # Create lag features
        for lag in [1, 6, 12, 24]:  # 5min, 30min, 1hour, 2hours lags
            df[f'{metric_type}_lag_{lag}'] = df[metric_type].shift(lag)
        
        # Create rolling features
        for window in [6, 12, 24]:  # 30min, 1hour, 2hours windows
            df[f'{metric_type}_rolling_mean_{window}'] = df[metric_type].rolling(window).mean()
            df[f'{metric_type}_rolling_std_{window}'] = df[metric_type].rolling(window).std()
        
        # Create trend features
        df[f'{metric_type}_trend'] = df[metric_type].diff()
        
        # Drop NaN values
        df = df.dropna()
        
        if len(df) == 0:
            return np.array([]), np.array([])
        
        # Feature columns
        feature_cols = [col for col in df.columns if col != metric_type]
        X = df[feature_cols].values
        y = df[metric_type].values
        
        return X, y
    
    def _prepare_forecast_data(self, historical_data: List[float], metric_type: str) -> pd.DataFrame:
        """Prepare historical data for forecasting"""
        if not historical_data:
            return pd.DataFrame()
        
        # Create DataFrame
        timestamps = [datetime.now() - timedelta(minutes=5*i) for i in range(len(historical_data)-1, -1, -1)]
        df = pd.DataFrame({
            'timestamp': timestamps,
            metric_type: historical_data
        })
        
        return df
    
    def _create_future_features(self, df: pd.DataFrame, steps_ahead: int, metric_type: str) -> Optional[np.ndarray]:
        """Create features for future prediction"""
        if len(df) < 24:
            return None
        
        # Use last known values and trends
        last_values = df[metric_type].tail(24).values
        
        # Simple feature engineering for future prediction
        features = [
            np.mean(last_values),  # Average
            np.std(last_values),   # Volatility
            last_values[-1],      # Last value
            np.mean(last_values[-6:]),  # Recent average
        ]
        
        # Add trend
        if len(last_values) >= 2:
            trend = last_values[-1] - last_values[-2]
            features.append(trend)
        else:
            features.append(0.0)
        
        return np.array(features)
    
    def _post_process_forecast(self, metric_type: str, forecast_values: List[float]) -> List[float]:
        """Post-process forecast values"""
        processed = []
        
        for value in forecast_values:
            # Apply metric-specific constraints
            if metric_type in ['cpu', 'memory']:
                # Utilization should be between 0 and 100
                value = max(0.0, min(100.0, value))
            elif metric_type == 'cost':
                # Cost should be positive
                value = max(0.0, value)
            
            processed.append(value)
        
        return processed
    
    def _generate_fallback_forecast(self, metric_type: str, forecast_horizon: int) -> Dict[str, Any]:
        """Generate fallback forecast when insufficient data"""
        # Use simple linear extrapolation
        if metric_type in ['cpu', 'memory']:
            base_value = 50.0  # Default utilization
        elif metric_type == 'cost':
            base_value = 25.0  # Default hourly cost
        else:
            base_value = 0.0
        
        forecast_values = [base_value] * forecast_horizon
        confidence_scores = [0.3] * forecast_horizon
        timestamps = [(datetime.now() + timedelta(hours=i+1)).isoformat() for i in range(forecast_horizon)]
        
        return {
            'predictions': forecast_values,
            'confidence_scores': confidence_scores,
            'timestamps': timestamps,
            'model_version': f"fallback_{metric_type}_v1.0",
            'feature_importance': {},
            'forecast_horizon': forecast_horizon,
            'metric_type': metric_type
        }
    
    def _save_models(self, metric_type: str):
        """Save trained models to disk"""
        try:
            for model_name, model in self.models[metric_type].items():
                model_path = os.path.join(self.model_dir, f"{metric_type}_{model_name}.joblib")
                joblib.dump(model, model_path)
            
            scaler_path = os.path.join(self.model_dir, f"{metric_type}_scaler.joblib")
            joblib.dump(self.scalers[metric_type], scaler_path)
            
            logger.info(f"Saved {metric_type} models to disk")
        except Exception as e:
            logger.error(f"Error saving {metric_type} models: {str(e)}")
    
    def load_models(self, metric_type: str) -> bool:
        """Load trained models from disk"""
        try:
            for model_name in self.models[metric_type].keys():
                model_path = os.path.join(self.model_dir, f"{metric_type}_{model_name}.joblib")
                if os.path.exists(model_path):
                    self.models[metric_type][model_name] = joblib.load(model_path)
            
            scaler_path = os.path.join(self.model_dir, f"{metric_type}_scaler.joblib")
            if os.path.exists(scaler_path):
                self.scalers[metric_type] = joblib.load(scaler_path)
            
            logger.info(f"Loaded {metric_type} models from disk")
            return True
        except Exception as e:
            logger.error(f"Error loading {metric_type} models: {str(e)}")
            return False
