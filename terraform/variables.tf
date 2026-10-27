variable "aws_region" {
  description = "AWS region for CloudPulse"
  type        = string
  default     = "ap-south-1"
}
variable "project_name" {
  type    = string
  default = "cloudpulse"
}
variable "instance_type" {
  description = "Choose a type available to your account and budget"
  type        = string
  default     = "t3.micro"
}
variable "min_size" {
  type    = number
  default = 2
}
variable "max_size" {
  type    = number
  default = 3
}
variable "desired_capacity" {
  type    = number
  default = 2
}

variable "container_image" {
  description = "Public container image URI, for example docker.io/yourname/cloudpulse:1.0.0"
  type        = string
}
