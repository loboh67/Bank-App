package com.lobosoft.api.service;

public class EmailAlreadyRegisteredApiException extends RuntimeException {
    public EmailAlreadyRegisteredApiException(String message) {
        super(message);
    }
}