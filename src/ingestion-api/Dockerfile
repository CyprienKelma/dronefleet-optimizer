FROM python:3.11-slim

WORKDIR /app

# build argument set during CI/CD builds (... --build-arg ENVIRONMENT=prod ...)
ARG ENVIRONMENT=local

# set environment variable from build arg
# allow global_config.py to determine which .env file to load
ENV ENVIRONMENT=${ENVIRONMENT}

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# instal uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# keep cache of dependencies
COPY pyproject.toml uv.lock /app/

# uv sync to install dependencies
# --frozen ensures we use exact versions from uv.lock (reproducible builds)
RUN uv sync --frozen --no-cache --no-install-project

ENV PATH="/app/.venv/bin:$PATH"

# allow access to 'shared' modules with data formats
COPY src /app/src

# to get approp env files
COPY config /app/config

# to make imports work
ENV PYTHONPATH=/app:/app/src

EXPOSE 8000

CMD ["uvicorn", "src.ingestion-api.main:app", "--host", "0.0.0.0", "--port", "8000"]
