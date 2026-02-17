# Configuration

## Environment Variables

Each service reads configuration from environment variables loaded via `.env` files in `configs/`:

- `configs/local.env` - Local development with emulators
- `configs/dev.env` - GCP dev environment
- `configs/prod.env` - GCP production environment

**Key Variables:**

| Variable | Description | Default (local) |
|----------|-------------|-----------------|
| `ENVIRONMENT` | Deployment environment | `local` |
| `PROJECT_ID` | GCP project ID | `local-emulator` |
| `PUBSUB_EMULATOR_HOST` | Pub/Sub emulator address | `localhost:8085` |
| `FIRESTORE_EMULATOR_HOST` | Firestore emulator address | `localhost:8080` |
| `STATE_MANAGER_URL` | State Manager base URL | `http://localhost:8080` |

## Terraform Configuration

Infrastructure is managed via Terraform with separate state per environment:

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
