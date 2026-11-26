# Lookup Service v1: Team Plan

This is the single source of truth for the four-member build. Every agent reads the **Common** section and then only its own member section. [AGENTS.md](../AGENTS.md) explains how each agent finds its member number. The design background (schema, Twilio and SendGrid details, errors) is in [DESIGN.md](DESIGN.md). The project overview is in the [root README](../README.md).

Goal: all four members build in parallel against the same contracts, and the code fits together with no design changes.

---

## Common

### Scope

- Synchronous Spring Boot 4.2.0, Java 17, Maven. Base package `com.twilio.lookup`, groupId `com.twilio`, artifactId `lookup-service`.
- Two endpoints: `POST /api/v1/lookups/phone` and `POST /api/v1/lookups/email`.
- Two Mongo collections: `phone_lookups` (`_id` = E.164 number) and `email_lookups` (`_id` = normalized email).
- Provider is mocked in v1. Services only ever see the provider interfaces; the real Twilio and SendGrid clients come later.
- Not in v1: cache, circuit breaker, batch and queues, security.
- No retry library. Retry is Member 4's job, and the pom carries a TODO saying so.

### Package and file ownership

| Area | Files | Owner |
| --- | --- | --- |
| Phone slice | `controller/PhoneLookupController`, `dto/PhoneLookupRequest`, `dto/PhoneLookupResponse`, `util/PhoneNormalizer`, `service/PhoneLookupService` | Member 1 |
| Email slice | `controller/EmailLookupController`, `dto/EmailLookupRequest`, `dto/EmailLookupResponse`, `util/EmailNormalizer`, `service/EmailLookupService` | Member 2 |
| Foundation | `pom.xml`, `src/main/resources/application.properties`, `LookupServiceApplication` (package root), `config/LookupProperties`, `config/MongoConfig`, `config/ClockConfig`, `dto/LookupStatus`, `dto/LookupSource`, `model/*`, `repository/*`, `service/StalenessPolicy`, test base class, `DESIGN.md` developer guide, springdoc | Member 3 |
| Providers and errors | `provider/*` (interfaces, response records, mappers, mocks), `config/ProviderProperties`, `exception/*`, `dto/ApiError`, `GlobalExceptionHandler`, `config/LookupLoggingAdvice`, actuator health | Member 4 |

Rule: if you need a change in a file you do not own, ask its owner or send them a small PR. Do not edit their file directly.

### Request flow (phone and email are identical)

1. Bean validation on the request body. Failure gives 400 with `ApiError`.
2. `normalizer.normalize(...)` returns `Optional<String>`. If empty, the service throws `InvalidLookupException`, which gives 400 with `ApiError`. Nothing is read or stored, and no provider call is made.
3. `repository.findById(normalized)`. If the record exists and `!stalenessPolicy.isStale(checkedAt)`, return it with `source=DATABASE`, `stale=false`.
4. Otherwise call `provider.lookup(normalized)`, then `mapper.toEntity(normalized, response, existingOrNull)`, then `repository.save(...)`. Return with `source=PROVIDER`, `stale=false`.
5. If the provider throws and an older record exists, return that record with `source=DATABASE`, `stale=true`. This follows the "Errors" table in DESIGN.md. If no record exists, rethrow. The handler maps the exception. Nothing is saved on failure.
6. Only provider-backed results are ever saved.

### Status mapping

- Phone: `valid=true` gives `VALID`, otherwise `INVALID`.
- Email verdict: `Valid` and `Risky` give `VALID`, `Invalid` gives `INVALID`. `verdict` and `score` carry the detail.
- Input rejected locally never returns a status. It returns 400, so no v1 response has a `source` for it. `LookupSource` has only `DATABASE` and `PROVIDER` in v1.
- A provider verdict of invalid is a real answer. It returns 200 with `status=INVALID` and is stored.

### Exact contracts

These signatures are fixed. Change them only with the whole group.

