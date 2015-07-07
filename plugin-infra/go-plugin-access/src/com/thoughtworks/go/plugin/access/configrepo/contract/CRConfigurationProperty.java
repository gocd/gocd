package com.thoughtworks.go.plugin.access.configrepo.contract;

public class CRConfigurationProperty {
    private final String key;
    private final String value;
    private final String encryptedValue;

    public CRConfigurationProperty(String configKeyName, String value,String encryptedValue) {
        this.key = configKeyName;
        this.value = value;
        this.encryptedValue = encryptedValue;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }
}
