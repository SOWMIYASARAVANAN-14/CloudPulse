output "application_url" {
  description = "CloudPulse load balancer URL (once the app image is configured)"
  value       = "http://${aws_lb.app.dns_name}"
}
output "autoscaling_group_name" {
  value = aws_autoscaling_group.app.name
}
