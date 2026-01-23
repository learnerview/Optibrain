# OptiBrain: AI-Powered Cloud Cost Optimization

OptiBrain is a comprehensive FinOps and cloud resource management platform. It uses machine learning to monitor cloud spending, identify waste, and provide autonomous or human-approved cost-saving recommendations.

## 🚀 Project Overview

The system is composed of three primary services:
1.  **Backend (Spring Boot)**: The core engine that handles business logic, security, and cloud infrastructure integration.
2.  **Frontend (Next.js)**: A modern web interface for visualizing cloud metrics and managing cost-optimization workflows.
3.  **ML-Service (FastAPI)**: An AI/ML layer for analyzing usage patterns, forecasting costs, and predicting spot instance availability.

## 🛠️ Key Capabilities

- **📊 Intelligent Analytics**: Cost variance reporting and unit economic analysis.
- **🛡️ Automation Rails**: Safety-first autonomous resource allocation with dry-run and cooldown support.
- **💰 FinOps Toolkit**: 
    - Rightsizing recommendations for EC2/ASG.
    - Orphaned resource detection (EBS, EIP, Snapshots).
    - ROI reporting and savings summaries.
- **🧠 AI Insights**: Anomaly detection, forecasting, and spot instance prediction.
- **🔒 Security Scanning**: Automatic detection of misconfigured cloud resources.

## 💻 Tech Stack

- **Backend**: Java 21, Spring Boot 3.2.0, Spring Security, JPA/Hibernate, H2 Database, Maven.
- **Frontend**: Next.js 14, TypeScript, Tailwind CSS, Radix UI.
- **ML Service**: Python 3.10+, FastAPI, PyTorch, TensorFlow, scikit-learn.
- **Infrastructure**: AWS SDK v2, LocalStack (for local emulation).

## ⚡ Quick Start (Manual Setup)

Follow these steps to run the complete project locally. **Docker is not used in this environment.**

### Prerequisites
- **Java**: 21 or higher
- **Node.js**: 18.x or higher
- **Python**: 3.10 or higher
- **Maven**: 3.6 or higher

### 1. Launch Backend
Navigate to the backend directory and run the Spring Boot application using the Maven wrapper:
```bash
cd backend
./mvnw spring-boot:run
```
*Accessible at: `http://localhost:8080`*

### 2. Launch ML-Service
Setup a virtual environment, install dependencies, and start the FastAPI server:
```bash
cd ml-service
pip install -r requirements.txt
python main.py
```
*Accessible at: `http://localhost:8000`*

### 3. Launch Frontend
Install node dependencies and start the Next.js development server:
```bash
cd frontend
npm install
npm run dev
```
*Dashboard available at: `http://localhost:3000`*

## ⚙️ Configuration

The system supports multiple modes via `application.properties`:
- **Mode**: `MOCK` (Default), `LOCALSTACK`, or `AWS`.
- **Dry Run**: Set `cloud.dry-run=true` to simulate actions without modifying resources.

## 🔒 Security
Authentication is handled via JWT. The system includes built-in roles for Financial Analysts, DevOps Engineers, and System Administrators.

---
**Built for the Open-Source FinOps Community.**
