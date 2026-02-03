# IAM Configuration for OptiBrain

## Required AWS Permissions

OptiBrain requires specific IAM permissions to perform cost analysis, resource discovery, and optimization tasks. This document outlines the minimum required permissions following the principle of least privilege.

## IAM Policy for OptiBrain Service

Create an IAM role or user with the following policy attached:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EC2ReadAccess",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:DescribeVolumes",
        "ec2:DescribeSnapshots",
        "ec2:DescribeAddresses",
        "ec2:DescribeInstanceTypes",
        "ec2:DescribeInstanceStatus",
        "ec2:DescribeRegions",
        "ec2:DescribeAvailabilityZones"
      ],
      "Resource": "*"
    },
    {
      "Sid": "EC2ManagementAccess",
      "Effect": "Allow",
      "Action": [
        "ec2:StopInstances",
        "ec2:StartInstances",
        "ec2:TerminateInstances",
        "ec2:ModifyInstanceAttribute",
        "ec2:DeleteVolume",
        "ec2:ReleaseAddress"
      ],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "aws:RequestedRegion": ["us-east-1", "us-west-2"]
        }
      }
    },
    {
      "Sid": "AutoScalingAccess",
      "Effect": "Allow",
      "Action": [
        "autoscaling:DescribeAutoScalingGroups",
        "autoscaling:DescribeAutoScalingInstances",
        "autoscaling:SetDesiredCapacity",
        "autoscaling:UpdateAutoScalingGroup"
      ],
      "Resource": "*"
    },
    {
      "Sid": "CloudWatchMetrics",
      "Effect": "Allow",
      "Action": [
        "cloudwatch:GetMetricStatistics",
        "cloudwatch:GetMetricData",
        "cloudwatch:ListMetrics"
      ],
      "Resource": "*"
    },
    {
      "Sid": "CostExplorerReadAccess",
      "Effect": "Allow",
      "Action": [
        "ce:GetCostAndUsage",
        "ce:GetCostForecast",
        "ce:GetReservationUtilization",
        "ce:GetSavingsPlansUtilization",
        "ce:GetRightsizingRecommendation"
      ],
      "Resource": "*"
    },
    {
      "Sid": "RDSReadAccess",
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances",
        "rds:DescribeDBSnapshots",
        "rds:ListTagsForResource"
      ],
      "Resource": "*"
    },
    {
      "Sid": "S3ReadAccess",
      "Effect": "Allow",
      "Action": [
        "s3:ListAllMyBuckets",
        "s3:GetBucketLocation",
        "s3:GetBucketTagging",
        "s3:GetMetricsConfiguration"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ELBReadAccess",
      "Effect": "Allow",
      "Action": [
        "elasticloadbalancing:DescribeLoadBalancers",
        "elasticloadbalancing:DescribeTargetGroups",
        "elasticloadbalancing:DescribeListeners",
        "elasticloadbalancing:DescribeTargetHealth"
      ],
      "Resource": "*"
    },
    {
      "Sid": "PricingAccess",
      "Effect": "Allow",
      "Action": [
        "pricing:GetProducts",
        "pricing:DescribeServices"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SavingsPlansReadAccess",
      "Effect": "Allow",
      "Action": [
        "savingsplans:DescribeSavingsPlans",
        "savingsplans:DescribeSavingsPlansOfferingRates",
        "savingsplans:DescribeSavingsPlansOfferings"
      ],
      "Resource": "*"
    }
  ]
}
```

## Read-Only Mode Policy (Recommended for Initial Testing)

If you want to test OptiBrain without allowing any resource modifications:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadOnlyAccess",
      "Effect": "Allow",
      "Action": [
        "ec2:Describe*",
        "autoscaling:Describe*",
        "cloudwatch:Get*",
        "cloudwatch:List*",
        "ce:Get*",
        "rds:Describe*",
        "rds:ListTagsForResource",
        "s3:ListAllMyBuckets",
        "s3:GetBucket*",
        "elasticloadbalancing:Describe*",
        "pricing:GetProducts",
        "pricing:DescribeServices",
        "savingsplans:Describe*"
      ],
      "Resource": "*"
    }
  ]
}
```

## Environment-Specific Recommendations

### Development Environment
- Use LocalStack or AWS test accounts
- Apply read-only policy
- Test with tagging filters (e.g., `Environment:Dev`)

### Production Environment
- Use IAM roles attached to EC2/ECS/Lambda (not IAM users)
- Enable CloudTrail logging for all OptiBrain actions
- Implement approval workflow before executing optimization actions
- Use resource tagging to restrict actions (e.g., `OptiBrain:Managed=true`)
- Set `app.recommendations.auto-execute=false` to require manual approval

## AWS Credentials Configuration

### Method 1: Environment Variables (Recommended)
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-east-1
```

### Method 2: IAM Role (Production - Best Practice)
When running on AWS infrastructure (EC2, ECS, Lambda):
- Attach IAM role to the compute resource
- No credentials needed in application.properties
- Credentials are automatically retrieved via AWS SDK

### Method 3: AWS Credentials File
Place credentials in `~/.aws/credentials`:
```ini
[default]
aws_access_key_id = your_access_key
aws_secret_access_key = your_secret_key
region = us-east-1
```

## Security Best Practices

1. **Never commit AWS credentials to version control**
2. **Rotate credentials regularly** (every 90 days minimum)
3. **Enable MFA** for IAM users with powerful permissions
4. **Use separate IAM roles** for dev, staging, and production
5. **Enable CloudTrail** to audit all AWS API calls
6. **Set up billing alerts** to detect unusual activity
7. **Use AWS Organizations SCPs** to enforce guardrails
8. **Tag all resources** created or managed by OptiBrain

## Resource Tagging Strategy

OptiBrain should tag any resources it creates or modifies:

```
OptiBrain:ManagedBy = OptiBrain
OptiBrain:Action = StopInstance | TerminateInstance | Resize | etc.
OptiBrain:Date = 2024-01-15
OptiBrain:RecommendationId = rec-12345
```

This enables tracking and rollback if needed.
