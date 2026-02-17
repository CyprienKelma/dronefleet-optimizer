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

# variables
locals {
  common_labels = {
    environment = var.environment
    project     = "dronefleet"
    managed_by  = "terraform"
  }

  # sub names for IAM
  # static names instead of dynamic id avoid "known after apply" bugs
  ingestion_publisher_topics  = ["telemetry", "orders", "decisions"]
  state_manager_subscriptions = ["telemetry-sub", "orders-sub", "commands-sub", "decisions-sub"]

  # artifact registry image base path
  image_base = "${var.region}-docker.pkg.dev/${var.project_id}/drone-fleet"
}

# all services :

resource "google_project_service" "cloud_scheduler" {
  project = var.project_id
  service = "cloudscheduler.googleapis.com"

  disable_on_destroy = true
}

resource "google_project_service" "cloud_run" {
  project = var.project_id
  service = "run.googleapis.com"

  disable_on_destroy = true
}

resource "google_project_service" "cloud_billing_budget" {
  project = var.project_id
  service = "billingbudgets.googleapis.com"

  disable_on_destroy = false
}

# pubsub topics :

module "dead_letter_topic" {
  source = "../../modules/pubsub"

  project_id = var.project_id
  topic_name = "dead-letter-queue"
  labels     = local.common_labels
}

module "telemetry_topic" {
  source = "../../modules/pubsub"

  project_id        = var.project_id
  topic_name        = "telemetry"
  labels            = local.common_labels
  dead_letter_topic = module.dead_letter_topic.topic_id
}

module "orders_topic" {
  source = "../../modules/pubsub"

  project_id        = var.project_id
  topic_name        = "orders"
  labels            = local.common_labels
  dead_letter_topic = module.dead_letter_topic.topic_id
}

module "commands_topic" {
  source = "../../modules/pubsub"

  project_id        = var.project_id
  topic_name        = "commands"
  labels            = local.common_labels
  dead_letter_topic = module.dead_letter_topic.topic_id
}

module "decisions_topic" {
  source = "../../modules/pubsub"

  project_id        = var.project_id
  topic_name        = "decisions"
  labels            = local.common_labels
  dead_letter_topic = module.dead_letter_topic.topic_id
}

resource "google_firestore_database" "drone_fleet" {
  project     = var.project_id
  name        = "(default)"
  location_id = var.firestore_location
  type        = "FIRESTORE_NATIVE"

  delete_protection_state = "DELETE_PROTECTION_DISABLED"
  deletion_policy         = "DELETE"

  concurrency_mode            = "OPTIMISTIC"
  app_engine_integration_mode = "DISABLED"
}

resource "google_artifact_registry_repository" "drone_fleet" {
  location      = var.region
  repository_id = "drone-fleet"
  format        = "DOCKER"

  labels = local.common_labels
}

# all SA :

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

resource "google_service_account" "visualizer" {
  account_id   = "visualizer"
  display_name = "Visualizer Service Account"
  project      = var.project_id
}

# IAM for pubsub

# to pub orders/telemetry topics
resource "google_pubsub_topic_iam_member" "ingestion_publisher" {
  for_each = toset(local.ingestion_publisher_topics)

  project = var.project_id
  topic   = each.value
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.ingestion_api.email}"

  depends_on = [
    module.telemetry_topic,
    module.orders_topic
  ]
}

# to sub to telemetry-sub, orders-sub, commands-sub, decisions-sub
resource "google_pubsub_subscription_iam_member" "state_manager_subscriber" {
  for_each = toset(local.state_manager_subscriptions)

  project      = var.project_id
  subscription = each.value
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:${google_service_account.state_manager.email}"

  depends_on = [
    module.telemetry_topic,
    module.orders_topic,
    module.commands_topic,
    module.decisions_topic
  ]
}

# to pub to decisions topic
resource "google_pubsub_topic_iam_member" "optimizer_publisher" {
  project = var.project_id
  topic   = "decisions"
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.optimizer.email}"

  depends_on = [module.decisions_topic]
}

# visualizer needs to subscribe to telemetry-sub
resource "google_pubsub_subscription_iam_member" "visualizer_subscriber" {
  project      = var.project_id
  subscription = "telemetry-sub"
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:${google_service_account.visualizer.email}"

  depends_on = [module.telemetry_topic]
}

