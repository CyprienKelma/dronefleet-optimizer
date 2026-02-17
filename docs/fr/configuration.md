# Configuration

### Variables d'environnement

Chaque service lit sa configuration depuis les variables d'environnement chargées via les fichiers `.env` dans `configs/` :
- `configs/local.env` - Développement local avec émulateurs.
- `configs/dev.env` - Environnement de développement GCP.
- `configs/prod.env` - Environnement de production GCP.

| Variable | Description | Défaut (local) |
|----------|-------------|-----------------|
| `ENVIRONMENT` | Environnement de déploiement | `local` |
| `PROJECT_ID` | Identifiant du projet GCP | `local-emulator` |
| `PUBSUB_EMULATOR_HOST` | Adresse de l'émulateur Pub/Sub | `localhost:8085` |
| `FIRESTORE_EMULATOR_HOST` | Adresse de l'émulateur Firestore | `localhost:8080` |
| `STATE_MANAGER_URL` | URL de base du Gestionnaire d'état | `http://localhost:8080` |

### Configuration Terraform

L'infrastructure est gérée via Terraform avec un état séparé par environnement :

```
infra/terraform/
├── environments/
│   ├── dev/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── backend.tf
│   └── prod/
│       ├── main.tf
│       ├── variables.tf
│       └── backend.tf
└── modules/
    ├── cloud-run/
    ├── pubsub/
    ├── firestore/
    └── iam/
```
