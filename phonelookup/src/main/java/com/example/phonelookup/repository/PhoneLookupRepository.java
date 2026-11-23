package com.example.phonelookup.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.phonelookup.model.PhoneLookupDocument;

public interface PhoneLookupRepository extends MongoRepository<PhoneLookupDocument, String> {

    Optional<PhoneLookupDocument> findByPhoneE164(String phoneE164);
}
