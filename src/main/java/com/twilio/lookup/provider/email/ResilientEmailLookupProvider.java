package com.twilio.lookup.provider.email;

import com.twilio.lookup.provider.ProviderQualifiers;
import com.twilio.lookup.provider.ResilientCall;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** The {@code @Primary} email provider: adds timeout and retry around the raw provider. */
@Component
@Primary
public class ResilientEmailLookupProvider implements EmailLookupProvider {

	private final EmailLookupProvider delegate;
	private final ResilientCall resilientCall;

	public ResilientEmailLookupProvider(@Qualifier(ProviderQualifiers.DELEGATE) EmailLookupProvider delegate,
			ResilientCall resilientCall) {
		this.delegate = delegate;
		this.resilientCall = resilientCall;
	}

	@Override
	public ProviderEmailResponse lookup(String normalizedEmail) {
		return resilientCall.execute(() -> delegate.lookup(normalizedEmail));
	}
}
