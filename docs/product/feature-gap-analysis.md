# Feature Gap Analysis

Assessment of OptiBrain against the 2026 cloud cost management market. Demand is rated 1
to 5, where 5 indicates a capability expected of any product at this category.

## Implemented

| Capability | Notes |
|---|---|
| Resource inventory | Twelve resource types across EC2, EBS, networking, RDS, autoscaling and serverless (Lambda, DynamoDB, S3, SQS) |
| Cost attribution | Service, region, linked account, daily series |
| Telemetry | Any CloudWatch namespace and dimension |
| Remediation | Ten action types with risk levels, dry-run, protection guard |
| Commitment coverage | Measured via Cost Explorer |
| Anomaly detection | Rolling median with median absolute deviation |
| Forecasting | Statistical, reports unavailable without history |
| Copilot | Answers from measurement, refuses otherwise |
| Tenant management | Full CRUD, optimistic locking, soft delete |
| Audit trail | Every remediation recorded |
| Sandbox | LocalStack, identical code path |

## Not implemented

| Capability | Demand | Current state | Assessment |
|---|---|---|---|
| Commitment management and purchasing | 5 | Coverage measured; no recommendations | Largest untapped pool. Advisory engine is the next backlog item |
| Serverless cost | 5 | Absent | A blind spot on a typical bill. `MetricQuery` needs no change |
| Data transfer and network cost | 5 | Absent | A top cause of billing surprise |
| Chargeback and showback | 5 | Absent | Requires the cost fact table first |
| Unit economics | 5 | Absent | The 2026 differentiator. Customers supply the unit metric |
| Tag governance | 5 | Absent | Force multiplier for allocation and budgets |
| Budgets and alerting | 5 | Absent | Highest perceived value per engineering hour |
| Anomaly root-cause attribution | 5 | Detection only | An unexplained alert gets muted |
| AI and GPU cost | 5 | GPU instances detected as a type; no attribution | Fastest-growing line item. AWS exposes four attribution paths |
| Approval workflows and RBAC | 5 | Binary global dry-run | Enterprise procurement gate |
| Single sign-on | 5 | Form login only | Enterprise procurement gate |
| Integrations | 4 | Absent | Where recommendations otherwise die |
| Data export | 4 | Absent | Finance analysts require warehouse output |
| Savings Plans and RI purchase automation | 5 | Absent | Deliberately deferred until approvals exist |
| Model Context Protocol | 5 | Absent | Table stakes in 2026 |
| Graviton and architecture migration | 4 | Absent | Cheap to add once analytics consume first-party recommendations |
| Storage lifecycle | 4 | Absent | AWS automates it; detection is the useful part |
| Spot orchestration | 4 | Absent | Out of scope by decision, recorded in position |
| Kubernetes and EKS | 4 | Raw compute captured, no cluster awareness | Out of scope by decision |
| Carbon and sustainability | 3 | Absent | Compliance-driven. A straightforward read-and-report feature |
| Confidence calibration | 3 | Absent | No market leader; available ground |
| Non-production scheduling | 3 | Absent | Large saving in non-production accounts |
| Data platform cost | 3 | Absent | Newer category |
| SaaS and licence spend | 3 | Absent | Increasingly managed by FinOps teams |

## AI and agent infrastructure

Model Context Protocol is table stakes for 2026. OpenCost ships a server in its official
Helm chart. Vantage, CloudZero and Finout operate hosted servers. AWS, Azure and Google
ship managed cost servers, and the FinOps Foundation runs a dedicated working group on it.

AWS AI cost investigation is worth reading for its framing: it prefers an honest statement
of what it can and cannot confirm over a confident answer that may be wrong. That is the
same contract OptiBrain applies in code.

## Structural limits

**Cost Explorer cannot attribute spend to a resource id.** Every per-resource cost figure
is a list-price estimate. Totals come from Cost Explorer and are correct; single-resource
figures are estimates, and the API returns `null` where attribution fails rather than
presenting an estimate as a measurement.

**Recommendation detail is unavailable per instance.** Commitment purchase recommendations
apply to a service and region, not to an identified instance, so a rightsizing saving
cannot be exactly netted against a commitment.

**AWS provides the analytics for free.** Cost Optimization Hub, Compute Optimizer and Cost
Anomaly Detection improve continuously without a purchase. Competing on analytics depth
means competing against a first-party service at no customer cost. See
[position.md](position.md).