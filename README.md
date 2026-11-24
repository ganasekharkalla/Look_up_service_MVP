A failed Twilio call returns HTTP 502 and does not refresh `updatedAt`. There is no retry.
## Request path
`POST /api/v1/lookup/phone`
```json
{
  "phoneNumber": "9876543210",
  "defaultRegion": "IN"
}
```
`phoneNumber` is required. `defaultRegion` is optional and defaults to `IN`.
1. **Normalize.** Trim the value, drop a trailing extension (`ext 12`, `x22`), remove spaces, dashes, dots, and brackets, and turn a leading `00` into `+`.
2. **Choose the country.** A number that starts with `+` keeps the country code in the number. libphonenumber does not let `defaultRegion` override it. A number with no country code is parsed in `defaultRegion`.
3. **Validate locally.** `PhoneNumberUtil.isValidNumber` checks the numbering plan: length and assigned prefix. This is not a live line check. A failure is HTTP 400 and neither MongoDB nor Twilio is called.
4. **Cache read.** Look up `phoneE164`. A row is fresh when `updatedAt` is within `lookup.freshness-days` (90).
5. **Twilio.** `GET /v2/PhoneNumbers/{e164}?Fields=line_type_intelligence` with HTTP Basic auth. The mapped fields are upserted on `phoneE164`. `createdAt` is set only on the first insert.
Examples:
| Input | Region | Result |
|---|---|---|
| `9876543210` | `IN` or blank | `+919876543210` |
| `+91 98765 43210` | `US` | `+919876543210`. The `+91` wins. |
| `9876543210` | `US` | 400. No country code, so it is parsed as a US number and does not match the US plan. |
| `+1 415 555 2671` | `IN` | `+14155552671`. The `+1` wins. |
| `not-a-phone` | `IN` | 400. |
`valid` in the response is Twilio's flag for a number that already passed local validation. It means the number fits the provider's data for that range. It does not prove the line is active.
## API
### Success, HTTP 200
```json
{
  "phoneE164": "+919876543210",
  "valid": true,
  "countryCode": "IN",
  "callingCountryCode": "91",
  "nationalFormat": "098765 43210",
  "lineType": "mobile",
  "carrierName": "Airtel",
  "source": "TWILIO",
  "createdAt": "2026-11-24T08:30:19Z",
  "updatedAt": "2026-11-24T08:30:19Z"
}
```
### Errors
| HTTP | When |
|---|---|
| 400 | Blank number, unparseable number, or number that fails the numbering plan. |
| 502 | Twilio is unreachable, returns an empty body, or returns an HTTP error. |
```json
{
  "timestamp": "2026-11-24T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid phone number: 9876543210"
}
```
## Database schema
Database: `phone_lookup_db`  
Collection: `phone_lookups`  
Index: unique on `phoneE164` (`spring.data.mongodb.auto-index-creation=true`)
MongoDB does not enforce a JSON schema. The document shape is `PhoneLookupDocument`.
```json
{
  "_id": "ObjectId",
  "phoneE164": "+919876543210",
  "valid": true,
  "countryCode": "IN",
  "callingCountryCode": "91",
  "nationalFormat": "098765 43210",
  "lineType": "mobile",
  "carrierName": "Airtel",
  "rawTwilioPayload": {},
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```
| Field | Source | Notes |
|---|---|---|
| `phoneE164` | libphonenumber | Cache key. Unique. |
| `valid`, `countryCode`, `callingCountryCode`, `nationalFormat` | Twilio | Copied from the Lookup response. |
| `lineType`, `carrierName` | Twilio `line_type_intelligence` | Carrier is the provider value, not a value chosen by this service. |
| `rawTwilioPayload` | Twilio | Full JSON body. |
| `createdAt` | This service | Set on first insert only. |
| `updatedAt` | This service | Freshness clock. Refreshed only after a successful Twilio call. |
Rows older than 90 days stay in the collection. The next lookup refreshes them. There is no TTL index.
## Configuration
`src/main/resources/application.yml`
| Property | Default | Purpose |
|---|---|---|
| `server.port` | `8080` | HTTP port. |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/phone_lookup_db` | MongoDB. Pool size 100. |
| `twilio.base-url` | `https://lookups.twilio.com` | Override with `TWILIO_BASE_URL`. |
| `twilio.account-sid` | `test_sid` | Override with `TWILIO_ACCOUNT_SID`. |
| `twilio.auth-token` | `test_token` | Override with `TWILIO_AUTH_TOKEN`. |
| `lookup.freshness-days` | `90` | Cache window. |
Put real credentials in the environment, not in the YAML file:
```bash
export TWILIO_ACCOUNT_SID="your_sid"
export TWILIO_AUTH_TOKEN="your_token"
```
For local development without Twilio, point the client at the in-app mock. The mock is active on every profile except `prod` and returns a random carrier from a fixed list. That carrier is not a live operator lookup.
```bash
export TWILIO_BASE_URL="http://127.0.0.1:8080"
```
## Run
Requirements: JDK 17 and Maven. This machine also has a Java 21 runtime on `PATH`. Maven must use JDK 17 or compilation fails with `release version 17 not supported`.
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
docker compose up -d
mvn test
mvn spring-boot:run
```
`docker-compose.yml` starts MongoDB 7 only, published on port `27017`. The API runs on the host.
```bash
curl -s -X POST http://localhost:8080/api/v1/lookup/phone \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"9876543210","defaultRegion":"IN"}'
```
Call the same number again. The second response should have `"source": "DATABASE"`.
## Project layout
```text
src/main/java/com/example/phonelookup
  controller/PhoneLookupController.java    POST /api/v1/lookup/phone
  controller/MockTwilioController.java     local Twilio stand-in, profile !prod
  service/PhoneSanitizerService.java       clean, parse, E.164
  service/PhoneLookupService.java          cache-aside
  service/TwilioProxyService.java          one Twilio GET, no retry
  model/PhoneLookupDocument.java           phone_lookups
```
## Tests
```bash
mvn test
```
`PhoneLookupServiceTest` covers an invalid number, a fresh cache hit, a cache miss, and a stale row. The sanitizer and Twilio client are mocked in that test. MongoDB is not required for `mvn test`.
## MVP limits
- One number per request.
- One Twilio attempt. Transport and HTTP failures become 502.
- The local mock picks a carrier at random. Use it to exercise the cache, not to judge the real operator.
- Two concurrent cache misses for a new number can both call Twilio. The unique index still keeps a single document.
- `createdAt` stored in MongoDB is the original insert time. The HTTP body of a refresh currently reports `createdAt` as the refresh time.
