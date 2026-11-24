package com.twilio.lookup.dto;

import java.time.Instant;

/**
 * Body of every error response.
 *
 * @param timestamp when the error was produced
 * @param status    HTTP status code
 * @param error     HTTP reason phrase
 * @param message   human-readable detail
 * @param path      request path
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {
}
