/configs
  shared.env
  prod.env
  prod.env
  dev.env

/docs
  getting_started.md
  architecture.md
  logical_stream.md
  solver.md

/infra
  prod.tfvars
  dev.tfvars
  /local
    /scripts
      create_topics.py
    docker-compose.yml
  /terraform
    main.tf
    variables.tf
    outputs.tf
  /scripts
    test_*.py
  mise.toml

/services
  /path_optimizer
    /src
      /path_optimizer  # <--- HINT: Use a named subdir inside src for cleaner imports (e.g., import path_optimizer.main)
        __init__.py
        main.py
    pyproject.toml
    config.yml
    Dockerfile # <--- HINT: Must be built from repo root: `docker build -f services/path_optimizer/Dockerfile .`
    mise.toml

  /state_manager
    /src/main/java/com/dronefleet/statemanager  # <--- HINT: Directory depth must match package declaration
      Application.java
      ...
    build.gradle
    config.yml
    Dockerfile # <--- HINT: Copy parent 'shared' & 'libs' folders in Dockerfile
    mise.toml

  /simulators
    /src/simulators # <--- HINT: Named python package dir
      ...
    pyproject.toml
    config.yml
    Dockerfile
    mise.toml

  /ingestion
    /src/ingestion # <--- HINT: Named python package dir
      ...
    pyproject.toml
    config.yml
    Dockerfile
    mise.toml

  /visualizer
    /src
      index.ts
      ...
    package.json
    tsconfig.json
    config.yml
    Dockerfile
    mise.toml

  /ps_bridge
    /src
      index.ts
      ...
    package.json
    tsconfig.json
    config.yml
    Dockerfile
    mise.toml

/shared
  /java
    build.gradle
    mise.toml
    /src/main/java/com/dronefleet/shared # <--- HINT: Namespace your shared code to avoid collisions
      /models
        Drone.java
      /utils
        GlobalConfig.java
  /python
    pyproject.toml
    mise.toml
    /src/dronefleet_shared # <--- HINT: CRITICAL: Do not put files directly in src/. Use a unique package name folder.
      __init__.py
      /models
        drone.py
      /utils
        global_config.py
  /ts
    package.json
    tsconfig.json
    mise.toml
    /src
      /models
        drone.ts
      /utils
        globalConfig.ts

/libs
  /java
    /logging
      build.gradle
      mise.toml
      /src/main/java/com/dronefleet/lib/logging # <--- HINT: Full package path
        LogWrapper.java
    /config
      build.gradle
      mise.toml
      /src/main/java/com/dronefleet/lib/config
        ConfigLoader.java

  /python
    /logging
      pyproject.toml
      mise.toml
      /src/dronefleet_logging # <--- HINT: Allows `from dronefleet_logging import logger`
        __init__.py
        wrapper.py
    /config
      pyproject.toml
      mise.toml
      /src/dronefleet_config # <--- HINT: Allows `from dronefleet_config import loader`
        __init__.py
        loader.py

  /ts
    /logging
      package.json # <--- HINT: Name this "@dronefleet/logging" in package.json
      tsconfig.json
      mise.toml
      /src
        index.ts
    /config
      package.json # <--- HINT: Name this "@dronefleet/config"
      tsconfig.json
      mise.toml
      /src
        index.ts


# ROOT ORCHESTRATION
build.gradle          # Shared Java plugins/versions
settings.gradle       # HINT: include 'services:state_manager', 'shared:java', 'libs:java:logging'

pyproject.toml        # Workspace: ["services/*", "shared/python", "libs/python/*"]
uv.lock               # Single lockfile for all Python projects

package.json          # Workspace: ["services/*", "shared/ts", "libs/ts/*"]
pnpm-workspace.yaml
tsconfig.base.json    # HINT: Base TS config extended by all sub-projects

.env #define user current working env -> ENVIRONMENT=...
mise.toml # Manages Java 21, Node 20, Python 3.12 versions globally, and glue repo using monorepo layout `https://mise.jdx.dev/tasks/monorepo.html`
README.md
LICENSE #mit

.gitignore

## Configuration Files for Code Style
biome.json
.editorconfig
...
