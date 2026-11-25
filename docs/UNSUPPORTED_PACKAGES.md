# Unsupported data packages

This note covers what happens when a client asks for a Lookup v2 data package that Twilio cannot provide for a phone number. Examples are a `caller_name` request for a non-US number, or a `reassigned_number` request outside the US. It describes how this service handles that case. Items marked **unverified** have not been confirmed against a live Twilio response.

## What Twilio documents

- Basic formatting and validation are always returned. They do not depend on which packages were requested.
- Each data package is a separate object in the response (`line_type_intelligence`, `caller_name`, and so on). Each object has an `error_code` field.
- A package object with `error_code: null` holds data. Twilio bills a package only when its response has `error_code: null`.
- Unsupported-country failures use error code `60607` ("No provider found to satisfy the Lookup request"). Twilio's documentation says this is not billed. The search result we used confirmed this; the pricing page itself was not read in full.

Sources: [Lookup v2 API](https://www.twilio.com/docs/lookup/v2-api), [Identity Match billing rule](https://www.twilio.com/docs/lookup/v2-api/identity-match), [60607 error](https://www.twilio.com/docs/api/errors/60607).

## What we assume

We assume an unsupported package comes back inside a successful response, as a package object with `error_code` set and no data. The basic fields are still present. This is **unverified**. The alternative is that Twilio fails the whole request, in which case the provider client throws and the service behaves as for any provider failure: it returns a stale record if one exists, otherwise `502` or `503`.

## How this service handles it

Implemented in `PhoneProviderMapper` (`src/main/java/com/twilio/lookup/provider/phone/PhoneProviderMapper.java`):

- **Stored:** the package object is stored with its `error_code`, so the error is visible in the database.
- **Listed:** the package name is added to `packages` only when its `error_code` is null or blank. A package with an error is not listed, so `packages` contains only packages that returned data.
- **Mobile flag:** `mobile` is set from `line_type_intelligence.type` only, so an errored line-type package gives `mobile=false`.

Test: `PhoneProviderMapperTests.packageReturnedWithErrorCodeIsStoredButNotListed`.

## Effect on freshness

The record is stored like any other provider result, so an errored package stays in the database for the whole staleness window (`lookup.staleness-ttl-days`, 30 by default). A later request for the same number does not retry that package until the record goes stale. If the error was temporary, a client has to wait for the window to expire, or the record must be cleared, before the package is requested again.

## How to verify

Run one call on a test account:

```bash
curl -i -G "https://lookups.twilio.com/v2/PhoneNumbers/%2B447911123456" \
  --data-urlencode "Fields=caller_name" \
  -u "$TWILIO_API_KEY:$TWILIO_API_SECRET"
```

Check two things:

1. **Status code.** `200` supports the assumption above. An error status means the whole request fails, and the handling in this note needs to change.
2. **Body.** Look at `caller_name`. If it is an object with `error_code` set, the mapper is correct. If it is `null`, the mapper still stores nothing for that package, which gives the same `packages` result.

Record the result here once it is known.
