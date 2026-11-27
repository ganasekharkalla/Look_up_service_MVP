#!/usr/bin/env bash
# Compares JVM configurations of the packaged jar under the same stepped load.
#
#   A  JDK 17, platform threads
#   B  JDK 21, platform threads
#   C  JDK 21, virtual threads (spring.threads.virtual.enabled=true)
#
# Each case: fresh app on its own port, throwaway database (dropped first), CPU pinning,
# a discarded warm-up, then load-test/find-max.sh. Results go to $OUT_DIR/<case>.txt.
#
# Usage:
#   load-test/compare-jvm.sh                      # all cases, both provider latencies
#   CASES="B C" LATENCIES="300" load-test/compare-jvm.sh
#
# Requirements: the jar built (./mvnw -DskipTests package), Docker with a `mongo` container,
# k6 at ~/.local/bin/k6, taskset, jq.

set -euo pipefail
cd "$(dirname "$0")/.."

JAR_SRC="${JAR:-target/lookup-service-0.1.0-MVP.jar}"
WORK="${WORK:-/tmp/lookup-bench}"
OUT_DIR="${OUT_DIR:-$WORK/results}"
PORT="${PORT:-8090}"
DB="${DB:-lookup_bench}"
MONGO_URI="${MONGO_URI:-mongodb://mongo:12345678@localhost:27017/${DB}?authSource=admin}"
APP_CPUS="${APP_CPUS:-4-9}"
K6_CPUS="${K6_CPUS:-0-3}"
CASES="${CASES:-A B C}"
LATENCIES="${LATENCIES:-50 300}"
RATES_50="${RATES_50:-2500 3000 3500 4000 4500 5000 6000 8000}"
RATES_300="${RATES_300:-1000 1500 2000 2500 3000 4000 6000}"
DURATION="${DURATION:-20s}"
WARMUP_RATE="${WARMUP_RATE:-2000}"
WARMUP_DURATION="${WARMUP_DURATION:-45s}"
MAX_VUS="${MAX_VUS:-4000}"

JDK17=/usr/lib/jvm/java-17-openjdk-amd64/bin/java
JDK21=/usr/lib/jvm/java-21-openjdk-amd64/bin/java

mkdir -p "$WORK" "$OUT_DIR"
cp "$JAR_SRC" "$WORK/app.jar"
APP_PID=""

stop_app() {
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
  APP_PID=""
}
trap stop_app EXIT

drop_db() {
  docker exec mongo mongosh -u mongo -p 12345678 --authenticationDatabase admin --quiet \
    --eval "db.getSiblingDB('${DB}').dropDatabase()" >/dev/null
}

run_case() {
  local name="$1" java="$2" latency="$3"; shift 3
  local extra=("$@") rates
  [[ "$latency" == "50" ]] && rates="$RATES_50" || rates="$RATES_300"
  local tag="${name}-${latency}ms"
  local log="$OUT_DIR/${tag}.app.log"
  echo "=== Case $name | latency ${latency}ms | $("$java" -version 2>&1 | head -n 1) | extra: ${extra[*]:-none}"

  drop_db
  MONGODB_URI="$MONGO_URI" taskset -c "$APP_CPUS" "$java" -jar "$WORK/app.jar" \
    --server.port="$PORT" --provider.mock.latency-ms="$latency" "${extra[@]}" >"$log" 2>&1 &
  APP_PID=$!

  for _ in $(seq 1 90); do
    if curl -fs "http://localhost:${PORT}/actuator/health" >/dev/null 2>&1; then break; fi
    sleep 1
  done
  curl -fs "http://localhost:${PORT}/actuator/health" >/dev/null || { echo "app failed to start, see $log"; exit 1; }

  echo "warm-up: ${WARMUP_RATE} req/s for ${WARMUP_DURATION} (discarded)"
  BASE_URL="http://localhost:${PORT}" RATES="$WARMUP_RATE" DURATION="$WARMUP_DURATION" MAX_VUS="$MAX_VUS" \
    taskset -c "$K6_CPUS" load-test/find-max.sh >/dev/null 2>&1 || true

  # Sample the app's CPU (all threads, 100% = one core) every 2s during the sweep.
  stdbuf -oL pidstat -p "$APP_PID" 2 >"$OUT_DIR/${tag}.pidstat" 2>/dev/null &
  local top_pid=$!

  BASE_URL="http://localhost:${PORT}" RATES="$rates" DURATION="$DURATION" MAX_VUS="$MAX_VUS" \
    taskset -c "$K6_CPUS" load-test/find-max.sh | tee "$OUT_DIR/${tag}.txt"

  kill "$top_pid" 2>/dev/null || true
  wait "$top_pid" 2>/dev/null || true
  awk -v pid="$APP_PID" '$1 != "Average:" && $3 == pid {print $8}' "$OUT_DIR/${tag}.pidstat" >"$OUT_DIR/${tag}.cpu"
  echo "peak app CPU sample: $(sort -n "$OUT_DIR/${tag}.cpu" | tail -n 1)% (6 cores = 600%)" | tee -a "$OUT_DIR/${tag}.txt"
  stop_app
  sleep 5
}

for latency in $LATENCIES; do
  for c in $CASES; do
    case "$c" in
      A) run_case A "$JDK17" "$latency" ;;
      B) run_case B "$JDK21" "$latency" ;;
      C) run_case C "$JDK21" "$latency" --spring.threads.virtual.enabled=true ;;
      *) echo "unknown case $c"; exit 1 ;;
    esac
  done
done

drop_db
echo "All done. Results in $OUT_DIR"
