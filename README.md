# CloudPulse — Self-Healing Cloud Application Platform

CloudPulse is a learning project demonstrating a Java/Spring Boot health API, Docker packaging, GitHub Actions CI, and AWS infrastructure-as-code for an Application Load Balancer and Auto Scaling Group.

## Architecture

Client → Application Load Balancer → EC2 Auto Scaling Group → Spring Boot container  
ALB health checks use `GET /api/health`. The Auto Scaling Group uses ELB health checks to replace unhealthy instances. CloudWatch tracks EC2 CPU metrics.

## Stack

- Java 21, Spring Boot, Maven
- Docker
- GitHub Actions
- Terraform, AWS EC2, ALB, Auto Scaling, CloudWatch

## Run locally

Prerequisites: JDK 21 and Maven.

```bash
mvn clean verify
mvn spring-boot:run
```

Try:
- `http://localhost:8080/api/health`
- `http://localhost:8080/api/info`
- `http://localhost:8080/actuator/health`

Build and run container:

```bash
mvn clean package
docker build -t cloudpulse:local .
docker run --rm -p 8080:8080 cloudpulse:local
```

## CI

Push to `main` or `master`; GitHub Actions runs tests, packages the app, and builds a Docker image.

## AWS / Terraform warning

**This is a learning starter, not a production-ready deployment.** First build and publish your CloudPulse image to a public registry, then set `container_image` in your local `terraform.tfvars` to that image URI. The instances pull and run that image on port 8080. Verify your default VPC has suitable subnets in at least two Availability Zones. Review all costs before creating resources.

To inspect the plan:

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform fmt -recursive
terraform validate
terraform plan
```

Only apply after reviewing the plan and completing the image configuration. Resources such as an ALB and EC2 instances can incur charges. When finished, run `terraform destroy` from this directory and confirm the plan before approving.

## Security

- Never commit AWS access keys, tokens, `.env` files, or real `terraform.tfvars`.
- Prefer AWS IAM roles and short-lived credentials.
- Restrict public ingress where possible.
- Use versioned, trusted container images.

## Current limitations

- The GitHub Actions workflow tests and builds locally in CI; it does not yet publish or deploy the image.
- The CPU alarm is observability only; a scaling policy/action still needs to be attached to make CPU-based scale-out automatic.
- CloudWatch Agent/log shipping is not configured in this starter.
