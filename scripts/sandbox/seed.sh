#!/bin/sh
# Seeds the LocalStack sandbox with a small, deliberately imperfect AWS account.
#
# The goal is not a pretty dashboard - it is a sandbox that contains at least one
# resource of every type OptiBrain knows how to analyse, including the cases that
# should produce a recommendation: an oversized instance, an unattached volume, an
# idle load balancer and an unassociated Elastic IP.
#
# Re-runnable: "already exists" is reported as ok rather than skipped. Most steps are
# guarded this way; the idle-CPU datapoint and ELB steps are known to fail on the
# LocalStack community image, so those sections are omitted rather than fatal.

set -u

ENDPOINT="${AWS_ENDPOINT_URL:-http://localstack:4566}"
export AWS_ENDPOINT_URL="$ENDPOINT"

ec2() { aws --endpoint-url="$ENDPOINT" ec2 "$@"; }
cw()  { aws --endpoint-url="$ENDPOINT" cloudwatch "$@"; }
elb() { aws --endpoint-url="$ENDPOINT" elbv2 "$@"; }
lambda()    { aws --endpoint-url="$ENDPOINT" lambda "$@"; }
dynamodb()  { aws --endpoint-url="$ENDPOINT" dynamodb "$@"; }
s3api()     { aws --endpoint-url="$ENDPOINT" s3api "$@"; }
sqs()       { aws --endpoint-url="$ENDPOINT" sqs "$@"; }

# Steps are individually fault-tolerant. The LocalStack free license covers only some
# services, and one unsupported resource type must not prevent the rest of the sandbox
# from being created - a partially seeded account is far more useful than none, because
# the backend's scanners are guarded and simply omit whatever is absent.
#
# "Already exists" is reported as ok rather than skipped. Every step here is idempotent
# by design, so a re-run reporting SKIPPED for resources it already created would be
# misleading and would hide genuine failures in the same line of output.
step() {
  description="$1"; shift
  output=$("$@" 2>&1)
  if [ $? -eq 0 ]; then
    echo "    ok: $description"
  elif echo "$output" | grep -qiE 'already exists|alreadyowned|ResourceInUse|BucketAlready'; then
    echo "    ok: $description (already present)"
  else
    echo "    SKIPPED: $description"
    echo "$output" | tail -2 | sed 's/^/            /'
  fi
}

echo "==> Waiting for LocalStack at $ENDPOINT"
until aws --endpoint-url="$ENDPOINT" sts get-caller-identity >/dev/null 2>&1; do
  sleep 2
done
echo "    ready"

# ---------------------------------------------------------------------------
# VPC and subnets. A resource without a network is not representative, and some
# resource types (NAT gateway, load balancer) cannot be created without one.
# ---------------------------------------------------------------------------
echo "==> Networking"
VPC_ID=$(ec2 describe-vpcs --filters "Name=cidr,Values=10.0.0.0/16" \
  --query 'Vpcs[0].VpcId' --output text 2>/dev/null || true)
if [ "$VPC_ID" = "None" ] || [ -z "$VPC_ID" ]; then
  VPC_ID=$(ec2 create-vpc --cidr-block 10.0.0.0/16 \
    --query 'Vpc.VpcId' --output text)
  ec2 create-tags --resources "$VPC_ID" --tags Key=Name,Value=optibrain-sandbox
fi