# pubsub SA need to be set as pub on DLQ to pub failed ones
resource "google_pubsub_topic_iam_member" "pubsub_dlq_publisher" {
  project = var.project_id
  topic   = "dead-letter-queue"
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"

  depends_on = [module.dead_letter_topic]
}

# pubsub SA need to be set as sub to sub failed ones
resource "google_pubsub_subscription_iam_member" "pubsub_dlq_subscriber" {
  for_each = toset(["telemetry-sub", "orders-sub", "commands-sub", "decisions-sub"])

  project      = var.project_id
  subscription = each.value
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"

  depends_on = [
    module.telemetry_topic,
    module.orders_topic,
    module.commands_topic,
    module.decisions_topic
  ]
}

# IAM for firestore :

resource "google_project_iam_member" "state_manager_firestore" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.state_manager.email}"
}

resource "google_project_iam_member" "optimizer_firestore" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.optimizer.email}"
}

# cloud scheduler to trgigers path-optimizer job
resource "google_cloud_scheduler_job" "trigger_optimizer" {
  name     = "trigger-path-optimizer"
  project  = var.project_id
  region   = var.region
  schedule = "* * * * *" # every minute

  description = "Triggers the path-optimizer Cloud Run Job every minute"
  time_zone   = "Europe/Paris"

  http_target {
    http_method = "POST"
    uri         = "https://${var.region}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${var.project_id}/jobs/path-optimizer:run"

    oauth_token {
      service_account_email = google_service_account.scheduler.email
      scope                 = "https://www.googleapis.com/auth/cloud-platform"
    }
  }

  depends_on = [google_project_service.cloud_scheduler]
}

# SA for cloud scheduler
resource "google_service_account" "scheduler" {
  account_id   = "scheduler"
  display_name = "Cloud Scheduler Service Account"
  project      = var.project_id
}

# permission to invoke cloud run jobs
resource "google_project_iam_member" "scheduler_run_invoker" {
  project = var.project_id
  role    = "roles/run.invoker"
  member  = "serviceAccount:${google_service_account.scheduler.email}"
}

# data sources
data "google_project" "current" {
  project_id = var.project_id
}

# SA for simulator — needs to call Ingestion API (Cloud Run Service) via HTTP
resource "google_service_account" "simulator" {
  account_id   = "simulator"
  display_name = "Simulator Service Account"
  project      = var.project_id
}

# SA for seed-firestore — needs direct Firestore write access
resource "google_service_account" "seed_firestore" {
  account_id   = "seed-firestore"
  display_name = "Seed Firestore Service Account"
  project      = var.project_id
}

# Seed SA needs Firestore access
resource "google_project_iam_member" "seed_firestore_datastore" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.seed_firestore.email}"
}

# Cloud Run Service: ingestion API
resource "google_cloud_run_v2_service" "ingestion" {
  name     = "ingestion"
  location = var.region
  project  = var.project_id
  labels   = local.common_labels

  template {
    service_account = google_service_account.ingestion_api.email

    scaling {
      min_instance_count = 0
      max_instance_count = 2
    }

    containers {
      image = "${local.image_base}/ingestion:latest"

      env {
        name  = "ENVIRONMENT"
        value = var.environment
      }
      env {
        name  = "PROJECT_ID"
        value = var.project_id
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }
    }
  }

  depends_on = [google_project_service.cloud_run]

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image,
      template[0].containers[0].env,
    ]
  }
}

# Cloud Run Service: state-manager
resource "google_cloud_run_v2_service" "state_manager" {
  name     = "state-manager"
  location = var.region
  project  = var.project_id
  labels   = local.common_labels

  template {
    service_account = google_service_account.state_manager.email

    scaling {
      min_instance_count = 1
      max_instance_count = 3
    }

    containers {
      image = "${local.image_base}/state-manager:latest"

      env {
        name  = "ENVIRONMENT"
        value = var.environment
      }
      env {
        name  = "PROJECT_ID"
        value = var.project_id
      }
      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.environment
      }

      resources {
        limits = {
          cpu    = "2"
          memory = "1Gi"
        }
      }
    }
  }

  depends_on = [google_project_service.cloud_run]

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image,
      template[0].containers[0].env,
    ]
  }
}

