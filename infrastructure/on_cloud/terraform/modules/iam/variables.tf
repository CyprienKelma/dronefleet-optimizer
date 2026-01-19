variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "telemetry_topic_name" {
  description = "Name of the telemetry Pub/Sub topic"
  type        = string
}

variable "orders_topic_name" {
  description = "Name of the orders Pub/Sub topic"
  type        = string
}

variable "telemetry_subscription_name" {
  description = "Name of the telemetry Pub/Sub subscription"
  type        = string
}

variable "orders_subscription_name" {
  description = "Name of the orders Pub/Sub subscription"
  type        = string
}

variable "commands_subscription_name" {
  description = "Name of the commands Pub/Sub subscription"
  type        = string
}

variable "commands_topic_name" {
  description = "Name of the commands Pub/Sub topic"
  type        = string
}
