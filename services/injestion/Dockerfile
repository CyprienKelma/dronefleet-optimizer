FROM python:3.11-slim

WORKDIR /app

# build argument set during CI/CD builds (... --build-arg ENVIRONMENT=prod ...)
ARG ENVIRONMENT=local

# set environment variable from build arg
# allow global_config.py to determine which .env file to load
ENV ENVIRONMENT=${ENVIRONMENT}

# Exclude development dependencies from production builds
ENV UV_NO_DEV=1

# No system dependencies needed

# install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# Install dependencies using cache mounts and bind mounts for better performance
# This keeps dependency files out of the image layer and caches package downloads
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    uv sync --frozen --no-cache --no-install-project

ENV PATH="/app/.venv/bin:$PATH"

# allow access to 'shared' modules with data formats
COPY src /app/src

# to get approp env files
COPY config /app/config

# to make imports work
ENV PYTHONPATH=/app:/app/src

EXPOSE 8000

CMD ["uvicorn", "src.ingestion.main:app", "--host", "0.0.0.0", "--port", "8000"]
