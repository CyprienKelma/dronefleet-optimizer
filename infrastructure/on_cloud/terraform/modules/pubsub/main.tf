# Create a Pub/Sub topic
resource "google_pubsub_topic" "topic" {
  name    = var.topic_name
  project = var.project_id

  labels = var.labels

  message_retention_duration = var.message_retention_duration
}

# Create a subscription with Dead Letter Queue
resource "google_pubsub_subscription" "subscription" {
  name    = "${var.topic_name}-sub"
  topic   = google_pubsub_topic.topic.name
  project = var.project_id

  labels = var.labels

  ack_deadline_seconds = var.ack_deadline_seconds

  retry_policy {
    minimum_backoff = var.retry_minimum_backoff
    maximum_backoff = var.retry_maximum_backoff
  }

  dead_letter_policy {
    dead_letter_topic     = var.dead_letter_topic
    max_delivery_attempts = var.max_delivery_attempts
  }

  enable_message_ordering = var.enable_message_ordering
}
