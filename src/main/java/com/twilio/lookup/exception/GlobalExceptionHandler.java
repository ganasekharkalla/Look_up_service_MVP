package com.twilio.lookup.exception;

import com.twilio.lookup.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps every failure to an {@link ApiError}: validation, unreadable body and {@link InvalidLookupException} give 400,
 * {@link TransientProviderException} 503, {@link PermanentProviderException} 502, anything else 500. Standard MVC
 * errors (404, 405 and so on) keep their own status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final Clock clock;

	public GlobalExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(InvalidLookupException.class)
	ResponseEntity<ApiError> handleInvalidLookup(InvalidLookupException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(TransientProviderException.class)
	ResponseEntity<ApiError> handleTransient(TransientProviderException ex, HttpServletRequest request) {
		log.warn("Provider unavailable: {}", ex.getMessage());
		return build(HttpStatus.SERVICE_UNAVAILABLE, "Lookup provider is temporarily unavailable", request.getRequestURI());
	}

	@ExceptionHandler(PermanentProviderException.class)
	ResponseEntity<ApiError> handlePermanent(PermanentProviderException ex, HttpServletRequest request) {
		log.error("Provider failed permanently: {}", ex.getMessage());
		return build(HttpStatus.BAD_GATEWAY, "Lookup provider returned an error", request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest request) {
		log.error("Unhandled error", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request.getRequestURI());
	}

	/** Re-shapes the standard Spring MVC errors (validation, unreadable body, 404, 405, ...) into {@link ApiError}. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		String message;
		if (ex instanceof MethodArgumentNotValidException invalid) {
			message = invalid.getBindingResult().getFieldErrors().stream()
					.map(e -> e.getField() + " " + e.getDefaultMessage())
					.collect(Collectors.joining("; "));
			if (message.isEmpty()) {
				message = "Validation failed";
			}
		}
		else if (ex instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null) {
			message = errorResponse.getBody().getDetail();
		}
		else {
			message = ex.getMessage();
		}
		String path = request instanceof ServletWebRequest servletRequest ? servletRequest.getRequest().getRequestURI() : "";
		ApiError error = new ApiError(clock.instant(), statusCode.value(), reason(statusCode), message, path);
		return new ResponseEntity<>(error, headers, statusCode);
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String message, String path) {
		return ResponseEntity.status(status)
				.body(new ApiError(clock.instant(), status.value(), status.getReasonPhrase(), message, path));
	}

	private static String reason(HttpStatusCode code) {
		HttpStatus status = HttpStatus.resolve(code.value());
		return status != null ? status.getReasonPhrase() : String.valueOf(code.value());
	}
}
