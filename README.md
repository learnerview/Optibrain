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
- **AI Service**: FastAPI with scikit-learn, TensorFlow, and PyTorch for ML capabilities
- **Database**: H2 for development, configurable for production databases
- **Cloud Integration**: AWS SDK v2 with LocalStack for development
- **Monitoring**: Prometheus integration with Grafana dashboards
- **Containerization**: Docker and Docker Compose for easy deployment

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   AI Service    │
│   (Vanilla JS)  │◄──►│   (Spring Boot) │◄──►│   (FastAPI)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   LocalStack    │
                       │   (AWS Emulator)│
                       └─────────────────┘
```

## 🚀 Quick Start (No AWS Required)

### Prerequisites
- Docker and Docker Compose
- Java 21+ and Maven 3.6+
- Python 3.10+ and pip

### One-Command Demo
```bash
# Make the demo script executable and run it
chmod +x scripts/demo.sh
./scripts/demo.sh
```

This command will:
1. Start LocalStack with AWS services
2. Seed mock EC2/ASG resources
3. Start the backend service
4. Start the AI service
5. Run demonstration API calls
6. Open the web interface

### Manual Setup

#### 1. Start LocalStack
```bash
docker compose up -d localstack
```

#### 2. Seed Mock Resources
```bash
./scripts/seed-localstack.sh
./scripts/seed-asg-localstack.sh
```

#### 3. Start Backend Service
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=localstack
```

#### 4. Start AI Service
```bash
cd ../services/ai-service
pip install -r requirements.txt
python main.py
```

#### 5. Access the Interface
Open `frontend/public/index.html` in your browser or use the API endpoints directly.

## 🔧 Configuration & Modes

### Cloud Operation Modes
- **`cloud.mode=MOCK`**: Fully simulated metrics (default)
- **`cloud.mode=LOCALSTACK`**: Pulls from LocalStack CloudWatch
- **`cloud.mode=PROMETHEUS`**: Queries Prometheus for metrics
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

# Get pending recommendations for approval
GET /recommendations/pending

# Approve/reject/execute recommendations
POST /recommendations/{id}/approve
POST /recommendations/{id}/reject
POST /recommendations/{id}/execute
```

### Financial Operations
```bash
# Get comprehensive savings report
GET /savings/summary

# Reserved Instances & Savings Plans
GET /api/ri/recommend/{tenantId}      # Get RI recommendations
POST /api/ri/purchase/{tenantId}      # Submit purchase request

# Advanced Analytics
GET /api/analytics/report/{tenantId}  # Get financial variance & unit cost report
```

### Resource Management & Security
```bash
# Orphaned Resource Cleanup
GET /api/cleanup/recommendations/{tenantId}  # Detect unused volumes, IPs, etc.
POST /api/cleanup/execute/{tenantId}/{id}    # Execute cleanup (supports dry-run)

# Security Scanning
GET /api/security/scan/{tenantId}            # Scan for vulnerabilities
POST /api/security/remediate/{findingId}     # Auto-remediate security issues
```

### Audit Trail
```bash
GET /audit/recent
GET /audit/decision/{decisionId}
```

### AI Assistant
```bash
# Chat with AI assistant
POST /chat

# Get predictions and forecasts
POST /ai/predict
POST /ai/forecast
```

### System Health
```bash
# Application health
GET /actuator/health
GET /actuator/info
GET /actuator/metrics

# API Documentation
GET /swagger-ui.html
```

## 🛡️ Safety & Governance

OptiBrain includes multiple layers of safety to prevent unintended cloud changes:

### Safety Rails
- **Dry-run Mode**: All actions are simulated by default
- **Cooldown Periods**: Prevents rapid repeated actions
- **Execution Limits**: Caps the number of changes per hour
- **Audit Trail**: Complete, immutable logging of all operations
- **Approval Workflow**: Human approval required for execution

### Security Features
- JWT-based authentication
- Role-based access control
- Encrypted configuration
- Secure API endpoints

## 🐳 Docker Deployment

### Basic Setup
```bash
docker compose up -d
```

### Enhanced Stack (Recommended)
```bash
docker compose -f docker-compose.enhanced.yml up -d
```

The enhanced stack includes:
- LocalStack for AWS emulation
- Backend service with health checks
- AI service with model persistence
- Prometheus for metrics collection
- Grafana for visualization
- Redis for caching

## 🚀 Production Deployment

### Prerequisites
- A PostgreSQL database instance
- A Redis instance
- Environment variables configured (see below)

### Deployment Steps
1. **Configure Environment**:
   Set the following essential environment variables:
   - `SPRING_PROFILES_ACTIVE=prod,localstack` (assuming LocalStack for development/demo, or just `prod` for real AWS)
   - `JWT_SECRET`: A long, random string for JWT signing
   - `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`: PostgreSQL connection details
   - `CORS_ALLOWED_ORIGINS`: Comma-separated list of allowed frontend origins (e.g., `https://your-frontend-domain.com`)

2. **Build the Application**:
   ```bash
   cd backend
   ./mvnw clean package -DskipTests
   ```

3. **Run with Docker Compose**:
   ```bash
   docker compose -f docker-compose.enhanced.yml up -d
   ```

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