# Cloud Run Service: visualizer
resource "google_cloud_run_v2_service" "visualizer" {
  name     = "visualizer"
  location = var.region
  project  = var.project_id
  labels   = local.common_labels

  template {
    service_account = google_service_account.visualizer.email

    scaling {
      min_instance_count = 0
      max_instance_count = 2
    }

    containers {
      image = "${local.image_base}/visualizer:latest"

      env {
        name  = "PROJECT_ID"
        value = var.project_id
      }
      env {
        name  = "NODE_ENV"
        value = "production"
      }
      env {
        name  = "PUBSUB_SUBSCRIPTION"
        value = "telemetry-sub"
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }
    }
  }

  depends_on = [google_project_service.cloud_run]

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image,
      template[0].containers[0].env,
    ]
  }
}

# Cloud Run Job: path-optimizer
resource "google_cloud_run_v2_job" "path_optimizer" {
  name     = "path-optimizer"
  location = var.region
  project  = var.project_id
  labels   = local.common_labels

  template {
    template {
      service_account = google_service_account.optimizer.email
      timeout         = "300s"
      max_retries     = 1

      containers {
        image = "${local.image_base}/path-optimizer:latest"

        env {
          name  = "ENVIRONMENT"
          value = var.environment
        }
        env {
          name  = "PROJECT_ID"
          value = var.project_id
        }
        env {
          name  = "STATE_MANAGER_URL"
          value = google_cloud_run_v2_service.state_manager.uri
        }

        resources {
          limits = {
            cpu    = "2"
            memory = "2Gi"
          }
        }
      }
    }
  }

  depends_on = [google_project_service.cloud_run]

  lifecycle {
    ignore_changes = [
      template[0].template[0].containers[0].image,
      template[0].template[0].containers[0].env,
    ]
  }
}

# Cloud Run Job: simulator
resource "google_cloud_run_v2_job" "simulator" {
  name     = "simulator"
  location = var.region
  project  = var.project_id
  labels   = local.common_labels

  template {
    template {
      service_account = google_service_account.simulator.email
      timeout         = "600s"
      max_retries     = 0

      containers {
        image = "${local.image_base}/simulator:latest"

        env {
          name  = "ENVIRONMENT"
          value = var.environment
        }
        env {
          name  = "SIMULATION_DURATION_SECONDS"
          value = "300"
        }
        env {
          name  = "INGESTION_API_URL"
          value = google_cloud_run_v2_service.ingestion.uri
        }

        resources {
          limits = {
            cpu    = "1"
            memory = "512Mi"
          }
        }
      }
    }
  }

  depends_on = [google_project_service.cloud_run]

  lifecycle {
    ignore_changes = [
      template[0].template[0].containers[0].image,
      template[0].template[0].containers[0].env,
    ]
  }
}

# cloud Run Job: seed-firestore (base data)
resource "google_cloud_run_v2_job" "seed_firestore" {
  name     = "seed-firestore"
  location = var.region
  project  = var.project_id
  labels   = local.common_labels

  template {
    template {
      service_account = google_service_account.seed_firestore.email
      timeout         = "120s"
      max_retries     = 0

      containers {
        image = "${local.image_base}/seed-firestore:latest"

        env {
          name  = "PROJECT_ID"
          value = var.project_id
        }
        env {
          name  = "DATASET_SIZE"
          value = "large"
        }

        resources {
          limits = {
            cpu    = "1"
            memory = "512Mi"
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.cloud_run,
    google_firestore_database.drone_fleet
  ]

  lifecycle {
    ignore_changes = [
      template[0].template[0].containers[0].image,
      template[0].template[0].containers[0].env,
    ]
  }
}

# alert on budget
resource "google_billing_budget" "dev_budget" {
  count = var.billing_account != null ? 1 : 0

  billing_account = var.billing_account
  display_name    = "DroneFleet Optimizer - DEV - ${var.budget_amount} EUR"

  budget_filter {
    projects = ["projects/${data.google_project.current.number}"]
  }

  amount {
    specified_amount {
      currency_code = "EUR"
      units         = tostring(var.budget_amount)
    }
  }

  threshold_rules {
    threshold_percent = 0.5
  }
  threshold_rules {
    threshold_percent = 0.8
  }
  threshold_rules {
    threshold_percent = 1.0
  }

  depends_on = [google_project_service.cloud_billing_budget]
}
