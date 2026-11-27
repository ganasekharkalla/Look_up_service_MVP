// Fixed-rate k6 test used to find the maximum throughput of the Lookup Service.
//
// Unlike lookup-load.js there is no think time: k6 fires RATE requests per second
// regardless of how fast the app answers (open model, "constant-arrival-rate").
// If the app cannot keep up, k6 reports `dropped_iterations` and latency climbs.
//
// Usage (normally driven by load-test/find-max.sh):
//   RATE=1000 DURATION=20s k6 run load-test/max-throughput.js
//
// Env vars:
//   RATE         target requests per second           (default 500)
//   DURATION     how long to hold that rate           (default 20s)
//   FRESH_RATIO  share of requests using a new identity, i.e. provider call + DB write (default 0.4)
//   BASE_URL     app URL                              (default http://localhost:8080)
//   MAX_VUS      cap on VUs k6 may allocate           (default max(200, 2 x RATE))

import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = parseInt(__ENV.RATE || '500', 10);
const DURATION = __ENV.DURATION || '20s';
const FRESH_RATIO = parseFloat(__ENV.FRESH_RATIO || '0.4');

export const options = {
  scenarios: {
    max: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      // VUs needed is roughly rate x latency; leave plenty of headroom.
      preAllocatedVUs: Math.max(50, Math.ceil(RATE / 10)),
      maxVUs: parseInt(__ENV.MAX_VUS || String(Math.max(200, RATE * 2)), 10),
    },
  },
  // No thresholds here: we want the raw numbers at every rate, pass or fail.
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

const CACHED_PHONES = ['+14159929960', '+12025550123', '+442071838750', '+61291234567', '+919876543210'];
const CACHED_EMAILS = ['jane.doe@example.com', 'ops@example.org', 'support@example.net', 'sales@example.co'];
const PARAMS = { headers: { 'Content-Type': 'application/json' } };

function randomDigits(n) {
  let s = '';
  for (let i = 0; i < n; i++) s += Math.floor(Math.random() * 10);
  return s;
}
function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
  const fresh = Math.random() < FRESH_RATIO;
  const phone = Math.random() < 0.5;
  let url, body;

  if (phone) {
    url = `${BASE_URL}/api/v1/lookups/phone`;
    // Exchange digit must be 2-9 or libphonenumber returns 400.
    const number = fresh ? `+1415${2 + Math.floor(Math.random() * 8)}${randomDigits(6)}` : pick(CACHED_PHONES);
    body = JSON.stringify({ phoneNumber: number, countryCode: 'US' });
  } else {
    url = `${BASE_URL}/api/v1/lookups/email`;
    const email = fresh ? `max.${__VU}.${__ITER}.${randomDigits(6)}@example.com` : pick(CACHED_EMAILS);
    body = JSON.stringify({ email });
  }

  const res = http.post(url, body, PARAMS);
  check(res, { 'status 200': (r) => r.status === 200 });
}
