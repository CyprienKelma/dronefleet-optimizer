FROM python:3.11-slim

WORKDIR /app

# build argument set during CI/CD builds (... --build-arg ENVIRONMENT=prod ...)
ARG ENVIRONMENT=local

# set environment variable from build arg
# allow global_config.py to determine which .env file to load
ENV ENVIRONMENT=${ENVIRONMENT}

# Exclude development dependencies from production builds
ENV UV_NO_DEV=1

# install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# Install dependencies using cache mounts and bind mounts
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    uv sync --frozen --no-cache --no-install-project

ENV PATH="/app/.venv/bin:$PATH"

# Copy shared code and service code
COPY shared/python/src/dronefleet_shared /app/dronefleet_shared
COPY services/injestion/src/injestion /app/injestion

# Copy config files
COPY configs /app/configs

# Set PYTHONPATH to include /app where packages are located
ENV PYTHONPATH=/app

EXPOSE 8000

CMD ["uvicorn", "injestion.main:app", "--host", "0.0.0.0", "--port", "8000"]
