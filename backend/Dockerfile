# Shared Shopping List backend image.
#
# The same image is used for the `backend` service (uvicorn) and the
# `backend-test` service (ruff + pytest), so the dev tooling is installed too.
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# Install dependencies + application first (better layer caching).
COPY pyproject.toml ./
COPY app ./app
COPY tests ./tests

RUN pip install --upgrade pip && pip install ".[dev]" && rm -rf /app/build

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
