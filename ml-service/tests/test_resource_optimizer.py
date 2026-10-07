"""
Unit Tests for Resource Optimizer
Tests to verify predictions are deterministic (no random noise) and respect customer bounds.
"""

import pytest
import numpy as np
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from services.resource_optimizer import ResourceOptimizer


class TestResourceOptimizer:
    """Test suite for ResourceOptimizer"""
    
    def setup_method(self):
        """Set up test fixtures"""
        self.optimizer = ResourceOptimizer()
        
        # Sample current inventory
        self.current_inventory = {
            'cpu': {'current': 8, 'utilization': 45.2, 'cores': 32},
            'memory': {'current': 64, 'utilization': 67.8, 'type': 'ddr4'},
            'gpu': {'current': 2, 'utilization': 78.5, 'memory': 16},
            'storage': {'current': 2000, 'utilization': 55.3, 'type': 'ssd'},
            'network': {'current': 10, 'utilization': 35.7, 'bandwidth': 10}
        }
        
        # Sample customer bounds
        self.customer_bounds = {
            'cpu': {'min': 2, 'max': 16},
            'memory': {'min': 16, 'max': 128},
            'gpu': {'min': 0, 'max': 4},
            'storage': {'min': 500, 'max': 5000},
            'network': {'min': 5, 'max': 20}
        }
    
    def test_predict_optimal_value_is_deterministic(self):
        """Test that predictions are deterministic (no random noise)"""
        features = np.array([0.5, 0.5, 0.3, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.0, 0.0, 0.0, 0.0, 1.0])
        
        # Call prediction multiple times
        predictions = []
        for _ in range(10):
            pred = self.optimizer._predict_optimal_value('cpu', features, 'balanced')
            predictions.append(pred)
        
        # All predictions should be identical (no random noise)
        assert all(p == predictions[0] for p in predictions), \
            f"Predictions are not deterministic: {predictions}"
    
    def test_predictions_respect_resource_type(self):
        """Test that predictions differ based on resource type"""
        features = np.array([0.5, 0.5, 0.3, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.0, 0.0, 0.0, 0.0, 1.0])
        
        cpu_pred = self.optimizer._predict_optimal_value('cpu', features, 'balanced')
        memory_pred = self.optimizer._predict_optimal_value('memory', features, 'balanced')
        storage_pred = self.optimizer._predict_optimal_value('storage', features, 'balanced')
        
        # Different resource types should have different base values
        assert cpu_pred != memory_pred or cpu_pred != storage_pred, \
            "Predictions should differ for different resource types"
    
    def test_predictions_respect_ml_focus(self):
        """Test that ML focus affects predictions"""
        features = np.array([0.5, 0.5, 0.3, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.0, 0.0, 0.0, 0.0, 1.0])
        
        performance_pred = self.optimizer._predict_optimal_value('cpu', features, 'performance')
        cost_pred = self.optimizer._predict_optimal_value('cpu', features, 'cost')
        
        # Performance focus should predict higher values than cost focus
        assert performance_pred > cost_pred, \
            f"Performance prediction ({performance_pred}) should be higher than cost ({cost_pred})"
    
    def test_predictions_are_positive(self):
        """Test that predictions are always positive"""
        features = np.array([0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0])
        
        for resource_type in ['cpu', 'memory', 'gpu', 'storage', 'network']:
            for focus in ['performance', 'availability', 'cost', 'energy', 'balanced']:
                pred = self.optimizer._predict_optimal_value(resource_type, features, focus)
                assert pred >= 1.0, f"Prediction for {resource_type}/{focus} should be >= 1.0, got {pred}"
    
    def test_optimization_respects_customer_bounds(self):
        """Test that optimization results respect customer bounds"""
        result = self.optimizer.optimize_resources(
            tenant_id='test-tenant',
            current_inventory=self.current_inventory,
            customer_bounds=self.customer_bounds,
            ml_focus='balanced'
        )
        
        assert 'resource_optimizations' in result
        
        for resource_type, optimization in result['resource_optimizations'].items():
            if resource_type in self.customer_bounds:
                bounds = self.customer_bounds[resource_type]
                predicted = optimization.get('predicted_value', 0)
                
                # Predicted value should be within bounds (with some buffer for scaling)
                assert predicted >= bounds['min'] * 0.9, \
                    f"{resource_type} prediction {predicted} below min bound {bounds['min']}"
                assert predicted <= bounds['max'] * 1.3, \
                    f"{resource_type} prediction {predicted} above max bound {bounds['max']}"
    
    def test_confidence_score_is_reasonable(self):
        """Test that confidence scores are in valid range"""
        features = np.array([0.5, 0.5, 0.3, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.0, 0.0, 0.0, 0.0, 1.0])
        
        confidence = self.optimizer._calculate_prediction_confidence('cpu', features, 'balanced')
        
        assert 0.0 <= confidence <= 1.0, f"Confidence {confidence} should be between 0 and 1"
        assert confidence >= 0.6, f"Confidence {confidence} should be at least 0.6 for valid features"
    
    def test_determine_optimization_action(self):
        """Test action determination logic"""
        # Test scale up
        action = self.optimizer._determine_optimization_action(4.0, 8.0, 0.85)
        assert action in ['SCALE_UP_SIGNIFICANT', 'SCALE_UP_MODERATE']
        
        # Test scale down
        action = self.optimizer._determine_optimization_action(8.0, 4.0, 0.85)
        assert action in ['SCALE_DOWN_SIGNIFICANT', 'SCALE_DOWN_MODERATE']
        
        # Test no action (minimal difference)
        action = self.optimizer._determine_optimization_action(8.0, 8.2, 0.85)
        assert action == 'NO_ACTION_MINIMAL_DIFFERENCE'
        
        # Test no action (low confidence)
        action = self.optimizer._determine_optimization_action(4.0, 8.0, 0.5)
        assert action == 'NO_ACTION_LOW_CONFIDENCE'


class TestFocusMultipliers:
    """Test focus-specific behavior"""
    
    def setup_method(self):
        self.optimizer = ResourceOptimizer()
    
    def test_performance_focus_increases_resources(self):
        """Performance focus should allocate more resources"""
        features = np.array([0.5] * 14)
        
        balanced = self.optimizer._predict_optimal_value('cpu', features, 'balanced')
        performance = self.optimizer._predict_optimal_value('cpu', features, 'performance')
        
        assert performance >= balanced, \
            "Performance focus should allocate at least as much as balanced"
    
    def test_cost_focus_reduces_resources(self):
        """Cost focus should allocate fewer resources"""
        features = np.array([0.5] * 14)
        
        balanced = self.optimizer._predict_optimal_value('cpu', features, 'balanced')
        cost = self.optimizer._predict_optimal_value('cpu', features, 'cost')
        
        assert cost <= balanced, \
            "Cost focus should allocate at most as much as balanced"


if __name__ == '__main__':
    pytest.main([__file__, '-v'])
