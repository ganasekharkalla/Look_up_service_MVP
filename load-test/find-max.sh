#!/usr/bin/env bash
# Steps the request rate up and prints one row per step, so you can see where the
# service stops keeping up (achieved rate flattens, errors or dropped iterations appear,
# p95 latency climbs).
#
# Usage:
#   load-test/find-max.sh                       # default steps
#   RATES="500 1000 2000" load-test/find-max.sh
#   FRESH_RATIO=0 load-test/find-max.sh         # cache-hit-only traffic
#   DURATION=30s K6=/path/to/k6 load-test/find-max.sh

set -euo pipefail
cd "$(dirname "$0")/.."

K6="${K6:-$HOME/.local/bin/k6}"
RATES="${RATES:-200 400 800 1200 1600 2000 3000 4000 6000}"
DURATION="${DURATION:-20s}"
FRESH_RATIO="${FRESH_RATIO:-0.4}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
OUT_DIR="$(mktemp -d)"

printf "Target: %s | step: %s | fresh ratio: %s\n\n" "$BASE_URL" "$DURATION" "$FRESH_RATIO"
printf "%-8s %-10s %-9s %-9s %-9s %-9s %-9s\n" "target" "achieved" "errors%" "dropped" "avg(ms)" "p95(ms)" "p99(ms)"

for rate in $RATES; do
  summary="$OUT_DIR/summary-$rate.json"
  RATE="$rate" DURATION="$DURATION" FRESH_RATIO="$FRESH_RATIO" BASE_URL="$BASE_URL" \
    "$K6" run --quiet --summary-export "$summary" load-test/max-throughput.js >/dev/null 2>&1 || true

  jq -r --arg rate "$rate" '
    .metrics as $m |
    [ $rate,
      (($m.http_reqs.rate // 0) | floor | tostring),
      ((($m.http_req_failed.value // 0) * 100) | . * 100 | floor / 100 | tostring),
      (($m.dropped_iterations.count // 0) | tostring),
      (($m.http_req_duration.avg // 0) | floor | tostring),
      (($m.http_req_duration["p(95)"] // 0) | floor | tostring),
      (($m.http_req_duration["p(99)"] // 0) | floor | tostring)
    ] | @tsv' "$summary" |
    awk -F'\t' '{ printf "%-8s %-10s %-9s %-9s %-9s %-9s %-9s\n", $1, $2, $3, $4, $5, $6, $7 }'

  sleep 3   # let the app settle between steps
done

echo
echo "Raw k6 summaries: $OUT_DIR"
