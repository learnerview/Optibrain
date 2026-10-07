import asyncio
from services.ml_service import MLService
from config.settings import Settings


def run_optimize(tenant_id, current_metrics, aws_data, generate_defaults):
    settings = Settings()
    ml_service = MLService(settings)
    ml_service.session = "mock_session"
    return asyncio.run(
        ml_service.optimize_costs(
            tenant_id=tenant_id,
            current_metrics=current_metrics,
            budget_constraints=None,
            aws_data=aws_data,
            generate_defaults=generate_defaults,
        )
    )


def test_optimize_costs_does_not_fabricate_config():
    aws_data = {
        "resources": [{"id": "i-1"}, {"id": "i-2"}],
        "costData": {"monthlyCost": 1500.0},
        "tags": {"commonTags": ["env:prod", "team:cloud_intelligence"]},
    }

    result = run_optimize("test-tenant-dynamic", None, aws_data, True)

    for fabricated_key in (
        "optimization_bounds",
        "decision_engine",
        "autonomous_settings",
        "user_permissions",
        "alert_thresholds",
    ):
        assert fabricated_key not in result

    assert result["recommended_actions"] == []
    assert result["expected_savings"] == 0.0
    assert result["confidence"] == 0.0
    assert "No current metrics were supplied" in result["risk_assessment"]


def test_optimize_costs_reports_measured_idle_workload():
    class Metrics:
        cpu_utilization = 4.0
        memory_utilization = 12.0
        hourly_cost = 0.35

    result = run_optimize("test-tenant-idle", Metrics(), None, False)

    assert any(a["type"] == "RIGHTSIZE_OR_TERMINATE" for a in result["recommended_actions"])
    assert result["expected_savings"] == 0.0
    assert result["confidence"] == 0.5
    assert "supplied metrics only" in result["risk_assessment"]


if __name__ == "__main__":
    test_optimize_costs_does_not_fabricate_config()
    test_optimize_costs_reports_measured_idle_workload()