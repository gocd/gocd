package com.thoughtworks.go.plugin.configrepo;

public abstract class CRBase {
    private String location;

    public ErrorCollection getErrors()
    {
        ErrorCollection errors = new ErrorCollection();
        this.getErrors(errors);
        return errors;
    }

    public abstract void getErrors(ErrorCollection errors);

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
