# IAM Policy for OptiBrain

The policy below grants exactly the actions the application calls. Every entry maps to a
specific AWS API invocation; actions the code does not use are omitted.

Attach the policy to an IAM role and attach the role to the workload. An IAM role is the
appropriate mechanism, because `AwsClientFactory` resolves credentials through
`DefaultCredentialsProvider`, which prefers an instance or task role over static keys.

## Read access

Required for inventory, cost and telemetry. Safe to grant in a read-only account.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Identity",
      "Effect": "Allow",
      "Action": ["sts:GetCallerIdentity"]
    },
    {
      "Sid": "ComputeInventory",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:DescribeInstanceTypes",
        "ec2:DescribeVolumes",
        "ec2:DescribeSnapshots",
        "ec2:DescribeAddresses",
        "ec2:DescribeNatGateways"
      ]
    },
    {
      "Sid": "NetworkInventory",
      "Effect": "Allow",
      "Action": ["elasticloadbalancing:DescribeLoadBalancers"]
    },
    {
      "Sid": "ScalingInventory",
      "Effect": "Allow",
      "Action": ["autoscaling:DescribeAutoScalingGroups"]
    },
    {
      "Sid": "DatabaseInventory",
      "Effect": "Allow",
      "Action": ["rds:DescribeDBInstances"]
    },
    {
      "Sid": "Telemetry",
      "Effect": "Allow",
      "Action": [
        "cloudwatch:GetMetricStatistics",
        "cloudwatch:PutMetricAlarm"
      ]
    },
    {
      "Sid": "Cost",
      "Effect": "Allow",
      "Action": [
        "ce:GetCostAndUsage",
        "ce:GetReservationPurchaseRecommendation",
        "ce:GetReservationCoverage"
      ]
    }
  ]
}
```

`cloudwatch:PutMetricAlarm` is optional. It supports the sandbox seed script; remove it
from a production role.

## Remediation access

Required only when `cloud.dry-run=false`. These actions stop, resize and terminate
resources. Grant them only in an account where that is acceptable, and only after the
read-only policy has been running and reviewed.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Remediation",
      "Effect": "Allow",
      "Action": [
        "ec2:StopInstances",
        "ec2:StartInstances",
        "ec2:TerminateInstances",
        "ec2:DeleteVolume",
        "ec2:DeleteSnapshot",
        "ec2:ReleaseAddress",
        "ec2:CreateTags",
        "autoscaling:UpdateAutoScalingGroup"
      ]
    }
  ]
}
```

The dry-run interlock in `AwsCloudProviderAdapter#execute` blocks these actions before any
call is issued, so a read-only role plus `cloud.dry-run=true` permits full analysis with no
write capability at all.

## Principal

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AssumeOptiBrain",
      "Effect": "Allow",
      "Principal": { "AWS": "arn:aws:iam::<account-id>:role/<workload-role>" },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

## Verification

`AwsClientFactory` fails at startup when `cloud.mode=AWS` and no credential resolves. An
inventory request returning `connectedToCloud: false` indicates the role is missing a
describe permission.

## Not required

The application calls no S3, Pricing, Savings Plans or Cost Anomaly Detection API, so no
corresponding permissions appear above. `elasticloadbalancing:Describe*` is absent from
the read policy because the client is `ElasticLoadBalancingV2Client`; the v2 action is
`elasticloadbalancing:DescribeLoadBalancers`.