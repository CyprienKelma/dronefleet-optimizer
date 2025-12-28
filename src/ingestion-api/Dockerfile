FROM python:3.11-slim

WORKDIR /app

# Install dependencies
# We use a simple requirements generation or copy pyproject.toml
# For now, let's install the known dependencies directly to keep it simple and robust
# assuming we might not have a requirements.txt generated yet.
RUN pip install --no-cache-dir fastapi[standard]>=0.112.0 uvicorn google-cloud-pubsub>=2.18.0 python-multipart pydantic

# Copy the source code
# We copy the entire 'src' directory to allow access to 'shared' modules
COPY src /app/src

# Set PYTHONPATH to include /app/src so that "from shared..." works
ENV PYTHONPATH=/app:/app/src

# Expose port
EXPOSE 8000

# Run the application
CMD ["uvicorn", "src.ingestion-api.main:app", "--host", "0.0.0.0", "--port", "8000"]

