FROM python:3.11-slim

WORKDIR /app

# Build argument set during CI/CD builds (... --build-arg ENVIRONMENT=prod ...)
ARG ENVIRONMENT=local

# Set environment variable from build arg
# Allows global_config.py to determine which .env file to load
ENV ENVIRONMENT=${ENVIRONMENT}

# Exclude development dependencies from production builds
ENV UV_NO_DEV=1

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# Install dependencies using cache mounts and bind mounts
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    --mount=type=bind,source=services/path_optimizer/pyproject.toml,target=services/path_optimizer/pyproject.toml \
    --mount=type=bind,source=shared/python/pyproject.toml,target=shared/python/pyproject.toml \
    --mount=type=bind,source=libs/python/config/pyproject.toml,target=libs/python/config/pyproject.toml \
    --mount=type=bind,source=libs/python/logging/pyproject.toml,target=libs/python/logging/pyproject.toml \
    --mount=type=bind,source=libs/python/messaging/pyproject.toml,target=libs/python/messaging/pyproject.toml \
    uv sync --frozen --no-cache --no-install-project

ENV PATH="/app/.venv/bin:$PATH"

# Copy shared code and service code
COPY shared/python/src/dronefleet_shared /app/dronefleet_shared
COPY libs/python/config/src/dronefleet_config /app/dronefleet_config
COPY libs/python/logging/src/dronefleet_logging /app/dronefleet_logging
COPY libs/python/messaging/src/dronefleet_messaging /app/dronefleet_messaging
COPY services/path_optimizer/src/path_optimizer /app/path_optimizer

# Copy config files
COPY configs /app/configs

# Set PYTHONPATH to include /app where packages are located
ENV PYTHONPATH=/app

CMD ["python", "-m", "path_optimizer.main"]
