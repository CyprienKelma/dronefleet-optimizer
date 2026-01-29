FROM python:3.11-slim

WORKDIR /app

# Install uv for fast dependency management
RUN pip install uv

# Copy project files
COPY pyproject.toml uv.lock ./

# Install dependencies using uv
RUN uv sync --frozen

# Copy source code
COPY src/shared ./src/shared
COPY src/optimizer ./src/optimizer
COPY src/ingestion ./src/ingestion

# Set Python path to include src
ENV PYTHONPATH=/app/src

# Entry point
CMD ["uv", "run", "python", "-m", "optimizer.main"]