```java
// util (Members 1 and 2). Both are @Component.
Optional<String> PhoneNormalizer.normalize(String rawNumber, String countryCode); // empty = invalid
Optional<String> EmailNormalizer.normalize(String rawEmail);                      // empty = invalid

// service (Members 1 and 2)
PhoneLookupResponse PhoneLookupService.lookup(PhoneLookupRequest request);
// constructor, in this order:
// (PhoneNormalizer, PhoneLookupRepository, StalenessPolicy, PhoneLookupProvider, PhoneProviderMapper)
EmailLookupResponse EmailLookupService.lookup(EmailLookupRequest request);
// constructor, in this order:
// (EmailNormalizer, EmailLookupRepository, StalenessPolicy, EmailLookupProvider, EmailProviderMapper)

// controllers: constructor takes only the service, return the response DTO with HTTP 200

// policy (Member 3)
boolean StalenessPolicy.isStale(Instant checkedAt); // uses LookupProperties.stalenessTtlDays and the Clock bean

// provider (Member 4). Services inject only the interface.
ProviderPhoneResponse PhoneLookupProvider.lookup(String normalizedNumber);
ProviderEmailResponse EmailLookupProvider.lookup(String normalizedEmail);

// mappers (Member 4), in provider/
PhoneLookup PhoneProviderMapper.toEntity(String normalizedNumber, ProviderPhoneResponse response, PhoneLookup existingOrNull);
EmailLookup EmailProviderMapper.toEntity(String normalizedEmail, ProviderEmailResponse response, EmailLookup existingOrNull);
// update existingOrNull in place when non-null (keeps createdAt and version); set checkedAt from Clock

// exceptions (Member 4), all runtime, in exception/
ProviderException(String message) / (String message, Throwable cause)   // abstract base
TransientProviderException   // timeouts, 5xx, 429: retryable
PermanentProviderException   // 400, auth errors, unparseable responses: not retryable
InvalidLookupException(String message)  // local normalization failed: 400
```

### Response DTOs (fields)

- `PhoneLookupResponse`: `normalizedNumber`, `status`, `source`, `valid`, `callingCountryCode`, `countryCode`, `nationalFormat`, `validationErrors`, `lineType`, `carrier`, `mobile`, `packages`, `stale`, `checkedAt`.
- `EmailLookupResponse`: `normalizedEmail`, `status`, `source`, `verdict`, `score`, `local`, `host`, `suggestion`, `checks`, `stale`, `checkedAt`.
- `checks` is one record with six booleans: `hasValidAddressSyntax`, `hasMxOrARecord`, `suspectedDisposableAddress`, `suspectedRoleAddress`, `hasKnownBounces`, `hasSuspectedBounces`.
- `ApiError`: `timestamp`, `status`, `error`, `message`, `path`.
- Enums in `dto/`: `LookupStatus` (`VALID`, `INVALID`, `ERROR`) and `LookupSource` (`DATABASE`, `PROVIDER`).

### Entities (Member 3, in `model/`)

- `PhoneLookup` (`phone_lookups`): `@Id String id` (E.164), `channel`, `checkedAt`, `source` (`phone-proxy`), `packages` (`List<String>`), `valid`, `mobile`, `result` (`PhoneResult`: `callingCountryCode`, `countryCode`, `nationalFormat`, `validationErrors`, `lineTypeIntelligence` (`type`, `carrierName`, `mobileCountryCode`, `mobileNetworkCode`, `errorCode`)), `createdAt`, `updatedAt`, `@Version Long version`.
- `EmailLookup` (`email_lookups`): `@Id String id` (normalized email), `channel`, `checkedAt`, `source` (`email-proxy`), `verdict`, `score`, `result` (`EmailResult`: `local`, `host`, `suggestion`, `checks` (`domain`: `hasValidAddressSyntax`, `hasMxOrARecord`, `suspectedDisposableAddress`; `localPart`: `suspectedRoleAddress`; `additional`: `hasKnownBounces`, `hasSuspectedBounces`)), `createdAt`, `updatedAt`, `@Version Long version`.
- Lombok getters and setters. Services never touch `createdAt`, `updatedAt` or `version`.
- Mongo annotations stay in `model/`, `repository/` and `MongoConfig`. Nowhere else, so a later DynamoDB swap stays contained.
- `IdentityMatch` data and the email `ipAddress` are never stored.

### Mapping rules (Member 4 mappers)

- `packages` is `["line_type_intelligence"]` when `lineTypeIntelligence` is non-null, otherwise empty.
- `mobile` is true only when `lineTypeIntelligence.type` equals `mobile`.
- Response `lineType` and `carrier` come from `result.lineTypeIntelligence.type` and `carrierName`.

### Config (Member 3 declares, Member 4 uses)

