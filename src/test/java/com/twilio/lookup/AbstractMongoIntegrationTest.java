package com.twilio.lookup;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for tests that need a real MongoDB. Starts one {@code mongo:8.0} container for the whole test JVM
 * and makes the mock provider deterministic and fast.
 *
 * <p>The container is started once in a static initializer instead of with {@code @Container}. Spring caches the
 * application context across test classes, so a container restarted per class would leave the cached context
 * pointing at a stopped Mongo. Testcontainers' Ryuk container removes it when the JVM exits.
 */
@SpringBootTest
@TestPropertySource(properties = {
		"provider.mock.failure-rate=0.0",
		"provider.mock.permanent-failure-rate=0.0",
		"provider.mock.latency-ms=0",
		"provider.retry.initial-backoff-ms=5"
})
public abstract class AbstractMongoIntegrationTest {

	static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:8.0"));

	static {
		MONGO.start();
	}

	@DynamicPropertySource
	static void mongoProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
	}

}
