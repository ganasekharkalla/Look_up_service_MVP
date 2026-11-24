package com.twilio.lookup.provider.email;

import com.twilio.lookup.provider.MockFailureSimulator;
import com.twilio.lookup.provider.ProviderQualifiers;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * SendGrid-shaped mock. The verdict comes from the address: a bad shape is {@code Invalid}, a role local part
 * (admin, info, ...) or a disposable host gives {@code Risky}, everything else is {@code Valid}.
 */
@Component
@Profile("!real-provider")
@Qualifier(ProviderQualifiers.DELEGATE)
public class MockEmailLookupProvider implements EmailLookupProvider {

	private static final Set<String> ROLE_LOCAL_PARTS = Set.of("admin", "info", "support", "sales", "contact", "postmaster", "noreply", "no-reply");
	private static final Set<String> DISPOSABLE_HOSTS = Set.of("mailinator.com", "10minutemail.com", "guerrillamail.com", "tempmail.com");

	private final MockFailureSimulator simulator;

	MockEmailLookupProvider(MockFailureSimulator simulator) {
		this.simulator = simulator;
	}

	@Override
	public ProviderEmailResponse lookup(String normalizedEmail) {
		simulator.simulate();
		int at = normalizedEmail.indexOf('@');
		String local = at > 0 ? normalizedEmail.substring(0, at) : normalizedEmail;
		String host = at > 0 ? normalizedEmail.substring(at + 1) : "";
		boolean syntax = at > 0 && at == normalizedEmail.lastIndexOf('@') && host.contains(".")
				&& !host.startsWith(".") && !host.endsWith(".");
		boolean disposable = DISPOSABLE_HOSTS.contains(host);
		boolean role = ROLE_LOCAL_PARTS.contains(local);

		String verdict;
		double score;
		if (!syntax) {
			verdict = "Invalid";
			score = 0.01;
		}
		else if (disposable || role) {
			verdict = "Risky";
			score = 0.45;
		}
		else {
			verdict = "Valid";
			score = 0.95;
		}
		return new ProviderEmailResponse(
				normalizedEmail,
				verdict,
				score,
				local,
				host,
				null,
				new ProviderEmailResponse.Checks(
						new ProviderEmailResponse.Domain(syntax, syntax, disposable),
						new ProviderEmailResponse.LocalPart(role),
						new ProviderEmailResponse.Additional(false, false)),
				"127.0.0.1",
				"MOCK");
	}
}
