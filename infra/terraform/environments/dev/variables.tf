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

variable "billing_account" {
  description = "Billing account ID for budget alerts (optional, format: XXXXXX-XXXXXX-XXXXXX)"
  type        = string
  default     = null
}

variable "budget_amount" {
  description = "Monthly budget amount in EUR"
  type        = number
  default     = 5
}