- `LookupProperties` (prefix `lookup`): `stalenessTtlDays`.
- `ProviderProperties` (prefix `provider`): `mock.failureRate`, `mock.permanentFailureRate`, `mock.latencyMs`, `timeoutMs`, `retry.maxAttempts`, `retry.initialBackoffMs`, `retry.multiplier`.
- Property keys in `application.properties` match the prompt names (`provider.mock.failure-rate` and so on). Use kebab-case in the file, camelCase in Java.
- `ClockConfig` provides a `Clock.systemUTC()` bean. Everything that needs the time injects `Clock`.
- Tests set both mock failure rates to 0, latency to 0, and retry backoff to a few milliseconds through `@TestPropertySource` on the test base class. Do not add a test copy of `application.properties`, because it would shadow the main one.

### HTTP mapping (Member 4 owns the handler, applies to everyone)

| Situation | Status |
| --- | --- |
| Bean validation or malformed body | 400 |
| `InvalidLookupException` (local normalization failed) | 400 |
| Successful lookup, including provider verdict invalid | 200 |
| `TransientProviderException` after retries, and no stale record | 503 |
| `PermanentProviderException`, and no stale record | 502 |
| Anything else | 500 |

All errors use `ApiError`. A provider failure with a stale record returns 200 with `stale=true`, per step 5.

### Provider bean contract (Member 4)

- Exactly one `@Primary` bean per provider interface. It applies the timeout and retry.
- Mock beans are active unless the Spring profile `real-provider` is on. Real Twilio and SendGrid clients come later, behind the same interfaces.
- Services never retry and never reference a mock class.

### Starting point

- The repository started with only `LookupServiceApplication`, the context test, `pom.xml`, `application.properties` and the docs. There were no stubs and no `TODO(Member N)` markers. The single-request scaffold is now complete on this branch; this section is kept for history.
- Each member creates their own files from the ownership table, using the names, signatures and types in Common and in "Integration details".
- Until Member 3 and Member 4 merge their contracts, Members 1 and 2 can write against the signatures in Common, but their code compiles only after those merges.

### Merge order

1. Starting point (application class, properties, docs). Already in place.
2. Member 3 foundation (pom, config, entities, repositories, `StalenessPolicy`, test base class). Merges first.
3. Member 4 mock provider beans, mappers and exception handler, without retry.
4. Members 1 and 2 slices, with their service dependencies added.
5. Member 4 retry and timeout wrapper, then integration and end-to-end tests.

Members 1 and 2 can start before step 3, but they merge after it so the project compiles at every merge.

### Working rules

- Short-lived branches, small PRs, at least one reviewer each. Pairs: 1 reviews 2, 2 reviews 1, 3 reviews 4, 4 reviews 3.
- Provider access only through the interfaces.
- Agree naming and service structure between Members 1 and 2 early, so the two slices match.
- Mock failure rate is configurable and is set to zero in deterministic tests.
- Don't persist local validation failures or provider failures.

### Integration details (agreed names, so the slices compile together)

**Application and build**
- `LookupServiceApplication` lives in `com.twilio.lookup` (the root, not `config/`) so component scanning covers every package. It carries `@SpringBootApplication` and `@ConfigurationPropertiesScan`.
- Use `spring-boot-starter-data-mongodb` and `spring-boot-starter-data-mongodb-test`. `spring-boot-starter-mongodb` does not include Spring Data.
- Lombok (`@Getter @Setter @NoArgsConstructor`) is used on entities and their nested classes. Member 3 adds the dependency.
- There is no cache in v1. Flow: validate, normalize, database, provider.

**Entity types** (Member 3, in `model/`; the mappers and services depend on these names)
- `PhoneLookup` holds `PhoneResult result`. `PhoneResult` holds `PhoneResult.LineTypeIntelligence lineTypeIntelligence`.
- `EmailLookup` holds `EmailResult result`. `EmailResult` holds `EmailResult.Checks checks`, which holds `Domain domain`, `LocalPart localPart` and `Additional additional`, all nested in `EmailResult`.
- Types: instants are `Instant`, `score` is `double`, flags are primitive `boolean`, `packages` and `validationErrors` are `List<String>`.
- Stored field names are the Java camelCase names. The snake_case in DESIGN.md documents is illustrative.

