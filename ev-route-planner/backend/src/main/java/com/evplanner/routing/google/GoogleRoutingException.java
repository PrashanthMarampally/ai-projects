package com.evplanner.routing.google;

public class GoogleRoutingException extends RuntimeException {

    public GoogleRoutingException(String message) {
        super(message);
    }

    public GoogleRoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}
