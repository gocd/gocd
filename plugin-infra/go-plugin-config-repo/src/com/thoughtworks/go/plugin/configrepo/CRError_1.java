package com.thoughtworks.go.plugin.configrepo;

public class CRError_1 {
    private String location;
    private String message;

    public CRError_1(){}
    public CRError_1(String message){
        this.message = message;
    }
    public CRError_1(String message,String location)
    {
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
