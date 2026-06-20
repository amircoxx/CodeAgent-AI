#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DEMO_EMAIL="${DEMO_EMAIL:-demo@example.com}"
DEMO_PASSWORD="${DEMO_PASSWORD:-password123}"

echo "CodeGuard AI demo seed"
echo "API: ${API_BASE_URL}"
echo

echo "1. Checking backend health..."
curl -fsS "${API_BASE_URL}/api/health" >/dev/null
echo "   Backend is healthy."

echo "2. Registering demo user: ${DEMO_EMAIL}"
REGISTER_RESPONSE="$(curl -sS -X POST "${API_BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Demo User\",\"email\":\"${DEMO_EMAIL}\",\"password\":\"${DEMO_PASSWORD}\"}")"

if echo "${REGISTER_RESPONSE}" | grep -q '"token"'; then
  TOKEN="$(node -e "const data=JSON.parse(process.argv[1]); console.log(data.token)" "${REGISTER_RESPONSE}")"
  echo "   Registered new demo user."
else
  echo "   Registration did not return a token. Trying login in case the demo user already exists."
  LOGIN_RESPONSE="$(curl -sS -X POST "${API_BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${DEMO_EMAIL}\",\"password\":\"${DEMO_PASSWORD}\"}")"
  TOKEN="$(node -e "const data=JSON.parse(process.argv[1]); if (!data.token) { console.error(process.argv[1]); process.exit(1); } console.log(data.token)" "${LOGIN_RESPONSE}")"
  echo "   Logged in as existing demo user."
fi

echo "3. Creating demo project..."
PROJECT_RESPONSE="$(curl -sS -X POST "${API_BASE_URL}/api/projects" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"name":"CodeGuard Demo Project","description":"Demo project for manual and GitHub PR reviews"}')"
PROJECT_ID="$(node -e "const data=JSON.parse(process.argv[1]); if (!data.id) { console.error(process.argv[1]); process.exit(1); } console.log(data.id)" "${PROJECT_RESPONSE}")"
echo "   Created project #${PROJECT_ID}."

echo "4. Creating manual review..."
MANUAL_RESPONSE="$(curl -sS -X POST "${API_BASE_URL}/api/reviews" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"projectId\":${PROJECT_ID},\"title\":\"Demo Login Validation Review\",\"language\":\"JavaScript\",\"code\":\"function login(email, password) { return fetch('/login', { method: 'POST', body: JSON.stringify({ email, password }) }) }\"}")"
MANUAL_ID="$(node -e "const data=JSON.parse(process.argv[1]); if (!data.id) { console.error(process.argv[1]); process.exit(1); } console.log(data.id)" "${MANUAL_RESPONSE}")"
echo "   Created manual review #${MANUAL_ID}."

echo "5. Creating GitHub PR review..."
GITHUB_RESPONSE="$(curl -sS -X POST "${API_BASE_URL}/api/github/pull-request-review" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"projectId\":${PROJECT_ID},\"pullRequestUrl\":\"https://github.com/octocat/Hello-World/pull/1\"}")"
GITHUB_ID="$(node -e "const data=JSON.parse(process.argv[1]); if (!data.id) { console.error(process.argv[1]); process.exit(1); } console.log(data.id)" "${GITHUB_RESPONSE}")"
echo "   Created GitHub PR review #${GITHUB_ID}."

echo
echo "Demo data ready."
echo "Email: ${DEMO_EMAIL}"
echo "Password: ${DEMO_PASSWORD}"
echo "Project ID: ${PROJECT_ID}"
echo "Manual Review ID: ${MANUAL_ID}"
echo "GitHub Review ID: ${GITHUB_ID}"
