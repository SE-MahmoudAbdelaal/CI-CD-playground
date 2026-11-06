#!/usr/bin/env bash
# Releases an image to production on Back4app Containers (continuous delivery):
#   1. points the Dockerfile on the "deploy" branch at the image and pushes it
#   2. a person deploys that commit in Back4app (the free plan has no automatic deploys)
#   3. this script waits until the public URL serves the new version
#
#   IMAGE           the image to release, e.g. ghcr.io/owner/ci-cd-demo:sha-24405c0
#   APP_URL         the app's public URL on Back4app, e.g. https://cicddemo-abc123.back4.app
#   DEPLOY_DIR      a checkout of the deploy branch (default: deploy)
#   TIMEOUT_SECONDS how long to wait for someone to deploy it (default: 1800)
set -euo pipefail

: "${IMAGE:?}"
: "${APP_URL:?}"
deploy_dir="${DEPLOY_DIR:-deploy}"
timeout="${TIMEOUT_SECONDS:-1800}"
tag="${IMAGE##*:}"

summary() {
  if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then printf '%s\n' "$@" >>"$GITHUB_STEP_SUMMARY"; fi
}

# 1. Point the deploy branch at the new image
cd "$deploy_dir"
sed -i "s|^FROM .*|FROM ${IMAGE}|" Dockerfile
if git diff --quiet -- Dockerfile; then
  echo "The deploy branch already points at $IMAGE, so there is nothing to commit"
else
  git config user.name "github-actions[bot]"
  git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
  git commit --quiet -m "Release ${tag}" -- Dockerfile
  git push --quiet origin HEAD:deploy
  echo "Pushed $IMAGE to the deploy branch"
fi
cd - >/dev/null

echo "::notice title=Release ready for production::Open Back4app, app ci-cd-demo, and deploy the latest commit of the deploy branch. This job waits up to $((timeout / 60)) minutes and turns green once $APP_URL serves $tag."
summary "### Release ready: $tag" "" \
  "1. Open [Back4app](https://containers.back4app.com/) and the **ci-cd-demo** app." \
  "2. Deploy the latest commit of the \`deploy\` branch (\`Release ${tag}\`)." \
  "3. This job checks $APP_URL/api/health and turns green when it reports **$tag**." ""

# 2. Wait until production serves it. Images built by CI report their sha-<commit> tag as the
# version; for other tags (latest, master) the script can only check that the app is up.
expected=""
if [[ "$tag" == sha-* ]]; then
  expected="$tag"
fi
echo "Waiting for $APP_URL to serve ${expected:-a healthy version}..."
deadline=$((SECONDS + timeout))
while ((SECONDS < deadline)); do
  body=$(curl -fsS --max-time 30 "$APP_URL/api/health" 2>/dev/null || true)
  if grep -q '"status":"UP"' <<<"$body" && { [[ -z "$expected" ]] || grep -q "\"version\":\"$expected\"" <<<"$body"; }; then
    echo "Live: $body"
    summary "### Deployed to production" "" "**$IMAGE** is live on Back4app: $APP_URL/api/health"
    exit 0
  fi
  sleep 15
done

echo "::error::$APP_URL did not serve ${expected:-a healthy version} within $((timeout / 60)) minutes. Deploy the latest commit of the deploy branch in Back4app, then re-run this job to check it."
exit 1
