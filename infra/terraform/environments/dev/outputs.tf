# Cloud Run Service URLs
output "ingestion_url" {
  description = "Ingestion API Cloud Run service URL"
  value       = google_cloud_run_v2_service.ingestion.uri
}

output "state_manager_url" {
  description = "State Manager Cloud Run service URL"
  value       = google_cloud_run_v2_service.state_manager.uri
}

output "visualizer_url" {
  description = "Visualizer Cloud Run service URL"
  value       = google_cloud_run_v2_service.visualizer.uri
}

# Pub/Sub Topic IDs
output "telemetry_topic_id" {
  description = "Telemetry Pub/Sub topic ID"
  value       = module.telemetry_topic.topic_id
}

output "orders_topic_id" {
  description = "Orders Pub/Sub topic ID"
  value       = module.orders_topic.topic_id
}

output "commands_topic_id" {
  description = "Commands Pub/Sub topic ID"
  value       = module.commands_topic.topic_id
}

output "decisions_topic_id" {
  description = "Decisions Pub/Sub topic ID"
  value       = module.decisions_topic.topic_id
}

output "dead_letter_topic_id" {
  description = "Dead Letter Queue Pub/Sub topic ID"
  value       = module.dead_letter_topic.topic_id
}

# Firestore
output "firestore_database_name" {
  description = "Firestore database name"
  value       = google_firestore_database.drone_fleet.name
}

# Service Accounts
output "ingestion_sa_email" {
  description = "Ingestion service account email"
  value       = google_service_account.ingestion_api.email
}

output "state_manager_sa_email" {
  description = "State Manager service account email"
  value       = google_service_account.state_manager.email
}

output "optimizer_sa_email" {
  description = "Optimizer service account email"
  value       = google_service_account.optimizer.email
}

output "visualizer_sa_email" {
  description = "Visualizer service account email"
  value       = google_service_account.visualizer.email
}
