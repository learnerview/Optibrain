# OptiBrain: AI-Powered Cloud Cost Intelligence Platform

OptiBrain is an AI-powered Cloud Cost Intelligence Platform that analyzes cloud usage patterns, detects anomalies, and provides intelligent cost optimization recommendations.

## 🏗️ System Architecture

OptiBrain is built as a distributed three-tier system:

*   **⚡ Cost Intelligence Backend**: A robust Spring Boot service managing the core business logic, AWS service integrations (via LocalStack), audit logging, and security.
*   **🎨 Analytics Frontend**: A Next.js-powered dashboard providing executive visibility into cloud spend, recommendation management, and system health.
*   **🧠 AI Prediction Service**: A FastAPI-based machine learning engine specialized in resource usage forecasting, spot price prediction, and anomaly detection.

---

## 🚀 Core Features

### 1. Cloud Cost Analytics
- **Cost Variance Analysis**: Detect and report on budget deviations in real-time.
- **Unit Economics**: Track cloud costs relative to business KPIs.
- **ROI Tracking**: Visualize the financial impact of automated optimizations.

### 2. Autonomous Resource Optimization
- **Intelligent Rightsizing**: AI-driven instance type recommendations and automated resizing.
- **Waste Elimination**: Detection and cleanup of unattached EBS volumes, elastic IPs, and orphaned snapshots.
- **Savings Plans/RI Management**: Automated purchase recommendations for reserved capacity.

### 3. AI & Prediction Engine
- **Anomaly Detection**: identifying unusual spending patterns before they impact the budget.
- **Cost Forecasting**: Deep learning-based predictions for future cloud expenditures.
- **Spot Predictor**: Predicting spot instance termination risks for safe workload allocation.

### 4. Safety & Governance
- **Dry-Run Mode**: Test optimizations without affecting real resources.
- **Policy-Driven Safety**: Integrated cooldown periods and change-rate limits.
- **Audit Logging**: Comprehensive, immutable trail of every decision and action.

---

## 🛠️ Technology Stack

| Component | technologies |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.2, Spring Security, JPA/Hibernate, H2, Maven |
| **Frontend** | Next.js 14, TypeScript, Tailwind CSS, Shadcn UI, Radix |
| **AI Service** | Python 3.10+, FastAPI, PyTorch, TensorFlow, Scikit-learn, Pandas |
| **Infrastructure** | AWS SDK v2, LocalStack (Emulation), OpenAPI/Swagger |

---

## 📂 Project Structure

### Backend (`/backend`)
- `com.optibrain.analytics`: Logic for financial reporting and unit economics.
- `com.optibrain.autonomous`: Advanced automation services and real-time monitoring.
- `com.optibrain.autoscaling`: Scaling logic and spot instance management.
- `com.optibrain.cleanup`: Scheduled tasks for resource waste removal.
- `com.optibrain.decision`: The core engine for evaluating and scoring cloud actions.
- `com.optibrain.metrics`: Telemetry ingestion and metric transformation.

### Frontend (`/frontend`)
- `app/`: Next.js App Router for layout and routing.
- `components/`: Modular UI units (Auth, Dashboard, Metrics).
- `lib/`: Shared utilities and API client implementations.
- `public/`: Static assets and themes.

### ML Service (`/ml-service`)
- `services/`: Core ML logic (Anomaly detection, forecasting, model management).
- `models/`: Pre-trained and persistent ML model storage.
- `utils/`: Data processing and feature engineering utilities.

---

## 🚦 Local Setup & Installation

**Important**: This project is optimized for local execution using LocalStack. Docker is not required for this setup.

### Prerequisites
- **JDK 21**
- **Node.js 18+**
- **Python 3.10+**
- **Maven 3.6+**

### Step 1: Start the Backend
```bash
cd backend
./mvnw spring-boot:run
```
*Backend initializes on port 8080 with H2 Console at `/h2-console`.*

### Step 2: Start the AI Service
```bash
cd ml-service
# Recommended: Create a virtualenv
pip install -r requirements.txt
python main.py
```
*FastAPI server starts on port 8000. API Docs at `/docs`.*

### Step 3: Start the Frontend
```bash
cd frontend
npm install
npm run dev
```
*Frontend available at `http://localhost:3000`.*

---

## 📡 API Reference

### Metrics & Decisions
- `GET /metrics/current`: Retrieve live telemetry.
- `POST /decisions/evaluate`: Trigger the decision engine for current state.
- `POST /autoscaling/evaluate-and-act`: Execute automated scaling.

### Recommendations
- `POST /recommendations/generate`: Build new cost-saving proposals.
- `GET /recommendations/pending`: Review and approve pending actions.

### AI Endpoints
- `POST /ai/predict`: Get future usage predictions.
- `POST /ai/forecast`: Generate long-term budget forecasts.

---

## 🤝 Contributing
Contributions are welcome! Please ensure all code adheres to the project's security and testing standards. LocalStack should be used for all feature development.

## 📄 License
This project is licensed under the MIT License.
