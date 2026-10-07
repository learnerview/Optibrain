# OptiBrain ML Service

FastAPI service (`ml-service/`) that exposes forecasting, anomaly detection and
cost-optimisation surfaces for the OptiBrain backend. It runs as a separate process on
port 8000.

## Data honesty

The service distinguishes three outcomes for every operation and reports which applies:

| Outcome | Meaning |
|---|---|
| A result | Computed over the supplied history |
| `available: false` / explicit reason | No model or no data - the reason is returned |
| Failure | The operation raised; `500` with the reason |

The service holds **no trained forecasting model** and no external data feeds. Whenever a
feature cannot be delivered honestly, it says so instead of inventing a number:

- `/models/status` reports forecasting `available: false` ("persistence baseline only;
  train a forecaster for seasonal behavior") and spot interruption `available: false`
  ("no market-data dependency configured").
- `/predict/spot` returns `recommendation: "UNAVAILABLE"` with `interruption_risk: null`
  and `confidence: null` - AWS exposes no public API for spot market depth, so no risk
  score is produced.
- `/chat/intelligent` does not host a chat LLM and does not redirect: it returns an honest
  message pointing at the backend's `/api/copilot/chat`.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Liveness (public) |
| POST | `/predict/forecast` | Time-series forecast over supplied history |
| POST | `/predict/spot` | Spot interruption availability (always unavailable) |
| POST | `/detect/anomalies` | Robust z-score outlier detection |
| POST | `/optimize/cost` | Deterministic-rule cost optimisation analysis |
| POST | `/train/models` | Training request (no trainable model configured) |
| GET | `/models/status` | Model availability |
| GET | `/features/{tenant_id}` | Engineered features (empty) |
| POST | `/feedback` | In-memory feedback capture |
| POST | `/chat/intelligent`, `/chat/message`, `WS /ws/chat/{user_id}` | Honest "served by backend" answers |

`GET /docs`, `/openapi.json` and `/redoc` are public; every other route requires the
`X-Api-Key` header.

## Endpoint behaviour

**`POST /predict/forecast`** - body `{ tenant_id, metric_type, forecast_horizon (1..720),
historical_data }`. With fewer than eight numeric points it returns `available: false`
with `reason: "insufficient history (N points; need at least 8)"` and empty predictions.
With eight or more it returns a persistence forecast - the last observed value repeated -
with a ±10% confidence band, because the service has no trained forecaster. The metadata
states `method: "persistence"` and the note "persistence model only; train a forecaster
for seasonal behaviour".

**`POST /predict/spot`** - always returns `available: false`, `recommendation:
"UNAVAILABLE"`, null `interruption_risk` / `confidence`, and `reason: "No Spot
market-data dependency is configured, so no risk score is produced."`

**`POST /detect/anomalies`** — body `{ tenant_id, metrics_data[], sensitivity (0.001..1.0,
default 0.1) }`. Runs a robust z-score per metric (`cpu_utilization`,
`memory_utilization`, `hourly_cost`, `instance_count`): points are flagged when
`abs(0.6745 * (x - median) / MAD) > threshold`, where `threshold = clamp(3.5 * 0.1 /
sensitivity, 1.5, 3.5)`. With fewer than eight points it returns `available: false` with a
reason. Anomalies carry `anomalous_metrics`, `confidence`, `values`, `severity`
(`HIGH` when confidence ≥ 0.6 else `MEDIUM`) and `type`
(`RESOURCE_OVERLOAD` vs `GENERAL_ANOMALY`). Note the wire response model serialises
`anomalies`, `anomaly_score`, `sensitivity`, `total_data_points`, `anomaly_count` - the
`available`/`reason`/`detection_method` fields computed internally are not exposed on the
wire.

**`POST /optimize/cost`** — body `{ tenant_id, current_metrics?, budget_constraints?,
aws_data?, analysis_type?, generate_defaults? }`. Without `current_metrics` it returns
empty `recommended_actions`, `expected_savings: 0.0`, `confidence: 0.0` and
`risk_assessment: "No current metrics were supplied."`. With metrics it applies
deterministic rules (idle resource under 10% CPU / 20% memory; cost with very low CPU) and
never estimates a saving (`expected_savings` stays `0.0`; `confidence` is `0.5` when a
rule fires, else `0.0`).

**`POST /train/models`** — responds `{ "message": "Model training started", "status":
"in_progress" }`; the underlying `train_all_models` returns `{ "status":
"not_configured" }` and that result is discarded. There is no trainable model in this
deployment.

**`GET /models/status`** — returns:

```json
{
  "anomaly_detection": { "available": true, "method": "robust_zscore" },
  "forecasting": { "available": false, "reason": "persistence baseline only; train a forecaster for seasonal behavior" },
  "spot_interruption": { "available": false, "reason": "no market-data dependency configured" },
  "optimization": { "available": true, "method": "deterministic_rules" }
}
```

**`GET /features/{tenant_id}`** — returns `{ "tenant_id": ..., "features": {} }`; no
feature engineering is implemented.

**`POST /feedback`** — appends feedback to an in-memory list (capped at 1000) and
confirms.

**Chat routes** — `/chat/intelligent`, `/chat/message` and the WebSocket route all state
that chat/copilot is served by the backend's `/api/copilot/chat`, never a fabricated
conversation. `JAVA_BACKEND_URL` is not used.

## API key

Every route except `/health`, `/docs`, `/openapi.json` and `/redoc` requires the
`X-Api-Key` header matching `ML_SERVICE_API_KEY` (default `dev-local-ml-api-key`). Known
defect: the key check raises `HTTPException(401)` from the HTTP middleware, outside
Starlette's exception handling, so a missing or invalid key currently surfaces as
`500 "Internal Server Error"` rather than `401`. The backend sends the key configured by
`ml.service.api-key`; the same default value is used in development.

## CORS

Allowed origins: `http://localhost:8080`, `http://optibrain-backend:8080` and
`http://localhost:3000`; allowed methods `GET`, `POST`.

## Running

```bash
python -m pip install -r requirements.txt
python -m uvicorn main:app --port 8000
```

The backend's `run-all.ps1` (and its `run-all.sh` shim) starts this exact command. The
backend reaches the service at `ml.service.url` (`http://localhost:8000`) through
`PyBridgeService`, which degrades to an explicit unavailable result when the service is
down - the platform does not depend on it.

## Requirements

Python 3.14. The pin on `numpy>=2` is required: NumPy 1.x publishes no cp314 wheels, so
pip falls back to a source build that fails. `prophet` and `tensorflow` are excluded from
`requirements.txt` because no module imports them; the comments in that file explain the
conditions for reintroducing each. The modules `forecasting_service.py`, `chat_service.py`,
`anomaly_detection.py`, `model_manager.py` and `feature_engineering.py` exist in
`services/` but are not imported by `main.py` or the `MLService` used at runtime.