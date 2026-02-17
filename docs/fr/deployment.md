# Déploiement

### Pipeline CI/CD

Utilisation de GitHub Actions avec deux workflows :

**1. Intégration Continue (`.github/workflows/ci.yml`)**
Déclenché sur PR et push vers main. Vérifie le Protobuf, lance les tests unitaires et linting par service, build Docker dry-run et validation Terraform.

**2. Déploiement Continu (`.github/workflows/cd-dev.yml`)**
Déclenché sur push main ou manuel. Applique Terraform, build et push les images Docker sur Artifact Registry, déploie sur Cloud Run/Cloud Run Jobs et configure Cloud Scheduler.

### Déploiement manuel (GCP Dev)

```bash
# Authentification
gcloud auth login
gcloud config set project drone-fleet-optimizer-dev

# Infrastructure
cd infra/terraform/environments/dev
terraform init
terraform apply

# Image (exemple Ingestion)
docker build -t europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest \
  -f services/ingestion/Dockerfile .
docker push europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest

# Cloud Run
gcloud run deploy ingestion \
  --image europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest \
  --region europe-west1 \
  --platform managed
```

### Monitoring et Observabilité

- **Logging** : Logs JSON structurés via Cloud Logging.
- **Metrics** : Métriques natives Cloud Run (latence, erreurs), Firestore et Pub/Sub.
- **Alertes** : Alertes budgétaires via Terraform, surveillance DLQ et taux d'erreurs élevé.
