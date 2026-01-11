FROM python:3.11-slim

WORKDIR /app

# build argument for environment,defaults to 'local' if not provided
# This is set during CI/CD builds (ex: --build-arg ENVIRONMENT=prod)
ARG ENVIRONMENT=local

# set environment variable from build arg
# this allows global_config.py to determine which .env file to load
ENV ENVIRONMENT=${ENVIRONMENT}

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# tnstall uv
RUN curl -LsSf https://astral.sh/uv/install.sh | sh
ENV PATH="/root/.cargo/bin:${PATH}"

# Copy dependency files uv.lock for layer caching
COPY pyproject.toml uv.lock /app/

# Install Python dependencies using uv
# uv sync : reads pyproject.toml and uv.lock automatically
# --system : installs into the system Python (no virtual env needed in Docker)
# --frozen : ensures we use exact versions from uv.lock (reproducible builds)
RUN uv sync --system --frozen --no-cache

# Copy the source code
# We copy the entire 'src' directory to allow access to 'shared' modules
COPY src /app/src

# Copy configuration files directory
# This contains the .env files (dev.env, prod.env, local.env) that pydantic_settings will load
COPY config /app/config

# Set PYTHONPATH to include /app/src so make import works
ENV PYTHONPATH=/app:/app/src

# Expose port
EXPOSE 8000

# Run the application
CMD ["uvicorn", "src.ingestion-api.main:app", "--host", "0.0.0.0", "--port", "8000"]

