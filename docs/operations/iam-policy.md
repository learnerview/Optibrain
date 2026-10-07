# IAM Policy for OptiBrain

The policy below grants exactly the AWS API actions the application calls. Every entry
maps to a specific invocation in the code; actions the code does not use are omitted.

Attach the policy to an IAM role and attach the role to the workload (for a single-account
deployment). `AwsClientFactory` resolves credentials through `DefaultCredentialsProvider`,
which prefers an instance or task role over static keys. In the multi-tenant `AWS` mode the
application additionally assumes each tenant's role via `sts:AssumeRole` configured from
`awsRoleArn` / `awsExternalId` - see the *Per-tenant roles* section.

## Read access

Required for inventory, cost and telemetry. Safe to grant in a read-only account.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ComputeInventory",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:DescribeInstanceTypes",
        "ec2:DescribeVolumes",
        "ec2:DescribeSnapshots",
        "ec2:DescribeAddresses",
        "ec2:DescribeNatGateways",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DescribeSecurityGroups"
      ],
      "Resource": "*"
    },
    {
      "Sid": "LoadBalancerAndScalingInventory",
      "Effect": "Allow",
      "Action": [
        "elasticloadbalancing:DescribeLoadBalancers",
        "elasticloadbalancing:DescribeTags",
        "autoscaling:DescribeAutoScalingGroups"
      ],
      "Resource": "*"
    },
    {
      "Sid": "DatabaseInventory",
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances",
        "rds:DescribeDBClusters"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ContainerInventory",
      "Effect": "Allow",
      "Action": [
        "ecs:ListClusters",
        "ecs:DescribeClusters",
        "ecs:ListServices",
        "ecs:DescribeServices"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ServerlessAndDataStoreInventory",
      "Effect": "Allow",
      "Action": [
        "lambda:ListFunctions",
        "dynamodb:ListTables",
        "dynamodb:DescribeTable",
        "s3:ListBuckets"
      ],
      "Resource": "*"
    },
    {
      "Sid": "Telemetry",
      "Effect": "Allow",
      "Action": ["cloudwatch:GetMetricStatistics"],
      "Resource": "*"
    },
    {
      "Sid": "Cost",
      "Effect": "Allow",
      "Action": [
        "ce:GetCostAndUsage",
        "ce:GetCostForecast",
        "ce:GetReservationCoverage",
        "ce:GetReservationPurchaseRecommendation",
        "ce:GetSavingsPlansUtilization"
      ],
      "Resource": "*"
    }
  ]
}
```

Notes:

- `ec2:DescribeInstanceTypes` backs the cached catalogue used to populate vCPU and memory
  specs; without it, instance specs are blank but inventory still works.
- `elasticloadbalancing:DescribeLoadBalancers` and `DescribeTags` are the ELBv2 API;
  the v1 action names would not cover them.
- Cost Explorer calls are only needed for the cost surface; an environment without cost
  access still gets inventory, recommendations (orphan and rightsizing) and remediation.

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
        "ec2:ModifyInstanceAttribute",
        "autoscaling:UpdateAutoScalingGroup"
      ],
      "Resource": "*"
    }
  ]
}
```

The dry-run interlock in `AwsCloudProviderAdapter#execute` blocks these actions before any
call is issued, so a read-only role plus `cloud.dry-run=true` permits full analysis with no
write capability at all. `ec2:ModifyInstanceAttribute` supports `RESIZE_INSTANCE` - which
further requires the target to already be `stopped`, so an operator who can reach the call
still has to stop the instance first (`ec2:StopInstances`) and restart it afterwards
(`ec2:StartInstances`); grant all three or the resize path is blocked.
`autoscaling:UpdateAutoScalingGroup` supports `SCALE_GROUP`. Actions the adapter does not
implement (commitment purchase, bucket deletion, network-resource deletion, data-store
deletion) require no permission, because they are refused before any call is made.

## Per-tenant roles

In `cloud.mode=AWS` with multiple tenants, grant each tenant role trust to be assumed by
the application role, and grant the application role `sts:AssumeRole`:

```json
{
  "Sid": "AssumeTenantRoles",
  "Effect": "Allow",
  "Action": ["sts:AssumeRole"],
  "Resource": [
    "arn:aws:iam::<account>:role/<tenant-role-a>",
    "arn:aws:iam::<account>:role/<tenant-role-b>"
  ]
}
```

Each tenant role then carries the read/remediation statements above for its own account,
and its trust policy permits the application role with `sts:ExternalId` (matching the
tenant's `awsExternalId`).

## Principal

For a single workload identity:

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
inventory request omitting a section (for example no load balancers) usually means that
service's describe permission is missing; the scanners log each omission and the product
renders the section as empty rather than failing.

## Not required

The application calls no S3 write, SQS, SNS, ECR, Pricing, CloudFormation, or Elasticache
API, so none feature above. `cloudwatch:PutMetricAlarm` is used only by the LocalStack seed
script (which runs the AWS CLI from `compose.yaml`), not by the application, so it is
omitted from a production role. `sts:GetCallerIdentity` is used by the seed script's
readiness probe, not by the application.