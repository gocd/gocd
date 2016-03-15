package com.thoughtworks.go.plugin.api.exceptions;

public class UnhandledRequestTypeException extends Exception {
    public UnhandledRequestTypeException(String requestType) {
        super("This is an invalid request type :" + requestType);
    }
}
