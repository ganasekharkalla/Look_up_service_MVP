package com.example.phonelookup.exception;

public class UpstreamProviderException extends RuntimeException {

    public UpstreamProviderException(String message) {
        super(message);
    }

    public UpstreamProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
