package com.thoughtworks.go.plugin.configrepo;

public abstract class CRBase {
    private String location;

    public abstract void getErrors(ErrorCollection errors);

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
