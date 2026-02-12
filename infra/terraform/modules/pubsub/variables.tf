variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "topic_name" {
  description = "Name of the Pub/Sub topic"
  type        = string
}

variable "labels" {
  description = "Labels to apply to resources"
  type        = map(string)
  default     = {}
}

variable "message_retention_duration" {
  description = "Message retention duration"
  type        = string
  default     = "604800s" # 7 days
}

variable "ack_deadline_seconds" {
  description = "Acknowledgment deadline in seconds"
  type        = number
  default     = 60
}

variable "retry_minimum_backoff" {
  description = "Minimum retry backoff"
  type        = string
  default     = "10s"
}

variable "retry_maximum_backoff" {
  description = "Maximum retry backoff"
  type        = string
  default     = "600s"
}

variable "dead_letter_topic" {
  description = "Dead letter topic for failed messages (null to disable DLQ)"
  type        = string
  default     = null
}

variable "max_delivery_attempts" {
  description = "Maximum delivery attempts before DLQ"
  type        = number
  default     = 5
}

variable "enable_message_ordering" {
  description = "Enable message ordering"
  type        = bool
  default     = false
}