**Provider records** (Member 4, in `provider/`; they mirror the DESIGN.md JSON in camelCase)
- `ProviderPhoneResponse` nests `LineTypeIntelligence(type, carrierName, mobileCountryCode, mobileNetworkCode, errorCode)`.
- `ProviderEmailResponse` nests `Checks(Domain domain, LocalPart localPart, Additional additional)`. `Domain`, `LocalPart` and `Additional` carry the booleans named in the DESIGN.md table.
- `score` is `double`, `valid` is `boolean`, `validationErrors` is `List<String>`.
- The mocks build these in Java. snake_case mapping is the job of the real clients later.

**Response DTO types**
- Same types as the entities. `status` is `LookupStatus`, `source` is `LookupSource`, `checkedAt` is `Instant`.
- `EmailLookupResponse.Checks` is a nested record with the six booleans, flattened from the entity's domain, localPart and additional.
- Service APIs use camelCase JSON, the Boot default.

**Mapper rules** (Member 4)
- Phone entity: `channel=phone`, `source=phone-proxy`, `valid`, and `result.*` copied from the response. `url` is never stored.
- Email entity: `channel=email`, `source=email-proxy`, `verdict`, `score`, and `result.local`, `host`, `suggestion` and `checks` copied. Provider `ipAddress` and `source` are never stored.
- Both: `checkedAt` comes from `Clock`. Update `existingOrNull` in place. Services never touch `createdAt`, `updatedAt` or `version`.

**Logging** (Member 4)
- `LookupLoggingAdvice` is a `ResponseBodyAdvice` that logs `source` for the phone and email responses. No other member calls it or depends on its signature.

**Combining**
- Everyone keeps the project compiling at every merge.
- Before merging, run `./mvnw clean verify` with Docker running.
- Raise any change to a signature, type or name in this section with the whole group.

---

## Member 1: Phone slice

**Owns:** `PhoneLookupController`, `PhoneLookupRequest`, `PhoneLookupResponse`, `PhoneNormalizer`, `PhoneLookupService`, and the phone tests.

**Tasks**
- `PhoneLookupController`: `POST /api/v1/lookups/phone`, `@Validated`, `@Valid @RequestBody`, returns `PhoneLookupResponse`.
- `PhoneLookupRequest`: `@NotBlank phoneNumber`, optional `countryCode` with a two-letter ISO `@Pattern`.
- `PhoneNormalizer`: libphonenumber (`9.0.44`). Parses with the region from `countryCode` (default: none, so a national number without a country code is invalid). Returns E.164 when `isValidNumber` is true, otherwise empty. Blank input is empty.
- `PhoneLookupService`: implements steps 2 to 5 of the request flow for phone numbers.
- Unit tests: the normalizer (valid national and E.164 numbers, `+141599299600` invalid, blank, bad country code) and the service with mocked repository, provider, mapper and normalizer.
- End-to-end test: extends the Mongo base class, uses the mock provider with rates at zero, covers DB hit, provider call and save, invalid input returning 400, and stale fallback.

**Consumes:** `PhoneLookupRepository`, `StalenessPolicy`, `PhoneLookupProvider`, `PhoneProviderMapper`, `ProviderPhoneResponse`, `LookupStatus`, `LookupSource`, `InvalidLookupException`.

**Unblocks:** the phone endpoint for end-to-end tests.

**Definition of done:** controller and service tests pass, the phone endpoint returns the documented 200 and 400 responses, and no file outside your list changed.

---

## Member 2: Email slice

**Owns:** `EmailLookupController`, `EmailLookupRequest`, `EmailLookupResponse`, `EmailNormalizer`, `EmailLookupService`, and the email tests.

**Tasks**
- `EmailLookupController`: `POST /api/v1/lookups/email`, same pattern as phone.
- `EmailLookupRequest`: `@NotBlank @Email email`.
- `EmailNormalizer`: trims the value, lowercases it, and rejects input without exactly one `@`, with a blank local part or domain, or whose domain has no top-level domain of at least 2 characters (the part after the last dot). `EmailLookupRequest` enforces the same TLD rule with `@Pattern`, and `email.html` mirrors it. Returns empty when rejected.
- `EmailLookupService`: same flow as phone, with `EmailLookupRepository`, `EmailLookupProvider` and `EmailProviderMapper`.
- Unit tests: the normalizer (case and whitespace, missing `@`, two `@`, blank) and the service.
- End-to-end test: mirrors the phone test for email.
- Agree naming and service structure with Member 1 before writing code.

