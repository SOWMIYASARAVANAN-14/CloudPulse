# CloudPulse Technical Interview Guide & Resume Talking Points

This document provides architectural rationales, trade-off analyses, and interview answers for describing the CloudPulse project.

---

## 1. Resume Bullet Points (Copy & Adapt)

- **Cloud Infrastructure & IaC**: Architected and provisioned a highly available AWS infrastructure using Terraform, incorporating an Application Load Balancer (ALB), Auto Scaling Group (ASG), and CloudFront CDN across multiple Availability Zones in `ap-south-1`.
- **Automated CI/CD & Rolling Deployments**: Implemented a GitHub Actions pipeline building containerized Java 21 Spring Boot microservices, pushing multi-tagged Docker images, and executing zero-downtime rolling updates via AWS Auto Scaling Instance Refreshes with post-deployment canary health checks.
- **Elasticity & Autoscaling**: Configured target-tracking scaling policies maintaining 50% average EC2 CPU utilization, validated via controlled load scaling tests and round-robin traffic alternation across healthy target group instances.
- **Monitoring & Incident Response**: Designed dual-action CloudWatch metric alarms monitoring cluster CPU utilization with automated SNS notifications for both alarm breaches and recovery events delivered to operational emails.
- **Security Engineering & IMDSv2 Hardening**: Hardened EC2 instances by enforcing IMDSv2 with hop limit 1 to prevent SSRF vulnerabilities, implemented least-privilege IAM policies, and deployed CloudFront Edge TLS termination with zero caching for dynamic API endpoints.

---

## 2. Frequently Asked Technical Questions & Answers

### Q1: Why did you choose Amazon CloudFront in front of an Application Load Balancer?
**Answer**:
1. **Edge SSL/TLS Termination**: CloudFront provides a browser-trusted HTTPS endpoint using its free default wildcard certificate (`*.cloudfront.net`), eliminating the requirement and recurring cost of acquiring and managing a custom domain and ACM certificate.
2. **DDoS Protection**: Traffic terminates at AWS edge locations, shielding the backend ALB from direct Layer 3/4 network attacks through AWS Shield Standard.
3. **Optimized AWS Global Backbone**: Viewer requests connect to the nearest CloudFront Point of Presence (PoP) and travel across AWS's low-latency, private global network rather than the public internet.

### Q2: What is the difference between Edge TLS termination and End-to-End TLS in your setup?
**Answer**:
In our architecture, the client communicates with CloudFront over HTTPS on port 443 with TLS 1.2/1.3. However, CloudFront forwards requests to the ALB over HTTP port 80 (`origin_protocol_policy = "http-only"`). This is known as **Edge TLS Termination** or **SSL Offloading**. 
Because AWS ALBs cannot directly hold CloudFront's default certificate and ACM requires an owned custom domain for certificate validation, terminating TLS at the edge allows for a secure, browser-accessible demo without paying for or managing a third-party domain.

### Q3: Why is IMDSv2 important, and why set the hop limit to 1?
**Answer**:
Instance Metadata Service Version 1 (IMDSv1) relies on simple HTTP GET requests without session authentication. If a web application contains a Server-Side Request Forgery (SSRF) vulnerability or misconfigured open proxy, an attacker can trick the server into querying `http://169.254.169.254/latest/meta-data/iam/security-credentials/` to steal IAM role credentials.
IMDSv2 mitigates this by requiring a session token obtained via an HTTP PUT request with custom headers. Setting the `http_put_response_hop_limit = 1` ensures that IP packets cannot traverse network routers or Docker bridge interfaces, making it impossible for container escapes or forwarded requests to access the host's metadata credentials.

### Q4: Why did you choose Target Tracking over Simple or Step Scaling?
**Answer**:
Target Tracking operates like a thermostat: you specify the desired metric value (e.g., 50% CPU utilization), and AWS automatically calculates the number of instances to add or remove proportional to the load fluctuation. 
Unlike Simple Scaling (which adds a fixed number of instances regardless of load severity and requires manual cooldown calculations), Target Tracking continuously adjusts capacity to mirror real-time demand, smoothing out scaling spikes and minimizing instance thrashing.

### Q5: How did you achieve zero downtime during application deployments?
**Answer**:
In our GitHub Actions workflow, after pushing the new Docker image, the pipeline calls `aws autoscaling start-instance-refresh` with `min-healthy-percentage = 50`. 
1. Auto Scaling terminates an old instance while launching a replacement node with the new Launch Template.
2. The ALB target group holds traffic until the new container passes two consecutive `GET /api/health` checks (healthy threshold).
3. Auto Scaling initiates connection draining (deregistration delay) on the old instance, allowing active in-flight HTTP requests to complete before terminating it.
4. Only when the new instance is confirmed `healthy` does the refresh process terminate the remaining old instances.

### Q6: Why did you use `Managed-CachingDisabled` and `Managed-AllViewerExceptHostHeader` for CloudFront?
**Answer**:
- **CachingDisabled (`4135ea2d...`)**: CloudPulse is a dynamic health and monitoring microservice where responses include live hostnames, container IDs, and timestamps. Setting cache TTL to 0 ensures callers always receive fresh real-time metrics.
- **AllViewerExceptHostHeader (`b680b3d7...`)**: ALBs match incoming traffic using listener rules and DNS names. Forwarding the viewer's `Host` header (`*.cloudfront.net`) would cause the ALB to fail routing or reject the request. This policy replaces the `Host` header with the ALB's DNS name while forwarding all other viewer headers (such as `X-Stress-Key` and `Accept`) and query strings down to the application.
