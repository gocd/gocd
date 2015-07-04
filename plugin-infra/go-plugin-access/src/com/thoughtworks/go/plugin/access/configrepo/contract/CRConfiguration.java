package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRConfiguration {
    private String key;
    private String value;
    private String encryptedValue;

    public CRConfiguration(String configKeyName) {
        this.key = configKeyName;
    }
    public CRConfiguration(String configKeyName, String value) {
        this.key = configKeyName;
        this.value = value;
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
