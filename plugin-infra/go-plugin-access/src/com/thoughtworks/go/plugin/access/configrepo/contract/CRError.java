package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRError {
    private String location;
    private String message;

    public CRError(String location, String message) {
        this.location = location;
        this.message = message;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
