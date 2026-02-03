# Security Policy

## ⚠️ CRITICAL SECURITY WARNINGS

OptiBrain is a cloud cost optimization platform that **CAN MODIFY, STOP, AND TERMINATE** AWS resources if configured to do so. Please read this entire document before deploying to production.

## 🛡️ Safe Operation Guidelines

### 1. Default Safety Mode

By default, OptiBrain operates in **DRY-RUN mode**:
- All AWS actions are logged but NOT executed
- Set `cloud.dryRun=false` ONLY after thorough testing
- Always test in a non-production AWS account first

### 2. Read-Only Testing Phase

**RECOMMENDED**: Start with read-only IAM permissions for the first 7-14 days:
- Monitor recommendations without risk
- Validate cost calculations
- Ensure no false positives in idle resource detection
- Review all recommendations manually

See `IAM_POLICY.md` for read-only policy example.

### 3. Protected Resources

Configure resources that should NEVER be modified:

```properties
# application-prod.properties
app.protected-resources=i-production-web-01,vol-critical-database,sg-production-vpc
```

Or via environment variable:
```bash
export PROTECTED_RESOURCES=i-production-web-01,vol-critical-database
```

## 🚨 AWS Resource Risks

### HIGH RISK Operations

These operations can cause **service disruptions** if executed incorrectly:

| Operation | Risk Level | Impact |
|-----------|-----------|--------|
| **Terminate EC2 Instance** | 🔴 CRITICAL | Permanent data loss, service outage |
| **Delete EBS Volume** | 🔴 CRITICAL | Permanent data loss |
| **Stop RDS Instance** | 🟠 HIGH | Database unavailable until restarted |
| **Modify Instance Type** | 🟠 HIGH | Requires instance stop/start |
| **Delete Elastic IP** | 🟡 MEDIUM | IP address change, DNS updates needed |
| **Stop EC2 Instance** | 🟡 MEDIUM | Temporary service interruption |

### MEDIUM RISK Operations

| Operation | Risk Level | Impact |
|-----------|-----------|--------|
| **Detach EBS Volume** | 🟡 MEDIUM | Application may lose access to data |
| **Modify Security Group** | 🟡 MEDIUM | May affect network access |
| **Remove S3 Lifecycle Policy** | 🟢 LOW | No immediate impact |

## 🔐 Credential Security

### NEVER Do This

```properties
# ❌ NEVER commit credentials to version control
aws.access-key=AKIAIOSFODNN7EXAMPLE
aws.secret-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

### ✅ ALWAYS Do This

**Option 1: Environment Variables (Production)**
```bash
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
export AWS_REGION=us-east-1
```

**Option 2: IAM Roles (RECOMMENDED for EC2/ECS/Lambda)**
No credentials needed - attach IAM role with required policies.

**Option 3: AWS Credentials File**
```bash
# ~/.aws/credentials
[default]
aws_access_key_id = your_key
aws_secret_access_key = your_secret
```

### Credential Rotation

- Rotate AWS access keys every 90 days
- Use temporary credentials (STS) for applications
- Enable MFA for IAM users with powerful permissions
- Use AWS Secrets Manager for credential storage in multi-tenant scenarios

## 🏢 Multi-Tenant Security

If running OptiBrain for multiple AWS accounts:

1. **Use AWS STS AssumeRole**
   - Each tenant has a dedicated IAM role
   - OptiBrain assumes roles with temporary credentials
   - Credentials automatically expire

2. **Store Credentials Securely**
   - Use AWS Secrets Manager
   - OR HashiCorp Vault
   - NEVER store credentials in database plaintext

3. **Audit Logging**
   - Enable AWS CloudTrail in all accounts
   - Log all OptiBrain actions
   - Set up alerts for critical operations

## 📊 Monitoring & Auditing

### Enable Comprehensive Logging

```properties
# application-prod.properties
logging.level.com.optibrain=INFO
logging.level.software.amazon.awssdk=INFO

