#!/bin/sh
set -eu

echo "Waiting for MinIO service..."
attempt=0
until mc alias set local "$MINIO_ENDPOINT" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 30 ]; then
    echo "MinIO is not reachable after 30 attempts."
    exit 1
  fi
  sleep 2
done

if mc ls local/"$MINIO_BUCKET" >/dev/null 2>&1; then
  echo "Bucket already exists: $MINIO_BUCKET"
else
  echo "Creating bucket: $MINIO_BUCKET"
  mc mb local/"$MINIO_BUCKET"
fi

echo "MinIO initialization completed."
