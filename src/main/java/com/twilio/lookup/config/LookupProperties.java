package com.twilio.lookup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Lookup settings, bound from the {@code lookup.*} properties.
 *
 * @param stalenessTtlDays number of days a stored lookup stays fresh
 */
@ConfigurationProperties(prefix = "lookup")
public record LookupProperties(@DefaultValue("30") int stalenessTtlDays) {
}
