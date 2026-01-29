# Store Terraform state in GCS (avoid losing state)
terraform {
  backend "gcs" {
    bucket = "drone-fleet-optimizer-terraform-state-dev"
    prefix = "terraform/state"
  }
}
