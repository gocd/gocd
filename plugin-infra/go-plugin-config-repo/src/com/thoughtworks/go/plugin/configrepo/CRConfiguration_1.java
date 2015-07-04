package com.thoughtworks.go.plugin.configrepo;

public class CRConfiguration_1 {
    private String key;
    private String value;
    private String encryptedValue;

    public CRConfiguration_1() {
    }
    public CRConfiguration_1(String key,String value,String encryptedValue){
        this.key = key;
        this.value = value;
        this.encryptedValue = encryptedValue;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
