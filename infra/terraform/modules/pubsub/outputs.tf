output "topic_id" {
  description = "The ID of the Pub/Sub topic"
  value       = google_pubsub_topic.topic.id
}

output "subscription_id" {
  description = "The ID of the Pub/Sub subscription"
  value       = google_pubsub_subscription.subscription.id
}
