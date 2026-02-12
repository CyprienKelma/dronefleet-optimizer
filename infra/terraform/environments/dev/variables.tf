variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "europe-west1"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  validation {
    condition     = can(regex("^(local|dev|prod)$", var.environment))
    error_message = "Environment must be 'local', 'dev', or 'prod'."
  }
}

variable "firestore_location" {
  description = "Firestore database location"
  type        = string
  default     = "europe-west1"
}

variable "min_instances" {
  description = "Minimum Cloud Run instances"
  type        = number
  default     = 0
  validation {
    condition     = var.min_instances >= 0 && var.min_instances <= 100
    error_message = "Min instances must be between 0 and 100."
  }
}

variable "log_level" {
  description = "Log level for the application"
  type        = string
  default     = "INFO"
  validation {
    condition     = can(regex("^(DEBUG|INFO|WARNING|ERROR)$", var.log_level))
    error_message = "Log level must be DEBUG, INFO, WARNING, or ERROR."
  }
}

variable "billing_account" {
  description = "Billing account ID for budget alerts (optional, format: XXXXXX-XXXXXX-XXXXXX)"
  type        = string
  default     = null
}

variable "budget_amount" {
  description = "Monthly budget amount in EUR"
  type        = number
  default     = 10
}
