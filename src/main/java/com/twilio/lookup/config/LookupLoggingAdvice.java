package com.twilio.lookup.config;

import com.twilio.lookup.dto.email.EmailLookupResponse;
import com.twilio.lookup.dto.phone.PhoneLookupResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Logs where each phone and email lookup result came from ({@code DATABASE} or {@code PROVIDER}). Does not change the body. */
@RestControllerAdvice
public class LookupLoggingAdvice implements ResponseBodyAdvice<Object> {

	private static final Logger log = LoggerFactory.getLogger(LookupLoggingAdvice.class);

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		Class<?> type = returnType.getParameterType();
		return PhoneLookupResponse.class.isAssignableFrom(type) || EmailLookupResponse.class.isAssignableFrom(type);
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		if (body instanceof PhoneLookupResponse phone) {
			log.info("Phone lookup served from {} (stale={})", phone.source(), phone.stale());
		}
		else if (body instanceof EmailLookupResponse email) {
			log.info("Email lookup served from {} (stale={})", email.source(), email.stale());
		}
		return body;
	}
}
