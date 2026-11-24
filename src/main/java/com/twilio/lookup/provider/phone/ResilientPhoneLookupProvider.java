package com.twilio.lookup.provider.phone;

import com.twilio.lookup.provider.ProviderQualifiers;
import com.twilio.lookup.provider.ResilientCall;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** The {@code @Primary} phone provider: adds timeout and retry around the raw provider. */
@Component
@Primary
public class ResilientPhoneLookupProvider implements PhoneLookupProvider {

	private final PhoneLookupProvider delegate;
	private final ResilientCall resilientCall;

	public ResilientPhoneLookupProvider(@Qualifier(ProviderQualifiers.DELEGATE) PhoneLookupProvider delegate,
			ResilientCall resilientCall) {
		this.delegate = delegate;
		this.resilientCall = resilientCall;
	}

	@Override
	public ProviderPhoneResponse lookup(String normalizedNumber) {
		return resilientCall.execute(() -> delegate.lookup(normalizedNumber));
	}
}
