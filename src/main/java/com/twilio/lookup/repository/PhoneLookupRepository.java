package com.twilio.lookup.repository;

import com.twilio.lookup.model.PhoneLookup;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Access to {@code phone_lookups}. The id is the E.164 number. */
public interface PhoneLookupRepository extends MongoRepository<PhoneLookup, String> {
}
