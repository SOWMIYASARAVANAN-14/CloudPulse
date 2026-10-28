# CloudPulse Step 7: AWS Cost Breakdown & Safe Teardown Guide

This guide explains the cost profile of the CloudPulse infrastructure and provides exact, safe procedures for decommissioning resources when you want to avoid recurring AWS charges.

---

## 1. Ongoing AWS Infrastructure Cost Breakdown

| Component | AWS Resource | Monthly Estimated Cost | Daily Estimated Cost | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Ingress** | Application Load Balancer (ALB) | ~$16.20 - $18.00 | ~$0.54 - $0.60 | Billed at $0.0225/hr + LCU usage. Primary cost driver. |
| **Compute** | EC2 `t3.micro` (1 baseline instance) | ~$7.50 (or $0.00 if in 12-mo free tier) | ~$0.25 (or $0.00) | Billed at $0.0104/hr in `ap-south-1`. |
| **Edge CDN** | Amazon CloudFront | $0.00 | $0.00 | Covered by Always-Free Tier (1 TB data out + 10M requests/mo). |
| **Alerting** | Amazon SNS & CloudWatch Alarm | $0.00 | $0.00 | Covered by AWS Free Tier (10 alarms + 1,000 email notifications free). |
| **Storage** | EBS Root Volume (8 GB gp3) | ~$0.80 | ~$0.03 | Standard EBS storage fee. |
| **Total** | **All CloudPulse Resources** | **~$24.50 - $26.00 / mo** | **~$0.82 - $0.88 / day** | Driven almost exclusively by the ALB and EC2 instance. |

---

## 2. Deciding Whether to Keep Running or Destroy

### Option A: Keep Live for Active Interviews / Demonstrations
- **When**: If recruiters or interviewers are actively testing your live HTTPS URL.
- **Cost**: Approximately **$0.85 per day**.
- **Monitoring**: Check AWS Cost Explorer weekly to monitor charges.

### Option B: Teardown with Preserved Portfolio Evidence (Recommended After Interview Cycles)
- **When**: When you are not actively showcasing the live endpoint.
- **Cost**: **$0.00 / month**.
- **Evidence Preserved**: Your GitHub repository, full Git commit history, architecture diagrams, verified test evidence logs, Docker Hub images, and Terraform scripts remain 100% intact. An interviewer can verify your work via the repository documentation or ask you to spin it back up via `terraform apply` in under 3 minutes!

---

## 3. Safe Teardown Procedure (`terraform destroy`)

> [!WARNING]
> Do NOT run teardown until you have recorded all desired screenshots, outputs, and proof of testing! Only run this when you explicitly decide to decommission resources.

### Step-by-Step Command:
1. Open PowerShell and navigate to the Terraform directory:
   ```powershell
   cd "C:\Users\RAMYA SOWMYA\Desktop\CLOUD\CloudPulse\CloudPulse\terraform"
   ```
2. Inspect the planned destructions:
   ```powershell
   C:\terraform\terraform.exe plan -destroy
   ```
3. When approved, execute the destruction:
   ```powershell
   C:\terraform\terraform.exe destroy
   ```
   Type `yes` when prompted.

---

## 4. Post-Teardown AWS Verification Checklist

After running `terraform destroy`, confirm in the AWS Management Console (`ap-south-1` region) that the following resources are deleted:

- [ ] **EC2 Instances**: Navigate to *EC2 > Instances*. Confirm instances are `Terminated`.
- [ ] **Auto Scaling Groups**: Navigate to *EC2 > Auto Scaling Groups*. Confirm `cloudpulse-asg` is deleted.
- [ ] **Load Balancers**: Navigate to *EC2 > Load Balancers*. Confirm `cloudpulse-alb` is deleted.
- [ ] **Target Groups**: Navigate to *EC2 > Target Groups*. Confirm `cloudpulse-tg` is deleted.
- [ ] **CloudWatch Alarms**: Navigate to *CloudWatch > In alarm / All alarms*. Confirm `cloudpulse-high-cpu` is deleted.
- [ ] **SNS Topics**: Navigate to *SNS > Topics*. Confirm `cloudpulse-alerts` is deleted.
- [ ] **CloudFront Distributions**: Navigate to *CloudFront > Distributions*. Confirm `CloudPulse HTTPS CDN Distribution` is deleted or disabled.

Confirming all checkboxes guarantees that your AWS account accrues **$0.00** ongoing hourly infrastructure charges!
