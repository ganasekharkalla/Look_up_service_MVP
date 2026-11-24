package com.example.phonelookup.exception;

public class UpstreamProviderException extends RuntimeException {

    private final int statusCode;
    private final String providerResponse;

    public UpstreamProviderException(String message, int statusCode, String providerResponse) {
        super(message);
        this.statusCode = statusCode;
        this.providerResponse = providerResponse;
    }

    public UpstreamProviderException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 502;
        this.providerResponse = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getProviderResponse() {
        return providerResponse;
    }
}
