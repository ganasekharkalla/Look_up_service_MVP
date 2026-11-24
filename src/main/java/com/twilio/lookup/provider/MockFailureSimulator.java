package com.twilio.lookup.provider;

import com.twilio.lookup.config.ProviderProperties;
import com.twilio.lookup.exception.PermanentProviderException;
import com.twilio.lookup.exception.TransientProviderException;
import java.util.Random;
import org.springframework.stereotype.Component;

/** Applies the configured latency and random failures for the mock providers. Deterministic when all rates are zero. */
@Component
public class MockFailureSimulator {

	private final ProviderProperties.Mock settings;
	private final Random random = new Random();

	public MockFailureSimulator(ProviderProperties properties) {
		this.settings = properties.mock();
	}

	/** Sleeps for the configured latency, then maybe throws a permanent or a transient failure. */
	public void simulate() {
		if (settings.latencyMs() > 0) {
			try {
				Thread.sleep(settings.latencyMs());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new TransientProviderException("Mock provider interrupted", ex);
			}
		}
		double draw = random.nextDouble();
		if (draw < settings.permanentFailureRate()) {
			throw new PermanentProviderException("Mock provider permanent failure");
		}
		if (draw < settings.permanentFailureRate() + settings.failureRate()) {
			throw new TransientProviderException("Mock provider transient failure");
		}
	}
}
