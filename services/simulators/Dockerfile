FROM python:3.11-slim

WORKDIR /app

# build arg set on cicd builds (--build-arg ENVIRONMENT=dev)
ARG ENVIRONMENT=local

# global_config.py determine which .env file to load
ENV ENVIRONMENT=${ENVIRONMENT}

COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# download dependencies using cache mounts and bind mounts
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    --mount=type=bind,source=services/simulators/pyproject.toml,target=services/simulators/pyproject.toml \
    --mount=type=bind,source=shared/python/pyproject.toml,target=shared/python/pyproject.toml \
    --mount=type=bind,source=libs/python/config/pyproject.toml,target=libs/python/config/pyproject.toml \
    --mount=type=bind,source=libs/python/logging/pyproject.toml,target=libs/python/logging/pyproject.toml \
    --mount=type=bind,source=libs/python/messaging/pyproject.toml,target=libs/python/messaging/pyproject.toml \
    uv sync --frozen --no-cache --no-install-project

ENV PATH="/app/.venv/bin:$PATH"

# shared & service code
COPY shared/python/src/dronefleet_shared /app/dronefleet_shared
COPY libs/python/config/src/dronefleet_config /app/dronefleet_config
COPY libs/python/logging/src/dronefleet_logging /app/dronefleet_logging
COPY libs/python/messaging/src/dronefleet_messaging /app/dronefleet_messaging
COPY services/simulators/src/simulators /app/simulators

COPY configs /app/configs

# include /app (where packages are located)
ENV PYTHONPATH=/app

CMD ["python", "-m", "simulators.main"]
