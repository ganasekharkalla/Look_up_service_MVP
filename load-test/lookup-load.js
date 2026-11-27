// k6 load test for the Lookup Service.
//
// Run:
//   k6 run load-test/lookup-load.js                       # default "load" profile
//   PROFILE=smoke k6 run load-test/lookup-load.js         # 1 VU sanity check
//   PROFILE=stress k6 run load-test/lookup-load.js        # push until it breaks
//   BASE_URL=http://localhost:8080 k6 run load-test/lookup-load.js
//
// Traffic mix:
//   - "cached" scenarios reuse a small pool of identities, so most requests hit
//     MongoDB (source=DATABASE) after the first call.
//   - "fresh" scenarios use a unique identity per request, so every request goes
//     to the mock provider (source=PROVIDER) and writes a new document.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PROFILE = __ENV.PROFILE || 'load';

const PROFILES = {
  smoke: { stages: [{ duration: '30s', target: 1 }] },
  load: {
    stages: [
      { duration: '30s', target: 20 },
      { duration: '2m', target: 20 },
      { duration: '30s', target: 0 },
    ],
  },
  stress: {
    stages: [
      { duration: '1m', target: 50 },
      { duration: '2m', target: 100 },
      { duration: '2m', target: 200 },
      { duration: '1m', target: 0 },
    ],
  },
};

const stages = (PROFILES[PROFILE] || PROFILES.load).stages;

export const options = {
  scenarios: {
    phone_cached: { executor: 'ramping-vus', exec: 'phoneCached', stages, gracefulRampDown: '10s' },
    email_cached: { executor: 'ramping-vus', exec: 'emailCached', stages, gracefulRampDown: '10s' },
    phone_fresh: { executor: 'ramping-vus', exec: 'phoneFresh', stages, gracefulRampDown: '10s' },
    email_fresh: { executor: 'ramping-vus', exec: 'emailFresh', stages, gracefulRampDown: '10s' },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{endpoint:phone}': ['p(95)<500'],
    'http_req_duration{endpoint:email}': ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

const providerSourced = new Rate('lookup_provider_sourced');
const dbSourced = new Rate('lookup_db_sourced');
const phoneLatency = new Trend('phone_lookup_ms', true);
const emailLatency = new Trend('email_lookup_ms', true);

const CACHED_PHONES = ['+14159929960', '+12025550123', '+442071838750', '+61291234567', '+919876543210'];
const CACHED_EMAILS = ['jane.doe@example.com', 'ops@example.org', 'support@example.net', 'sales@example.co'];

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

function randomDigits(n) {
  let s = '';
  for (let i = 0; i < n; i++) s += Math.floor(Math.random() * 10);
  return s;
}

function uniquePhone() {
  // NANP numbers: the exchange must start with 2-9, or libphonenumber rejects them (400).
  // Each call yields a new identity, so every request goes to the provider.
  const exchange = 2 + Math.floor(Math.random() * 8);
  return `+1415${exchange}${randomDigits(6)}`;
}

function uniqueEmail() {
  return `load.${__VU}.${__ITER}.${randomDigits(6)}@example.com`;
}

function postPhone(phoneNumber, tag) {
  const res = http.post(
    `${BASE_URL}/api/v1/lookups/phone`,
    JSON.stringify({ phoneNumber, countryCode: 'US' }),
    { ...JSON_HEADERS, tags: { endpoint: 'phone', kind: tag } },
  );
  phoneLatency.add(res.timings.duration);
  const ok = check(res, {
    'phone: status 200': (r) => r.status === 200,
  });
  if (ok) {
    const body = res.json();
    dbSourced.add(body.source === 'DATABASE');
    providerSourced.add(body.source === 'PROVIDER');
  }
  return res;
}

function postEmail(email, tag) {
  const res = http.post(
    `${BASE_URL}/api/v1/lookups/email`,
    JSON.stringify({ email }),
    { ...JSON_HEADERS, tags: { endpoint: 'email', kind: tag } },
  );
  emailLatency.add(res.timings.duration);
  const ok = check(res, {
    'email: status 200': (r) => r.status === 200,
  });
  if (ok) {
    const body = res.json();
    dbSourced.add(body.source === 'DATABASE');
    providerSourced.add(body.source === 'PROVIDER');
  }
  return res;
}

export function phoneCached() {
  postPhone(CACHED_PHONES[Math.floor(Math.random() * CACHED_PHONES.length)], 'cached');
  sleep(0.2);
}

export function emailCached() {
  postEmail(CACHED_EMAILS[Math.floor(Math.random() * CACHED_EMAILS.length)], 'cached');
  sleep(0.2);
}

export function phoneFresh() {
  postPhone(uniquePhone(), 'fresh');
  sleep(0.2);
}

export function emailFresh() {
  postEmail(uniqueEmail(), 'fresh');
  sleep(0.2);
}