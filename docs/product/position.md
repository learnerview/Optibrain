# Product Position

## What OptiBrain is

OptiBrain reads an AWS account, measures cost and utilisation, recommends changes, and
executes the ones an operator approves.

Two properties define the product:

**An uncertainty contract.** Every read path distinguishes data available, no data, and
source unreachable. The third never renders as the second. A forecast without history
returns an empty list and a reason. A copilot asked about spend with no Cost Explorer
access says so. Recommendations for instances without telemetry are skipped rather than
estimated.

**An execution layer with guardrails.** A dry-run interlock defaults to on, protected-tag
exclusion is enforced beside it, every action carries a risk level, and each execution is
audited.

## Market position

The read side of cloud cost management is effectively free and near-perfect. AWS Cost
Optimization Hub provides eighteen recommendation types. AWS Compute Optimizer provides
93-day lookback with organisation-level preferences. AWS Cost Anomaly Detection provides
root causes with dollar contribution, and its 2026 AI investigation correlates CloudTrail
to specific API calls and IAM principals. All of it ships at no additional cost.

That shapes strategy. OptiBrain does not compete on analytics depth against a free,
continuously-improving first-party service. It competes on the two things AWS does not
provide: a mechanism to execute a change safely, and a guarantee that a number is either
measured or explicitly absent.

The competitive set splits cleanly:

| Group | Capability | Gap |
|---|---|---|
| AWS native, Cost Optimization Hub, Compute Optimizer, Anomaly Detection | Analytics | No execution |
| Kubecost, OpenCost | Allocation, honesty | No execution |
| Vantage, nOps, CAST AI, ProsperOps | Execution | No published uncertainty contract |
| OptiBrain | Execution plus uncertainty contract | Analytics depth |

## Strategic implications

**Spend the differentiation budget on execution, attribution and guarantees.** Analytics
composition on AWS primitives is cheap and improves without investment.

**Treat commitment management as the largest untapped pool.** Industry benchmarking
indicates a majority of AWS organisations hold no Savings Plan or Reserved Instance at
all, and AWS now offers four Savings Plan families: Compute (up to 66%), EC2 Instance
(72%), Database (35%) and SageMaker AI (64%). OptiBrain scans RDS but surfaces no Database
Savings Plan opportunity.

**Discount-aware savings is a correctness requirement.** Pricing a saving from on-demand
list rates overstates the benefit for a committed workload by up to 70%. Commitment coverage
is measured and reported with every projection for this reason.

**Model Context Protocol is table stakes.** OpenCost ships an MCP server in its official
Helm chart. Vantage, CloudZero and Finout operate hosted servers. AWS, Azure and Google
all ship managed cost MCP servers, and the FinOps Foundation runs a dedicated MCP working
group.

The competitive discourse is dominated by read access to a dashboard, which a client can
already obtain by opening the dashboard. OptiBrain can expose a guarded write surface:
`RemediationExecutor` already provides a dry-run interlock, a protection guard, per-action
risk levels and an audit log. A write-capable, guarded MCP tool surface is a defensible
position no read-only competitor holds.

## Deliberate omissions

**Kubernetes and Karpenter.** Demand is high in absolute terms, but this product holds no
advantage there and the leaders are well established. Raw EKS compute is already captured
through the EC2 and Auto Scaling scanners, so the incremental cost of cluster-aware
tagging is small when it is chosen.

**Spot orchestration.** High value and high operational liability. Differentiating here
requires reliability engineering that competes directly with specialists.

**Carbon reporting.** Compliance-driven rather than cost-driven, and a straightforward
read-and-report feature over AWS Sustainability. Cheap when wanted, not foundational.

**Building a tag-enforcement engine.** AWS tag policies and service control policies
already provide it. OptiBrain reports compliance and drives adoption rather than
reimplementing enforcement.

**Sourcing business metrics for unit economics.** Customers supply these. Attempting to
infer them produces unreliable figures.