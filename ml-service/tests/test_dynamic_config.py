import pytest
import asyncio
from services.ml_service import MLService
from config.settings import Settings

@pytest.mark.asyncio
async def test_generate_dynamic_configuration():
    # Setup
    settings = Settings()
    ml_service = MLService(settings)
    # Mock initializing to avoid external calls
    ml_service.session = "mock_session" 
    
    # Mock AWS Data
    aws_data = {
        "resources": [{"id": "i-1"}, {"id": "i-2"}],
        "costData": {"monthlyCost": 1500.0},
        "tags": {"commonTags": ["env:prod", "team:finops"]}
    }
    
    tenant_id = "test-tenant-dynamic"
    
    # Execute
    result = await ml_service.optimize_costs(
        tenant_id=tenant_id,
        current_metrics=None,
        aws_data=aws_data,
        generate_defaults=True
    )
    
    # Verify Structure
    assert "optimization_bounds" in result
    assert "decision_engine" in result
    assert "autonomous_settings" in result
    assert "user_permissions" in result
    assert "alert_thresholds" in result
    
    # Verify Logic (High Value / Prod)
    assert result["optimization_bounds"]["recommended_budget"] == 1500.0 * 1.2
    assert result["decision_engine"]["require_human_approval"] is True # > 1000 cost
    assert result["alert_thresholds"]["cost_spike_threshold"] == 1.5 # High value
    
    print("Dynamic Configuration Test Passed!")

if __name__ == "__main__":
    # Allow running directly
    loop = asyncio.get_event_loop()
    loop.run_until_complete(test_generate_dynamic_configuration())
