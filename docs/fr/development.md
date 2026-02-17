# Développement

### Standards de qualité de code

**Services Python :** Linting avec `ruff`, types avec `mypy` (mode strict), formatage `ruff format`, tests `pytest` avec couverture.

**Services Java :** Linting Checkstyle (Style Google), formatage Spotless, tests JUnit 5.

**Services TypeScript :** Linting et formatage Biome, mode strict TypeScript.

### Hooks de pré-commit
Le dépôt utilise des hooks `pre-commit` pour assurer la synchronisation des modèles Protobuf, le formatage, le linting et les conventions de messages de commit.

```bash
pre-commit install
```

### Tests

**Tests Unitaires :**

```bash
# Services Python
cd services/ingestion
uv run pytest tests/

# Services Java
cd services/state_manager
./gradlew test
```

**Tests d'Intégration :** Situés dans `tests/integration/` — testent les flux complets avec émulateurs.

**Tests End-to-End :** Situés dans `tests/e2e/` — testent le système complet avec flotte de drones simulée.
