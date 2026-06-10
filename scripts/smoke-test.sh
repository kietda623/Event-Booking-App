#!/usr/bin/env sh
set -eu

failures=0

pass() {
  printf 'PASS %s\n' "$1"
}

fail() {
  printf 'FAIL %s\n' "$1"
  failures=$((failures + 1))
}

wait_for_health() {
  endpoint='backend health'
  attempts=60
  while [ "$attempts" -gt 0 ]; do
    body="$(curl -fsS http://localhost:8080/actuator/health 2>/dev/null || true)"
    if printf '%s' "$body" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
      pass "$endpoint"
      return 0
    fi
    attempts=$((attempts - 1))
    sleep 2
  done
  fail "$endpoint"
  return 1
}

expect_status() {
  name="$1"
  expected="$2"
  shift 2
  status="$(curl -sS -o /tmp/event-booking-smoke-response.txt -w '%{http_code}' "$@" || true)"
  if [ "$status" = "$expected" ]; then
    pass "$name"
  else
    fail "$name expected $expected got $status"
    cat /tmp/event-booking-smoke-response.txt 2>/dev/null || true
    printf '\n'
  fi
}

wait_for_health || true

expect_status 'GET /api/events' 200 http://localhost/api/events

email="smoke-$(date +%s)@example.com"
expect_status 'POST /api/auth/register' 200 \
  -H 'Content-Type: application/json' \
  -X POST \
  -d "{\"fullName\":\"Smoke Test\",\"email\":\"$email\",\"password\":\"password123\"}" \
  http://localhost/api/auth/register

if [ "$failures" -gt 0 ]; then
  printf 'FAIL smoke tests (%s failure(s))\n' "$failures"
  exit 1
fi

printf 'PASS smoke tests\n'
