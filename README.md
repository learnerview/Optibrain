# OptiBrain: AI-Powered Cloud Cost Optimization & Autonomous Resource Allocation

OptiBrain is an intelligent FinOps control plane that monitors cloud spend, recommends cost-saving actions, and can autonomously execute them—safely. It operates without requiring a real AWS account by using LocalStack and simulated metrics, making it perfect for development, testing, and demonstrations.

## 🚀 Key Features

### Core Capabilities
- **📊 Telemetry Ingestion**: Pluggable providers supporting mock data, LocalStack CloudWatch, and Prometheus metrics
- **🧠 Intelligent Decision Engine**: Policy-driven scoring with trend-aware autoscaling logic and ML-powered predictions
- **🛡️ Safety Rails**: Dry-run mode, cooldowns, max-change limits, and comprehensive audit logging
- **💰 FinOps Automation**:
  - **Rightsizing**: AI-driven instance type optimization
  - **Commitment Management**: Reserved Instance & Savings Plan analysis and purchase workflows
  - **Waste Reduction**: Automated detection and cleanup of orphaned resources (EBS, IPs, Snapshots)
  - **Advanced Analytics**: Financial variance reports, unit economics, and forecast accuracy tracking
- **🔒 Security & Compliance**: Automated scanning for public buckets, open security groups, and IAM risks
- **🤖 AI Assistant**: Chat interface, anomaly detection, and forecasting powered by FastAPI Python service

### Technology Stack
- **Backend**: Spring Boot 3.2.0 (Java 21) with Spring Security, JPA, and OpenAPI documentation
- **Frontend**: Next.js 14 with TypeScript and Tailwind CSS
- **AI Service**: FastAPI with scikit-learn, TensorFlow, and PyTorch for ML capabilities
- **Database**: H2 for development (no external DB required)
- **Monitoring**: Integration-ready for Prometheus and Grafana

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   AI Service    │
│   (Next.js)     │◄──►│   (Spring Boot) │◄──►│   (FastAPI)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   LocalStack    │
                       │   (AWS Emulator)│
                       └─────────────────┘
```

## 🚀 Quick Start (Local Run)

### Prerequisites
- Java 21+ and Maven 3.6+
- Node.js 18+ and npm/yarn
- Python 3.10+ and pip

### Running the Project

#### 1. Start Backend Service
```bash
cd backend
./mvnw spring-boot:run
```
The backend will start on `http://localhost:8080` with an H2 in-memory database.

#### 2. Start AI Service
```bash
cd services/ai-service
pip install -r requirements.txt
python main.py
```
The AI service will start on `http://localhost:8000`.

#### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
Access the dashboard at `http://localhost:3000`.

## 🔧 Configuration & Modes

### Cloud Operation Modes
- **`cloud.mode=MOCK`**: Fully simulated metrics (default)
- **`cloud.mode=LOCALSTACK`**: Pulls from LocalStack CloudWatch (requires LocalStack running locally)
- **`cloud.mode=AWS`**: Real AWS integration (requires credentials)

### Safety Configuration
- **`cloud.dry-run=true`**: Actions are logged but not executed (default)
- **Cooldowns**: Same action blocked within 10 minutes
- **Max-change limits**: At most 5 executions per hour
- **Audit logging**: Immutable trail of every decision and action

## 📡 Key API Endpoints

### Core Operations
```bash
# Get current telemetry data
GET /metrics/current

# Evaluate and score decisions
POST /decisions/evaluate

# Execute autoscaling actions with safety checks
POST /autoscaling/evaluate-and-act

# Generate cost optimization recommendations
POST /recommendations/generate
```

### Financial Operations
```bash
# Get comprehensive savings report
GET /savings/summary

# Reserved Instances & Savings Plans
GET /api/ri/recommend/{tenantId}      # Get RI recommendations
```

### AI Assistant
```bash
# Chat with AI assistant
POST /chat

# Get predictions and forecasts
POST /ai/predict
POST /ai/forecast
```

## 🛡️ Safety & Governance

OptiBrain includes multiple layers of safety to prevent unintended cloud changes:

- **Dry-run Mode**: All actions are simulated by default
- **Cooldown Periods**: Prevents rapid repeated actions
- **Execution Limits**: Caps the number of changes per hour
- **Audit Trail**: Complete, immutable logging of all operations
- **Approval Workflow**: Human approval required for execution


### Security Considerations
- **JWT Secret**: Never use the default secret in production.
- **CORS**: Always restrict to known origins.
- **Database**: Use `spring.jpa.hibernate.ddl-auto=validate` (default in `prod` profile) to prevent data loss.
- **Actuator**: Review `/actuator` visibility; production defaults are restrictive.

## 🔧 Development

### Running Tests
```bash
cd backend
./mvnw test

# Run specific test categories
./mvnw test -Dtest="*UnitTest"
./mvnw test -Dtest="*IntegrationTest"
```

### Code Quality
```bash
# Run with Maven wrapper
./mvnw clean compile
./mvnw spring-boot:run

# Check dependencies
./mvnw dependency:tree
```

### API Documentation
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

## 📊 Monitoring & Observability

### Prometheus Metrics
- Application metrics exposed at `/actuator/prometheus`
- Custom business metrics for decisions, recommendations, and savings
- JVM metrics for performance monitoring

### Grafana Dashboards
- Pre-configured dashboards for cost optimization
- Autoscaling activity visualization
- AI service performance metrics

### Logging
- Structured logging with correlation IDs
- Configurable log levels (DEBUG, INFO, WARN, ERROR)
- Audit trail for compliance

## 🔄 Extending OptiBrain

### Adding New Telemetry Providers
Implement the `MetricsProvider` interface:
```java
@Component
public class CustomMetricsProvider implements MetricsProvider {
    public List<MetricData> getCurrentMetrics() { ... }
}
```

### Adding Cloud Adapters
Implement the `CloudAdapter` interface:
```java
@Component
public class CustomCloudAdapter implements CloudAdapter {
    public void executeAction(CloudAction action) { ... }
}
```

### Extending Pricing Data
Update `pricing-ec2-us-east-1.csv` with new instance types or regions.

### Custom Recommendation Logic
Extend `RecommendationService` with new heuristics and algorithms.

## 📚 Documentation

- [LocalStack Setup Guide](README-LOCALSTACK.md)
- [Telemetry Provider Configuration](README-TELEMETRY.md)
- [Autoscaling & Safety Features](README-AUTOSCALING.md)
- [FinOps & Approval Workflow](README-FINOPS.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- **Source Code**: https://github.com/your-org/optibrain
- **Issues**: https://github.com/your-org/optibrain/issues
- **Documentation**: https://docs.optibrain.io
- **Community**: https://discuss.optibrain.io

---

**Built with ❤️ for the cloud cost optimization community**
