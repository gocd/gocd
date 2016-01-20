package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

public abstract class CRBase implements Locatable {
    // plugin can optionally assign location on any configuration element
    protected String location;

    //TODO rename to collectErrors
    public abstract void getErrors(ErrorCollection errors,String parentLocation);

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public ErrorCollection getErrors() // shorthand for tests
    {
        ErrorCollection errors = new ErrorCollection();
        getErrors(errors,"Unknown");
        return errors;
    }
}
