"""
Resource Optimization Service for Granular Resource Control
Optimizes specific resource types based on customer bounds and ML focus
"""

import logging
import numpy as np
from typing import Dict, List, Any, Tuple
from datetime import datetime, timedelta
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.linear_model import LinearRegression
import json

logger = logging.getLogger(__name__)

class ResourceOptimizer:
    """
    Optimizes individual resource types based on:
    - Customer-set upper/lower bounds
    - ML algorithm focus (performance vs availability vs cost vs energy)
    - Predictive analytics for optimal resource levels
    """
    
    def __init__(self):
        self.models = {
            'performance': {
                'cpu': GradientBoostingRegressor(n_estimators=100, random_state=42),
                'memory': GradientBoostingRegressor(n_estimators=100, random_state=42),
                'gpu': GradientBoostingRegressor(n_estimators=100, random_state=42),
                'storage': RandomForestRegressor(n_estimators=100, random_state=42),
                'network': GradientBoostingRegressor(n_estimators=100, random_state=42)
            },
            'availability': {
                'cpu': RandomForestRegressor(n_estimators=150, random_state=42),
                'memory': RandomForestRegressor(n_estimators=150, random_state=42),
                'gpu': RandomForestRegressor(n_estimators=100, random_state=42),
                'storage': RandomForestRegressor(n_estimators=150, random_state=42),
                'network': RandomForestRegressor(n_estimators=150, random_state=42)
            },
            'cost': {
                'cpu': LinearRegression(),
                'memory': LinearRegression(),
                'gpu': LinearRegression(),
                'storage': LinearRegression(),
                'network': LinearRegression()
            },
            'energy': {
                'cpu': RandomForestRegressor(n_estimators=100, random_state=42),
                'memory': RandomForestRegressor(n_estimators=100, random_state=42),
                'gpu': RandomForestRegressor(n_estimators=100, random_state=42),
                'storage': RandomForestRegressor(n_estimators=100, random_state=42),
                'network': RandomForestRegressor(n_estimators=100, random_state=42)
            },
            'balanced': {
                'cpu': GradientBoostingRegressor(n_estimators=100, random_state=42),
                'memory': GradientBoostingRegressor(n_estimators=100, random_state=42),
                'gpu': RandomForestRegressor(n_estimators=100, random_state=42),
                'storage': RandomForestRegressor(n_estimators=100, random_state=42),
                'network': GradientBoostingRegressor(n_estimators=100, random_state=42)
            }
        }
        
        # Focus-specific parameters
        self.focus_parameters = {
            'performance': {
                'scaling_factor': 1.15,  # 15% more resources for performance
                'buffer_ratio': 0.10,    # 10% buffer for performance spikes
                'prediction_horizon': 6,  # 6 hours for performance optimization
                'confidence_threshold': 0.75
            },
            'availability': {
                'scaling_factor': 1.25,  # 25% more resources for availability
                'buffer_ratio': 0.20,    # 20% buffer for redundancy
                'prediction_horizon': 24, # 24 hours for availability planning
                'confidence_threshold': 0.85
            },
            'cost': {
                'scaling_factor': 0.85,  # 15% less resources for cost
                'buffer_ratio': 0.05,    # 5% buffer for cost optimization
                'prediction_horizon': 168, # 7 days for cost planning
                'confidence_threshold': 0.65
            },
            'energy': {
                'scaling_factor': 0.90,  # 10% less resources for energy
                'buffer_ratio': 0.08,    # 8% buffer for energy efficiency
                'prediction_horizon': 24, # 24 hours for energy planning
                'confidence_threshold': 0.70
            },
            'balanced': {
                'scaling_factor': 1.00,  # No scaling for balanced
                'buffer_ratio': 0.12,    # 12% buffer for balanced approach
                'prediction_horizon': 24, # 24 hours for balanced planning
                'confidence_threshold': 0.80
            }
        }
    
    def optimize_resources(self, tenant_id: str, current_inventory: Dict[str, Any], 
                          customer_bounds: Dict[str, Any], ml_focus: str) -> Dict[str, Any]:
        """
        Main optimization function for all resource types
        """
        try:
            logger.info(f"Starting resource optimization for tenant {tenant_id} with focus: {ml_focus}")
            
            optimization_results = {}
            
            # Optimize each resource type
            for resource_type in ['cpu', 'memory', 'gpu', 'storage', 'network', 'servers']:
                try:
                    resource_result = self._optimize_single_resource(
                        tenant_id, resource_type, current_inventory, customer_bounds, ml_focus
                    )
                    optimization_results[resource_type] = resource_result
                except Exception as e:
                    logger.error(f"Error optimizing {resource_type}: {str(e)}")
                    optimization_results[resource_type] = self._get_fallback_optimization(resource_type)
            
            # Calculate overall optimization impact
            overall_impact = self._calculate_overall_impact(optimization_results, current_inventory)
            
            # Generate optimization recommendations
            recommendations = self._generate_recommendations(optimization_results, customer_bounds, ml_focus)
            
            return {
                'tenant_id': tenant_id,
                'ml_focus': ml_focus,
                'optimization_timestamp': datetime.now().isoformat(),
                'resource_optimizations': optimization_results,
                'overall_impact': overall_impact,
                'recommendations': recommendations,
                'execution_plan': self._generate_execution_plan(optimization_results)
            }
            
        except Exception as e:
            logger.error(f"Error in resource optimization: {str(e)}")
            raise
    
    def _optimize_single_resource(self, tenant_id: str, resource_type: str, 
                                current_inventory: Dict[str, Any], 
                                customer_bounds: Dict[str, Any], 
                                ml_focus: str) -> Dict[str, Any]:
        """
        Optimize a single resource type
        """
        
        # Get current state
        current_data = current_inventory.get(resource_type, {})
        current_value = current_data.get('current', 0)
        current_utilization = current_data.get('utilization', 0.0)
        
        # Get customer bounds
        bounds_data = customer_bounds.get(resource_type, {})
        min_bound = bounds_data.get('min', 1)
        max_bound = bounds_data.get('max', 100)
        
        # Get ML focus parameters
        focus_params = self.focus_parameters.get(ml_focus, self.focus_parameters['balanced'])
        
        # Generate features for ML prediction
        features = self._generate_features(resource_type, current_data, current_inventory, ml_focus)
        
        # Get ML prediction
        predicted_value = self._predict_optimal_value(resource_type, features, ml_focus)
        
        # Apply focus-specific scaling
        scaled_prediction = predicted_value * focus_params['scaling_factor']
        
        # Apply bounds constraints
        bounded_prediction = max(min_bound, min(max_bound, scaled_prediction))
        
        # Apply buffer for safety
        buffered_prediction = bounded_prediction * (1 + focus_params['buffer_ratio'])
        final_prediction = max(min_bound, min(max_bound, buffered_prediction))
        
        # Calculate confidence
        confidence = self._calculate_prediction_confidence(resource_type, features, ml_focus)
        
        # Determine action
        action = self._determine_optimization_action(current_value, final_prediction, confidence)
        
        # Calculate impact
        cost_impact = self._calculate_cost_impact(resource_type, current_value, final_prediction)
        energy_impact = self._calculate_energy_impact(resource_type, current_value, final_prediction)
        performance_impact = self._calculate_performance_impact(resource_type, current_value, final_prediction, ml_focus)
        
        return {
            'resource_type': resource_type,
            'current_value': current_value,
            'current_utilization': current_utilization,
            'predicted_value': final_prediction,
            'raw_prediction': predicted_value,
            'customer_bounds': {'min': min_bound, 'max': max_bound},
            'confidence': confidence,
            'action': action,
            'ml_focus': ml_focus,
            'focus_parameters': focus_params,
            'impacts': {
                'cost': cost_impact,
                'energy': energy_impact,
                'performance': performance_impact
            },
            'execution_details': {
                'estimated_time': self._estimate_execution_time(resource_type, action),
                'estimated_downtime': self._estimate_downtime(resource_type, action),
                'complexity': self._assess_complexity(resource_type, action)
            }
        }
    
    def _generate_features(self, resource_type: str, current_data: Dict[str, Any], 
                          inventory: Dict[str, Any], ml_focus: str) -> np.ndarray:
        """
        Generate features for ML prediction
        """
        features = []
        
        # Current utilization
        features.append(current_data.get('utilization', 0.0) / 100.0)
        
        # Time-based features
        now = datetime.now()
        features.append(now.hour / 24.0)  # Hour of day
        features.append(now.weekday() / 7.0)  # Day of week
        features.append(now.day / 31.0)  # Day of month
        
        # Resource-specific features
        if resource_type == 'cpu':
            features.extend([
                current_data.get('cores', 4) / 64.0,  # Normalized cores
                current_data.get('frequency', 2.4) / 4.0,  # Normalized frequency
            ])
        elif resource_type == 'memory':
            features.extend([
                current_data.get('type', 'ddr4') == 'ddr4',  # Memory type
                current_data.get('speed', 2666) / 4000.0,  # Normalized speed
            ])
        elif resource_type == 'gpu':
            features.extend([
                current_data.get('memory', 8) / 32.0,  # GPU memory
                current_data.get('compute_capability', 7.0) / 8.0,  # Compute capability
            ])
        elif resource_type == 'storage':
            features.extend([
                current_data.get('type', 'ssd') == 'ssd',  # Storage type
                current_data.get('iops', 5000) / 10000.0,  # Normalized IOPS
            ])
        elif resource_type == 'network':
            features.extend([
                current_data.get('bandwidth', 10) / 100.0,  # Normalized bandwidth
                current_data.get('latency', 1.0) / 10.0,  # Normalized latency
            ])
        
        # Cross-resource features
        features.extend([
            inventory.get('cpu', {}).get('utilization', 0.0) / 100.0,
            inventory.get('memory', {}).get('utilization', 0.0) / 100.0,
            inventory.get('network', {}).get('utilization', 0.0) / 100.0,
        ])
        
        # Focus-specific features
        focus_encoding = {
            'performance': [1, 0, 0, 0, 0],
            'availability': [0, 1, 0, 0, 0],
            'cost': [0, 0, 1, 0, 0],
            'energy': [0, 0, 0, 1, 0],
            'balanced': [0, 0, 0, 0, 1]
        }
        features.extend(focus_encoding.get(ml_focus, focus_encoding['balanced']))
        
        return np.array(features)
    
    def _predict_optimal_value(self, resource_type: str, features: np.ndarray, ml_focus: str) -> float:
        """
        Predict optimal resource value using trained ML model.
        Returns deterministic predictions without random noise.
        """
        try:
            model = self.models[ml_focus].get(resource_type)
            
            # Check if model is fitted (has been trained)
            if model is not None and hasattr(model, 'n_features_in_'):
                # Use the trained model for prediction
                # Ensure features match expected dimensions
                expected_features = model.n_features_in_
                if len(features) >= expected_features:
                    prediction_features = features[:expected_features].reshape(1, -1)
                else:
                    # Pad features if needed
                    padded = np.zeros(expected_features)
                    padded[:len(features)] = features
                    prediction_features = padded.reshape(1, -1)
                
                predicted_value = model.predict(prediction_features)[0]
                return max(1.0, float(predicted_value))
            
            # Fallback: Use intelligent estimation based on current utilization
            # This is deterministic (no random noise)
            utilization = features[0] if len(features) > 0 else 0.5
            
            # Resource-specific base values and scaling
            resource_base = {
                'cpu': 4.0,      # Base 4 cores
                'memory': 16.0,  # Base 16 GB
                'gpu': 1.0,      # Base 1 GPU
                'storage': 100.0, # Base 100 GB
                'network': 5.0,  # Base 5 Gbps
                'servers': 2.0   # Base 2 servers
            }
            
            base_value = resource_base.get(resource_type, 4.0)
            
            # Focus-specific multipliers (deterministic)
            focus_multipliers = {
                'performance': 1.3,   # 30% more for performance
                'availability': 1.4,  # 40% more for availability
                'cost': 0.8,          # 20% less for cost
                'energy': 0.85,       # 15% less for energy
                'balanced': 1.0       # No adjustment
            }
            
            multiplier = focus_multipliers.get(ml_focus, 1.0)
            
            # Calculate prediction based on utilization and focus
            # High utilization (>0.7) suggests need for more resources
            # Low utilization (<0.3) suggests opportunity to reduce
            if utilization > 0.7:
                predicted_value = base_value * (1.0 + (utilization - 0.5) * 0.5) * multiplier
            elif utilization < 0.3:
                predicted_value = base_value * (0.7 + utilization) * multiplier
            else:
                predicted_value = base_value * multiplier
            
            return max(1.0, float(predicted_value))
            
        except Exception as e:
            logger.error(f"Error predicting optimal value for {resource_type}: {str(e)}")
            # Deterministic fallback based on resource type
            fallback_values = {
                'cpu': 4.0, 'memory': 16.0, 'gpu': 1.0, 
                'storage': 100.0, 'network': 5.0, 'servers': 2.0
            }
            return fallback_values.get(resource_type, 4.0)
    
    def _calculate_prediction_confidence(self, resource_type: str, features: np.ndarray, ml_focus: str) -> float:
        """
        Calculate confidence in prediction
        """
        # Base confidence from data quality
        base_confidence = 0.85
        
        # Adjust based on focus
        focus_confidence = {
            'performance': 0.80,
            'availability': 0.90,
            'cost': 0.75,
            'energy': 0.82,
            'balanced': 0.85
        }
        
        confidence = base_confidence * focus_confidence.get(ml_focus, 0.85)
        
        # Adjust based on data completeness
        data_completeness = min(1.0, len(features) / 15.0)  # Normalize by expected feature count
        confidence *= data_completeness
        
        return min(0.95, max(0.60, confidence))
    
    def _determine_optimization_action(self, current: float, predicted: float, confidence: float) -> str:
        """
        Determine what action to take based on prediction
        """
        if confidence < 0.70:
            return "NO_ACTION_LOW_CONFIDENCE"
        
        diff_percentage = abs(predicted - current) / current if current > 0 else 0
        
        if diff_percentage < 0.05:  # Less than 5% difference
            return "NO_ACTION_MINIMAL_DIFFERENCE"
        elif predicted > current:
            if diff_percentage > 0.20:  # More than 20% increase
                return "SCALE_UP_SIGNIFICANT"
            else:
                return "SCALE_UP_MODERATE"
        else:
            if diff_percentage > 0.20:  # More than 20% decrease
                return "SCALE_DOWN_SIGNIFICANT"
            else:
                return "SCALE_DOWN_MODERATE"
    
    def _calculate_cost_impact(self, resource_type: str, current: float, predicted: float) -> Dict[str, Any]:
        """
        Calculate cost impact of resource change
        """
        # Cost per unit for each resource type (monthly)
        cost_per_unit = {
            'cpu': 30.0,      # $30 per CPU core per month
            'memory': 4.0,    # $4 per GB per month
            'gpu': 300.0,    # $300 per GPU per month
            'storage': 0.1,  # $0.1 per GB per month
            'network': 50.0   # $50 per Gbps per month
        }
        
        current_cost = current * cost_per_unit.get(resource_type, 10.0)
        predicted_cost = predicted * cost_per_unit.get(resource_type, 10.0)
        cost_difference = predicted_cost - current_cost
        
        return {
            'current_cost': current_cost,
            'predicted_cost': predicted_cost,
            'cost_difference': cost_difference,
            'cost_percentage_change': (cost_difference / current_cost) * 100 if current_cost > 0 else 0
        }
    
    def _calculate_energy_impact(self, resource_type: str, current: float, predicted: float) -> Dict[str, Any]:
        """
        Calculate energy impact of resource change
        """
        # Energy consumption per unit (kWh per month)
        energy_per_unit = {
            'cpu': 45.0,      # 45 kWh per CPU core per month
            'memory': 8.0,    # 8 kWh per GB per month
            'gpu': 350.0,    # 350 kWh per GPU per month
            'storage': 2.0,  # 2 kWh per GB per month
            'network': 120.0  # 120 kWh per Gbps per month
        }
        
        current_energy = current * energy_per_unit.get(resource_type, 10.0)
        predicted_energy = predicted * energy_per_unit.get(resource_type, 10.0)
        energy_difference = predicted_energy - current_energy
        
        # Carbon emission (kg CO2 per kWh)
        carbon_factor = 0.5
        carbon_difference = energy_difference * carbon_factor
        
        return {
            'current_energy': current_energy,
            'predicted_energy': predicted_energy,
            'energy_difference': energy_difference,
            'carbon_difference': carbon_difference,
            'energy_percentage_change': (energy_difference / current_energy) * 100 if current_energy > 0 else 0
        }
    
    def _calculate_performance_impact(self, resource_type: str, current: float, predicted: float, ml_focus: str) -> Dict[str, Any]:
        """
        Calculate performance impact of resource change
        """
        # Performance impact based on focus
        focus_multipliers = {
            'performance': 1.2,   # Performance focus amplifies impact
            'availability': 1.1, # Availability focus moderate impact
            'cost': 0.8,        # Cost focus reduces performance impact
            'energy': 0.9,      # Energy focus slight reduction
            'balanced': 1.0     # Balanced focus neutral
        }
        
        multiplier = focus_multipliers.get(ml_focus, 1.0)
        
        # Calculate performance change
        if predicted > current:
            performance_change = ((predicted - current) / current) * multiplier if current > 0 else 0.1
        else:
            performance_change = -((current - predicted) / current) * multiplier if current > 0 else -0.05
        
        return {
            'performance_change': performance_change,
            'performance_impact': 'positive' if performance_change > 0.05 else 'negative' if performance_change < -0.05 else 'neutral',
            'focus_multiplier': multiplier
        }
    
    def _estimate_execution_time(self, resource_type: str, action: str) -> str:
        """
        Estimate execution time for resource action
        """
        execution_times = {
            'cpu': {
                'SCALE_UP_SIGNIFICANT': '15-30 minutes',
                'SCALE_UP_MODERATE': '5-15 minutes',
                'SCALE_DOWN_SIGNIFICANT': '10-20 minutes',
                'SCALE_DOWN_MODERATE': '5-10 minutes'
            },
            'memory': {
                'SCALE_UP_SIGNIFICANT': '2-5 minutes',
                'SCALE_UP_MODERATE': '1-2 minutes',
                'SCALE_DOWN_SIGNIFICANT': '2-5 minutes',
                'SCALE_DOWN_MODERATE': '1-2 minutes'
            },
            'gpu': {
                'SCALE_UP_SIGNIFICANT': '10-20 minutes',
                'SCALE_UP_MODERATE': '5-10 minutes',
                'SCALE_DOWN_SIGNIFICANT': '5-15 minutes',
                'SCALE_DOWN_MODERATE': '2-5 minutes'
            },
            'storage': {
                'SCALE_UP_SIGNIFICANT': '30-60 minutes',
                'SCALE_UP_MODERATE': '10-30 minutes',
                'SCALE_DOWN_SIGNIFICANT': '5-15 minutes',
                'SCALE_DOWN_MODERATE': '2-10 minutes'
            },
            'network': {
                'SCALE_UP_SIGNIFICANT': '5-15 minutes',
                'SCALE_UP_MODERATE': '2-5 minutes',
                'SCALE_DOWN_SIGNIFICANT': '5-15 minutes',
                'SCALE_DOWN_MODERATE': '2-5 minutes'
            }
        }
        
        return execution_times.get(resource_type, {}).get(action, '5-10 minutes')
    
    def _estimate_downtime(self, resource_type: str, action: str) -> str:
        """
        Estimate downtime for resource action
        """
        downtime_map = {
            'cpu': {
                'SCALE_UP_SIGNIFICANT': '2-5 minutes',
                'SCALE_UP_MODERATE': '1-2 minutes',
                'SCALE_DOWN_SIGNIFICANT': '2-5 minutes',
                'SCALE_DOWN_MODERATE': '1-2 minutes'
            },
            'memory': {
                'SCALE_UP_SIGNIFICANT': '1-2 minutes',
                'SCALE_UP_MODERATE': '30-60 seconds',
                'SCALE_DOWN_SIGNIFICANT': '1-2 minutes',
                'SCALE_DOWN_MODERATE': '30-60 seconds'
            },
            'gpu': {
                'SCALE_UP_SIGNIFICANT': '5-10 minutes',
                'SCALE_UP_MODERATE': '2-5 minutes',
                'SCALE_DOWN_SIGNIFICANT': '5-10 minutes',
                'SCALE_DOWN_MODERATE': '2-5 minutes'
            },
            'storage': {
                'SCALE_UP_SIGNIFICANT': '0-30 seconds',
                'SCALE_UP_MODERATE': '0-15 seconds',
                'SCALE_DOWN_SIGNIFICANT': '0-30 seconds',
                'SCALE_DOWN_MODERATE': '0-15 seconds'
            },
            'network': {
                'SCALE_UP_SIGNIFICANT': '1-2 minutes',
                'SCALE_UP_MODERATE': '30-60 seconds',
                'SCALE_DOWN_SIGNIFICANT': '1-2 minutes',
                'SCALE_DOWN_MODERATE': '30-60 seconds'
            }
        }
        
        return downtime_map.get(resource_type, {}).get(action, '1-2 minutes')
    
    def _assess_complexity(self, resource_type: str, action: str) -> str:
        """
        Assess complexity of resource action
        """
        complexity_map = {
            'cpu': 'medium',
            'memory': 'low',
            'gpu': 'high',
            'storage': 'medium',
            'network': 'medium'
        }
        
        return complexity_map.get(resource_type, 'medium')
    
    def _calculate_overall_impact(self, optimizations: Dict[str, Any], inventory: Dict[str, Any]) -> Dict[str, Any]:
        """
        Calculate overall optimization impact
        """
        total_cost_savings = 0
        total_energy_savings = 0
        total_carbon_reduction = 0
        performance_impacts = []
        
        for resource_type, optimization in optimizations.items():
            impacts = optimization.get('impacts', {})
            cost_impact = impacts.get('cost', {})
            energy_impact = impacts.get('energy', {})
            performance_impact = impacts.get('performance', {})
            
            total_cost_savings += cost_impact.get('cost_difference', 0)
            total_energy_savings += energy_impact.get('energy_difference', 0)
            total_carbon_reduction += energy_impact.get('carbon_difference', 0)
            performance_impacts.append(performance_impact.get('performance_change', 0))
        
        # Calculate overall performance impact
        avg_performance_impact = sum(performance_impacts) / len(performance_impacts) if performance_impacts else 0
        
        return {
            'total_cost_savings': total_cost_savings,
            'total_energy_savings': total_energy_savings,
            'total_carbon_reduction': total_carbon_reduction,
            'average_performance_impact': avg_performance_impact,
            'overall_assessment': 'positive' if total_cost_savings > 0 or avg_performance_impact > 0.05 else 'neutral'
        }
    
    def _generate_recommendations(self, optimizations: Dict[str, Any], bounds: Dict[str, Any], ml_focus: str) -> List[str]:
        """
        Generate optimization recommendations
        """
        recommendations = []
        
        for resource_type, optimization in optimizations.items():
            action = optimization.get('action')
            confidence = optimization.get('confidence')
            current = optimization.get('current_value')
            predicted = optimization.get('predicted_value')
            
            if action.startswith('SCALE_DOWN') and confidence > 0.80:
                recommendations.append(f"Reduce {resource_type.upper()} from {current} to {predicted} for cost savings")
            elif action.startswith('SCALE_UP') and confidence > 0.80:
                recommendations.append(f"Increase {resource_type.upper()} from {current} to {predicted} for better performance")
            elif action == 'NO_ACTION_LOW_CONFIDENCE':
                recommendations.append(f"Monitor {resource_type.upper()} - low confidence in prediction")
        
        # Focus-specific recommendations
        if ml_focus == 'performance':
            recommendations.append("Consider additional monitoring for performance-sensitive workloads")
        elif ml_focus == 'availability':
            recommendations.append("Ensure redundancy for high-availability requirements")
        elif ml_focus == 'cost':
            recommendations.append("Review spot instance opportunities for additional cost savings")
        elif ml_focus == 'energy':
            recommendations.append("Consider renewable energy regions for sustainability")
        
        return recommendations
    
    def _generate_execution_plan(self, optimizations: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        Generate execution plan for optimizations
        """
        execution_plan = []
        
        # Sort optimizations by priority (confidence and impact)
        sorted_optimizations = sorted(
            optimizations.items(),
            key=lambda x: (x[1].get('confidence', 0), abs(x[1].get('predicted_value', 0) - x[1].get('current_value', 0))),
            reverse=True
        )
        
        for resource_type, optimization in sorted_optimizations:
            action = optimization.get('action')
            if action not in ['NO_ACTION_MINIMAL_DIFFERENCE', 'NO_ACTION_LOW_CONFIDENCE']:
                execution_plan.append({
                    'resource_type': resource_type,
                    'action': action,
                    'current_value': optimization.get('current_value'),
                    'target_value': optimization.get('predicted_value'),
                    'estimated_time': optimization.get('execution_details', {}).get('estimated_time'),
                    'estimated_downtime': optimization.get('execution_details', {}).get('estimated_downtime'),
                    'complexity': optimization.get('execution_details', {}).get('complexity'),
                    'priority': 'high' if optimization.get('confidence', 0) > 0.85 else 'medium'
                })
        
        return execution_plan
    
    def _get_fallback_optimization(self, resource_type: str) -> Dict[str, Any]:
        """
        Get fallback optimization for errors
        """
        return {
            'resource_type': resource_type,
            'current_value': 0,
            'predicted_value': 0,
            'confidence': 0.0,
            'action': 'NO_ACTION_ERROR',
            'impacts': {'cost': {'cost_difference': 0}, 'energy': {'energy_difference': 0}},
            'execution_details': {'estimated_time': 'N/A', 'complexity': 'unknown'}
        }
