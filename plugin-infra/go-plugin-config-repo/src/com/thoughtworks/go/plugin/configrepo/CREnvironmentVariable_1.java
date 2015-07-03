package com.thoughtworks.go.plugin.configrepo;

public class CREnvironmentVariable_1 {
    private String name;
    private String value;
    private String encryptedValue;

    public CREnvironmentVariable_1(){}
    public CREnvironmentVariable_1(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }
}