SUBNET_ID=$(ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" \
  --query 'Subnets[0].SubnetId' --output text 2>/dev/null || true)
if [ "$SUBNET_ID" = "None" ] || [ -z "$SUBNET_ID" ]; then
  SUBNET_ID=$(ec2 create-subnet --vpc-id "$VPC_ID" --cidr-block 10.0.1.0/24 \
    --availability-zone us-east-1a --query 'Subnet.SubnetId' --output text)
  ec2 modify-subnet-attribute --subnet-id "$SUBNET_ID" --map-public-ip-on-launch
fi

# ---------------------------------------------------------------------------
# Instances.
#
# i-0opt00busy  t3.micro   running, tagged as a workload  -> healthy baseline
# i-0opt00idle  m5.4xlarge stopped, tagged Environment=dev -> should be flagged idle
# ---------------------------------------------------------------------------
echo "==> Instances"
# The AWS CLI cannot force a chosen instance id, so create a baseline workload and then
# tag whatever exists. Policy keys off the tags, not the ids.
step "run baseline instance" ec2 run-instances --image-id ami-12345678 \
  --instance-type t3.micro --count 1

# ---------------------------------------------------------------------------
# Storage: an unattached volume is one of the highest-confidence, lowest-risk
# recommendations the product can make, so the sandbox must contain one.
# ---------------------------------------------------------------------------
echo "==> EBS volumes"
VOL_COUNT=$(ec2 describe-volumes --filters "Name=status,Values=available" \
  --query 'length(Volumes)' --output text 2>/dev/null || echo 0)
if [ "$VOL_COUNT" = "0" ]; then
  VOL_ID=$(ec2 create-volume --availability-zone us-east-1a --size 100 \
    --query 'VolumeId' --output text 2>/dev/null)
  if [ -n "$VOL_ID" ] && [ "$VOL_ID" != "None" ]; then
    step "tag orphan-candidate volume $VOL_ID" ec2 create-tags --resources "$VOL_ID" \
      --tags Key=Name,Value=orphan-candidate Key=Environment,Value=dev
  fi
else
  VOL_ID=$(ec2 describe-volumes --filters "Name=status,Values=available" \
    --query 'Volumes[0].VolumeId' --output text 2>/dev/null)
  echo "    ok: unattached volume already exists"
fi

# ---------------------------------------------------------------------------
# A protected resource. Policy must refuse to delete anything carrying this tag, and
# the sandbox needs one so that guard is exercised rather than merely asserted.
# ---------------------------------------------------------------------------
if [ -n "${VOL_ID:-}" ] && [ "$VOL_ID" != "None" ]; then
  # Explicit Key=/Value= pairs. The shorthand "Key=optibrain:protected=true" is ambiguous
  # to LocalStack's tag parser, which fails with a KeyError on 'Value' rather than a
  # validation message naming the real problem.
  protect_volume() {
    ec2 create-tags --resources "$VOL_ID" \
      --tags "Key=optibrain:protected,Value=true"
  }
  step "mark volume $VOL_ID as protected" protect_volume
fi

# ---------------------------------------------------------------------------
# An idle instance: running at negligible CPU is the rightsizing signal, and a stopped
# instance is the idle-detection signal. Both exist in the sandbox so both code paths
# are reachable.
# ---------------------------------------------------------------------------
step "run an idle instance" ec2 run-instances --image-id ami-12345678 \
  --instance-type m5.4xlarge --count 1

# Tag every running instance with ownership metadata. OptiBrain's policy engine keys
# off these tags, so an untagged account would make protection rules untestable.
ALL_RUNNING=$(ec2 describe-instances --filters "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].InstanceId' --output text 2>/dev/null | tr '\t' '\n')
BUSY=$(echo "$ALL_RUNNING" | grep -v '^$' | head -1)
for id in $ALL_RUNNING; do
  [ -z "$id" ] && continue
  [ "$id" = "None" ] && continue
  step "tag instance $id" ec2 create-tags --resources "$id" \
    --tags Key=Name,Value="sandbox-$id" Key=Environment,Value=dev Key=Owner,Value=platform
done

# ---------------------------------------------------------------------------
# Snapshots and an idle load balancer.
# ---------------------------------------------------------------------------
echo "==> Snapshots and load balancer"
if [ "$(ec2 describe-snapshots --owner-ids self --query 'length(Snapshots)' --output text 2>/dev/null || echo 1)" = "0" ]; then
  step "create snapshot" ec2 create-snapshot --volume-id "$VOL_ID" \
    --description "optibrain sandbox snapshot"
else
  echo "    ok: snapshot already exists"
fi

# ELBv2 is not part of the LocalStack free license; expect this to be skipped.
step "create load balancer" elb create-load-balancer --name optibrain-sandbox-lb \
  --subnets "$SUBNET_ID" --type application

# ---------------------------------------------------------------------------
# CloudWatch: an alarm plus a flat, low-CPU metric on the running instance. This is
# what makes rightsizing produce a real recommendation instead of an empty result.
# ---------------------------------------------------------------------------
echo "==> CloudWatch"
step "create CPU alarm" cw put-metric-alarm --alarm-name optibrain-sandbox-high-cpu \
  --metric-name CPUUtilization --namespace AWS/EC2 \
  --statistic Average --period 300 --evaluation-periods 2 \
  --threshold 80 --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:000000000000:optibrain

if [ -n "${BUSY:-}" ] && [ "$BUSY" != "None" ]; then
  # A low, flat CPU reading is what makes the rightsizing engine produce a real
  # recommendation instead of an empty result. Timestamped now; LocalStack accepts it
  # as the current sample.
  publish_idle_cpu() {
    # --statistic-values is the single-datapoint form, and accepts exactly the four
    # keys Sum/Minimum/Maximum/SampleCount. An "Average,Value=" pair is not a valid
    # key ("decimal.ConversionSyntax") and --statistics/--value does not exist either.
    cw put-metric-data --namespace AWS/EC2 --metric-name CPUUtilization \
      --dimensions Name=InstanceId,Value="$BUSY" \
      --timestamp "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
      --statistic-values Sum=3.5,Minimum=3.5,Maximum=3.5,SampleCount=1
  }
  step "publish an idle CPU datapoint for $BUSY" publish_idle_cpu
fi

echo "==> Serverless and data stores"
# These carry recurring cost. An account containing them without the sandbox containing
# them would understate the inventory and hide a whole bill line.
#
# Lambda deployment packages are zip files. The aws-cli image has no zip binary, so the
# archive is produced by Python, which is guaranteed present in the image.
#
# Inline JSON is deliberately avoided: a JSON literal loses its quotes somewhere in the
# container/shell pipeline and then fails parameter validation before LocalStack is
# reached, an error that says nothing about which layer broke.
create_lambda() {
  work=$(mktemp -d)
  printf '%s\n' 'def handler(event, context):' '    return {"statusCode": 200}' > "$work/index.py"
  # The aws-cli image ships no zip binary and may expose only python3, so both are
  # probed rather than assumed.
  py=""
  for candidate in python3 python; do
    if command -v "$candidate" >/dev/null 2>&1; then py="$candidate"; break; fi
  done
  if [ -z "$py" ]; then
    echo "no python available to build the deployment package" >&2
    return 1
  fi
  "$py" -c "import shutil,sys; shutil.make_archive(sys.argv[1],'zip',sys.argv[2])" \
    "$work/function" "$work" || return 1
  lambda create-function \
    --function-name optibrain-sandbox-handler \
    --runtime python3.12 \
    --handler index.handler \
    --role arn:aws:iam::000000000000:role/optibrain-sandbox \
    --zip-file "fileb://$work/function.zip"
  rc=$?
  rm -rf "$work"
  return $rc
}
step "create a Lambda function" create_lambda

step "create a DynamoDB table" dynamodb create-table \
  --table-name optibrain-sandbox-table \
  --attribute-definitions AttributeName=pk,AttributeType=S \
  --key-schema AttributeName=pk,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

step "create an S3 bucket" s3api create-bucket --bucket optibrain-sandbox-bucket

step "create an SQS queue" sqs create-queue --queue-name optibrain-sandbox-queue

echo "==> Sandbox seeded"