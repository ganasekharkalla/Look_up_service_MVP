package com.twilio.lookup.config;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables {@code @CreatedDate} and {@code @LastModifiedDate} on the Mongo entities.
 * Audit timestamps come from the {@link Clock} bean so that all time comes from one source.
 */
@Configuration(proxyBeanMethods = false)
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class MongoConfig {

	@Bean
	public DateTimeProvider auditingDateTimeProvider(Clock clock) {
		return () -> Optional.of(clock.instant());
	}

}
