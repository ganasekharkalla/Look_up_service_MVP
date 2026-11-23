package com.twilio.lookup.repository;

import com.twilio.lookup.model.EmailLookup;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Access to {@code email_lookups}. The id is the normalized email address. */
public interface EmailLookupRepository extends MongoRepository<EmailLookup, String> {
}