**Consumes:** the same types as Member 1, with email in place of phone.

**Definition of done:** same as Member 1, for email.

---

## Member 3: Foundation and persistence

**Owns:** `pom.xml`, `application.properties`, `LookupServiceApplication`, `LookupProperties`, `MongoConfig`, `ClockConfig`, enums, `model/*`, `repository/*`, `StalenessPolicy`, the test base class, the Mongo integration tests, springdoc, and the DESIGN.md developer guide.

**Tasks**
- `pom.xml`: Boot 4.2.0 starters (`webmvc`, `validation`, `mongodb`, `actuator`, and their test starters), `libphonenumber` 9.0.44, optional Lombok, springdoc 3.2.1 (drop it if the context fails on Boot 4.2). Comment: retry and backoff will be added by the provider owner. Spring Framework 7 core retry may be enough.
- `application.properties` (single file, no profile files): app name `lookup-service`, `spring.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/lookup_local}`, actuator `health` and `info` endpoints, and the custom properties with default values. `spring.mongodb.uri` is the confirmed Boot 4.2 key.
- `MongoConfig`: `@EnableMongoAuditing`. `ClockConfig`: `Clock.systemUTC()`.
- `StalenessPolicy.isStale`: `checkedAt` plus `stalenessTtlDays`, compared against `Clock.instant()`.
- Entities and repositories exactly as listed under Common. `MongoRepository<..., String>`, no custom queries. No TTL index.
- Tests: `AbstractMongoIntegrationTest` (`@SpringBootTest`, `@TestPropertySource` with the zero mock rates and short retry backoff, static `@Container @ServiceConnection MongoDBContainer("mongo:8.0")`), a `contextLoads` test, repository integration tests (save, find, `@Version` bump, audit fields set).
- Springdoc config, and the developer guide in `DESIGN.md` (how to run, how to test with Docker, package overview, ownership table). Leave the design content of `DESIGN.md` as it is.

**Merges first.** Others depend on your entities, repositories and properties classes.

**Definition of done:** `./mvnw clean verify` passes with Docker running, the context starts against local Mongo with the default properties, and repository tests pass.

---

## Member 4: Providers, resilience and errors

**Owns:** `provider/*`, `ProviderProperties`, `exception/*`, `ApiError`, `GlobalExceptionHandler`, `LookupLoggingAdvice`, actuator health, and the retry tests.

**Tasks**
- Provider response records: `ProviderPhoneResponse` (`callingCountryCode`, `countryCode`, `phoneNumber`, `nationalFormat`, `valid`, `validationErrors`, `lineTypeIntelligence`, `url`) and `ProviderEmailResponse` (`email`, `verdict`, `score`, `local`, `host`, `suggestion`, `checks`, `ipAddress`, `source`), both mirroring the DESIGN.md JSON.
- Mock providers: `MockPhoneLookupProvider` and `MockEmailLookupProvider`. Twilio-shaped and SendGrid-shaped responses, configurable failure rate, latency and permanent-failure rate, all zero in tests. Deterministic when the rates are zero.
- `PhoneProviderMapper` and `EmailProviderMapper`, per the rules in Common.
- Strict timeout on every provider call (`provider.timeout-ms`).
- Retry with exponential backoff and jitter. Capped by `provider.retry.max-attempts`, counting the first call. Retries only `TransientProviderException`. Expose as the `@Primary` provider bean.
- Exceptions with Javadoc, and `InvalidLookupException`.
- `GlobalExceptionHandler` with the HTTP mapping above. `ApiError` as the body.
- `LookupLoggingAdvice`: logs the `source` of each lookup response.
- Actuator health for the provider (optional, low priority).
- Tests: retry recovers, retry exhausts, permanent failure does not retry, timeout fires, handler maps each exception to its status.
- Provider mocks and mappers can start before the foundation merges, using the entity names in "Integration details".

**Definition of done:** retry tests pass, handler tests pass, mock providers return Twilio- and SendGrid-shaped data, and the 400, 502, 503 and 500 mappings are covered.

---

## Shared tail (everyone)

- Cross-review: 1 reviews 2, 2 reviews 1, 3 reviews 4, 4 reviews 3.
- Phase 3 end-to-end tests and cross-review are shared work. Whoever finishes first takes testing gaps or v2 notes (cache and circuit breaker placement). No circuit breaker in v1.
