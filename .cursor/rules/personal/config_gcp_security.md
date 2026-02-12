## 3. Workload Identity Federation (WIF) -- explication detaillee

Pourquoi WIF ?
Avant WIF, pour que GitHub Actions puisse parler a GCP, tu devais :
1. Creer un Service Account
2. Exporter une clef JSON
3. La stocker en secret GitHub
4. L'utiliser dans le workflow
Probleme : cette clef ne expire jamais, si elle leak c'est game over.
WIF remplace ca par un echange de tokens ephemeres :
```
GitHub Actions                          Google Cloud
     │                                       │
     │  1. GitHub genere un OIDC token       │
     │     (JWT signe par GitHub, valide     │
     │      ~10min, contient le repo/branch) │
     │                                       │
     │  2. Envoie le JWT a Google STS ──────>│
     │                                       │
     │  3. Google verifie:                   │
     │     - Le JWT est signe par GitHub?    │
     │     - Le repo est autorise?           │
     │     - La branch est autorisee?        │
     │                                       │
     │  4. Google renvoie un token GCP <─────│
     │     temporaire (~1h)                  │
     │                                       │
     │  5. GitHub utilise ce token pour      │
     │     docker push, gcloud run deploy    │
     └───────────────────────────────────────┘
```
Zero clef stockee. Le token expire en 1h. On peut restreindre par repo ET par branch.
Comment configurer (etape par etape)
Prerequis : ton projet GCP drone-fleet-optimizer-dev existe deja, les APIs sont activees. Il faut aussi activer l'API IAM Credentials :
gcloud services enable iamcredentials.googleapis.com \
  --project=drone-fleet-optimizer-dev
Etape 1 : Creer le Workload Identity Pool
gcloud iam workload-identity-pools create "github-pool" \
  --project="drone-fleet-optimizer-dev" \
  --location="global" \
  --display-name="GitHub Actions Pool"
Le pool est un conteneur logique pour grouper les providers d'identite externes.
Etape 2 : Creer le Provider (lie a GitHub)
gcloud iam workload-identity-pools providers create-oidc "github-provider" \
  --project="drone-fleet-optimizer-dev" \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --display-name="GitHub Provider" \
  --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
  --attribute-condition="assertion.repository_owner == 'TON_USERNAME_GITHUB'" \
  --issuer-uri="https://token.actions.githubusercontent.com"
Points importants :
- attribute-condition restreint aux repos de ton compte GitHub uniquement
- attribute-mapping mappe les claims du JWT GitHub vers des attributs Google
- issuer-uri est l'endpoint OIDC de GitHub (fixe, toujours le meme)
Etape 3 : Creer le Service Account deployer
gcloud iam service-accounts create "github-deployer" \
  --project="drone-fleet-optimizer-dev" \
  --display-name="GitHub Actions Deployer"
Etape 4 : Donner les roles au deployer
PROJECT=drone-fleet-optimizer-dev
SA=github-deployer@${PROJECT}.iam.gserviceaccount.com
# Push des images Docker
gcloud projects add-iam-policy-binding $PROJECT \
  --member="serviceAccount:${SA}" \
  --role="roles/artifactregistry.writer"
# Deploy sur Cloud Run
gcloud projects add-iam-policy-binding $PROJECT \
  --member="serviceAccount:${SA}" \
  --role="roles/run.admin"
# Impersonation des SA des services (pour assigner les SA aux Cloud Run)
gcloud projects add-iam-policy-binding $PROJECT \
  --member="serviceAccount:${SA}" \
  --role="roles/iam.serviceAccountUser"
Etape 5 : Autoriser GitHub a impersonner le SA
POOL_ID=$(gcloud iam workload-identity-pools describe "github-pool" \
  --project="drone-fleet-optimizer-dev" \
  --location="global" \
  --format="value(name)")
# Autoriser UNIQUEMENT ton repo specifique
gcloud iam service-accounts add-iam-policy-binding \
  "github-deployer@drone-fleet-optimizer-dev.iam.gserviceaccount.com" \
  --project="drone-fleet-optimizer-dev" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/${POOL_ID}/attribute.repository/TON_USERNAME/drone-fleet-optimizer"
C'est ici que la securite est la plus forte : seul ton repo sur ton compte peut obtenir un token.
Etape 6 : Recuperer les valeurs pour GitHub Secrets
# WIF Provider (format complet)
gcloud iam workload-identity-pools providers describe "github-provider" \
  --project="drone-fleet-optimizer-dev" \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --format="value(name)"
# Output: projects/123456/locations/global/workloadIdentityPools/github-pool/providers/github-provider
Etape 7 : Configurer les secrets GitHub
Dans ton repo GitHub > Settings > Secrets and variables > Actions :
- WIF_PROVIDER_DEV = projects/123456/locations/global/workloadIdentityPools/github-pool/providers/github-provider
- WIF_SERVICE_ACCOUNT_DEV = github-deployer@drone-fleet-optimizer-dev.iam.gserviceaccount.com
Etape 8 : Utilisation dans le workflow
- uses: google-github-actions/auth@v2
  with:
    workload_identity_provider: ${{ secrets.WIF_PROVIDER_DEV }}
    service_account: ${{ secrets.WIF_SERVICE_ACCOUNT_DEV }}
C'est tout. Pas de clef JSON, pas de secret qui expire, tracabilite complete.
