# OptiBrain ML Service

FastAPI service providing forecasting and anomaly detection for the OptiBrain backend.

## Data honesty

The service distinguishes three outcomes for every prediction and reports which applies.

| Outcome | Meaning |
|---|---|
| A prediction | Fitted over supplied history |
| `available: false` | History is insufficient to fit a model |
| Failure | The operation raised; the reason is returned |

A missing forecast returns an empty prediction list with a reason. It never returns a
flat or default-valued series, because a consumer cannot distinguish that from a real
forecast.

`DeepLearningForecaster` holds no trained model and reports itself unavailable. It
exposes no `accuracy_score`, because no accuracy is measured.

`SpotPredictor` returns `risk_score: null`. AWS exposes no public API for spot market
depth or interruption notices, so no verdict is produced.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Liveness |
| POST | `/predict/forecast` | Time series forecast |
| POST | `/predict/spot` | Spot interruption availability |
| POST | `/detect/anomalies` | Outlier detection |
| POST | `/optimize/cost` | Cost optimisation analysis |
| POST | `/train/models` | Trigger training |
| GET | `/models/status` | Model state |
| GET | `/features/{tenant_id}` | Engineered features |
| POST | `/chat/intelligent` | Redirects to the backend copilot |

## Running

```bash
python -m pip install -r requirements.txt
python -m uvicorn main:app --port 8000
```

`JAVA_BACKEND_URL` points at the Spring backend. The service reads history from it and
returns nothing when the request fails.

## Requirements

Python 3.14. The pin on `numpy>=2` is required: NumPy 1.x publishes no cp314 wheels and
pip falls back to a source build that fails. `prophet` and `tensorflow` are excluded from
`requirements.txt` because no module imports them; see the comments in that file for the
reasoning and the conditions for reintroducing each.