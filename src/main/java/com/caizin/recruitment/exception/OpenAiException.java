package com.caizin.recruitment.exception;

public class OpenAiException extends IntegrationException {
    public OpenAiException(String message) {
        super(message);
    }

    public OpenAiException(String message, Throwable cause) {
        super(message, cause);
    }
}

