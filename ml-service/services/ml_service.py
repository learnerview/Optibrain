from __future__ import annotations

import asyncio
import math
from datetime import datetime
from typing import Any, Dict, List, Optional

import numpy as np

from config.settings import Settings
from utils.logger import setup_logger

logger = setup_logger(__name__)


class MLService:
    """Honest ML service surface.

    Every method states what it measured or why it could not. There are no synthetic
    fallbacks: a method that has no model and no data says so, rather than inventing a
    number.
    """

    MIN_POINTS_FORECAST = 8
    MIN_POINTS_ANOMALY = 8

    def __init__(self, settings: Settings):
        self.settings = settings
        self._feedback: List[Dict[str, Any]] = []
        self._initialized = False

    async def initialize(self) -> None:
        self._initialized = True
        logger.info("MLService initialized in honest mode (no synthetic fallbacks)")

    async def cleanup(self) -> None:
        self._initialized = False

    async def generate_forecast(
        self,
        tenant_id: str,
        metric_type: str,
        forecast_horizon: int,
        historical_data: Optional[List[float]],
    ) -> Dict[str, Any]:
        data = [float(x) for x in (historical_data or []) if isinstance(x, (int, float))]
        if len(data) < self.MIN_POINTS_FORECAST:
            return {
                "predictions": [],
                "confidence_intervals": [],
                "model_metadata": {
                    "available": False,
                    "reason": f"insufficient history ({len(data)} points; need at least {self.MIN_POINTS_FORECAST})",
                    "method": "none",
                },
                "forecast_horizon": forecast_horizon,
                "metric_type": metric_type,
            }

        last = data[-1] if data else 0.0
        predictions = [last for _ in range(max(0, forecast_horizon))]
        band = max(abs(last) * 0.10, 1e-6)
        intervals = [[p - band, p + band] for p in predictions]
        return {
            "predictions": predictions,
            "confidence_intervals": intervals,
            "model_metadata": {
                "available": True,
                "method": "persistence",
                "points_used": len(data),
                "note": "persistence model only; train a forecaster for seasonal behaviour",
            },
            "forecast_horizon": forecast_horizon,
            "metric_type": metric_type,
        }

    async def generate_spot_prediction(self, instance_type: str, region: str) -> Dict[str, Any]:
        return {
            "available": False,
            "recommendation": "UNAVAILABLE",
            "interruption_risk": None,
            "confidence": None,
            "reason": "No Spot market-data dependency is configured, so no risk score is produced.",
            "instance_type": instance_type,
            "region": region,
        }

    async def detect_anomalies(self, tenant_id: str, metrics_data: List[Any], sensitivity: float) -> Dict[str, Any]:
        rows = metrics_data or []
        if len(rows) < self.MIN_POINTS_ANOMALY:
            return {
                "anomalies": [],
                "anomaly_score": 0.0,
                "sensitivity": sensitivity,
                "total_data_points": len(rows),
                "anomaly_count": 0,
                "available": False,
                "reason": f"insufficient history ({len(rows)} points; need at least {self.MIN_POINTS_ANOMALY})",
            }

        metrics = {
            "cpu_utilization": [float(getattr(r, "cpu_utilization", 0.0) or 0.0) for r in rows],
            "memory_utilization": [float(getattr(r, "memory_utilization", 0.0) or 0.0) for r in rows],
            "hourly_cost": [float(getattr(r, "hourly_cost", 0.0) or 0.0) for r in rows],
            "instance_count": [float(getattr(r, "instance_count", 0) or 0) for r in rows],
        }

        threshold = 3.5
        if sensitivity > 0:
            threshold = max(1.5, min(3.5, 3.5 * (0.1 / sensitivity)))

        anomaly_rows: Dict[int, Dict[str, Any]] = {}
        for name, values in metrics.items():
            arr = np.asarray(values, dtype=float)
            median = float(np.median(arr))
            mad = float(np.median(np.abs(arr - median))) or 1e-9
            z = np.abs(0.6745 * (arr - median) / mad)
            for i, zi in enumerate(z):
                if zi > threshold:
                    entry = anomaly_rows.setdefault(i, {
                        "timestamp": getattr(rows[i], "timestamp", None),
                        "anomalous_metrics": [],
                        "confidence": float(min(1.0, zi / (2 * threshold))),
                        "values": {k: v[i] for k, v in metrics.items()},
                    })
                    entry["anomalous_metrics"].append(name)

        for entry in anomaly_rows.values():
            entry["severity"] = "HIGH" if entry["confidence"] >= 0.6 else "MEDIUM"
            entry["type"] = "RESOURCE_OVERLOAD" if any(m in entry["anomalous_metrics"] for m in ("cpu_utilization", "memory_utilization")) else "GENERAL_ANOMALY"

        anomalies = list(anomaly_rows.values())
        overall = float(np.mean([e["confidence"] for e in anomalies])) if anomalies else 0.0
        return {
            "anomalies": anomalies,
            "anomaly_score": overall,
            "sensitivity": sensitivity,
            "total_data_points": len(rows),
            "anomaly_count": len(anomalies),
            "available": True,
            "detection_method": "robust_zscore",
        }

    async def optimize_costs(
        self,
        tenant_id: str,
        current_metrics: Optional[Any],
        budget_constraints: Optional[Dict[str, float]],
        aws_data: Optional[Dict[str, Any]],
        generate_defaults: Optional[bool],
    ) -> Dict[str, Any]:
        actions: List[Dict[str, Any]] = []
        if current_metrics is None:
            return {
                "recommended_actions": [],
                "expected_savings": 0.0,
                "confidence": 0.0,
                "risk_assessment": "No current metrics were supplied.",
                "implementation_priority": "none",
            }

        cpu = float(getattr(current_metrics, "cpu_utilization", 0.0) or 0.0)
        memory = float(getattr(current_metrics, "memory_utilization", 0.0) or 0.0)
        cost = float(getattr(current_metrics, "hourly_cost", 0.0) or 0.0)
        if cpu < 10.0 and memory < 20.0:
            actions.append({
                "type": "RIGHTSIZE_OR_TERMINATE",
                "resource": "the observed workload",
                "rationale": f"cpu_utilization={cpu:.1f}% and memory_utilization={memory:.1f}% over the supplied window",
                "basis": "measured telemetry only; no savings figure is estimated without a baseline",
            })
        if cost > 0.0 and cpu < 5.0:
            actions.append({
                "type": "REVIEW_COST_ATTRIBUTION",
                "resource": "the observed workload",
                "rationale": f"hourly_cost={cost:.2f} with cpu_utilization={cpu:.1f}%",
                "basis": "measured telemetry only",
            })
        return {
            "recommended_actions": actions,
            "expected_savings": 0.0,
            "confidence": 0.5 if actions else 0.0,
            "risk_assessment": "deterministic rules from supplied metrics only; no commitment, inventory or telemetry baseline was provided",
            "implementation_priority": "medium" if actions else "none",
        }

    async def train_all_models(self) -> Dict[str, Any]:
        logger.info("Model training requested; no trainable model is configured in this deployment")
        return {"status": "not_configured"}

    async def get_model_status(self) -> Dict[str, Any]:
        return {
            "anomaly_detection": {"available": True, "method": "robust_zscore"},
            "forecasting": {"available": False, "reason": "persistence baseline only; train a forecaster for seasonal behavior"},
            "spot_interruption": {"available": False, "reason": "no market-data dependency configured"},
            "optimization": {"available": True, "method": "deterministic_rules"},
        }

    async def get_tenant_features(self, tenant_id: str) -> Dict[str, Any]:
        return {}

    async def record_feedback(self, tenant_id: str, prediction_id: str, actual_value: float, predicted_value: float) -> None:
        self._feedback.append({
            "tenant_id": tenant_id,
            "prediction_id": prediction_id,
            "actual_value": actual_value,
            "predicted_value": predicted_value,
            "recorded_at": datetime.utcnow().isoformat() + "Z",
        })
        if len(self._feedback) > 1000:
            self._feedback = self._feedback[-1000:]
