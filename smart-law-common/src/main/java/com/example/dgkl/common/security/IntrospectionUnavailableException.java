package com.example.dgkl.common.security;

public class IntrospectionUnavailableException extends RuntimeException {
    public IntrospectionUnavailableException() {
        super("OAuth2 token introspection is unavailable");
    }
}
