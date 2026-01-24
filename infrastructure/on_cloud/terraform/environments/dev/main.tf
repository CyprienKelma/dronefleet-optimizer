terraform {
  required_version = ">= 1.9.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Locals for common tags
locals {
  common_labels = {
    environment = var.environment
    project     = "dronefleet"
    managed_by  = "terraform"
  }

  # Static topic and subscription names for IAM binding
  # Use static names instead of dynamic IDs to avoid "known after apply" errors
  ingestion_publisher_topics  = ["telemetry", "orders"]
  state_manager_subscriptions = ["telemetry-sub", "orders-sub", "commands-sub"]
}

# Dead Letter Queue (DLQ) topic
module "dead_letter_topic" {
  source = "../../modules/pubsub"

  project_id        = var.project_id
  topic_name        = "dead-letter-queue"
  labels            = local.common_labels
  dead_letter_topic = module.dead_letter_topic.topic_id
}

# Telemetry Topic
module "telemetry_topic" {
  source = "../../modules/pubsub"

  project_id        = var.project_id
  topic_name        = "telemetry"
  labels            = local.common_labels
  dead_letter_topic = module.dead_letter_topic.topic_id
}

# Orders Topic
module "orders_topic" {
  source = "../../modules/pubsub"

  project_id        = var.project_id
  topic_name        = "orders"
  labels            = local.common_labels
  dead_letter_topic = module.dead_letter_topic.topic_id
}

# Commands Topic
module "commands_topic" {
  source = "../../modules/pubsub"

  project_id        = var.project_id
  topic_name        = "commands"
  labels            = local.common_labels
  dead_letter_topic = module.dead_letter_topic.topic_id
}

# module "iam" {
#   source = "../../modules/iam"

#   project_id                  = var.project_id
#   telemetry_topic_name        = module.pubsub_telemetry.topic_name
#   telemetry_subscription_name = module.pubsub_telemetry.subscription_name
#   orders_topic_name           = module.pubsub_orders.topic_name
#   orders_subscription_name    = module.pubsub_orders.subscription_name
#   commands_topic_name         = module.pubsub_commands.topic_name
#   commands_subscription_name  = module.pubsub_commands.subscription_name
# }

# Firestore Database
resource "google_firestore_database" "drone_fleet" {
  project     = var.project_id
  name        = "(default)"
  location_id = var.firestore_location
  type        = "FIRESTORE_NATIVE"

  # For dev, we can use a smaller instance
  concurrency_mode            = "OPTIMISTIC"
  app_engine_integration_mode = "DISABLED"
}

# Artifact Registry (for Docker images)
resource "google_artifact_registry_repository" "drone_fleet" {
  location      = var.region
  repository_id = "drone-fleet"
  format        = "DOCKER"

  labels = local.common_labels
}

# Service Accounts
resource "google_service_account" "ingestion_api" {
  account_id   = "ingestion"
  display_name = "Ingestion API Service Account"
  project      = var.project_id
}

resource "google_service_account" "state_manager" {
  account_id   = "state-manager"
  display_name = "State Manager Service Account"
  project      = var.project_id
}

resource "google_service_account" "optimizer" {
  account_id   = "optimizer"
  display_name = "Optimizer Service Account"
  project      = var.project_id
}

# IAM Permissions
# Ingestion API can publish to Pub/Sub topics
resource "google_pubsub_topic_iam_member" "ingestion_publisher" {
  for_each = toset(local.ingestion_publisher_topics)

  project = var.project_id
  topic   = each.value # Static topic name (telemetry, orders)
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.ingestion_api.email}"

  # Ensure topics exist before granting IAM permissions
  depends_on = [
    module.telemetry_topic,
    module.orders_topic
  ]
}

# State Manager can subscribe to Pub/Sub subscriptions
resource "google_pubsub_subscription_iam_member" "state_manager_subscriber" {
  for_each = toset(local.state_manager_subscriptions)

  project      = var.project_id
  subscription = each.value # Static subscription name (telemetry-sub, orders-sub, commands-sub)
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:${google_service_account.state_manager.email}"

  # Ensure subscriptions exist before granting IAM permissions
  depends_on = [
    module.telemetry_topic,
    module.orders_topic,
    module.commands_topic
  ]
}

# State Manager can write to Firestore
resource "google_project_iam_member" "state_manager_firestore" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.state_manager.email}"
}

# Optimizer can read Firestore and publish commands
resource "google_project_iam_member" "optimizer_firestore" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.optimizer.email}"
}

resource "google_pubsub_topic_iam_member" "optimizer_publisher" {
  project = var.project_id
  topic   = "commands" # Static topic name instead of dynamic ID
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.optimizer.email}"

  # Ensure topic exists before granting IAM permissions
  depends_on = [module.commands_topic]
}
