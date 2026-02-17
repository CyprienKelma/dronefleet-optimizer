# Deployment

## CI/CD Pipeline

The project uses GitHub Actions with two workflows:

**1. Continuous Integration (`.github/workflows/ci.yml`)**

Triggered on: Pull requests to main, pushes to main

Steps:
1. Detect changed services via path filters
2. Protobuf validation (lint, breaking changes)
3. Service-specific checks (lint, type check, unit tests)
4. Docker build dry-run
5. Terraform validation

**2. Continuous Deployment (`.github/workflows/cd-dev.yml`)**

Triggered on: Push to main, manual dispatch

Steps:
1. Detect changed services
2. Terraform apply (infrastructure updates)
3. Build and push Docker images to Artifact Registry
4. Deploy services to Cloud Run
5. Deploy Cloud Run Job for optimizer
6. Configure Cloud Scheduler trigger

## Deployment to GCP Dev Environment

```bash
# Authenticate to GCP
gcloud auth login
gcloud config set project drone-fleet-optimizer-dev

# Deploy infrastructure
cd infra/terraform/environments/dev
terraform init
terraform apply

# Deploy services (handled by CI/CD, or manually)
# Build and push images
docker build -t europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest \
  -f services/ingestion/Dockerfile .
docker push europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest

# Deploy to Cloud Run
gcloud run deploy ingestion \
  --image europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest \
  --region europe-west1 \
  --platform managed
```

## Monitoring and Observability

**Logging:**
- Structured JSON logs via `dronefleet_shared.utils.logging_config`
- Google Cloud Logging integration
- Log levels: DEBUG (local), INFO (dev), WARN (prod)

**Metrics:**
- Cloud Run built-in metrics (request count, latency, errors)
- Firestore operation metrics
- Pub/Sub message delivery metrics

**Alerts:**
- Budget alerts configured via Terraform
- Dead Letter Queue monitoring
- High error rate alerts (configured in GCP)
