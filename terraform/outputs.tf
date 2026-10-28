output "application_url" {
  description = "CloudPulse load balancer URL (once the app image is configured)"
  value       = "http://${aws_lb.app.dns_name}"
}
output "autoscaling_group_name" {
  value = aws_autoscaling_group.app.name
}
output "sns_topic_arn" {
  description = "ARN of the SNS topic for CloudPulse alerts"
  value       = aws_sns_topic.alerts.arn
}
output "cloudfront_https_url" {
  description = "Browser-accessible HTTPS URL for resume and public demo"
  value       = "https://${aws_cloudfront_distribution.app.domain_name}"
}
