# CloudPulse Verification & Test Evidence

This document records the empirical verification and real test outputs across all project milestones.

---

## 1. Step 1: Base Application & Health Endpoint Verification

### Live ALB Health Endpoint
```bash
$ curl.exe -s http://cloudpulse-alb-1639669457.ap-south-1.elb.amazonaws.com/api/health
```
**Response:**
```json
{
  "version": "1.1.0",
  "status": "UP",
  "service": "CloudPulse",
  "instance": "c86669d8bef4",
  "timestamp": "2026-10-28T17:25:16.896854548Z"
}
```

### Application Info Endpoint
```bash
$ curl.exe -s http://cloudpulse-alb-1639669457.ap-south-1.elb.amazonaws.com/api/info
```
**Response:**
```json
{
  "name": "CloudPulse",
  "version": "1.1.0",
  "features": ["health checks", "container deployment", "cloud autoscaling", "automated-cd"],
  "description": "Self-healing cloud application demo"
}
```

---

## 2. Step 2: Automated CI/CD & Zero-Downtime Rolling Update

### GitHub Actions Pipeline Execution
- **Run ID**: `#40699136019`
- **Commit**: `de875f5` (Bumped version to 1.1.0 and added ASG Instance Refresh)
- **Status**: `success` (Completed in 4m 31s)
- **Steps Verified**:
  - `mvn clean verify` passed 3 unit tests.
  - Built Docker images:
    - `docker.io/sowmiyasaravanan14/cloudpulse:latest`
    - `docker.io/sowmiyasaravanan14/cloudpulse:de875f59fae61beea675cf38b8c2bc16db1a1ff2`
  - Started instance refresh on `cloudpulse-asg` with minimum healthy percentage `50%`.
  - Polled `describe-instance-refreshes` until status transitioned to `Successful`.
  - Canary verified ALB returned HTTP 200 with version `1.1.0`.

---

## 3. Step 3: Elastic Auto Scaling & Target Tracking Verification

### Controlled Scaling Test
To verify elasticity without generating unmetered CPU load or artificial spikes, the Auto Scaling group was scaled dynamically from 1 to 2 instances:
```bash
$ aws autoscaling set-desired-capacity --auto-scaling-group-name cloudpulse-asg --desired-capacity 2 --region ap-south-1
```

### Target Group Registration
```bash
$ aws elbv2 describe-target-health --target-group-arn "arn:aws:elasticloadbalancing:ap-south-1:241490787841:targetgroup/cloudpulse-tg/84cf0450252d516b" --region ap-south-1
```
**Result**: Both instances `i-02c4f3e6828881ed9` and `i-0b271917a6bba8fa6` transitioned to `healthy`.

### Load Balancing Verification (Round-Robin Traffic Distribution)
Consecutive HTTP queries to the ALB endpoint returned alternating container IDs:
- Request 1 -> Instance `4074ce17b848` (Host `i-02c4f3e6828881ed9`)
- Request 2 -> Instance `c86669d8bef4` (Host `i-0b271917a6bba8fa6`)

### Clean Scale-In (Zero Downtime)
Capacity restored from 2 to 1 desired:
- `cloudpulse-asg` initiated connection draining (graceful deregistrations).
- Old instance was terminated cleanly without dropping active HTTP requests.

---

## 4. Step 4: CloudWatch CPU Alarm & SNS Dual Alerting Flow

### CloudWatch Alarm Configuration
- **Alarm Name**: `cloudpulse-high-cpu`
- **Metric**: `CPUUtilization` on `cloudpulse-asg`
- **Threshold**: `> 70%` for two consecutive 60-second periods.
- **Alarm Actions**: `arn:aws:sns:ap-south-1:241490787841:cloudpulse-alerts`
- **OK Actions**: `arn:aws:sns:ap-south-1:241490787841:cloudpulse-alerts`

### SNS Topic & Subscription
- **Topic ARN**: `arn:aws:sns:ap-south-1:241490787841:cloudpulse-alerts`
- **Subscription ARN**: `arn:aws:sns:ap-south-1:241490787841:cloudpulse-alerts:3b352ecb-3be7-43dc-938f-8bfabb33c482`
- **Subscriber**: `sowmiyasaravanan3014@gmail.com` (Confirmed)

### Safe Alert Dispatch Test (Simulated State Transitions)
1. **Trigger Alarm Notification**:
   ```bash
   aws cloudwatch set-alarm-state --alarm-name "cloudpulse-high-cpu" --state-value ALARM --state-reason "CloudPulse Step 4 verification test: CPU utilization exceeded threshold" --region ap-south-1
   ```
   **Outcome**: AWS SNS dispatched notification email to subscriber with subject `ALARM: "cloudpulse-high-cpu" in Asia Pacific (Mumbai)`.
2. **Trigger Recovery Notification**:
   ```bash
   aws cloudwatch set-alarm-state --alarm-name "cloudpulse-high-cpu" --state-value OK --state-reason "CloudPulse Step 4 verification test: CPU utilization normalized below threshold (recovery test)" --region ap-south-1
   ```
   **Outcome**: AWS SNS dispatched recovery email to subscriber with subject `OK: "cloudpulse-high-cpu" in Asia Pacific (Mumbai)`.

---

## 5. Step 5: Security Hardening & Edge CloudFront HTTPS Verification

### Live CloudFront HTTPS Health Check
```bash
$ curl.exe -s -i https://d3v9i1q37unojr.cloudfront.net/api/health
```
**HTTP Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json
Connection: keep-alive
X-Cache: Miss from cloudfront
Via: 1.1 caf6336eaf07f9740378cad250c0289e.cloudfront.net (CloudFront)

{"version":"1.1.0","status":"UP","service":"CloudPulse","instance":"c86669d8bef4","timestamp":"2026-10-28T17:38:42.876664962Z"}
```

### Live CloudFront HTTPS App Info
```bash
$ curl.exe -s -i https://d3v9i1q37unojr.cloudfront.net/api/info
```
**HTTP Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json
Connection: keep-alive
X-Cache: Miss from cloudfront

{"name":"CloudPulse","version":"1.1.0","features":["health checks","container deployment","cloud autoscaling","automated-cd"],"description":"Self-healing cloud application demo"}
```

### HTTP to HTTPS Automatic Redirection Verification
```bash
$ curl.exe -s -I http://d3v9i1q37unojr.cloudfront.net/api/health
```
**HTTP Response:**
```http
HTTP/1.1 301 Moved Permanently
Server: CloudFront
Location: https://d3v9i1q37unojr.cloudfront.net/api/health
X-Cache: Redirect from cloudfront
```

### IMDSv2 Hardening (Launch Template Version 2)
Verified in EC2 Launch Template `lt-082c76d29ef34f2c8`:
```json
{
  "MetadataOptions": {
    "HttpTokens": "required",
    "HttpPutResponseHopLimit": 1,
    "HttpEndpoint": "enabled"
  }
}
```
- `http_tokens = "required"`: Prevents unauthorized metadata queries without session token.
- `http_put_response_hop_limit = 1`: Neutralizes SSRF vulnerabilities by disallowing token forwarding outside the host.