# Enable AWS CloudTrail
logging.audit.enabled=true
logging.audit.log-all-actions=true
```

### Required Monitoring

1. **OptiBrain Application Logs**
   - All AWS API calls
   - All resource modifications
   - All errors and exceptions

2. **AWS CloudTrail**
   - Monitor all API calls in your AWS account
   - Set up alerts for critical operations (TerminateInstances, DeleteVolume, etc.)

3. **Cost Monitoring**
   - Track actual cost changes after OptiBrain actions
   - Validate savings predictions

## 🧪 Testing Before Production

### 1. LocalStack Testing (Free)

Test OptiBrain with LocalStack (local AWS emulator):

```bash
docker run -d -p 4566:4566 localstack/localstack
export CLOUD_MODE=LOCALSTACK
export LOCALSTACK_ENDPOINT=http://localhost:4566
```

No AWS charges, no risk.

### 2. AWS Sandbox Account

Create a dedicated AWS test account:
- Use AWS Organizations to create a sandbox
- Set spending limits via AWS Budgets
- Test all features end-to-end
- Verify dry-run mode works correctly

### 3. Gradual Rollout

1. Week 1: Read-only mode, review recommendations
2. Week 2: Enable only low-risk actions (tag resources, modify schedules)
3. Week 3: Enable medium-risk actions (stop non-critical instances)
4. Week 4+: Enable high-risk actions (terminate, delete) with approval workflow

## 🚫 Known Limitations

### What OptiBrain DOES NOT Do

- ❌ Does not automatically restore deleted resources
- ❌ Does not create backups before deletion
- ❌ Does not verify resource dependencies before termination
- ❌ Does not integrate with on-call/paging systems by default
- ❌ Does not prevent AWS API throttling

### Best Practices

1. **Always use version control** for configuration changes
2. **Test in non-production** before enabling in production
3. **Review recommendations** before enabling auto-execution
4. **Set up alerts** for all critical operations
5. **Maintain backups** independent of OptiBrain
6. **Document exceptions** for protected resources

## 🆘 Emergency Procedures

### If OptiBrain Terminates a Critical Resource

1. **Stop OptiBrain immediately**
   ```bash
   kubectl scale deployment optibrain-backend --replicas=0
   # OR
   systemctl stop optibrain
   ```

2. **Check CloudTrail** for the termination event
3. **Attempt resource recovery** (if possible)
   - For EC2: Check if snapshots exist
   - For RDS: Check automated backups
   - For EBS: Check if snapshot exists

4. **Review and fix** the protection rules
5. **Add incident to post-mortem**

### If OptiBrain Stops Responding

1. Check application logs
2. Verify AWS credentials are valid
3. Check AWS service health dashboard
4. Verify IAM permissions haven't changed

## 📞 Reporting Security Issues

If you discover a security vulnerability in OptiBrain:

1. **DO NOT** open a public GitHub issue
2. Email: security@optibrain.example (or repository maintainer)
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if available)

## 🔒 Compliance Considerations

### GDPR / Data Privacy

- OptiBrain logs may contain AWS resource IDs
- Ensure logs are stored securely
- Implement data retention policies
- Document data processing in your DPA

### SOC 2 / ISO 27001

- Enable comprehensive audit logging
- Implement role-based access control
- Document change management procedures
- Conduct regular security assessments

### Industry-Specific

- **Healthcare (HIPAA)**: Ensure OptiBrain doesn't log PHI
- **Finance (PCI DSS)**: Protect cardholder data environment resources
- **Government**: Use AWS GovCloud regions if required

## ✅ Security Checklist Before Production

- [ ] Dry-run mode enabled and tested
- [ ] Protected resources configured
- [ ] IAM roles use least-privilege principle
- [ ] AWS CloudTrail enabled
- [ ] MFA enabled for all admin users
- [ ] Credentials never committed to version control
- [ ] All secrets in environment variables or secret manager
- [ ] Monitoring and alerting configured
- [ ] Tested in sandbox AWS account
- [ ] Incident response plan documented
- [ ] Team trained on OptiBrain capabilities and risks
- [ ] Backup verification process in place
- [ ] Regular security audits scheduled

---

**Remember**: With great power comes great responsibility. OptiBrain can significantly reduce AWS costs, but only when used carefully and responsibly.

**Last Updated**: 2026-02-03
**Next Review**: 2026-05-03
