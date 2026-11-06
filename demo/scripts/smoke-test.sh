#!/usr/bin/env bash
# Smoke test for the Docker image: starts a container, waits until it is healthy,
# then calls every API endpoint once. Used by CI; also works locally:
#   bash scripts/smoke-test.sh demo:latest [host-port] [expected-version]
set -euo pipefail

IMAGE="${1:?usage: $0 <image> [host-port] [expected-version]}"
PORT="${2:-8080}"
EXPECTED_VERSION="${3:-}" # the version CI baked into the image, e.g. sha-24405c0
BASE="http://localhost:${PORT}"
NAME="demo-smoke-test-$$"

cleanup() {
  local status=$?
  if [ "$status" -ne 0 ]; then
    echo "--- container logs ---"
    docker logs "$NAME" 2>&1 || true
  fi
  docker rm -f "$NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# Give the app its port in PORT, the way hosting platforms such as Render do
docker run -d --name "$NAME" -p "${PORT}:9090" -e PORT=9090 "$IMAGE" >/dev/null

echo "Waiting for the app to report UP..."
for _ in $(seq 1 30); do
  if curl -fsS "${BASE}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    break
  fi
  sleep 2
done
curl -fsS "${BASE}/actuator/health"
echo

echo "GET /api/health"
health=$(curl -fsS "${BASE}/api/health")
echo "$health"
echo "$health" | grep -q '"status":"UP"'
if [ -n "$EXPECTED_VERSION" ]; then
  echo "$health" | grep -q "\"version\":\"${EXPECTED_VERSION}\""
fi

echo "POST /api/todos"
created=$(curl -fsS -X POST "${BASE}/api/todos" -H 'Content-Type: application/json' -d '{"title":"smoke test"}')
echo "$created"
id=$(echo "$created" | sed -E 's/.*"id":([0-9]+).*/\1/')

echo "GET /api/todos/${id}"
curl -fsS "${BASE}/api/todos/${id}" | grep -q '"title":"smoke test"'

echo "GET /api/todos"
curl -fsS "${BASE}/api/todos" | grep -q '"smoke test"'

echo "DELETE /api/todos/${id}"
curl -fsS -X DELETE "${BASE}/api/todos/${id}"

status=$(curl -s -o /dev/null -w '%{http_code}' "${BASE}/api/todos/${id}")
if [ "$status" != "404" ]; then
  echo "Expected 404 after delete, got ${status}"
  exit 1
fi

echo "Smoke test passed"
