# Product Position

## What OptiBrain is

OptiBrain reads an AWS account, measures cost and utilisation, recommends changes, and
executes the ones an operator approves - through one guarded mutation path.

Two properties define the product:

**An uncertainty contract.** Every read path distinguishes *data available*, *no data*,
and *source unreachable*, and never renders the third as the second. A cost report without
Cost Explorer returns `status: UNAVAILABLE`, never a measured zero. An anomaly engine with
fewer than seven days of history returns `INSUFFICIENT_DATA`. An instance without
telemetry produces
no recommendation. The ML service returns `available: false` where it has no model or no
data, and the copilot says the data is unavailable rather than estimating.

**An execution layer with guardrails.** A dry-run interlock defaults to on
(`cloud.dry-run=true`) and blocks every mutation in `AwsCloudProviderAdapter#execute`, the
only mutation site. A protection tag (`optibrain:protected=true`) excludes resources from
destructive actions, every `ActionType` carries a `RiskLevel`, plans re-verify the target
against live state before execution, and every execution is persisted and audited.

## Market position

The read side of cloud cost management is effectively free and near-perfect. AWS Cost
Optimization Hub provides many recommendation types. AWS Compute Optimizer provides
93-day lookback with organisation-level preferences. AWS Cost Anomaly Detection correlates
CloudTrail to API calls and IAM principals for root cause. All of it ships at no
additional cost.

That shapes strategy. OptiBrain does not compete on analytics depth against a free,
continuously-improving first-party service. It competes on the two things AWS does not
provide: a mechanism to execute a change safely, and a guarantee that a number is either
measured or explicitly absent.

| Group | Capability | Gap |
|---|---|---|
| AWS native (Cost Optimization Hub, Compute Optimizer, Anomaly Detection) | Analytics | Read-only - no execution |
| Kubecost, OpenCost | Allocation, honesty | No execution |
| Vantage, nOps, CAST AI, ProsperOps | Execution | No published uncertainty contract |
| OptiBrain | Execution plus uncertainty contract | Analytics depth |

## Strategic implications

**Spend the differentiation budget on execution, attribution and guarantees.** Analytics
composition on AWS primitives is cheap and improves without investment.

**Treat commitment management as the largest untapped pool.** Industry benchmarking
indicates a majority of AWS organisations hold no Savings Plan or Reserved Instance at
all. The recommendation engine now derives advisory `RI_OPTIMIZATION` /
`SP_OPTIMIZATION` rows from a measured baseline (simulated discount, `basis:
simulated_baseline`), but automated purchasing remains absent - deliberately, because it
must sit behind the approval workflow.

**Discount-aware savings is a correctness requirement.** Pricing a saving from on-demand
list rates overstates the benefit for a committed workload. Commitment coverage is
measured and reported with every projection for this reason
(`effectiveMonthlySavings`, `commitmentAdjusted`).

**Model Context Protocol is table stakes.** OpenCost ships an MCP server in its official
Helm chart. Vantage, CloudZero and Finout operate hosted servers. AWS, Azure and Google
all ship managed cost MCP servers, and the FinOps Foundation runs a dedicated MCP working
group. A guarded write tool surface is a defensible position no read-only competitor
holds: the platform already exposes `plan_remediation` (a `RemediationPlan` that never
touches the account) and `execute_remediation` (subject to the dry-run interlock, the
protection guard, a plan-token staleness check, idempotency and a persisted audit).

## Deliberate omissions

**Kubernetes and Karpenter.** Demand is high in absolute terms, but this product holds no
advantage there and the leaders are well established. ECS services are inventoried
(`ecs:services`), and raw EC2 compute is captured through the EC2 scanner.

**Spot orchestration.** High value and high operational liability. The ML service's spot
surface returns `UNAVAILABLE` honestly rather than fabricating a risk score.

**Carbon reporting.** Compliance-driven rather than cost-driven, and a straightforward
read-and-report feature over AWS Sustainability. Cheap when wanted, not foundational.

**Building a tag-enforcement engine.** AWS tag policies and service control policies
already provide it. OptiBrain reports compliance and surfaces uncovered spend rather than
reimplementing enforcement.

**Sourcing business metrics for unit economics.** Customers supply these. Attempting to
infer them produces unreliable figures.