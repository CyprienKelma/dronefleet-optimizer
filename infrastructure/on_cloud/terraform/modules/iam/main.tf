# infrastructure/terraform/modules/iam/main.tf

# Service Account pour Ingestion API
resource "google_service_account" "ingestion_api" {
  account_id   = "ingestion-api"
  display_name = "Ingestion API Service Account"
  project      = var.project_id
  description  = "Used by Ingestion API to publish to Pub/Sub"
}

# Permissions: Publish to telemetry and orders topics
resource "google_pubsub_topic_iam_member" "ingestion_telemetry" {
  project = var.project_id
  topic   = var.telemetry_topic_name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.ingestion_api.email}"
}

resource "google_pubsub_topic_iam_member" "ingestion_orders" {
  project = var.project_id
  topic   = var.orders_topic_name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.ingestion_api.email}"
}

# Service Account pour State Manager
resource "google_service_account" "state_manager" {
  account_id   = "state-manager"
  display_name = "State Manager Service Account"
  project      = var.project_id
  description  = "Used by State Manager to consume Pub/Sub and write to Firestore"
}

# Permissions: Subscribe to all topics
resource "google_pubsub_subscription_iam_member" "state_manager_telemetry" {
  project      = var.project_id
  subscription = var.telemetry_subscription_name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:${google_service_account.state_manager.email}"
}

resource "google_pubsub_subscription_iam_member" "state_manager_orders" {
  project      = var.project_id
  subscription = var.orders_subscription_name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:${google_service_account.state_manager.email}"
}

resource "google_pubsub_subscription_iam_member" "state_manager_commands" {
  project      = var.project_id
  subscription = var.commands_subscription_name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:${google_service_account.state_manager.email}"
}

# Permissions: Read/Write Firestore
resource "google_project_iam_member" "state_manager_firestore" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.state_manager.email}"
}

# Service Account pour Optimizer
resource "google_service_account" "optimizer" {
  account_id   = "optimizer"
  display_name = "Optimizer Service Account"
  project      = var.project_id
  description  = "Used by Optimizer to read Firestore and publish commands"
}

# Permissions: Read Firestore (pas write)
resource "google_project_iam_member" "optimizer_firestore" {
  project = var.project_id
  role    = "roles/datastore.viewer" # Read-only
  member  = "serviceAccount:${google_service_account.optimizer.email}"
}

# Permissions: Publish to commands topic
resource "google_pubsub_topic_iam_member" "optimizer_commands" {
  project = var.project_id
  topic   = var.commands_topic_name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.optimizer.email}"
}

# Outputs (pour utiliser dans Cloud Run)
output "ingestion_api_sa_email" {
  value       = google_service_account.ingestion_api.email
  description = "Service Account email for Ingestion API"
}

output "state_manager_sa_email" {
  value       = google_service_account.state_manager.email
  description = "Service Account email for State Manager"
}

output "optimizer_sa_email" {
  value       = google_service_account.optimizer.email
  description = "Service Account email for Optimizer"
}
