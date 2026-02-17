# Mise en place

### Prérequis

- **Docker** et Docker Compose.
- **Mise** (gestionnaire de versions d'outils polyglotte) - [Installation](https://mise.jdx.dev/).
- **uv** (gestionnaire de paquets Python) - Installé via mise.
- **Buf** (outillage Protobuf) - Installé via mise.
- **Java 21** (distribution Temurin).
- **Bun** (runtime TypeScript).

### Configuration locale

1. **Cloner le dépôt**

```bash
git clone https://github.com/votreutilisateur/drone-fleet-optimizer.git
cd drone-fleet-optimizer
```

2. **Installer les outils via mise**

```bash
mise install
```

3. **Générer les modèles partagés à partir des définitions Protobuf**

```bash
mise run //shared/proto:generate
```

4. **Lancer l'infrastructure avec Docker Compose**

```bash
cd infra/local
docker-compose up -d --build
```

Ceci démarre :
- L'émulateur Pub/Sub (port 8085)
- L'émulateur Firestore (port 8080)

5. **Créer les topics Pub/Sub**

```bash
mise run //infra/local:create-topics
```

6. **Démarrer les services (dans des terminaux séparés)**

```bash
# API d'ingestion
cd services/ingestion
mise run dev

# Gestionnaire d'état (State Manager)
cd services/state_manager
./gradlew bootRun --args='--spring.profiles.active=local'

# Optimiseur de trajectoire (déclenchement manuel pour test)
cd services/path_optimizer
mise run start

# Simulateur
cd services/simulators
mise run dev
```

7. **Vérifier que le système fonctionne**

Consultez l'interface de l'émulateur Firestore : http://localhost:4000
Consultez la documentation de l'API d'ingestion : http://localhost:8000/docs
